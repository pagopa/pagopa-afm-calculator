package it.gov.pagopa.afm.calculator.repository;

import com.azure.cosmos.implementation.guava25.collect.Iterables;
import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.query.CosmosQuery;
import it.gov.pagopa.afm.calculator.entity.CiBundle;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.service.UtilityComponent;
import it.gov.pagopa.afm.calculator.util.CriteriaBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static it.gov.pagopa.afm.calculator.service.UtilityComponent.isGlobal;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.and;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.arrayContains;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.in;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.isEqualOrNull;

@Repository
public class CosmosRepository {

    @Autowired
    CosmosTemplate cosmosTemplate;

    @Autowired
    UtilityComponent utilityComponent;

    /**
     * @param ciFiscalCode fiscal code of the CI
     * @param bundle       a valid bundle
     * @return a list of CI-Bundle filtered by fiscal Code
     */
    private static List<CiBundle> filterByCI(String ciFiscalCode, ValidBundle bundle) {
        return bundle.getCiBundleList() != null ? bundle.getCiBundleList().parallelStream()
                .filter(ciBundle -> ciFiscalCode.equals(ciBundle.getCiFiscalCode()))
                .collect(Collectors.toList())
                : null;
    }

    @Cacheable(value = "findValidBundles")
    public List<ValidBundle> findByPaymentOption(PaymentOption paymentOption) {
        Iterable<ValidBundle> validBundles = findValidBundles(paymentOption);

        // Gets the GLOBAL bundles and PRIVATE|PUBLIC bundles of the CI
        return getFilteredBundles(paymentOption.getPrimaryCreditorInstitution(), validBundles);
    }

    /**
     * Null value are ignored -> they are skipped when building the filters
     *
     * @param paymentOption Get the Body of the Request
     * @return the filtered bundles
     */
    private Iterable<ValidBundle> findValidBundles(PaymentOption paymentOption) {

        // add filter by Payment Amount: minPaymentAmount <= paymentAmount < maxPaymentAmount
        var minFilter = CriteriaBuilder.lessThanEqual("minPaymentAmount", paymentOption.getPaymentAmount());
        var maxFilter = CriteriaBuilder.greaterThan("maxPaymentAmount", paymentOption.getPaymentAmount());
        var queryResult = and(minFilter, maxFilter);

        // add filter by Touch Point: touchpoint=<value> || touchpoint==null
        if (paymentOption.getTouchpointName() != null) {
            var touchpointNameFilter = CriteriaBuilder.isEqualOrNull("name", paymentOption.getTouchpointName());
            Iterable<Touchpoint> touchpoint = cosmosTemplate.find(new CosmosQuery(touchpointNameFilter),
                    Touchpoint.class, "touchpoints");

            if(Iterables.size(touchpoint) == 0){
                throw new AppException(HttpStatus.NOT_FOUND,
                        "Touchpoint not found", "Cannot find touchpont with name: '" + paymentOption.getTouchpointName()+"'");
            }

            var touchpointFilter = isEqualOrNull("idTouchpoint", touchpoint.iterator().next().getId());
            queryResult = and(queryResult, touchpointFilter);
        }

        // add filter by Payment Method: paymentMethod=<value> || paymentMethod==null
        if (paymentOption.getPaymentMethod() != null) {
            var paymentMethodFilter = isEqualOrNull("paymentMethod", paymentOption.getPaymentMethod().getValue());
            queryResult = and(queryResult, paymentMethodFilter);
        }

        // add filter by PSP: psp in list
        if (paymentOption.getIdPspList() != null && !paymentOption.getIdPspList().isEmpty()) {
            var pspFilter = in("idPsp", paymentOption.getIdPspList());
            queryResult = and(queryResult, pspFilter);
        }

        // add filter by Transfer Category: transferCategory[] contains one of paymentOption
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

        // execute the query
        return cosmosTemplate.find(new CosmosQuery(queryResult), ValidBundle.class, "validbundles");
    }

    /**
     * This filter is made with Java (not with cosmos query)
     *
     * @param ciFiscalCode fiscal code of the primary CI
     * @param validBundles a valid bundle
     * @return the GLOBAL bundles and PRIVATE|PUBLIC bundles of the CI
     */
    private List<ValidBundle> getFilteredBundles(String ciFiscalCode, Iterable<ValidBundle> validBundles) {
        return StreamSupport.stream(validBundles.spliterator(), true)
                .filter(bundle -> {
                    // filter the ci-bundle list
                    bundle.setCiBundleList(filterByCI(ciFiscalCode, bundle));
                    return isGlobal(bundle) || belongsCI(bundle);
                })
                .collect(Collectors.toList());
    }

    private static boolean belongsCI(ValidBundle bundle) {
        return bundle != null && bundle.getCiBundleList() != null && !bundle.getCiBundleList().isEmpty();
    }

}
