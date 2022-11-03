package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.Bundle;
import it.gov.pagopa.afm.calculator.entity.CiBundle;
import it.gov.pagopa.afm.calculator.model.BundleType;
import it.gov.pagopa.afm.calculator.model.PaymentMethod;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.Touchpoint;
import it.gov.pagopa.afm.calculator.model.TransferCategoryRelation;
import it.gov.pagopa.afm.calculator.model.TransferListItem;
import it.gov.pagopa.afm.calculator.model.calculator.Transfer;
import it.gov.pagopa.afm.calculator.repository.BundleRepository;
import it.gov.pagopa.afm.calculator.util.BundleSpecification;
import it.gov.pagopa.afm.calculator.util.BundleTransferCategoryListSpecification;
import it.gov.pagopa.afm.calculator.util.SearchCriteria;
import it.gov.pagopa.afm.calculator.util.SearchOperation;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CalculatorService {

    @Autowired
    BundleRepository bundleRepository;

    @Autowired
    UtilityComponent utilityComponent;

    @Autowired
    ModelMapper modelMapper;


    @Cacheable(value = "calculate")
    public List<Transfer> calculate(@Valid PaymentOption paymentOption, int limit) {
        // create filters
        Specification<Bundle> specifications = getQueryFilters(paymentOption);
        // do the query
        var bundles = bundleRepository.findAll(specifications);

        boolean primaryCiInTransferList = inTransferList(paymentOption.getPrimaryCreditorInstitution(), paymentOption.getTransferList());

        // calculate the taxPayerFee
        return calculateTaxPayerFee(paymentOption, limit, primaryCiInTransferList, bundles);
    }

    /**
     * Null value are ignored -> they are skipped when building the specification filter
     *
     * @param paymentOption Get the Body of the Request
     * @return map {@code paymentOption} parameter to {@code Specification} object to use in the query
     */
    private Specification<Bundle> getQueryFilters(PaymentOption paymentOption) {
        var touchpointFilter = new BundleSpecification(new SearchCriteria("touchpoint", SearchOperation.NULL_OR_EQUAL, paymentOption.getTouchpoint()));

        var paymentMethodFilter = new BundleSpecification(new SearchCriteria("paymentMethod", SearchOperation.NULL_OR_EQUAL, paymentOption.getPaymentMethod()));

        List<String> idPspList = paymentOption.getIdPspList() != null && paymentOption.getIdPspList().isEmpty() ? null : paymentOption.getIdPspList();
        var pspFilter = new BundleSpecification(new SearchCriteria("idPsp", SearchOperation.IN, idPspList));

        // retrieve public and private bundles
        var ciFilter = new BundleSpecification(new SearchCriteria("ciBundles.ciFiscalCode", SearchOperation.EQUAL, paymentOption.getPrimaryCreditorInstitution()));
        // retrieve global bundles
        var globalFilter = new BundleSpecification(new SearchCriteria("type", SearchOperation.EQUAL, BundleType.GLOBAL));

        // the payment amount should be in range [minPaymentAmount, maxPaymentAmount]
        var minPriceRangeFilter = new BundleSpecification(new SearchCriteria("minPaymentAmount", SearchOperation.LESS_THAN_EQUAL, paymentOption.getPaymentAmount()));
        var maxPriceRangeFilter = new BundleSpecification(new SearchCriteria("maxPaymentAmount", SearchOperation.GREATER_THAN_EQUAL, paymentOption.getPaymentAmount()));

        var bundleTransferCategoryListFilter = new BundleTransferCategoryListSpecification(utilityComponent.getTransferCategoryList(paymentOption));

        return Specification
                .where(touchpointFilter)
                .and(paymentMethodFilter)
                .and(pspFilter)
                .and(maxPriceRangeFilter)
                .and(minPriceRangeFilter)
                .and(ciFilter.or(globalFilter))
                .and(bundleTransferCategoryListFilter);
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
        // analyze public and private bundles
        for (CiBundle cibundle : bundle.getCiBundles()) {
            // check ciBundle belongs to primary CI
            if (cibundle.getCiFiscalCode().equals(paymentOption.getPrimaryCreditorInstitution())) {
                if (!cibundle.getAttributes().isEmpty()) {
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
        if (bundle.getType().equals(BundleType.GLOBAL) && bundle.getCiBundles().isEmpty()) {
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
                .touchpoint(bundle.getTouchpoint() == null ? Touchpoint.builder().name("ANY").build() :
                        modelMapper.map(bundle.getTouchpoint(), Touchpoint.class))
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
