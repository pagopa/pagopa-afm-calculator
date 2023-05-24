package it.gov.pagopa.afm.calculator.service;

import static it.gov.pagopa.afm.calculator.service.UtilityComponent.inTransferList;
import static it.gov.pagopa.afm.calculator.service.UtilityComponent.isGlobal;

import it.gov.pagopa.afm.calculator.entity.CiBundle;
import it.gov.pagopa.afm.calculator.entity.IssuerRangeEntity;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.exception.AppError;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.TransferCategoryRelation;
import it.gov.pagopa.afm.calculator.model.calculator.BundleOption;
import it.gov.pagopa.afm.calculator.model.calculator.Transfer;
import it.gov.pagopa.afm.calculator.repository.CosmosRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Setter
public class CalculatorService {

  @Value("${payment.amount.threshold}")
  private String amountThreshold;

  @Autowired CosmosRepository cosmosRepository;

  @Autowired UtilityComponent utilityComponent;

  @Autowired IssuersService issuersService;

  @Cacheable(value = "calculate")
  public List<Transfer> calculate(@Valid PaymentOption paymentOption, int limit, boolean allCcp) {
    List<ValidBundle> filteredBundles = cosmosRepository.findByPaymentOption(paymentOption, allCcp);

    return BundleOption.builder()
        .belowThreshold(isBelowThreshold(paymentOption.getPaymentAmount()))
        // calculate the taxPayerFee
        .bundleOptions(calculateTaxPayerFee(paymentOption, limit, filteredBundles))
        .build();
  }

  private List<Transfer> calculateTaxPayerFee(
      PaymentOption paymentOption, int limit, List<ValidBundle> bundles) {
    boolean primaryCiInTransferList =
        inTransferList(
            paymentOption.getPrimaryCreditorInstitution(), paymentOption.getTransferList());
    List<Transfer> transfers = new ArrayList<>();
    for (ValidBundle bundle : bundles) {

      // if primaryCi is in transfer list we should evaluate the related incurred fee
      if (primaryCiInTransferList) {
        // add in transfers!
        analyzeTransferList(transfers, paymentOption, bundle);
      } else {
        Transfer transfer =
            createTransfer(bundle.getPaymentAmount(), 0, bundle, null, paymentOption);
        transfers.add(transfer);
      }
    }

    // sort according onus and taxpayer fee
    Collections.sort(transfers);

    return transfers.stream().limit(limit).collect(Collectors.toList());
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
            cibundle.getAttributes().parallelStream()
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
                .collect(Collectors.toList()));
        transfers.addAll(
            cibundle.getAttributes().parallelStream()
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
                .collect(Collectors.toList()));
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
        .build();
  }

  private Boolean getOnUsValue(ValidBundle bundle, PaymentOption paymentOption) {
    boolean onusValue = false;

    // if PaymentType is CP and amount > threshold and bin is evaluated ---> calculate onus value
    if (bundle.getPaymentType() != null
        && StringUtils.equalsIgnoreCase(bundle.getPaymentType(), "cp")
        && !isBelowThreshold(paymentOption.getPaymentAmount())
        && StringUtils.isNotBlank(paymentOption.getBin())) {
      // get issuers by BIN
      List<IssuerRangeEntity> issuers = issuersService.getIssuersByBIN(paymentOption.getBin());
     
      // all extracted record must have the same ABI otherwise expetion raised
      // - the limit(2) operation is used to terminate as soon as two distinct ABI objects are found
      if (!CollectionUtils.isEmpty(issuers) && issuers.stream().map(IssuerRangeEntity::getAbi).distinct().limit(2).count() > 1) {
        throw new AppException(
            AppError.ISSUERS_BIN_WITH_DIFFERENT_ABI_ERROR, paymentOption.getBin());
      }

      // check if the ABI of the bundle is the same as issuers pulled via BIN and that idChannel ends with the suffix _ONUS
      if (!CollectionUtils.isEmpty(issuers) 
          && issuers.get(0).getAbi().equalsIgnoreCase(bundle.getAbi())
          && StringUtils.endsWithIgnoreCase(bundle.getIdChannel(), "_ONUS")) {
        onusValue = true;
      }
    }
    return onusValue;
  }

  private boolean isBelowThreshold(long paymentAmount) {
    return paymentAmount < Long.parseLong(StringUtils.trim(amountThreshold));
  }
}
