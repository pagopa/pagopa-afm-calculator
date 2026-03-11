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
import java.util.stream.StreamSupport;

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
                ? bundle.getCiBundleList().parallelStream()
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
        boolean digitalStamp =
                bundle.getDigitalStamp() != null ? bundle.getDigitalStamp() : Boolean.FALSE;
        boolean digitalStampRestriction =
                bundle.getDigitalStampRestriction() != null
                        ? bundle.getDigitalStampRestriction()
                        : Boolean.FALSE;
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

    public List<ValidBundle> findByPaymentOption(PaymentOption paymentOption, boolean allCcp) {
        Iterable<ValidBundle> validBundles = findValidBundles(paymentOption, allCcp);
        return getFilteredBundles(paymentOption, validBundles);
    }

    public List<ValidBundle> findByPaymentOption(PaymentOptionMulti paymentOption, Boolean allCcp) {
        Iterable<ValidBundle> validBundles = findValidBundlesMulti(paymentOption, allCcp);
        return getFilteredBundlesMulti(paymentOption, validBundles);
    }

    /**
     * Null value are ignored -> they are skipped when building the filters
     *
     * @param paymentOptionMulti Get the Body of the Request
     * @return the filtered bundles
     */
    private Iterable<ValidBundle> findValidBundlesMulti(PaymentOptionMulti paymentOptionMulti, Boolean allCcp) {
        // Get all valid bundles from cache via service
        List<ValidBundle> allBundles = validBundleCacheService.getAllValidBundles();

        // Validate touchpoint if provided
        String touchpointName = null;
        if (paymentOptionMulti.getTouchpoint() != null
                && !paymentOptionMulti.getTouchpoint().equalsIgnoreCase("any")) {
            Optional<Touchpoint> touchpoint = touchpointRepository.findByName(paymentOptionMulti.getTouchpoint());
            if (touchpoint.isEmpty()) {
                throw new AppException(
                        HttpStatus.NOT_FOUND,
                        "Touchpoint not found",
                        "Cannot find touchpoint with name: '" + paymentOptionMulti.getTouchpoint() + "'");
            }
            touchpointName = touchpoint.get().getName();
        }

        // Validate payment type if provided
        String paymentTypeName = null;
        if (paymentOptionMulti.getPaymentMethod() != null
                && !paymentOptionMulti.getPaymentMethod().equalsIgnoreCase("any")) {
            Optional<PaymentType> paymentType = paymentTypeRepository.findByName(paymentOptionMulti.getPaymentMethod());
            if (paymentType.isEmpty()) {
                throw new AppException(
                        HttpStatus.NOT_FOUND,
                        "PaymentType not found",
                        "Cannot find payment type with name: '" + paymentOptionMulti.getPaymentMethod() + "'");
            }
            paymentTypeName = paymentType.get().getName();
        }

        // Get PSP list and transfer categories
        List<PspSearchCriteria> pspList = Optional.ofNullable(paymentOptionMulti.getIdPspList())
                .orElse(Collections.emptyList());
        List<String> categoryListMulti = utilityComponent.getTransferCategoryList(paymentOptionMulti);

        // Apply filters in memory
        final String finalTouchpointName = touchpointName;
        final String finalPaymentTypeName = paymentTypeName;
        
        return allBundles.parallelStream()
                .filter(bundle -> filterByPaymentAmount(bundle, paymentOptionMulti.getPaymentAmount()))
                .filter(bundle -> filterByTouchpoint(bundle, finalTouchpointName))
                .filter(bundle -> filterByPaymentType(bundle, finalPaymentTypeName))
                .filter(bundle -> filterByPspList(bundle, pspList))
                .filter(bundle -> filterByTransferCategory(bundle, categoryListMulti))
                .filter(bundle -> filterByAllCcp(bundle, allCcp))
                .filter(this::filterByPspBlacklist)
                .filter(bundle -> filterByCart(bundle, paymentOptionMulti.getPaymentNotice().size() > 1))
                .collect(Collectors.toList());
    }

    /**
     * Null value are ignored -> they are skipped when building the filters
     *
     * @param paymentOption Get the Body of the Request
     * @return the filtered bundles
     */
    private Iterable<ValidBundle> findValidBundles(PaymentOption paymentOption, boolean allCcp) {
        // Get all valid bundles from cache via service
        List<ValidBundle> allBundles = validBundleCacheService.getAllValidBundles();

        // Validate touchpoint if provided
        String touchpointName = null;
        if (paymentOption.getTouchpoint() != null
                && !paymentOption.getTouchpoint().equalsIgnoreCase("any")) {
            Optional<Touchpoint> touchpoint = touchpointRepository.findByName(paymentOption.getTouchpoint());
            if (touchpoint.isEmpty()) {
                throw new AppException(
                        HttpStatus.NOT_FOUND,
                        "Touchpoint not found",
                        "Cannot find touchpont with name: '" + paymentOption.getTouchpoint() + "'");
            }
            touchpointName = touchpoint.get().getName();
        }

        // Validate payment type if provided
        String paymentTypeName = null;
        if (paymentOption.getPaymentMethod() != null
                && !paymentOption.getPaymentMethod().equalsIgnoreCase("any")) {
            Optional<PaymentType> paymentType = paymentTypeRepository.findByName(paymentOption.getPaymentMethod());
            if (paymentType.isEmpty()) {
                throw new AppException(
                        HttpStatus.NOT_FOUND,
                        "PaymentType not found",
                        "Cannot find payment type with name: '" + paymentOption.getPaymentMethod() + "'");
            }
            paymentTypeName = paymentType.get().getName();
        }

        // Get PSP list and transfer categories
        List<PspSearchCriteria> pspList = Optional.ofNullable(paymentOption.getIdPspList())
                .orElse(Collections.emptyList());
        List<String> categoryList = utilityComponent.getTransferCategoryList(paymentOption);

        // Apply filters in memory
        final String finalTouchpointName = touchpointName;
        final String finalPaymentTypeName = paymentTypeName;
        
        return allBundles.parallelStream()
                .filter(bundle -> filterByPaymentAmount(bundle, paymentOption.getPaymentAmount()))
                .filter(bundle -> filterByTouchpoint(bundle, finalTouchpointName))
                .filter(bundle -> filterByPaymentType(bundle, finalPaymentTypeName))
                .filter(bundle -> filterByPspList(bundle, pspList))
                .filter(bundle -> filterByTransferCategory(bundle, categoryList))
                .filter(bundle -> filterByAllCcp(bundle, allCcp))
                .filter(this::filterByPspBlacklist)
                .toList();
    }

    /**
     * These filters are done with Java (not with cosmos query)
     *
     * @param paymentOptionMulti the request
     * @param validBundles       the valid bundles
     * @return the GLOBAL bundles and PRIVATE|PUBLIC bundles of the CI
     */
    private List<ValidBundle> getFilteredBundlesMulti(
            PaymentOptionMulti paymentOptionMulti, Iterable<ValidBundle> validBundles) {

        // marca da bollo digitale check
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

        return StreamSupport.stream(validBundles.spliterator(), true)
                .filter(bundle -> digitalStampFilter(transferListSize, onlyMarcaBolloDigitale, bundle))
                // Gets the GLOBAL bundles and PRIVATE|PUBLIC bundles of the CI
                .filter(bundle -> globalAndRelatedFilter(paymentOptionMulti, bundle))
                .toList();
    }

    /**
     * These filters are done with Java (not with cosmos query)
     *
     * @param paymentOption the request
     * @param validBundles  the valid bundles
     * @return the GLOBAL bundles and PRIVATE|PUBLIC bundles of the CI
     */
    private List<ValidBundle> getFilteredBundles(
            PaymentOption paymentOption, Iterable<ValidBundle> validBundles) {
        var onlyMarcaBolloDigitale =
                paymentOption.getTransferList().stream()
                        .filter(Objects::nonNull)
                        .filter(elem -> Boolean.TRUE.equals(elem.getDigitalStamp()))
                        .count();
        var transferListSize = paymentOption.getTransferList().size();

        return StreamSupport.stream(validBundles.spliterator(), true)
                .filter(bundle -> digitalStampFilter(transferListSize, onlyMarcaBolloDigitale, bundle))
                // Gets the GLOBAL bundles and PRIVATE|PUBLIC bundles of the CI
                .filter(bundle -> globalAndRelatedFilter(paymentOption, bundle))
                .collect(Collectors.toList());
    }

    /**
     * Filter bundle by payment amount range
     */
    private boolean filterByPaymentAmount(ValidBundle bundle, Long paymentAmount) {
        return bundle.getMinPaymentAmount() <= paymentAmount 
                && bundle.getMaxPaymentAmount() > paymentAmount;
    }

    /**
     * Filter bundle by touchpoint
     */
    private boolean filterByTouchpoint(ValidBundle bundle, String touchpointName) {
        if (touchpointName == null) {
            return true;
        }
        return bundle.getTouchpoint() == null 
                || "ANY".equalsIgnoreCase(bundle.getTouchpoint())
                || touchpointName.equalsIgnoreCase(bundle.getTouchpoint());
    }

    /**
     * Filter bundle by payment type
     */
    private boolean filterByPaymentType(ValidBundle bundle, String paymentTypeName) {
        if (paymentTypeName == null) {
            return true;
        }
        return bundle.getPaymentType() == null || paymentTypeName.equalsIgnoreCase(bundle.getPaymentType());
    }

    /**
     * Filter bundle by PSP list
     */
    private boolean filterByPspList(ValidBundle bundle, List<PspSearchCriteria> pspList) {
        if (pspList == null || pspList.isEmpty()) {
            return true;
        }
        return pspList.stream().anyMatch(pspSearch -> {
            boolean matchPsp = pspSearch.getIdPsp().equalsIgnoreCase(bundle.getIdPsp());
            boolean matchChannel = StringUtils.isEmpty(pspSearch.getIdChannel()) 
                    || pspSearch.getIdChannel().equalsIgnoreCase(bundle.getIdChannel());
            boolean matchBroker = StringUtils.isEmpty(pspSearch.getIdBrokerPsp()) 
                    || pspSearch.getIdBrokerPsp().equalsIgnoreCase(bundle.getIdBrokerPsp());
            return matchPsp && matchChannel && matchBroker;
        });
    }

    /**
     * Filter bundle by transfer category
     */
    private boolean filterByTransferCategory(ValidBundle bundle, List<String> categoryList) {
        if (categoryList == null) {
            return bundle.getTransferCategoryList() == null;
        }
        if (bundle.getTransferCategoryList() == null) {
            return true;
        }
        return categoryList.stream()
                .filter(Objects::nonNull)
                .filter(elem -> !elem.isEmpty())
                .anyMatch(category -> bundle.getTransferCategoryList().contains(category));
    }

    /**
     * Filter bundle by allCcp flag (Poste bundles)
     */
    private boolean filterByAllCcp(ValidBundle bundle, Boolean allCcp) {
        if (Boolean.FALSE.equals(allCcp)) {
            return !pspPosteId.equalsIgnoreCase(bundle.getIdPsp());
        }
        return true;
    }

    /**
     * Filter bundle by PSP blacklist
     */
    private boolean filterByPspBlacklist(ValidBundle bundle) {
        if (CollectionUtils.isEmpty(pspBlacklist)) {
            return true;
        }
        return !pspBlacklist.contains(bundle.getIdPsp());
    }

    /**
     * Filter bundle by cart parameter
     */
    private boolean filterByCart(ValidBundle bundle, boolean isCart) {
        if (isCart) {
            return Boolean.TRUE.equals(bundle.getCart());
        }
        return true;
    }

}
