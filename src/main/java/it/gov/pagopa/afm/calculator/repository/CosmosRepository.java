package it.gov.pagopa.afm.calculator.repository;

import it.gov.pagopa.afm.calculator.entity.CiBundle;
import it.gov.pagopa.afm.calculator.entity.PaymentType;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.PspSearchCriteria;
import it.gov.pagopa.afm.calculator.model.TransferListItem;
import it.gov.pagopa.afm.calculator.service.UtilityComponent;
import it.gov.pagopa.afm.calculator.service.ValidBundleCacheService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static it.gov.pagopa.afm.calculator.service.UtilityComponent.isGlobal;

@Repository
public class CosmosRepository {
    private final TouchpointRepository touchpointRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final UtilityComponent utilityComponent;
    private final ValidBundleCacheService validBundleCacheService;
    private final String pspPosteId;
    private final List<String> pspBlacklist;

    @Autowired
    public CosmosRepository(
            TouchpointRepository touchpointRepository,
            PaymentTypeRepository paymentTypeRepository,
            UtilityComponent utilityComponent,
            ValidBundleCacheService validBundleCacheService,
            @Value("${pspPoste.id}") String pspPosteId,
            @Value("#{'${psp.blacklist}'.split(',')}") List<String> pspBlacklist
    ) {
        this.touchpointRepository = touchpointRepository;
        this.paymentTypeRepository = paymentTypeRepository;
        this.utilityComponent = utilityComponent;
        this.validBundleCacheService = validBundleCacheService;
        this.pspPosteId = pspPosteId;
        this.pspBlacklist = pspBlacklist;
    }

    /**
     * @param ciFiscalCode fiscal code of the CI
     * @param bundle       a valid bundle
     * @return a list of CI-Bundle filtered by fiscal Code
     */
    private static List<CiBundle> filterByCI(String ciFiscalCode, ValidBundle bundle) {
        return bundle.getCiBundleList() != null
                ? bundle.getCiBundleList().stream()
                .filter(ciBundle -> ciFiscalCode.equals(ciBundle.getCiFiscalCode()))
                .toList()
                : null;
    }

    /**
     * @param transferListSize       the number of transfer elements in the request
     * @param onlyMarcaBolloDigitale number of how many paymentOptions in the request has
     *                               marcaBolloDigitale equals to True
     * @param bundle                 a valid bundle to filter
     * @return True if the valid bundle meets the criteria.
     */
    protected static boolean digitalStampFilter(
            long transferListSize, long onlyMarcaBolloDigitale, ValidBundle bundle) {
        boolean digitalStamp = Boolean.TRUE.equals(bundle.getDigitalStamp());
        boolean digitalStampRestriction = Boolean.TRUE.equals(bundle.getDigitalStampRestriction());
        if(transferListSize == 0 && onlyMarcaBolloDigitale == 0){
            // skip this filter
            return true;
        }
        else if (onlyMarcaBolloDigitale == transferListSize) {
            // if marcaBolloDigitale is present in all paymentOptions
            return digitalStamp;
        } else if (onlyMarcaBolloDigitale >= 1 && onlyMarcaBolloDigitale < transferListSize) {
            // if some paymentOptions have marcaBolloDigitale but others do not
            return digitalStamp && !digitalStampRestriction;
        } else {
            // skip this filter
            return true;
        }
    }

    /**
     * Gets the GLOBAL bundles and PRIVATE|PUBLIC bundles of the CI
     *
     * @param paymentOption the request
     * @param bundle        a valid bundle
     * @return True if the valid bundle meets the criteria.
     */
    private static boolean globalAndRelatedFilter(PaymentOption paymentOption, ValidBundle bundle) {
        // filter the ci-bundle list
        bundle.setCiBundleList(filterByCI(paymentOption.getPrimaryCreditorInstitution(), bundle));
        return isGlobal(bundle) || belongsCI(bundle);
    }

    /**
     * Gets the GLOBAL bundles and PRIVATE|PUBLIC bundles of the CI
     *
     * @param paymentOptionMulti the request
     * @param bundle             a valid bundle
     * @return True if the valid bundle meets the criteria.
     */
    private static boolean globalAndRelatedFilter(PaymentOptionMulti paymentOptionMulti, ValidBundle bundle) {
        // filter the ci-bundle list
        bundle.setCiBundleList(filteredCiBundles(paymentOptionMulti, bundle));
        return isGlobal(bundle) || belongsCI(bundle);
    }

    /**
     * Check if all the ci fiscal codes in the payment notice are present in the ciBundle
     *
     * @param paymentOptionMulti the request
     * @param bundle             a valid bundle
     * @return empty list if at least one element is not present, otherwise the full list
     */
    private static List<CiBundle> filteredCiBundles(PaymentOptionMulti paymentOptionMulti, ValidBundle bundle) {
        if (bundle.getCiBundleList() != null) {
            List<String> ciBundlesFiscalCodes = new ArrayList<>();
            bundle.getCiBundleList().forEach(ciBundle -> ciBundlesFiscalCodes.add(ciBundle.getCiFiscalCode()));
            boolean allCiBundlesPresent = paymentOptionMulti.getPaymentNotice().stream()
                    .anyMatch(paymentNoticeItem -> ciBundlesFiscalCodes.contains(paymentNoticeItem.getPrimaryCreditorInstitution()));
            return allCiBundlesPresent ? bundle.getCiBundleList() : new ArrayList<>();
        }
        return new ArrayList<>();
    }

    /**
     * @param bundle a valid bundle
     * @return True if the bundle is related with the CI
     */
    private static boolean belongsCI(ValidBundle bundle) {
        return bundle != null
                && bundle.getCiBundleList() != null
                && !bundle.getCiBundleList().isEmpty();
    }

    // ---- Public query methods ----

    public List<ValidBundle> findByPaymentOption(PaymentOption paymentOption, Boolean allCcp) {
        List<ValidBundle> filtered = findValidBundles(paymentOption, allCcp);
        return getFilteredBundles(paymentOption, filtered);
    }

    public List<ValidBundle> findByPaymentOption(PaymentOptionMulti paymentOption, Boolean allCcp) {
        List<ValidBundle> filtered = findValidBundlesMulti(paymentOption, allCcp);
        return getFilteredBundlesMulti(paymentOption, filtered);
    }

    // ---- In-memory filtering (replaces Cosmos Criteria queries) ----

    private List<ValidBundle> findValidBundles(PaymentOption paymentOption, Boolean allCcp) {
        List<ValidBundle> allBundles = validBundleCacheService.getAllValidBundles();

        // Resolve touchpoint name
        String resolvedTouchpoint = resolveTouchpoint(paymentOption.getTouchpoint());

        // Resolve payment type name
        String resolvedPaymentType = resolvePaymentType(paymentOption.getPaymentMethod());

        // Build transfer category set
        List<String> categoryList = utilityComponent.getTransferCategoryList(paymentOption);
        boolean hasCategoryFilter = categoryList != null;
        Set<String> categorySet = toCategorySet(categoryList);
        boolean hasNonEmptyCategories = !categorySet.isEmpty();

        // PSP list
        List<PspSearchCriteria> pspList = Optional.ofNullable(paymentOption.getIdPspList())
                .orElse(Collections.emptyList());

        // add filter for PSP blacklist
        Set<String> blacklistSet = toBlacklistSet();

        return allBundles.stream()
                .filter(b -> b.getMinPaymentAmount() < paymentOption.getPaymentAmount())
                .filter(b -> b.getMaxPaymentAmount() >= paymentOption.getPaymentAmount())
                .filter(b -> matchesTouchpoint(b, resolvedTouchpoint))
                .filter(b -> matchesPaymentType(b, resolvedPaymentType))
                .filter(b -> matchesPspList(b, pspList))
                .filter(b -> matchesTransferCategory(b, hasCategoryFilter, hasNonEmptyCategories, categorySet))
                .filter(b -> !Boolean.FALSE.equals(allCcp) || !pspPosteId.equals(b.getIdPsp()))
                .filter(b -> !blacklistSet.contains(b.getIdPsp()))
                .collect(Collectors.toList());
    }

    private List<ValidBundle> findValidBundlesMulti(PaymentOptionMulti paymentOption, Boolean allCcp) {
        List<ValidBundle> allBundles = validBundleCacheService.getAllValidBundles();

        String resolvedTouchpoint = resolveTouchpoint(paymentOption.getTouchpoint());
        String resolvedPaymentType = resolvePaymentType(paymentOption.getPaymentMethod());

        List<String> categoryList = utilityComponent.getTransferCategoryList(paymentOption);
        boolean hasCategoryFilter = categoryList != null;
        Set<String> categorySet = toCategorySet(categoryList);
        boolean hasNonEmptyCategories = !categorySet.isEmpty();

        List<PspSearchCriteria> pspList = Optional.ofNullable(paymentOption.getIdPspList())
                .orElse(Collections.emptyList());

        Set<String> blacklistSet = toBlacklistSet();
        boolean isCart = paymentOption.getPaymentNotice().size() > 1;

        return allBundles.stream()
                .filter(b -> b.getMinPaymentAmount() < paymentOption.getPaymentAmount())
                .filter(b -> b.getMaxPaymentAmount() >= paymentOption.getPaymentAmount())
                .filter(b -> matchesTouchpoint(b, resolvedTouchpoint))
                .filter(b -> matchesPaymentType(b, resolvedPaymentType))
                .filter(b -> matchesPspList(b, pspList))
                .filter(b -> matchesTransferCategory(b, hasCategoryFilter, hasNonEmptyCategories, categorySet))
                .filter(b -> !Boolean.FALSE.equals(allCcp) || !pspPosteId.equals(b.getIdPsp()))
                .filter(b -> !blacklistSet.contains(b.getIdPsp()))
                .filter(b -> !isCart || Boolean.TRUE.equals(b.getCart()))
                .collect(Collectors.toList());
    }

    // ---- Predicate helpers ----

    /** Resolve touchpoint name, throwing if not found. Returns null if "any" or null. */
    private String resolveTouchpoint(String touchpointName) {
        if (touchpointName != null && !touchpointName.equalsIgnoreCase("any")) {
            Optional<Touchpoint> touchpoint = touchpointRepository.findByName(touchpointName);
            if (touchpoint.isEmpty()) {
                throw new AppException(
                        HttpStatus.NOT_FOUND,
                        "Touchpoint not found",
                        "Cannot find touchpont with name: '" + touchpointName + "'");
            }
            return touchpoint.get().getName();
        }
        return null; // no filter
    }

    /** Resolve payment type name, throwing if not found. Returns null if "any" or null. */
    private String resolvePaymentType(String paymentMethodName) {
        if (paymentMethodName != null && !paymentMethodName.equalsIgnoreCase("any")) {
            Optional<PaymentType> paymentType = paymentTypeRepository.findByName(paymentMethodName);
            if (paymentType.isEmpty()) {
                throw new AppException(
                        HttpStatus.NOT_FOUND,
                        "PaymentType not found",
                        "Cannot find payment type with name: '" + paymentMethodName + "'");
            }
            return paymentType.get().getName();
        }
        return null;
    }

    /** touchpoint matches value OR bundle touchpoint is "ANY" */
    private boolean matchesTouchpoint(ValidBundle bundle, String resolvedTouchpoint) {
        if (resolvedTouchpoint == null) return true;
        String bt = bundle.getTouchpoint();
        return resolvedTouchpoint.equalsIgnoreCase(bt) || "ANY".equalsIgnoreCase(bt);
    }

    /** paymentType matches value OR bundle paymentType is null */
    private boolean matchesPaymentType(ValidBundle bundle, String resolvedPaymentType) {
        if (resolvedPaymentType == null) return true;
        return bundle.getPaymentType() == null || resolvedPaymentType.equalsIgnoreCase(bundle.getPaymentType());
    }

    /** PSP list filter: if list is empty, all pass. Otherwise bundle must match one of the criteria. */
    private boolean matchesPspList(ValidBundle bundle, List<PspSearchCriteria> pspList) {
        if (pspList.isEmpty()) {
            return true;
        }
        
        return pspList.stream()
                .anyMatch(psc -> 
                    Objects.equals(psc.getIdPsp(), bundle.getIdPsp()) 
                    && (StringUtils.isEmpty(psc.getIdChannel()) || Objects.equals(psc.getIdChannel(), bundle.getIdChannel()))
                    && (StringUtils.isEmpty(psc.getIdBrokerPsp()) || Objects.equals(psc.getIdBrokerPsp(), bundle.getIdBrokerPsp()))
                );
    }

    /** Transfer category filter: replicates the Cosmos ARRAY_CONTAINS OR IS_NULL logic. */
    private boolean matchesTransferCategory(ValidBundle bundle, boolean hasCategoryFilter,
                                            boolean hasNonEmptyCategories, Set<String> categorySet) {
        if (!hasCategoryFilter) {
            return true;
        }
        List<String> bundleCategories = bundle.getTransferCategoryList();
        boolean bundleHasNoCategory = bundleCategories == null;
        if (!hasNonEmptyCategories) {
            // request had no valid categories -> only bundles with null transferCategoryList
            return bundleHasNoCategory;
        }
        // bundle has no category -> passes (OR IS_NULL)
        if (bundleHasNoCategory) {
            return true;
        }
        
        // bundle has categories -> at least one must be in the request set
        return bundleCategories.stream().anyMatch(categorySet::contains);
    }

    private Set<String> toCategorySet(List<String> categoryList) {
        if (categoryList == null) {
            return Collections.emptySet();
        }
        return categoryList.stream()
            .filter(cat -> cat != null && !cat.isEmpty())
            .collect(Collectors.toSet());
    }

    private Set<String> toBlacklistSet() {
        if (CollectionUtils.isEmpty(pspBlacklist)) {
            return Collections.emptySet();
        }
        return pspBlacklist.stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    // ---- Post-query Java filters (globalAndRelatedFilter mutates ciBundleList) ----

    private List<ValidBundle> getFilteredBundles(PaymentOption paymentOption, List<ValidBundle> validBundles) {
        var onlyMarcaBolloDigitale =
            paymentOption.getTransferList().stream()
                    .filter(Objects::nonNull)
                    .filter(elem -> Boolean.TRUE.equals(elem.getDigitalStamp()))
                    .count();
        var transferListSize = paymentOption.getTransferList().size();

        return validBundles.stream()
                .filter(bundle -> digitalStampFilter(transferListSize, onlyMarcaBolloDigitale, bundle))
                .map(this::shallowCopy)
                .filter(bundle -> globalAndRelatedFilter(paymentOption, bundle))
                .collect(Collectors.toList());
    }

    private List<ValidBundle> getFilteredBundlesMulti(PaymentOptionMulti paymentOptionMulti, List<ValidBundle> validBundles) {
        List<TransferListItem> transferList = new ArrayList<>();
        paymentOptionMulti.getPaymentNotice().forEach(paymentNoticeItem -> {
            if (paymentNoticeItem.getTransferList() != null) {
                transferList.addAll(paymentNoticeItem.getTransferList());
            }
        });
        var onlyMarcaBolloDigitale =
                transferList.stream()
                        .filter(Objects::nonNull)
                        .filter(elem -> Boolean.TRUE.equals(elem.getDigitalStamp()))
                        .count();
        var transferListSize = transferList.size();

        return validBundles.stream()
                .filter(bundle -> digitalStampFilter(transferListSize, onlyMarcaBolloDigitale, bundle))
                .map(this::shallowCopy)
                .filter(bundle -> globalAndRelatedFilter(paymentOptionMulti, bundle))
                .collect(Collectors.toList());
    }

    /**
     * Shallow copy to prevent globalAndRelatedFilter from mutating cached bundle instances.
     * Without this, setCiBundleList() would corrupt the shared cache across concurrent requests.
     */
    private ValidBundle shallowCopy(ValidBundle original) {
        ValidBundle copy = original.toBuilder().build();
        if (original.getCiBundleList() != null) {
            copy.setCiBundleList(new ArrayList<>(original.getCiBundleList()));
        }
        return copy;
    }
}
