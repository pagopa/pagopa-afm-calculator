package it.gov.pagopa.afm.calculator.repository;

import static it.gov.pagopa.afm.calculator.service.UtilityComponent.isGlobal;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.and;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.arrayContains;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.isEqual;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.isEqualOrAny;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.isEqualOrNull;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.isNull;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.or;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.azure.cosmos.implementation.guava25.collect.Iterables;
import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.query.CosmosQuery;
import com.azure.spring.data.cosmos.core.query.Criteria;

import it.gov.pagopa.afm.calculator.entity.CiBundle;
import it.gov.pagopa.afm.calculator.entity.PaymentType;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.PspSearchCriteria;
import it.gov.pagopa.afm.calculator.service.UtilityComponent;
import it.gov.pagopa.afm.calculator.util.CriteriaBuilder;

@Repository
public class CosmosRepository {

  @Autowired CosmosTemplate cosmosTemplate;

  @Autowired UtilityComponent utilityComponent;

  /**
   * @param ciFiscalCode fiscal code of the CI
   * @param bundle a valid bundle
   * @return a list of CI-Bundle filtered by fiscal Code
   */
  private static List<CiBundle> filterByCI(String ciFiscalCode, ValidBundle bundle) {
    return bundle.getCiBundleList() != null
        ? bundle.getCiBundleList().parallelStream()
            .filter(ciBundle -> ciFiscalCode.equals(ciBundle.getCiFiscalCode()))
            .collect(Collectors.toList())
            : null;
  }

  @Cacheable(value = "findValidBundles")
  public List<ValidBundle> findByPaymentOption(PaymentOption paymentOption) {
    Iterable<ValidBundle> validBundles = findValidBundles(paymentOption);
    return getFilteredBundles(paymentOption, validBundles);
  }

  /**
   * Null value are ignored -> they are skipped when building the filters
   *
   * @param paymentOption Get the Body of the Request
   * @return the filtered bundles
   */
  private Iterable<ValidBundle> findValidBundles(PaymentOption paymentOption) {

    // add filter by Payment Amount: minPaymentAmount <= paymentAmount < maxPaymentAmount
    var minFilter =
        CriteriaBuilder.lessThanEqual("minPaymentAmount", paymentOption.getPaymentAmount());
    var maxFilter =
        CriteriaBuilder.greaterThan("maxPaymentAmount", paymentOption.getPaymentAmount());
    var queryResult = and(minFilter, maxFilter);

    // add filter by Touch Point: touchpoint=<value> || touchpoint==null
    if (paymentOption.getTouchpoint() != null
        && !paymentOption.getTouchpoint().equalsIgnoreCase("any")) {
      var touchpointNameFilter = isEqualOrAny("name", paymentOption.getTouchpoint());
      Iterable<Touchpoint> touchpoint =
          cosmosTemplate.find(
              new CosmosQuery(touchpointNameFilter), Touchpoint.class, "touchpoints");

      if (Iterables.size(touchpoint) == 0) {
        throw new AppException(
            HttpStatus.NOT_FOUND,
            "Touchpoint not found",
            "Cannot find touchpont with name: '" + paymentOption.getTouchpoint() + "'");
      }

      var touchpointFilter = isEqualOrAny("touchpoint", touchpoint.iterator().next().getName());
      queryResult = and(queryResult, touchpointFilter);
    }

    // add filter by Payment Method: paymentMethod=<value> || paymentMethod==null
    if (paymentOption.getPaymentMethod() != null
        && !paymentOption.getPaymentMethod().equalsIgnoreCase("any")) {
      var paymentTypeNameFilter = isEqualOrNull("name", paymentOption.getPaymentMethod());
      Iterable<PaymentType> paymentType =
          cosmosTemplate.find(
              new CosmosQuery(paymentTypeNameFilter), PaymentType.class, "paymenttypes");

      if (Iterables.size(paymentType) == 0) {
        throw new AppException(
            HttpStatus.NOT_FOUND,
            "PaymentType not found",
            "Cannot find payment type with name: '" + paymentOption.getPaymentMethod() + "'");
      }

      var paymentTypeFilter = isEqualOrNull("paymentType", paymentType.iterator().next().getName());
      queryResult = and(queryResult, paymentTypeFilter);
    }

    // add filter by PSP: psp in list
    Iterator<PspSearchCriteria> iterator = Optional.ofNullable(paymentOption.getIdPspList()).orElse(Collections.<PspSearchCriteria>emptyList()).iterator();
    if(iterator.hasNext()){
      queryResult = and(queryResult, this.getPspFilterCriteria(iterator));
    }

    // add filter by Transfer Category: transferCategory[] contains one of paymentOption
    List<String> categoryList = utilityComponent.getTransferCategoryList(paymentOption);
    if (categoryList != null) {
      var taxonomyFilter =
          categoryList.parallelStream()
              .filter(Objects::nonNull)
              .filter(elem -> !elem.isEmpty())
              .map(elem -> arrayContains("transferCategoryList", elem))
              .reduce(CriteriaBuilder::or);

      if (taxonomyFilter.isPresent()) {
        var taxonomyOrNull = or(taxonomyFilter.get(), isNull("transferCategoryList"));
        queryResult = and(queryResult, taxonomyOrNull);
      }
    }
    // execute the query
    return cosmosTemplate.find(new CosmosQuery(queryResult), ValidBundle.class, "validbundles");
  }

  /**
   * These filters are done with Java (not with cosmos query)
   *
   * @param paymentOption the request
   * @param validBundles the valid bundles
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
   * @param transferListSize the number of transfer elements in the request
   * @param onlyMarcaBolloDigitale number of how many paymentOptions in the request has
   *     marcaBolloDigitale equals to True
   * @param bundle a valid bundle to filter
   * @return True if the valid bundle meets the criteria.
   */
  private static boolean digitalStampFilter(
      long transferListSize, long onlyMarcaBolloDigitale, ValidBundle bundle) {
    if (onlyMarcaBolloDigitale == transferListSize) {
      // if marcaBolloDigitale is present in all paymentOptions
      return bundle.getDigitalStamp();
    } else if (onlyMarcaBolloDigitale >= 1 && onlyMarcaBolloDigitale < transferListSize) {
      // if some paymentOptions have marcaBolloDigitale but others do not
      return bundle.getDigitalStamp() && !bundle.getDigitalStampRestriction();
    } else {
      // skip this filter
      return true;
    }
  }

  /**
   * Gets the GLOBAL bundles and PRIVATE|PUBLIC bundles of the CI
   *
   * @param paymentOption the request
   * @param bundle a valid bundle
   * @return True if the valid bundle meets the criteria.
   */
  private static boolean globalAndRelatedFilter(PaymentOption paymentOption, ValidBundle bundle) {
    // filter the ci-bundle list
    bundle.setCiBundleList(filterByCI(paymentOption.getPrimaryCreditorInstitution(), bundle));
    return isGlobal(bundle) || belongsCI(bundle);
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
  
  /**
   * @param iterator an iterator of PspSearchCriteria objects to generate filter criteria for the psp
   * @return Criteria an AND/OR concatenation of the global psp filter criteria
   */
  private Criteria getPspFilterCriteria(Iterator<PspSearchCriteria> iterator) {
    PspSearchCriteria pspSearch = iterator.next();
    var idPspFilter =  isEqual("idPsp", pspSearch.getIdPsp());
    var idChannelFilter = StringUtils.isNotEmpty(pspSearch.getIdChannel()) ? isEqual("idChannel", pspSearch.getIdChannel()):idPspFilter;
    var idBrokerPspFilter = StringUtils.isNotEmpty(pspSearch.getIdBrokerPsp()) ? isEqual("idBrokerPsp", pspSearch.getIdBrokerPsp()):idPspFilter;
    Criteria pspFilterCriteria = and(idPspFilter, and(idChannelFilter, idBrokerPspFilter));
    while(iterator.hasNext()){
      pspSearch = iterator.next();
      idPspFilter =  isEqual("idPsp", pspSearch.getIdPsp());
      idChannelFilter = StringUtils.isNotEmpty(pspSearch.getIdChannel()) ? isEqual("idChannel", pspSearch.getIdChannel()):idPspFilter;
      idBrokerPspFilter = StringUtils.isNotEmpty(pspSearch.getIdBrokerPsp()) ? isEqual("idBrokerPsp", pspSearch.getIdBrokerPsp()):idPspFilter;
      pspFilterCriteria = or(pspFilterCriteria, and(idPspFilter, and(idChannelFilter, idBrokerPspFilter)));
    }
    return pspFilterCriteria;
  }
}
