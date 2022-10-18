package it.gov.pagopa.afm.calculator.service;

import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.query.CosmosQuery;
import it.gov.pagopa.afm.calculator.entity.Bundle;
import it.gov.pagopa.afm.calculator.entity.CiBundle;
import it.gov.pagopa.afm.calculator.model.BundleType;
import it.gov.pagopa.afm.calculator.model.PaymentMethod;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.Touchpoint;
import it.gov.pagopa.afm.calculator.model.TransferCategoryRelation;
import it.gov.pagopa.afm.calculator.model.TransferListItem;
import it.gov.pagopa.afm.calculator.model.calculator.Transfer;
import it.gov.pagopa.afm.calculator.repository.CiBundleRepository;
import it.gov.pagopa.afm.calculator.util.CriteriaBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.and;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.arrayContains;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.in;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.isEqual;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.isEqualOrNull;

@Service
@Slf4j
public class CalculatorService {

    @Autowired
    CiBundleRepository ciBundleRepository;

    @Autowired
    UtilityComponent utilityComponent;

    @Autowired
    CosmosTemplate cosmosTemplate;


    @Cacheable(value = "calculate")
    public List<Transfer> calculate(@Valid PaymentOption paymentOption, int limit) {

        boolean primaryCiInTransferList = inTransferList(paymentOption.getPrimaryCreditorInstitution(), paymentOption.getTransferList());

        Iterable<Bundle> bundlesResult = getFilteredBundles(paymentOption);

        // get GLOBAL bundles and PRIVATE|PUBLIC bundles of the CI
        var fiscalCodeFilter = isEqual("ciFiscalCode", paymentOption.getPrimaryCreditorInstitution());
        var ciBundles= cosmosTemplate.find(new CosmosQuery(fiscalCodeFilter), CiBundle.class, "cibundles");

        var bundles = StreamSupport.stream(bundlesResult.spliterator(), true)
                .filter(bundle -> bundle.getType().equals(BundleType.GLOBAL)
                        || StreamSupport.stream(ciBundles.spliterator(), true).anyMatch(ciBundle-> ciBundle.getIdBundle().equals(bundle.getId())))
                .collect(Collectors.toList());

        // calculate the taxPayerFee
        return calculateTaxPayerFee(paymentOption, limit, primaryCiInTransferList, bundles);
    }

    /**
     * Null value are ignored -> they are skipped when building the filters
     *
     * @param paymentOption Get the Body of the Request
     * @return the filtered bundles
     */
    private Iterable<Bundle> getFilteredBundles(PaymentOption paymentOption) {
        var minFilter = CriteriaBuilder.lessThanEqual("minPaymentAmount", paymentOption.getPaymentAmount());
        var maxFilter = CriteriaBuilder.greaterThan("maxPaymentAmount", paymentOption.getPaymentAmount());
        var queryResult = and(minFilter, maxFilter);

        if (paymentOption.getTouchpoint() != null) {
            var touchpointFilter = isEqualOrNull("touchpoint", paymentOption.getTouchpoint().getValue());
            queryResult = and(queryResult, touchpointFilter);
        }

        if (paymentOption.getPaymentMethod() != null) {
            var paymentMethodFilter = isEqualOrNull("paymentMethod", paymentOption.getPaymentMethod().getValue());
            queryResult = and(queryResult, paymentMethodFilter);
        }

        if (paymentOption.getIdPspList() != null && !paymentOption.getIdPspList().isEmpty()) {
            var pspFilter = in("idPsp", paymentOption.getIdPspList());
            queryResult = and(queryResult, pspFilter);
        }

        List<String> categoryList = utilityComponent.getTransferCategoryList(paymentOption);
        if (categoryList != null) {
            var taxonomyFilter = categoryList.parallelStream()
                    .filter(Objects::nonNull)
                    .filter(elem -> !elem.isEmpty())
                    .map(elem -> arrayContains("transferCategoryList", elem))
                    .reduce(CriteriaBuilder::or);

            if (taxonomyFilter.isPresent()) {
                queryResult = and(queryResult, taxonomyFilter.get());
            }
        }

        return cosmosTemplate.find(new CosmosQuery(queryResult), Bundle.class, "bundles");
    }

    private List<Transfer> calculateTaxPayerFee(PaymentOption paymentOption, int limit, boolean primaryCiInTransferList, List<Bundle> bundles) {
        List<String> primaryTransferCategoryList = utilityComponent.getPrimaryTransferCategoryList(paymentOption, paymentOption.getPrimaryCreditorInstitution());
        List<Transfer> transfers = new ArrayList<>();
        for (Bundle bundle : bundles) {

            // if primaryCi is in transfer list we should evaluate the related incurred fee
            if (primaryCiInTransferList) {
                // add in transfers!
                analyzeTransferList(transfers, paymentOption, primaryTransferCategoryList, bundle);
            } else {
                Transfer transfer = createTransfer(bundle.getPaymentAmount(), 0, bundle, null);
                transfers.add(transfer);
            }
        }

        // sort according taxpayer fee
        Collections.sort(transfers);

        return transfers.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Add in {@code transfers} the created transfer objects
     *
     * @param transfers                   list of transfers where add the transfer
     * @param paymentOption               Request of the User
     * @param primaryTransferCategoryList transfers of the primary CI
     * @param bundle                      Bundle info
     */
    private void analyzeTransferList(List<Transfer> transfers, PaymentOption paymentOption, List<String> primaryTransferCategoryList, Bundle bundle) {
        var ciBundles = ciBundleRepository.findByIdBundle(bundle.getId());
        // analyze public and private bundles
        for (CiBundle cibundle : ciBundles) {
            // check ciBundle belongs to primary CI
            if (cibundle.getCiFiscalCode().equals(paymentOption.getPrimaryCreditorInstitution())) {
                if (cibundle.getAttributes() != null && !cibundle.getAttributes().isEmpty()) {
                    transfers.addAll(cibundle.getAttributes().parallelStream()
                            .filter(attribute -> (attribute.getTransferCategory() != null &&
                                    (TransferCategoryRelation.NOT_EQUAL.equals(attribute.getTransferCategoryRelation()) && primaryTransferCategoryList.contains(attribute.getTransferCategory())
                                    )))
                            .map(attribute -> createTransfer(bundle.getPaymentAmount(), 0, bundle, null)
                            )
                            .collect(Collectors.toList())
                    );
                    transfers.addAll(cibundle.getAttributes().parallelStream()
                            .filter(attribute -> (attribute.getTransferCategory() == null ||
                                    (TransferCategoryRelation.EQUAL.equals(attribute.getTransferCategoryRelation()) && primaryTransferCategoryList.contains(attribute.getTransferCategory()) ||
                                            (TransferCategoryRelation.NOT_EQUAL.equals(attribute.getTransferCategoryRelation()) && !primaryTransferCategoryList.contains(attribute.getTransferCategory()))
                                    )))
                            .map(attribute -> {
                                // primaryCiIncurredFee = min (paymentAmount, min(ciIncurredFee, PspFee))
                                // The second min is to prevent error in order to check that PSP payment amount should be always greater than CI one.
                                // Note: this check should be done on Marketplace.
                                long primaryCiIncurredFee = Math.min(paymentOption.getPaymentAmount(), Math.min(bundle.getPaymentAmount(), attribute.getMaxPaymentAmount()));
                                return createTransfer(Math.max(0, bundle.getPaymentAmount() - primaryCiIncurredFee),
                                        primaryCiIncurredFee, bundle, cibundle.getId());
                            })
                            .collect(Collectors.toList())
                    );
                } else {
                    transfers.add(createTransfer(bundle.getPaymentAmount(), 0, bundle, cibundle.getId()));
                }
            }
        }

        // analyze global bundles
        if (bundle.getType().equals(BundleType.GLOBAL) && ciBundles.isEmpty()) {
            // no incurred fee is present
            Transfer transfer = createTransfer(bundle.getPaymentAmount(), 0, bundle, null);
            transfers.add(transfer);
        }
    }

    /**
     * @param taxPayerFee          fee of the user
     * @param primaryCiIncurredFee fee of CI
     * @param bundle               info of the Bundle
     * @param idCiBundle           ID of CI-Bundle relation
     * @return Create transfer item
     */
    private Transfer createTransfer(long taxPayerFee, long primaryCiIncurredFee, Bundle bundle, String idCiBundle) {
        return Transfer.builder()
                .taxPayerFee(taxPayerFee)
                .primaryCiIncurredFee(primaryCiIncurredFee)
                .paymentMethod(bundle.getPaymentMethod() == null ? PaymentMethod.ANY : bundle.getPaymentMethod())
                .touchpoint(bundle.getTouchpoint() == null ? Touchpoint.ANY : bundle.getTouchpoint())
                .idBundle(bundle.getId())
                .bundleName(bundle.getName())
                .bundleDescription(bundle.getDescription())
                .idCiBundle(idCiBundle)
                .idPsp(bundle.getIdPsp())
                .build();
    }

    /**
     * @param creditorInstitutionFiscalCode primary CI fiscal code
     * @param transferList                  list of transfers
     * @return Check if creditor institution belongs to transfer list
     */
    private boolean inTransferList(String creditorInstitutionFiscalCode, ArrayList<TransferListItem> transferList) {
        return transferList.parallelStream()
                .anyMatch(transferListItem -> transferListItem.getCreditorInstitution().equals(creditorInstitutionFiscalCode));
    }

}
