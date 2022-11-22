package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.CiBundle;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.model.PaymentMethod;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.TransferCategoryRelation;
import it.gov.pagopa.afm.calculator.model.calculator.Transfer;
import it.gov.pagopa.afm.calculator.repository.CosmosRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static it.gov.pagopa.afm.calculator.service.UtilityComponent.inTransferList;
import static it.gov.pagopa.afm.calculator.service.UtilityComponent.isGlobal;

@Service
@Slf4j
public class CalculatorService {

    @Autowired
    CosmosRepository cosmosRepository;

    @Autowired
    UtilityComponent utilityComponent;


    @Cacheable(value = "calculate")
    public List<Transfer> calculate(@Valid PaymentOption paymentOption, int limit) {
        List<ValidBundle> filteredBundles = cosmosRepository.findByPaymentOption(paymentOption);

        // calculate the taxPayerFee
        return calculateTaxPayerFee(paymentOption, limit, filteredBundles);
    }


    private List<Transfer> calculateTaxPayerFee(PaymentOption paymentOption, int limit, List<ValidBundle> bundles) {
        boolean primaryCiInTransferList = inTransferList(paymentOption.getPrimaryCreditorInstitution(), paymentOption.getTransferList());
        List<Transfer> transfers = new ArrayList<>();
        for (ValidBundle bundle : bundles) {

            // if primaryCi is in transfer list we should evaluate the related incurred fee
            if (primaryCiInTransferList) {
                // add in transfers!
                analyzeTransferList(transfers, paymentOption, bundle);
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
     * @param transfers     list of transfers where add the transfer
     * @param paymentOption Request of the User
     * @param bundle        Bundle info
     */
    private void analyzeTransferList(List<Transfer> transfers, PaymentOption paymentOption, ValidBundle bundle) {
        List<String> primaryTransferCategoryList = utilityComponent.getPrimaryTransferCategoryList(paymentOption, paymentOption.getPrimaryCreditorInstitution());
        var ciBundles = bundle.getCiBundleList() != null ? bundle.getCiBundleList() : new ArrayList<CiBundle>();

        // analyze public and private bundles
        for (CiBundle cibundle : ciBundles) {
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

        // analyze global bundles
        if (isGlobal(bundle) && ciBundles.isEmpty()) {
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
    private Transfer createTransfer(long taxPayerFee, long primaryCiIncurredFee, ValidBundle bundle, String idCiBundle) {
        return Transfer.builder()
                .taxPayerFee(taxPayerFee)
                .primaryCiIncurredFee(primaryCiIncurredFee)
                .paymentMethod(bundle.getPaymentMethod() == null ? PaymentMethod.ANY : bundle.getPaymentMethod())
                .touchpoint(bundle.getTouchpoint())
                .idBundle(bundle.getId())
                .bundleName(bundle.getName())
                .bundleDescription(bundle.getDescription())
                .idCiBundle(idCiBundle)
                .idPsp(bundle.getIdPsp())
                .idBrokerPsp(bundle.getIdBrokerPsp())
                .idChannel(bundle.getIdChannel())
                .onUs(PaymentMethod.CP.equals(bundle.getPaymentMethod()) ? bundle.getOnUs() : null)
                .build();
    }

}
