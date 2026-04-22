package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.CiBundle;
import it.gov.pagopa.afm.calculator.entity.IssuerRangeEntity;
import it.gov.pagopa.afm.calculator.entity.PaymentType;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.model.PaymentNoticeItem;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.TransferCategoryRelation;
import it.gov.pagopa.afm.calculator.model.calculator.BundleOption;
import it.gov.pagopa.afm.calculator.model.calculator.Transfer;
import it.gov.pagopa.afm.calculator.model.calculatormulti.Fee;
import it.gov.pagopa.afm.calculator.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.repository.PaymentTypeRepository;
import it.gov.pagopa.afm.calculator.repository.TouchpointRepository;
import lombok.Setter;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.gov.pagopa.afm.calculator.exception.AppException;
import org.springframework.http.HttpStatus;

import static it.gov.pagopa.afm.calculator.service.UtilityComponent.inTransferList;
import static it.gov.pagopa.afm.calculator.service.UtilityComponent.isGlobal;

@Service
public class CalculatorService {

    private final String amountThreshold;
    private final UtilityComponent utilityComponent;
    private final IssuersService issuersService;
    private final String amexABI;
    private final String pspPosteId;
    private final List<String> pspBlacklist;
    private final ValidBundleCacheService validBundleCacheService;
    //private CosmosRepository cosmosRepository;
    private final TouchpointRepository touchpointRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    
    public CalculatorService(
            @Value("${payment.amount.threshold}") String amountThreshold,
            UtilityComponent utilityComponent,
            IssuersService issuersService,
            @Value("${pspAmex.abi:AMREX}") String amexABI,
            @Value("${pspPoste.id}") String pspPosteId,
            @Value("#{'${psp.blacklist}'.split(',')}") List<String> pspBlacklist,
            ValidBundleCacheService validBundleCacheService,
            TouchpointRepository touchpointRepository,
            PaymentTypeRepository paymentTypeRepository
    ) {
        this.amountThreshold = amountThreshold;
        this.utilityComponent = utilityComponent;
        this.issuersService = issuersService;
        this.amexABI = amexABI;
        this.pspPosteId = pspPosteId;
        this.pspBlacklist = pspBlacklist;
        this.validBundleCacheService = validBundleCacheService;
        this.touchpointRepository = touchpointRepository;
        this.paymentTypeRepository = paymentTypeRepository;
    }

    /**
     * sort by bundles' fee grouped by PSP
     *
     * @param transfers list of transfers to sort
     */
    private static void sortByFeePerPsp(List<Transfer> transfers) {
        transfers.sort(
                (t1, t2) -> {
                    int primarySort = t1.getIdPsp().compareTo(t2.getIdPsp());
                    if (primarySort == 0) {
                        // if two bundles are of the same PSP we'll sort by fees
                        return t1.getTaxPayerFee().compareTo(t2.getTaxPayerFee());
                    }
                    return 0; // fixed to 0 because we don't want to sort by PSP name.
                });
    }


    private static List<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> sortList(List<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> transfers, String orderType, boolean onUsFirst) {

        Optional<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> onUsBundle = Optional.of(transfers)
                .filter(ignored -> onUsFirst)
                .flatMap(t -> t.stream()
                        .filter(it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getOnUs)
                        .findFirst());

        LinkedList<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> orderedBundles = new LinkedList<>();
        switch (orderType != null ? orderType.toLowerCase() : "") {
            case "fee" -> groupByFee(transfers, onUsFirst).values().forEach(bundlesPerFee -> {
                bundlesPerFee.sort(
                        Comparator.comparing(it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getPspBusinessName,
                                Comparator.nullsLast(String::compareTo))
                );
                orderedBundles.addAll(bundlesPerFee);
            });

            case "feerandom" -> groupByFee(transfers, onUsFirst).values().forEach(bundlesPerFee -> {
                Collections.shuffle(bundlesPerFee);
                orderedBundles.addAll(bundlesPerFee);
            });
            case "pspname" -> {
                orderedBundles.addAll(applyOnUsFilter(transfers, onUsFirst));
                orderedBundles.sort(Comparator.comparing(
                    it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getPspBusinessName,
                    Comparator.nullsLast(String::compareTo)
                ));
            }
            default -> {
                orderedBundles.addAll(applyOnUsFilter(transfers, onUsFirst));
                Collections.shuffle(orderedBundles);
            }
        }

        onUsBundle.ifPresent(orderedBundles::addFirst);
        return orderedBundles.stream().toList();
    }

    private static List<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> applyOnUsFilter(
            List<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> transfers,
            boolean onUsFirst) {
        return transfers.stream().filter(element -> !onUsFirst || !element.getOnUs()).toList();
    }

    private static Map<Long, List<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer>> groupByFee(
        List<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> transfers,
        boolean onUsFirst
    ) {
        return transfers.stream()
            .filter(element -> !onUsFirst || !element.getOnUs())
            .collect(Collectors.groupingBy(
                it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getActualPayerFee,
                TreeMap::new,
                Collectors.toCollection(ArrayList::new)
            ));
    }

    /*

    public BundleOption calculate(@Valid PaymentOption paymentOption, int limit, boolean allCcp) {
        List<ValidBundle> filteredBundles = cosmosRepository.findByPaymentOption(paymentOption, allCcp);
        Collections.shuffle(filteredBundles, new Random());

        return BundleOption.builder()
                .belowThreshold(isBelowThreshold(paymentOption.getPaymentAmount()))
                // calculate the taxPayerFee
                .bundleOptions(calculateTaxPayerFee(paymentOption, limit, filteredBundles))
                .build();
    }
    
    public it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption calculateMulti(@Valid PaymentOptionMulti paymentOption, int limit, Boolean allCcp, boolean onUsFirst, String orderType) {
        List<ValidBundle> filteredBundles = cosmosRepository.findByPaymentOption(paymentOption, allCcp);

        return it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption.builder()
                .belowThreshold(isBelowThreshold(paymentOption.getPaymentAmount()))
                .bundleOptions(calculateTaxPayerFeeMulti(paymentOption, limit, filteredBundles, orderType, onUsFirst))
                .build();
    }
    
    */
    
    public BundleOption calculate(@Valid PaymentOption paymentOption, int limit, boolean allCcp) {
        // Apply the same business filters in memory on top of the cached valid bundles snapshot.
    	List<ValidBundle> filteredBundles = new ArrayList<>(filterValidBundlesInMemory(paymentOption, allCcp));
    	Collections.shuffle(filteredBundles, new Random());

        return BundleOption.builder()
                .belowThreshold(isBelowThreshold(paymentOption.getPaymentAmount()))
                .bundleOptions(calculateTaxPayerFee(paymentOption, limit, filteredBundles))
                .build();
    }
    
    public it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption calculateMulti(
            @Valid PaymentOptionMulti paymentOption,
            int limit,
            Boolean allCcp,
            boolean onUsFirst,
            String orderType
    ) {
        // Apply the same business filters in memory on top of the cached valid bundles snapshot.
        List<ValidBundle> filteredBundles = filterValidBundlesInMemory(paymentOption, Boolean.TRUE.equals(allCcp));

        return it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption.builder()
                .belowThreshold(isBelowThreshold(paymentOption.getPaymentAmount()))
                .bundleOptions(calculateTaxPayerFeeMulti(paymentOption, limit, filteredBundles, orderType, onUsFirst))
                .build();
    }
    
    public List<ValidBundle> getFilteredValidBundlesForPaymentMethods(
            @Valid PaymentOptionMulti paymentOption,
            boolean allCcp
    ) {
        // Reuse the same in-memory filtering logic already used by fee calculation endpoints.
        return filterValidBundlesInMemory(paymentOption, allCcp);
    }

    public it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption calculateForPaymentMethods(List<ValidBundle> filteredBundles, @Valid PaymentOptionMulti paymentOption, int limit, boolean onUsFirst, String orderType) {

      return it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption.builder()
          .belowThreshold(isBelowThreshold(paymentOption.getPaymentAmount()))
          .bundleOptions(calculateTaxPayerFeeMulti(paymentOption, limit, filteredBundles, orderType, onUsFirst))
          .build();
    }
    
    private List<Transfer> calculateTaxPayerFee(
            PaymentOption paymentOption, int limit, List<ValidBundle> bundles) {

        boolean primaryCiInTransferList =
                inTransferList(
                        paymentOption.getPrimaryCreditorInstitution(), paymentOption.getTransferList());

        List<Transfer> transfers = new ArrayList<>();

        // 1. Check if ONUS payment:
        // - ONUS payment = if the bundle ABI attribute matching the one extracted via BIN from the
        // issuers table
        // 2. The returned transfer list must contain:
        // - if ONUS payment = Only the bundles with the idChannel attribute ending in '_ONUS'
        // - if not ONUS payment = Only the bundles with the idChannel attribute NOT ending in '_ONUS'

        // 1.a: get issuers by BIN
        List<IssuerRangeEntity> issuers =
                checkValidityBin(paymentOption.getBin())
                        ? getIssuersByBIN(paymentOption.getBin())
                        : new ArrayList<>();

        // 1.b: all records extracted via a specific BIN must have the same ABI otherwise the exception
        // is raised
        // - the limit(2) operation is used to terminate as soon as two distinct ABI objects are found
        if (isUniqueAbi(issuers)) {
            // fix to solve the problem with overlapping ranges of some psps
            issuers.clear();
            //throw new AppException(AppError.ISSUERS_BIN_WITH_DIFFERENT_ABI_ERROR, paymentOption.getBin());
        }

        for (ValidBundle bundle : bundles) {

            // 1.c: check if onus payment type
            boolean isOnusPaymentType = isOnusPayment(issuers, bundle);

            // 2.a: if ONUS payment -> return the transfer list only for bundles with the idChannel
            // attribute ending in '_ONUS'
            if (isOnusPaymentType && isOnusBundle(bundle)) {
                transfers.addAll(this.getTransferList(paymentOption, primaryCiInTransferList, bundle));
            }
            // 2.b: if not ONUS payment -> return the transfer list only for bundles with the idChannel
            // attribute NOT ending in '_ONUS'
            if (!isOnusPaymentType && !isOnusBundle(bundle)) {
                transfers.addAll(this.getTransferList(paymentOption, primaryCiInTransferList, bundle));
            }
        }

        // if it is a payment on the AMEX circuit --> filter to return only AMEX_ONUS
        if (this.isAMEXAbi(issuers)) {
            Predicate<Transfer> abiPredicate = t -> amexABI.equalsIgnoreCase(t.getAbi());
            Predicate<Transfer> onusPredicate = t -> Boolean.TRUE.equals(t.getOnUs());
            transfers =
                    transfers.stream().filter(abiPredicate.and(onusPredicate)).collect(Collectors.toList());
        }

        // sort according onus and taxpayer fee
        Collections.sort(transfers);

        sortByFeePerPsp(transfers);

        return transfers.stream().limit(limit).toList();
    }

    private List<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> calculateTaxPayerFeeMulti(
            PaymentOptionMulti paymentOption, int limit, List<ValidBundle> bundles, String orderType, boolean onUsFirst) {

        Map<String, it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> pspTransfersMap = new HashMap<>();

        // 1. Check if ONUS payment:
        // - ONUS payment = if the bundle ABI attribute matching the one extracted via BIN from the
        // issuers table
        // 2. The returned transfer list must contain:
        // - if ONUS payment = Only the bundles with the idChannel attribute ending in '_ONUS'
        // - if not ONUS payment = Only the bundles with the idChannel attribute NOT ending in '_ONUS'

        // 1.a: get issuers by BIN
        List<IssuerRangeEntity> issuers =
                checkValidityBin(paymentOption.getBin())
                        ? getIssuersByBIN(paymentOption.getBin())
                        : new ArrayList<>();

        // 1.b: all records extracted via a specific BIN must have the same ABI otherwise the exception
        // is raised
        // - the limit(2) operation is used to terminate as soon as two distinct ABI objects are found
        if (isUniqueAbi(issuers)) {
            // fix to solve the problem with overlapping ranges of some psps
            issuers.clear();
            //throw new AppException(AppError.ISSUERS_BIN_WITH_DIFFERENT_ABI_ERROR, paymentOption.getBin());
        }

        for (ValidBundle bundle : bundles) {

            // 1.c: check if onus payment type
            boolean isOnusPaymentType = isOnusPayment(issuers, bundle);

            // 2.a: if ONUS payment -> return the transfer list only for bundles with the idChannel
            // attribute ending in '_ONUS'
            if (isOnusPaymentType && isOnusBundle(bundle)) {
                addToPspTransfersMap(pspTransfersMap, this.getTransferList(paymentOption, bundle));
            }
            // 2.b: if not ONUS payment -> return the transfer list only for bundles with the idChannel
            // attribute NOT ending in '_ONUS'
            if (!isOnusPaymentType && !isOnusBundle(bundle)) {
                addToPspTransfersMap(pspTransfersMap, this.getTransferList(paymentOption, bundle));
            }
        }

        // convert mapping of psp and transfer to transfer list
        List<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> transfers = new ArrayList<>(pspTransfersMap.values());

        // if it is a payment on the AMEX circuit --> filter to return only AMEX_ONUS
        if (this.isAMEXAbi(issuers)) {
            Predicate<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> abiPredicate = t -> amexABI.equalsIgnoreCase(t.getAbi());
            Predicate<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> onusPredicate = t -> Boolean.TRUE.equals(t.getOnUs());
            transfers =
                    transfers.stream().filter(abiPredicate.and(onusPredicate)).toList();
        }

        return sortList(transfers, orderType, onUsFirst).stream().limit(limit).toList();
    }

    private boolean isOnusBundle(ValidBundle bundle) {
        return Boolean.TRUE.equals(bundle.getOnUs());
    }

    private boolean isOnusPayment(List<IssuerRangeEntity> issuers, ValidBundle bundle) {
        return !CollectionUtils.isEmpty(issuers)
                && issuers.get(0).getAbi().equalsIgnoreCase(bundle.getAbi());
    }

    private boolean isUniqueAbi(List<IssuerRangeEntity> issuers) {
        return !CollectionUtils.isEmpty(issuers)
                && issuers.stream().map(IssuerRangeEntity::getAbi).distinct().limit(2).count() > 1;
    }

    private boolean isAMEXAbi(List<IssuerRangeEntity> issuers) {
        return !CollectionUtils.isEmpty(issuers) && issuers.get(0).getAbi().equalsIgnoreCase(amexABI);
    }

    private List<Transfer> getTransferList(
            PaymentOption paymentOption, boolean primaryCiInTransferList, ValidBundle bundle) {
        List<Transfer> transfers = new ArrayList<>();
        // if primaryCi is in transfer list we should evaluate the related incurred fee
        if (primaryCiInTransferList) {
            // add in transfers!
            analyzeTransferList(transfers, paymentOption, bundle);
        } else {
            Transfer transfer = createTransfer(bundle.getPaymentAmount(), 0, bundle, null, paymentOption);
            transfers.add(transfer);
        }
        return transfers;
    }

    private List<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> getTransferList(
            PaymentOptionMulti paymentOption, ValidBundle bundle) {
        List<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> transfers = new ArrayList<>();
        if (bundle.getCiBundleList().isEmpty()) {
            transfers.add(createTransfer(bundle, paymentOption, new ArrayList<>(), new ArrayList<>()));
            return transfers;
        }
        Map<String, List<Fee>> ciDiscountedFeesMap = new HashMap<>();
        for (PaymentNoticeItem paymentNoticeItem : paymentOption.getPaymentNotice()) {
            List<Fee> discountedFees = analyzeFee(paymentNoticeItem, bundle);
            if (!discountedFees.isEmpty()) {
                ciDiscountedFeesMap.put(paymentNoticeItem.getPrimaryCreditorInstitution(), discountedFees);
            } else {
                transfers.add(createTransfer(bundle, paymentOption, new ArrayList<>(), new ArrayList<>()));
                return transfers;
            }
        }
        List<List<Fee>> combinedFees = getCartesianProduct(new ArrayList<>(ciDiscountedFeesMap.values()));
        for (List<Fee> fees : combinedFees) {
            orderFee(bundle.getPaymentAmount(), fees);
            List<String> idsCiBundle = bundle.getCiBundleList().stream()
                    .filter(ciBundle -> getFiscalCodesFromFees(fees).contains(ciBundle.getCiFiscalCode()))
                    .map(CiBundle::getId).toList();
            transfers.add(createTransfer(bundle, paymentOption, fees, idsCiBundle));
        }
        return transfers;
    }

    private List<String> getFiscalCodesFromFees(List<Fee> fees) {
        return fees.stream().map(Fee::getCreditorInstitution).toList();
    }

    private void orderFee(long paymentAmount, List<Fee> fees) {
        Collections.shuffle(fees);
        for (Fee fee : fees) {
            if (paymentAmount - fee.getPrimaryCiIncurredFee() >= 0) {
                paymentAmount -= fee.getPrimaryCiIncurredFee();
                fee.setActualCiIncurredFee(fee.getPrimaryCiIncurredFee());
            } else {
                if (paymentAmount > 0) {
                    fee.setActualCiIncurredFee(paymentAmount);
                    paymentAmount = 0;
                } else {
                    break;
                }
            }
        }
    }

    private List<List<Fee>> getCartesianProduct(List<List<Fee>> sets) {
        return cartesianProduct(sets, 0).toList();
    }

    private Stream<List<Fee>> cartesianProduct(List<List<Fee>> sets, int index) {
        if (index == sets.size()) {
            List<Fee> emptyList = new ArrayList<>();
            return Stream.of(emptyList);
        }
        List<Fee> currentSet = sets.get(index);
        return currentSet.stream().flatMap(element -> cartesianProduct(sets, index + 1)
                .map(list -> {
                    List<Fee> newList = new ArrayList<>(list);
                    newList.add(0, SerializationUtils.clone(element));
                    return newList;
                }));
    }

    /**
     * Add in {@code transfers} the created transfer objects
     *
     * @param transfers     list of transfers where add the transfer
     * @param paymentOption Request of the User
     * @param bundle        Bundle info
     */
    private void analyzeTransferList(
            List<Transfer> transfers, PaymentOption paymentOption, ValidBundle bundle) {
        List<String> primaryTransferCategoryList =
                utilityComponent.getPrimaryTransferCategoryList(
                        paymentOption, paymentOption.getPrimaryCreditorInstitution());
        var ciBundles =
                bundle.getCiBundleList() != null ? bundle.getCiBundleList() : new ArrayList<CiBundle>();

        // analyze public and private bundles
        for (CiBundle cibundle : ciBundles) {
            if (cibundle.getAttributes() != null && !cibundle.getAttributes().isEmpty()) {
                transfers.addAll(
                        cibundle
                                .getAttributes()
                                .parallelStream()
                                .filter(
                                        attribute ->
                                                (attribute.getTransferCategory() != null
                                                        && (TransferCategoryRelation.NOT_EQUAL.equals(
                                                        attribute.getTransferCategoryRelation())
                                                        && primaryTransferCategoryList.contains(
                                                        attribute.getTransferCategory()))))
                                .map(
                                        attribute ->
                                                createTransfer(bundle.getPaymentAmount(), 0, bundle, null, paymentOption))
                                .toList());
                transfers.addAll(
                        cibundle
                                .getAttributes()
                                .parallelStream()
                                .filter(
                                        attribute ->
                                                (attribute.getTransferCategory() == null
                                                        || (TransferCategoryRelation.EQUAL.equals(
                                                        attribute.getTransferCategoryRelation())
                                                        && primaryTransferCategoryList.contains(
                                                        attribute.getTransferCategory())
                                                        || (TransferCategoryRelation.NOT_EQUAL.equals(
                                                        attribute.getTransferCategoryRelation())
                                                        && !primaryTransferCategoryList.contains(
                                                        attribute.getTransferCategory())))))
                                .map(
                                        attribute -> {
                                            // primaryCiIncurredFee = min (paymentAmount, min(ciIncurredFee, PspFee))
                                            // The second min is to prevent error in order to check that PSP payment
                                            // amount should be always greater than CI one.
                                            // Note: this check should be done on Marketplace.
                                            long primaryCiIncurredFee =
                                                    Math.min(
                                                            paymentOption.getPaymentAmount(),
                                                            Math.min(bundle.getPaymentAmount(), attribute.getMaxPaymentAmount()));
                                            return createTransfer(
                                                    Math.max(0, bundle.getPaymentAmount() - primaryCiIncurredFee),
                                                    primaryCiIncurredFee,
                                                    bundle,
                                                    cibundle.getId(),
                                                    paymentOption);
                                        })
                                .toList());
            } else {
                transfers.add(
                        createTransfer(bundle.getPaymentAmount(), 0, bundle, cibundle.getId(), paymentOption));
            }
        }

        // analyze global bundles
        if (isGlobal(bundle) && ciBundles.isEmpty()) {
            // no incurred fee is present
            Transfer transfer = createTransfer(bundle.getPaymentAmount(), 0, bundle, null, paymentOption);
            transfers.add(transfer);
        }
    }

    private List<Fee> analyzeFee(
            PaymentNoticeItem paymentNoticeItem, ValidBundle bundle) {
        List<Fee> fees = new ArrayList<>();
        List<String> primaryTransferCategoryList =
                utilityComponent.getPrimaryTransferCategoryListMulti(
                        paymentNoticeItem, paymentNoticeItem.getPrimaryCreditorInstitution());
        var ciBundles =
                bundle.getCiBundleList() != null ? bundle.getCiBundleList() : new ArrayList<CiBundle>();

        // analyze public and private bundles
        for (CiBundle cibundle : ciBundles) {
            if (cibundle.getAttributes() != null && !cibundle.getAttributes().isEmpty()
                    && paymentNoticeItem.getPrimaryCreditorInstitution().equals(cibundle.getCiFiscalCode())) {
                fees.addAll(
                        cibundle
                                .getAttributes()
                                .parallelStream()
                                .filter(
                                        attribute ->
                                                (attribute.getTransferCategory() == null
                                                        || (TransferCategoryRelation.EQUAL.equals(
                                                        attribute.getTransferCategoryRelation())
                                                        && primaryTransferCategoryList.contains(
                                                        attribute.getTransferCategory())
                                                        || (TransferCategoryRelation.NOT_EQUAL.equals(
                                                        attribute.getTransferCategoryRelation())
                                                        && !primaryTransferCategoryList.contains(
                                                        attribute.getTransferCategory())))))
                                .map(
                                        attribute -> createFee(attribute.getMaxPaymentAmount(), cibundle.getCiFiscalCode()))
                                .toList());
            }
        }
        return fees;
    }

    /**
     * @param creditorInstitution  code of the creditor instiution
     * @param primaryCiIncurredFee fee of CI
     * @return Create transfer item
     */
    private Fee createFee(
            long primaryCiIncurredFee,
            String creditorInstitution) {
        return Fee.builder()
                .creditorInstitution(creditorInstitution)
                .primaryCiIncurredFee(primaryCiIncurredFee)
                .build();
    }

    /**
     * @param bundle        info of the Bundle
     * @param paymentOption the payment option involved in the transaction
     * @param fees          the fees to include in the transfer
     * @return Create transfer item
     */
    private it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer createTransfer(
            ValidBundle bundle,
            PaymentOptionMulti paymentOption,
            List<Fee> fees,
            List<String> idsCiBundles) {
        long actualPayerFee = bundle.getPaymentAmount() - fees.stream().mapToLong(Fee::getActualCiIncurredFee).sum();
        return it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer.builder()
                .taxPayerFee(bundle.getPaymentAmount())
                .actualPayerFee(Math.max(0, actualPayerFee))
                .paymentMethod(bundle.getPaymentType() == null ? "ANY" : bundle.getPaymentType())
                .touchpoint(bundle.getTouchpoint())
                .idBundle(bundle.getId())
                .bundleName(bundle.getName())
                .bundleDescription(bundle.getDescription())
                .idPsp(bundle.getIdPsp())
                .idBrokerPsp(bundle.getIdBrokerPsp())
                .idChannel(bundle.getIdChannel())
                .idsCiBundle(idsCiBundles)
                .onUs(this.getOnUsValue(bundle, paymentOption))
                .abi(bundle.getAbi())
                .pspBusinessName(bundle.getPspBusinessName())
                .fees(fees)
                .build();
    }

    /**
     * @param taxPayerFee          fee of the user
     * @param primaryCiIncurredFee fee of CI
     * @param bundle               info of the Bundle
     * @param idCiBundle           ID of CI-Bundle relation
     * @return Create transfer item
     */
    private Transfer createTransfer(
            long taxPayerFee,
            long primaryCiIncurredFee,
            ValidBundle bundle,
            String idCiBundle,
            PaymentOption paymentOption) {
        return Transfer.builder()
                .taxPayerFee(taxPayerFee)
                .primaryCiIncurredFee(primaryCiIncurredFee)
                .paymentMethod(bundle.getPaymentType() == null ? "ANY" : bundle.getPaymentType())
                .touchpoint(bundle.getTouchpoint())
                .idBundle(bundle.getId())
                .bundleName(bundle.getName())
                .bundleDescription(bundle.getDescription())
                .idCiBundle(idCiBundle)
                .idPsp(bundle.getIdPsp())
                .idBrokerPsp(bundle.getIdBrokerPsp())
                .idChannel(bundle.getIdChannel())
                .onUs(this.getOnUsValue(bundle, paymentOption))
                .abi(bundle.getAbi())
                .pspBusinessName(bundle.getPspBusinessName())
                .build();
    }

    private Boolean getOnUsValue(ValidBundle bundle, PaymentOption paymentOption) {
        boolean onusValue = false;

        // if PaymentType is CP and amount > threshold and idChannel endsWith '_ONUS' ---> onus value
        // true
        if (bundle.getPaymentType() != null
                && StringUtils.equalsIgnoreCase(bundle.getPaymentType(), "cp")
                && !isBelowThreshold(paymentOption.getPaymentAmount())
                && isOnusBundle(bundle)) {

            onusValue = true;
        }
        return onusValue;
    }

    private Boolean getOnUsValue(ValidBundle bundle, PaymentOptionMulti paymentOption) {
        boolean onusValue = false;

        // if PaymentType is CP and amount > threshold and idChannel endsWith '_ONUS' ---> onus value
        // true
        if (bundle.getPaymentType() != null
                && StringUtils.equalsIgnoreCase(bundle.getPaymentType(), "cp")
                && !isBelowThreshold(paymentOption.getPaymentAmount())
                && isOnusBundle(bundle)) {

            onusValue = true;
        }
        return onusValue;
    }

    private boolean isBelowThreshold(long paymentAmount) {
        return paymentAmount < Long.parseLong(StringUtils.trim(amountThreshold));
    }

    private void addToPspTransfersMap(Map<String, it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> pspTransferMap,
                                      List<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> transferList) {
        for (it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer transfer : transferList) {
            if (!pspTransferMap.containsKey(transfer.getIdPsp()) ||
                    pspTransferMap.get(transfer.getIdPsp()).getActualPayerFee() > transfer.getActualPayerFee()) {
                pspTransferMap.put(transfer.getIdPsp(), transfer);
            }
        }
    }

    private List<IssuerRangeEntity> getIssuersByBIN(String bin) {
        long paddedBin = Long.parseLong(StringUtils.rightPad(bin, 19, '0'));

        List<IssuerRangeEntity> resultIssuerRangeEntityList = this.issuersService.getIssuerRangeTableCached();

        return resultIssuerRangeEntityList.parallelStream()
                .filter(el -> el.getLowRange() <= paddedBin && el.getHighRange() >= paddedBin)
                .collect(Collectors.toList());
    }

    private boolean checkValidityBin(String bin) {
        if (StringUtils.isNotBlank(bin)) {
            try {
                Long.parseLong(bin);
                return true;
            } catch (Exception e) {
                // Doing nothing if bin is not a long
            }
        }
        return false;
    }
    
    
    private List<ValidBundle> filterValidBundlesInMemory(PaymentOption paymentOption, boolean allCcp) {
        List<ValidBundle> cachedBundles = validBundleCacheService.getAllValidBundles();

        long onlyMarcaBolloDigitale = paymentOption.getTransferList().stream()
                .filter(Objects::nonNull)
                .filter(elem -> Boolean.TRUE.equals(elem.getDigitalStamp()))
                .count();
        long transferListSize = paymentOption.getTransferList().size();
        
        String resolvedTouchpoint = resolveTouchpointName(paymentOption.getTouchpoint());
        String resolvedPaymentMethod = resolvePaymentTypeName(paymentOption.getPaymentMethod());

        // Apply all repository-level and post-query filters in memory on top of the cached snapshot.
        return cachedBundles.stream()
                .filter(bundle -> matchesPaymentAmount(bundle, paymentOption.getPaymentAmount()))
                .filter(bundle -> matchesTouchpoint(bundle.getTouchpoint(), resolvedTouchpoint))
                .filter(bundle -> matchesPaymentType(bundle.getPaymentType(), resolvedPaymentMethod))
                .filter(bundle -> matchesPspList(bundle, paymentOption.getIdPspList()))
                .filter(bundle -> matchesTransferCategories(bundle, utilityComponent.getTransferCategoryList(paymentOption)))
                .filter(bundle -> matchesAllCcp(bundle, allCcp))
                .filter(bundle -> matchesPspBlacklist(bundle))
                .filter(bundle -> CosmosRepository.digitalStampFilter(transferListSize, onlyMarcaBolloDigitale, bundle))
                .map(this::copyValidBundleForRuntimeUse)
                .filter(bundle -> globalAndRelatedFilterInMemory(paymentOption, bundle))
                .toList();
    }
    
    private List<ValidBundle> filterValidBundlesInMemory(PaymentOptionMulti paymentOption, boolean allCcp) {
        List<ValidBundle> cachedBundles = validBundleCacheService.getAllValidBundles();

        List<it.gov.pagopa.afm.calculator.model.TransferListItem> transferList = new ArrayList<>();
        paymentOption.getPaymentNotice().forEach(paymentNoticeItem -> {
            if (paymentNoticeItem.getTransferList() != null) {
                transferList.addAll(paymentNoticeItem.getTransferList());
            }
        });

        long onlyMarcaBolloDigitale = transferList.stream()
                .filter(Objects::nonNull)
                .filter(elem -> Boolean.TRUE.equals(elem.getDigitalStamp()))
                .count();
        long transferListSize = transferList.size();
        
        String resolvedTouchpoint = resolveTouchpointName(paymentOption.getTouchpoint());
        String resolvedPaymentMethod = resolvePaymentTypeName(paymentOption.getPaymentMethod());

        return cachedBundles.stream()
                .filter(bundle -> matchesPaymentAmount(bundle, paymentOption.getPaymentAmount()))
                .filter(bundle -> matchesTouchpoint(bundle.getTouchpoint(), resolvedTouchpoint))
                .filter(bundle -> matchesPaymentType(bundle.getPaymentType(), resolvedPaymentMethod))
                .filter(bundle -> matchesPspList(bundle, paymentOption.getIdPspList()))
                .filter(bundle -> matchesTransferCategories(bundle, utilityComponent.getTransferCategoryList(paymentOption)))
                .filter(bundle -> matchesAllCcp(bundle, allCcp))
                .filter(bundle -> matchesPspBlacklist(bundle))
                .filter(bundle -> matchesCart(bundle, paymentOption))
                .filter(bundle -> CosmosRepository.digitalStampFilter(transferListSize, onlyMarcaBolloDigitale, bundle))
                .map(this::copyValidBundleForRuntimeUse)
                .filter(bundle -> globalAndRelatedFilterInMemory(paymentOption, bundle))
                .toList();
    }
    
    private String resolveTouchpointName(String requestTouchpoint) {
        if (requestTouchpoint == null || requestTouchpoint.equalsIgnoreCase("any")) {
            return null;
        }

        return touchpointRepository.findByName(requestTouchpoint)
                .map(Touchpoint::getName)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Touchpoint not found",
                        "Cannot find touchpoint with name: '" + requestTouchpoint + "'"
                ));
    }
    
    private String resolvePaymentTypeName(String requestPaymentMethod) {
        if (requestPaymentMethod == null || requestPaymentMethod.equalsIgnoreCase("any")) {
            return null;
        }

        return paymentTypeRepository.findByName(requestPaymentMethod)
                .map(PaymentType::getName)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "PaymentType not found",
                        "Cannot find payment type with name: '" + requestPaymentMethod + "'"
                ));
    }
    
    private boolean matchesPaymentAmount(ValidBundle bundle, long paymentAmount) {
        return bundle.getMinPaymentAmount() < paymentAmount && bundle.getMaxPaymentAmount() >= paymentAmount;
    }
    
    private boolean matchesTouchpoint(String bundleTouchpoint, String resolvedTouchpoint) {
        if (resolvedTouchpoint == null) {
            return true;
        }

        return bundleTouchpoint == null
                || bundleTouchpoint.equalsIgnoreCase("any")
                || bundleTouchpoint.equalsIgnoreCase(resolvedTouchpoint);
    }
    
    private boolean matchesPaymentType(String bundlePaymentType, String resolvedPaymentMethod) {
        if (resolvedPaymentMethod == null) {
            return true;
        }

        // Preserve repository behavior: exact match or null wildcard only.
        return bundlePaymentType == null || bundlePaymentType.equalsIgnoreCase(resolvedPaymentMethod);
    }
    
    private boolean matchesPspList(ValidBundle bundle, List<it.gov.pagopa.afm.calculator.model.PspSearchCriteria> idPspList) {
        if (idPspList == null || idPspList.isEmpty()) {
            return true;
        }

        return idPspList.stream().anyMatch(pspSearch ->
                Objects.equals(bundle.getIdPsp(), pspSearch.getIdPsp())
                        && (StringUtils.isEmpty(pspSearch.getIdChannel()) || Objects.equals(bundle.getIdChannel(), pspSearch.getIdChannel()))
                        && (StringUtils.isEmpty(pspSearch.getIdBrokerPsp()) || Objects.equals(bundle.getIdBrokerPsp(), pspSearch.getIdBrokerPsp()))
        );
    }
    
    private boolean matchesTransferCategories(ValidBundle bundle, List<String> categories) {
        if (categories == null) {
            return true;
        }

        List<String> nonEmptyCategories = categories.stream()
                .filter(Objects::nonNull)
                .filter(elem -> !elem.isEmpty())
                .toList();

        if (nonEmptyCategories.isEmpty()) {
            return bundle.getTransferCategoryList() == null;
        }

        return bundle.getTransferCategoryList() == null
                || bundle.getTransferCategoryList().stream().anyMatch(nonEmptyCategories::contains);
    }
    
    private boolean matchesAllCcp(ValidBundle bundle, boolean allCcp) {
        return allCcp || !Objects.equals(bundle.getIdPsp(), pspPosteId);
    }
    
    private boolean matchesPspBlacklist(ValidBundle bundle) {
        return pspBlacklist == null || pspBlacklist.isEmpty() || !pspBlacklist.contains(bundle.getIdPsp());
    }
    
    private boolean matchesCart(ValidBundle bundle, PaymentOptionMulti paymentOption) {
        if (paymentOption.getPaymentNotice().size() <= 1) {
            return true;
        }

        return Boolean.TRUE.equals(bundle.getCart());
    }
    
    private ValidBundle copyValidBundleForRuntimeUse(ValidBundle bundle) {
        // Return a defensive copy so downstream filters do not mutate the cached snapshot.
        return ValidBundle.builder()
                .id(bundle.getId())
                .idChannel(bundle.getIdChannel())
                .name(bundle.getName())
                .description(bundle.getDescription())
                .paymentAmount(bundle.getPaymentAmount())
                .minPaymentAmount(bundle.getMinPaymentAmount())
                .maxPaymentAmount(bundle.getMaxPaymentAmount())
                .touchpoint(bundle.getTouchpoint())
                .paymentType(bundle.getPaymentType())
                .idBrokerPsp(bundle.getIdBrokerPsp())
                .idPsp(bundle.getIdPsp())
                .abi(bundle.getAbi())
                .pspBusinessName(bundle.getPspBusinessName())
                .onUs(bundle.getOnUs())
                .digitalStamp(bundle.getDigitalStamp())
                .digitalStampRestriction(bundle.getDigitalStampRestriction())
                .transferCategoryList(bundle.getTransferCategoryList() == null ? null : new ArrayList<>(bundle.getTransferCategoryList()))
                .ciBundleList(bundle.getCiBundleList() == null ? new ArrayList<>() : new ArrayList<>(bundle.getCiBundleList()))
                .type(bundle.getType())
                .cart(bundle.getCart())
                .build();
    }
    
    private boolean globalAndRelatedFilterInMemory(PaymentOption paymentOption, ValidBundle bundle) {
        bundle.setCiBundleList(filterByCIInMemory(paymentOption.getPrimaryCreditorInstitution(), bundle));
        return UtilityComponent.isGlobal(bundle) || belongsCIInMemory(bundle);
    }
    
    private boolean globalAndRelatedFilterInMemory(PaymentOptionMulti paymentOption, ValidBundle bundle) {
        bundle.setCiBundleList(filteredCiBundlesInMemory(paymentOption, bundle));
        return UtilityComponent.isGlobal(bundle) || belongsCIInMemory(bundle);
    }
    
    private List<CiBundle> filterByCIInMemory(String ciFiscalCode, ValidBundle bundle) {
        return bundle.getCiBundleList() != null
                ? bundle.getCiBundleList().stream()
                        .filter(ciBundle -> ciFiscalCode.equals(ciBundle.getCiFiscalCode()))
                        .toList()
                : null;
    }

    private List<CiBundle> filteredCiBundlesInMemory(PaymentOptionMulti paymentOption, ValidBundle bundle) {
        if (bundle.getCiBundleList() != null) {
            List<String> ciBundlesFiscalCodes = bundle.getCiBundleList().stream()
                    .map(CiBundle::getCiFiscalCode)
                    .toList();

            boolean allCiBundlesPresent = paymentOption.getPaymentNotice().stream()
                    .anyMatch(paymentNoticeItem -> ciBundlesFiscalCodes.contains(paymentNoticeItem.getPrimaryCreditorInstitution()));

            return allCiBundlesPresent ? bundle.getCiBundleList() : new ArrayList<>();
        }
        return new ArrayList<>();
    }

    private boolean belongsCIInMemory(ValidBundle bundle) {
        return bundle.getCiBundleList() != null && !bundle.getCiBundleList().isEmpty();
    }


}
