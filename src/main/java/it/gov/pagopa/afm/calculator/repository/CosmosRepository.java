package it.gov.pagopa.afm.calculator.repository;

import com.azure.cosmos.implementation.guava25.collect.Iterables;
import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.query.CosmosQuery;
import com.azure.spring.data.cosmos.core.query.Criteria;
import com.azure.spring.data.cosmos.core.query.CriteriaType;
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
import it.gov.pagopa.afm.calculator.util.CriteriaBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static it.gov.pagopa.afm.calculator.service.UtilityComponent.isGlobal;
import static it.gov.pagopa.afm.calculator.util.CriteriaBuilder.*;

@Repository
public class CosmosRepository {

  @Autowired CosmosTemplate cosmosTemplate;

  @Autowired UtilityComponent utilityComponent;

  @Value("${pspPoste.id}")
  private String pspPosteId;

  @Value("#{'${psp.blacklist}'.split(',')}")
  private List<String> pspBlacklist;
  
  private static final String ID_PSP_PARAM = "idPsp";

  private static final String TRANSFER_CATEGORY_LIST = "transferCategoryList";

  private static final String CART_PARAM = "cart";

  /**
   * @param ciFiscalCode fiscal code of the CI
   * @param bundle a valid bundle
   * @return a list of CI-Bundle filtered by fiscal Code
   */
  private static List<CiBundle> filterByCI(String ciFiscalCode, ValidBundle bundle) {
    return bundle.getCiBundleList() != null
        ? bundle.getCiBundleList().parallelStream()
            .filter(ciBundle -> ciFiscalCode.equals(ciBundle.getCiFiscalCode()))
            .toList()
        : null;
  }

  @Cacheable(value = "findValidBundles")
  public List<ValidBundle> findByPaymentOption(PaymentOption paymentOption, boolean allCcp) {
    Iterable<ValidBundle> validBundles = findValidBundles(paymentOption, allCcp);
    return getFilteredBundles(paymentOption, validBundles);
  }

  @Cacheable(value = "findValidBundlesMulti")
  public List<ValidBundle> findByPaymentOption(PaymentOptionMulti paymentOption, boolean allCcp) {
    Iterable<ValidBundle> validBundles = findValidBundlesMulti(paymentOption, allCcp);
    return getFilteredBundlesMulti(paymentOption, validBundles);
  }

  /**
   * Null value are ignored -> they are skipped when building the filters
   *
   * @param paymentOptionMulti Get the Body of the Request
   * @return the filtered bundles
   */
  private Iterable<ValidBundle> findValidBundlesMulti(PaymentOptionMulti paymentOptionMulti, boolean allCcp) {

    // add filter by Payment Amount: minPaymentAmount <= paymentAmount < maxPaymentAmount
    var minFilter =
        CriteriaBuilder.lessThan("minPaymentAmount", paymentOptionMulti.getPaymentAmount());
    var maxFilter =
        CriteriaBuilder.greaterThanEqual("maxPaymentAmount", paymentOptionMulti.getPaymentAmount());
    var queryResult = and(minFilter, maxFilter);
    // add filter by Touch Point: touchpoint=<value> || touchpoint==null
    if (paymentOptionMulti.getTouchpoint() != null
        && !paymentOptionMulti.getTouchpoint().equalsIgnoreCase("any")) {
      var touchpointNameFilter = isEqualOrAny("name", paymentOptionMulti.getTouchpoint());
      Iterable<Touchpoint> touchpoint =
          cosmosTemplate.find(
              new CosmosQuery(touchpointNameFilter), Touchpoint.class, "touchpoints");

      if (Iterables.size(touchpoint) == 0) {
        throw new AppException(
            HttpStatus.NOT_FOUND,
            "Touchpoint not found",
            "Cannot find touchpont with name: '" + paymentOptionMulti.getTouchpoint() + "'");
      }

      var touchpointFilter = isEqualOrAny("touchpoint", touchpoint.iterator().next().getName());
      queryResult = and(queryResult, touchpointFilter);
    }

    // add filter by Payment Method: paymentMethod=<value> || paymentMethod==null
    if (paymentOptionMulti.getPaymentMethod() != null
        && !paymentOptionMulti.getPaymentMethod().equalsIgnoreCase("any")) {
      var paymentTypeNameFilter = isEqualOrNull("name", paymentOptionMulti.getPaymentMethod());
      Iterable<PaymentType> paymentType =
          cosmosTemplate.find(
              new CosmosQuery(paymentTypeNameFilter), PaymentType.class, "paymenttypes");

      if (Iterables.size(paymentType) == 0) {
        throw new AppException(
            HttpStatus.NOT_FOUND,
            "PaymentType not found",
            "Cannot find payment type with name: '" + paymentOptionMulti.getPaymentMethod() + "'");
      }

      var paymentTypeFilter = isEqualOrNull("paymentType", paymentType.iterator().next().getName());
      queryResult = and(queryResult, paymentTypeFilter);
    }

    // add filter by PSP: psp in list
    Iterator<PspSearchCriteria> iterator =
        Optional.ofNullable(paymentOptionMulti.getIdPspList())
            .orElse(Collections.<PspSearchCriteria>emptyList())
            .iterator();
    if (iterator.hasNext()) {
      queryResult = this.getPspFilterCriteria(queryResult, iterator);
    }

    // add filter by Transfer Category: transferCategory[] contains one of paymentOption
    List<String> categoryListMulti = utilityComponent.getTransferCategoryList(paymentOptionMulti);
    if (categoryListMulti != null) {
      var taxonomyFilter =
          categoryListMulti.parallelStream()
              .filter(Objects::nonNull)
              .filter(elem -> !elem.isEmpty())
              .map(elem -> arrayContains(TRANSFER_CATEGORY_LIST, elem))
              .reduce(CriteriaBuilder::or);

      if (taxonomyFilter.isPresent()) {
        var taxonomyOrNull = or(taxonomyFilter.get(), isNull(TRANSFER_CATEGORY_LIST));
        queryResult = and(queryResult, taxonomyOrNull);
      }
    }

    // add filter for Poste bundles
    if (!allCcp) {
      var allCcpFilter = isNotEqual(ID_PSP_PARAM, pspPosteId);
      queryResult = and(queryResult, allCcpFilter);
    }

    // add filter for PSP blacklist
    queryResult = blackListCriteria(queryResult);

    // add filter for cart bundle param
    if (paymentOptionMulti.getPaymentNotice().size() > 1) {
      var queryCart = Criteria.getInstance(
              CriteriaType.IS_EQUAL, CART_PARAM, Collections.singletonList(Boolean.TRUE), Part.IgnoreCaseType.NEVER);
      queryResult = and(queryResult, queryCart);
    }

    // execute the query
    return cosmosTemplate.find(new CosmosQuery(queryResult), ValidBundle.class, "validbundles");
  }

  /**
   * Null value are ignored -> they are skipped when building the filters
   *
   * @param paymentOption Get the Body of the Request
   * @return the filtered bundles
   */
  private Iterable<ValidBundle> findValidBundles(PaymentOption paymentOption, boolean allCcp) {

    // add filter by Payment Amount: minPaymentAmount <= paymentAmount < maxPaymentAmount
    var minFilter =
        CriteriaBuilder.lessThan("minPaymentAmount", paymentOption.getPaymentAmount());
    var maxFilter =
        CriteriaBuilder.greaterThanEqual("maxPaymentAmount", paymentOption.getPaymentAmount());
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
    Iterator<PspSearchCriteria> iterator =
        Optional.ofNullable(paymentOption.getIdPspList())
            .orElse(Collections.<PspSearchCriteria>emptyList())
            .iterator();
    if (iterator.hasNext()) {
      queryResult = this.getPspFilterCriteria(queryResult, iterator);
    }

    // add filter by Transfer Category: transferCategory[] contains one of paymentOption
    List<String> categoryList = utilityComponent.getTransferCategoryList(paymentOption);
    if (categoryList != null) {
      var taxonomyFilter =
          categoryList.parallelStream()
              .filter(Objects::nonNull)
              .filter(elem -> !elem.isEmpty())
              .map(elem -> arrayContains(TRANSFER_CATEGORY_LIST, elem))
              .reduce(CriteriaBuilder::or);

      if (taxonomyFilter.isPresent()) {
        var taxonomyOrNull = or(taxonomyFilter.get(), isNull(TRANSFER_CATEGORY_LIST));
        queryResult = and(queryResult, taxonomyOrNull);
      }
    }

    // add filter for Poste bundles
    if (!allCcp) {
      var allCcpFilter = isNotEqual(ID_PSP_PARAM, pspPosteId);
      queryResult = and(queryResult, allCcpFilter);
    }

    // add filter for PSP blacklist
    queryResult = blackListCriteria(queryResult);

    // execute the query
    return cosmosTemplate.find(new CosmosQuery(queryResult), ValidBundle.class, "validbundles");
  }

  /**
   * These filters are done with Java (not with cosmos query)
   *
   * @param paymentOptionMulti the request
   * @param validBundles the valid bundles
   * @return the GLOBAL bundles and PRIVATE|PUBLIC bundles of the CI
   */
  private List<ValidBundle> getFilteredBundlesMulti(
      PaymentOptionMulti paymentOptionMulti, Iterable<ValidBundle> validBundles) {

    // marca da bollo digitale check
    List<TransferListItem> transferList = new ArrayList<>();
    paymentOptionMulti.getPaymentNotice().forEach(paymentNoticeItem -> transferList.addAll(paymentNoticeItem.getTransferList()));
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
        .collect(Collectors.toList());
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
    boolean digitalStamp =
        bundle.getDigitalStamp() != null ? bundle.getDigitalStamp() : Boolean.FALSE;
    boolean digitalStampRestriction =
        bundle.getDigitalStampRestriction() != null
            ? bundle.getDigitalStampRestriction()
            : Boolean.FALSE;
    if (onlyMarcaBolloDigitale == transferListSize) {
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
   * @param bundle a valid bundle
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
   * @param bundle a valid bundle
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
   * @param bundle a valid bundle
   * @return empty list if at least one element is not present, otherwise the full list
   */
  private static List<CiBundle> filteredCiBundles(PaymentOptionMulti paymentOptionMulti, ValidBundle bundle) {
    if(bundle.getCiBundleList() != null) {
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

  /**
   * Criteria an AND/OR concatenation of the global psp filter criteria
   *
   * @param queryResult query to modify
   * @param iterator an iterator of PspSearchCriteria objects to generate filter criteria for the
   *     psp
   * @return the actual query
   */
  private Criteria getPspFilterCriteria(
      Criteria queryResult, Iterator<PspSearchCriteria> iterator) {
    Criteria queryTmp = null;
    while (iterator.hasNext()) {
      var pspSearch = iterator.next();
      var queryItem = isEqual(ID_PSP_PARAM, pspSearch.getIdPsp());
      if (StringUtils.isNotEmpty(pspSearch.getIdChannel())) {
        queryItem = and(queryItem, isEqual("idChannel", pspSearch.getIdChannel()));
      }
      if (StringUtils.isNotEmpty(pspSearch.getIdBrokerPsp())) {
        queryItem = and(queryItem, isEqual("idBrokerPsp", pspSearch.getIdBrokerPsp()));
      }
      if (queryTmp == null) {
        queryTmp = queryItem;
      } else {
        queryTmp = or(queryTmp, queryItem);
      }
    }
    return queryTmp != null ? and(queryResult, queryTmp) : queryResult;
  }

  private Criteria blackListCriteria(Criteria queryResult) {
    // add filter for PSP blacklist
    if (!CollectionUtils.isEmpty(pspBlacklist)) {
      var pspNotIn = notIn(ID_PSP_PARAM, pspBlacklist);
      queryResult = and(queryResult, pspNotIn);
    }
    return queryResult;
  }
}
