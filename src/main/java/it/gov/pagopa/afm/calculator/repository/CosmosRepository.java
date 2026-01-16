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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static it.gov.pagopa.afm.calculator.service.UtilityComponent.isGlobal;

@Repository
public class CosmosRepository {
    private final ValidBundlesProvider validBundlesProvider;
    private final TouchpointRepository touchpointRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final UtilityComponent utilityComponent;
    private final String pspPosteId;
    private final List<String> pspBlacklist;

    @Autowired
    public CosmosRepository(
            ValidBundlesProvider validBundlesProvider,
            TouchpointRepository touchpointRepository,
            PaymentTypeRepository paymentTypeRepository,
            UtilityComponent utilityComponent,
            @Value("${pspPoste.id}") String pspPosteId,
            @Value("#{'${psp.blacklist}'.split(',')}") List<String> pspBlacklist) {
        this.validBundlesProvider = validBundlesProvider;
        this.touchpointRepository = touchpointRepository;
        this.paymentTypeRepository = paymentTypeRepository;
        this.utilityComponent = utilityComponent;
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
        boolean digitalStamp = bundle.getDigitalStamp() != null ? bundle.getDigitalStamp() : Boolean.FALSE;
        boolean digitalStampRestriction = bundle.getDigitalStampRestriction() != null
                ? bundle.getDigitalStampRestriction()
                : Boolean.FALSE;
        if (transferListSize == 0 && onlyMarcaBolloDigitale == 0) {
            // skip this filter
            return true;
        } else if (onlyMarcaBolloDigitale == transferListSize) {
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
    private List<ValidBundle> findValidBundlesMulti(PaymentOptionMulti paymentOptionMulti, Boolean allCcp) {
        List<ValidBundle> bundles = validBundlesProvider.getAllValidBundles();
        Stream<ValidBundle> stream = bundles.stream();

        stream = filterBundles(stream, paymentOptionMulti.getPaymentAmount(), paymentOptionMulti.getTouchpoint(),
                paymentOptionMulti.getPaymentMethod(), paymentOptionMulti.getIdPspList(),
                utilityComponent.getTransferCategoryList(paymentOptionMulti), allCcp);

        // add filter for cart bundle param
        if (paymentOptionMulti.getPaymentNotice().size() > 1) {
            stream = stream.filter(bundle -> Boolean.TRUE.equals(bundle.getCart()));
        }

        return stream.toList();
    }

    /**
     * Null value are ignored -> they are skipped when building the filters
     *
     * @param paymentOption Get the Body of the Request
     * @return the filtered bundles
     */
    private List<ValidBundle> findValidBundles(PaymentOption paymentOption, boolean allCcp) {
        List<ValidBundle> bundles = validBundlesProvider.getAllValidBundles();
        Stream<ValidBundle> stream = bundles.stream();

        stream = filterBundles(stream, paymentOption.getPaymentAmount(), paymentOption.getTouchpoint(),
                paymentOption.getPaymentMethod(), paymentOption.getIdPspList(),
                utilityComponent.getTransferCategoryList(paymentOption), allCcp);

        return stream.toList();
    }

    private Stream<ValidBundle> filterBundles(Stream<ValidBundle> stream, Long paymentAmount, String touchpoint,
                                              String paymentMethod, List<PspSearchCriteria> pspList, List<String> transferCategoryList, boolean allCcp) {
        stream = filterByPaymentAmount(stream, paymentAmount);
        stream = filterByTouchpoint(stream, touchpoint);
        stream = filterByPaymentMethod(stream, paymentMethod);
        stream = filterByPsp(stream, pspList);
        stream = filterByTransferCategory(stream, transferCategoryList);
        stream = filterByPoste(stream, allCcp);
        stream = filterByBlacklist(stream);
        return stream;
    }

    private Stream<ValidBundle> filterByPaymentAmount(Stream<ValidBundle> stream, Long paymentAmount) {
        return stream.filter(bundle -> (bundle.getMinPaymentAmount() == null
                || bundle.getMinPaymentAmount() < paymentAmount)
                && (bundle.getMaxPaymentAmount() == null
                || bundle.getMaxPaymentAmount() >= paymentAmount));
    }

    private Stream<ValidBundle> filterByTouchpoint(Stream<ValidBundle> stream, String touchpointName) {
        if (touchpointName != null && !touchpointName.equalsIgnoreCase("any")) {
            Optional<Touchpoint> touchpoint = touchpointRepository.findByName(touchpointName);
            if (touchpoint.isEmpty()) {
                throw new AppException(
                        HttpStatus.NOT_FOUND,
                        "Touchpoint not found",
                        "Cannot find touchpoint with name: '" + touchpointName + "'");
            }
            return stream.filter(bundle -> bundle.getTouchpoint() != null
                    && (bundle.getTouchpoint().equalsIgnoreCase(touchpoint.get().getName())
                    || bundle.getTouchpoint().equalsIgnoreCase("ANY")));
        }
        return stream;
    }

    private Stream<ValidBundle> filterByPaymentMethod(Stream<ValidBundle> stream, String paymentMethodName) {
        if (paymentMethodName != null && !paymentMethodName.equalsIgnoreCase("any")) {
            Optional<PaymentType> paymentType = paymentTypeRepository.findByName(paymentMethodName);
            if (paymentType.isEmpty()) {
                throw new AppException(
                        HttpStatus.NOT_FOUND,
                        "PaymentType not found",
                        "Cannot find payment type with name: '" + paymentMethodName + "'");
            }
            return stream.filter(bundle -> bundle.getPaymentType() == null
                    || bundle.getPaymentType().equalsIgnoreCase(paymentType.get().getName()));
        }
        return stream;
    }

    private Stream<ValidBundle> filterByPsp(Stream<ValidBundle> stream, List<PspSearchCriteria> pspList) {
        if (pspList != null && !pspList.isEmpty()) {
            return stream
                    .filter(bundle -> pspList.stream()
                            .anyMatch(criteria -> bundle.getIdPsp().equalsIgnoreCase(criteria.getIdPsp())
                                    && (StringUtils.isEmpty(criteria.getIdChannel())
                                    || bundle.getIdChannel().equalsIgnoreCase(criteria.getIdChannel()))
                                    && (StringUtils.isEmpty(criteria.getIdBrokerPsp())
                                    || bundle.getIdBrokerPsp().equalsIgnoreCase(criteria.getIdBrokerPsp()))));
        }
        return stream;
    }

    private Stream<ValidBundle> filterByTransferCategory(Stream<ValidBundle> stream, List<String> categoryList) {
        if (categoryList != null) {
            return stream.filter(bundle -> bundle.getTransferCategoryList() == null
                    || !Collections.disjoint(bundle.getTransferCategoryList(), categoryList));
        }
        return stream;
    }

    private Stream<ValidBundle> filterByPoste(Stream<ValidBundle> stream, boolean allCcp) {
        if (Boolean.FALSE.equals(allCcp)) {
            return stream.filter(bundle -> !bundle.getIdPsp().equalsIgnoreCase(pspPosteId));
        }
        return stream;
    }

    private Stream<ValidBundle> filterByBlacklist(Stream<ValidBundle> stream) {
        if (pspBlacklist != null && !pspBlacklist.isEmpty()) {
            return stream.filter(bundle -> !pspBlacklist.contains(bundle.getIdPsp()));
        }
        return stream;
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
        var onlyMarcaBolloDigitale = transferList.stream()
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
        var onlyMarcaBolloDigitale = paymentOption.getTransferList().stream()
                .filter(Objects::nonNull)
                .filter(elem -> Boolean.TRUE.equals(elem.getDigitalStamp()))
                .count();
        var transferListSize = paymentOption.getTransferList().size();

        return StreamSupport.stream(validBundles.spliterator(), true)
                .filter(bundle -> digitalStampFilter(transferListSize, onlyMarcaBolloDigitale, bundle))
                // Gets the GLOBAL bundles and PRIVATE|PUBLIC bundles of the CI
                .filter(bundle -> globalAndRelatedFilter(paymentOption, bundle))
                .toList();
    }

}
