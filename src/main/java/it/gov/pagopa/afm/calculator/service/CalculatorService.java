package it.gov.pagopa.afm.calculator.service;

import static it.gov.pagopa.afm.calculator.service.UtilityComponent.inTransferList;
import static it.gov.pagopa.afm.calculator.service.UtilityComponent.isGlobal;

import it.gov.pagopa.afm.calculator.entity.CiBundle;
import it.gov.pagopa.afm.calculator.entity.IssuerRangeEntity;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.model.PaymentNoticeItem;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.TransferCategoryRelation;
import it.gov.pagopa.afm.calculator.model.calculator.BundleOption;
import it.gov.pagopa.afm.calculator.model.calculator.Transfer;
import it.gov.pagopa.afm.calculator.model.calculatormulti.Fee;
import it.gov.pagopa.afm.calculator.model.calculatormulti.OrderType;
import it.gov.pagopa.afm.calculator.repository.CosmosRepository;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.Valid;
import lombok.Setter;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Setter
public class CalculatorService {

  private static final String ONUS_BUNDLE_SUFFIX = "_ONUS";

  private final String amountThreshold;

  private CosmosRepository cosmosRepository;

  private final UtilityComponent utilityComponent;

  private final IssuersService issuersService;
  
  private final String amexABI;

  public CalculatorService(
          @Value("${payment.amount.threshold}") String amountThreshold,
          CosmosRepository cosmosRepository,
          UtilityComponent utilityComponent,
          IssuersService issuersService,
          @Value("${pspAmex.abi:AMREX}") String amexABI
  ) {
    this.amountThreshold = amountThreshold;
    this.cosmosRepository = cosmosRepository;
    this.utilityComponent = utilityComponent;
    this.issuersService = issuersService;
    this.amexABI = amexABI;
  }

  public BundleOption calculate(@Valid PaymentOption paymentOption, int limit, boolean allCcp) {
    List<ValidBundle> filteredBundles = cosmosRepository.findByPaymentOption(paymentOption, allCcp);
    Collections.shuffle(filteredBundles, new Random());

    return BundleOption.builder()
        .belowThreshold(isBelowThreshold(paymentOption.getPaymentAmount()))
        // calculate the taxPayerFee
        .bundleOptions(calculateTaxPayerFee(paymentOption, limit, filteredBundles))
        .build();
  }

  public it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption calculateMulti(@Valid PaymentOptionMulti paymentOption, int limit, boolean allCcp, boolean onUsFirst, OrderType orderType ) {
    List<ValidBundle> filteredBundles = cosmosRepository.findByPaymentOption(paymentOption, allCcp);
    Collections.shuffle(filteredBundles); //TODO may not be needed anymore

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
        StringUtils.isNotBlank(paymentOption.getBin())
            ? issuersService.getIssuersByBIN(paymentOption.getBin())
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
      PaymentOptionMulti paymentOption, int limit, List<ValidBundle> bundles,OrderType orderType, boolean onUsFirst) {

    Map<String, it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> pspTransfersMap = new HashMap<>();

    // 1. Check if ONUS payment:
    // - ONUS payment = if the bundle ABI attribute matching the one extracted via BIN from the
    // issuers table
    // 2. The returned transfer list must contain:
    // - if ONUS payment = Only the bundles with the idChannel attribute ending in '_ONUS'
    // - if not ONUS payment = Only the bundles with the idChannel attribute NOT ending in '_ONUS'

    // 1.a: get issuers by BIN
    List<IssuerRangeEntity> issuers =
        StringUtils.isNotBlank(paymentOption.getBin())
            ? issuersService.getIssuersByBIN(paymentOption.getBin())
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
          transfers.stream().filter(abiPredicate.and(onusPredicate)).collect(Collectors.toList());
    }


    Collections.sort(transfers, getDynamicComparator(orderType, onUsFirst));

    return transfers.stream().limit(limit).toList();
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
    if(bundle.getCiBundleList().isEmpty()) {
      transfers.add(createTransfer(bundle, paymentOption, new ArrayList<>(), new ArrayList<>()));
      return transfers;
    }
    Map<String, List<Fee>> ciDiscountedFeesMap = new HashMap<>();
    for(PaymentNoticeItem paymentNoticeItem: paymentOption.getPaymentNotice()) {
      List<Fee> discountedFees = analyzeFee(paymentNoticeItem, bundle);
      if(!discountedFees.isEmpty()){
        ciDiscountedFeesMap.put(paymentNoticeItem.getPrimaryCreditorInstitution(), discountedFees);
      } else {
        transfers.add(createTransfer(bundle, paymentOption, new ArrayList<>(), new ArrayList<>()));
        return transfers;
      }
    }
    List<List<Fee>> combinedFees = getCartesianProduct(new ArrayList<>(ciDiscountedFeesMap.values()));
    for(List<Fee> fees: combinedFees) {
      orderFee(bundle.getPaymentAmount(), fees);
      List<String> idsCiBundle = bundle.getCiBundleList().stream()
          .filter(ciBundle -> getFiscalCodesFromFees(fees).contains(ciBundle.getCiFiscalCode()))
          .map(CiBundle::getId).toList();
      transfers.add(createTransfer(bundle, paymentOption, fees, idsCiBundle));
    }
    return transfers;
  }

  private List<String> getFiscalCodesFromFees(List<Fee> fees){
    return fees.stream().map(Fee::getCreditorInstitution).toList();
  }

  private void orderFee (long paymentAmount, List<Fee> fees) {
    Collections.shuffle(fees);
    for(Fee fee: fees) {
      if(paymentAmount - fee.getPrimaryCiIncurredFee() >= 0) {
        paymentAmount -= fee.getPrimaryCiIncurredFee();
        fee.setActualCiIncurredFee(fee.getPrimaryCiIncurredFee());
      } else {
        if(paymentAmount > 0) {
          fee.setActualCiIncurredFee(paymentAmount);
          paymentAmount = 0;
        } else {
          break;
        }
      }
    }
  }

  private List<List<Fee>> getCartesianProduct(List<List<Fee>> sets) {
    return cartesianProduct(sets,0).toList();
  }

  private Stream<List<Fee>> cartesianProduct(List<List<Fee>> sets, int index) {
    if (index == sets.size()) {
      List<Fee> emptyList = new ArrayList<>();
      return Stream.of(emptyList);
    }
    List<Fee> currentSet = sets.get(index);
    return currentSet.stream().flatMap(element -> cartesianProduct(sets, index+1)
        .map(list -> {
          List<Fee> newList = new ArrayList<>(list);
          newList.add(0, SerializationUtils.clone(element));
          return newList;
        }));
  }

  /**
   * Add in {@code transfers} the created transfer objects
   *
   * @param transfers list of transfers where add the transfer
   * @param paymentOption Request of the User
   * @param bundle Bundle info
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
   * @param creditorInstitution code of the creditor instiution
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
   * @param bundle info of the Bundle
   * @param paymentOption the payment option involved in the transaction
   * @param fees the fees to include in the transfer
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
   * @param taxPayerFee fee of the user
   * @param primaryCiIncurredFee fee of CI
   * @param bundle info of the Bundle
   * @param idCiBundle ID of CI-Bundle relation
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
    for(it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer transfer : transferList) {
      if(!pspTransferMap.containsKey(transfer.getIdPsp()) ||
              pspTransferMap.get(transfer.getIdPsp()).getActualPayerFee() > transfer.getActualPayerFee()) {
        pspTransferMap.put(transfer.getIdPsp(), transfer);
      }
    }
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

  /**
   * sort by bundles' fee grouped by PSP
   *
   * @param transfers list of transfers to sort
   */
  private static void sortByFeePerPspMulti(List<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> transfers) {
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

  /**
   * Returns a dynamic comparator for {@code it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer}
   * based on the specified {@link OrderType} and whether to prioritize OnUs transfers.
   *
   * @param orderType  the type of ordering to apply (BYFEE, BYPSPNAME, RANDOM)
   * @param onUsFirst  if true, OnUs transfers are sorted before others
   * @return a comparator for {@code it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer}
   */
  private static Comparator<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> getDynamicComparator(OrderType orderType, boolean onUsFirst) {

    Comparator<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> comparator ;

    Comparator<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> onUsFirstComparator =
            Comparator.comparing((it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getOnUs),Comparator.reverseOrder());

    Comparator<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> byFeeComparator =
            Comparator.comparing(it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getActualPayerFee)
                    .thenComparing(it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getPspBusinessName);

    Comparator<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> byPspNameComparator =
            Comparator.comparing(it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getPspBusinessName);

    Comparator<it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer> randomComparator =
            (t1, t2) -> Integer.compare(new Random().nextInt(3) - 1, 0);

    switch (orderType) {
      case BYFEE -> comparator = byFeeComparator;
      case BYPSPNAME -> comparator = byPspNameComparator;
      case RANDOM -> comparator = randomComparator;
      default -> comparator = randomComparator;
    }

    return onUsFirst?onUsFirstComparator.thenComparing(comparator):comparator;

  }

}
