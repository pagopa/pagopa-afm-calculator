package it.gov.pagopa.afm.calculator.repository;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
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
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class CosmosRepository {
    private static final String ID_PSP_PARAM = "idPsp";
    private static final String TRANSFER_CATEGORY_LIST = "transferCategoryList";
    private static final String CART_PARAM = "cart";
    private static final String VALID_BUNDLES_CONTAINER = "validbundles";
    private final CosmosTemplate cosmosTemplate;
    private final TouchpointRepository touchpointRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final UtilityComponent utilityComponent;
    private final String pspPosteId;
    private final List<String> pspBlacklist;

    public CosmosRepository(
            CosmosTemplate cosmosTemplate,
            TouchpointRepository touchpointRepository,
            PaymentTypeRepository paymentTypeRepository,
            UtilityComponent utilityComponent,
            @Value("${pspPoste.id}") String pspPosteId,
            @Value("#{'${psp.blacklist}'.split(',')}") List<String> pspBlacklist
    ) {
        this.cosmosTemplate = cosmosTemplate;
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
    public static boolean digitalStampFilter(
            long transferListSize, long onlyMarcaBolloDigitale, ValidBundle bundle) {
        boolean digitalStamp =
                bundle.getDigitalStamp() != null ? bundle.getDigitalStamp() : Boolean.FALSE;
        boolean digitalStampRestriction =
                bundle.getDigitalStampRestriction() != null
                        ? bundle.getDigitalStampRestriction()
                        : Boolean.FALSE;
        if(transferListSize == 0 && onlyMarcaBolloDigitale == 0){
            // skip this filter
            return true;
        }
        else if (onlyMarcaBolloDigitale == transferListSize) {
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

    /*
    public List<ValidBundle> findAllValidBundles() {
        // Load the full valid bundles dataset to support in-memory filtering.
        Iterable<ValidBundle> validBundles = cosmosTemplate.findAll(VALID_BUNDLES_CONTAINER, ValidBundle.class);
        // Materialize the full snapshot as an immutable list for safe in-memory reuse.
        return StreamSupport.stream(validBundles.spliterator(), false).toList();
    }*/
    
    public List<ValidBundle> findAllValidBundles() {
        long start = System.currentTimeMillis();
        long lastProgressLogTime = start;
        long lastItemTime = start;

        log.info("Starting full validbundles load from Cosmos container '{}'", VALID_BUNDLES_CONTAINER);

        try {
            long beforeFindAll = System.currentTimeMillis();

            Iterable<ValidBundle> validBundles =
                    cosmosTemplate.findAll(VALID_BUNDLES_CONTAINER, ValidBundle.class);

            long afterFindAll = System.currentTimeMillis();

            log.info(
                    "Cosmos findAll() returned Iterable. findAllDurationMs={}, elapsedMs={}",
                    afterFindAll - beforeFindAll,
                    afterFindAll - start
            );

            long beforeIterator = System.currentTimeMillis();
            Iterator<ValidBundle> iterator = validBundles.iterator();
            long afterIterator = System.currentTimeMillis();

            log.info(
                    "Cosmos Iterable iterator created. iteratorCreationDurationMs={}, elapsedMs={}",
                    afterIterator - beforeIterator,
                    afterIterator - start
            );

            List<ValidBundle> result = new ArrayList<>();
            int count = 0;

            long slowHasNextCount = 0;
            long slowNextCount = 0;
            long maxHasNextMs = 0;
            long maxNextMs = 0;
            long totalHasNextMs = 0;
            long totalNextMs = 0;

            while (true) {
                long beforeHasNext = System.currentTimeMillis();
                boolean hasNext = iterator.hasNext();
                long hasNextMs = System.currentTimeMillis() - beforeHasNext;

                totalHasNextMs += hasNextMs;
                maxHasNextMs = Math.max(maxHasNextMs, hasNextMs);

                if (hasNextMs > 1_000) {
                    slowHasNextCount++;
                    log.warn(
                            "Slow Cosmos iterator.hasNext(). durationMs={}, itemCount={}, elapsedMs={}",
                            hasNextMs,
                            count,
                            System.currentTimeMillis() - start
                    );
                }

                if (!hasNext) {
                    break;
                }

                long beforeNext = System.currentTimeMillis();
                ValidBundle bundle = iterator.next();
                long nextMs = System.currentTimeMillis() - beforeNext;

                totalNextMs += nextMs;
                maxNextMs = Math.max(maxNextMs, nextMs);

                if (nextMs > 1_000) {
                    slowNextCount++;
                    log.warn(
                            "Slow Cosmos iterator.next(). durationMs={}, itemCount={}, elapsedMs={}",
                            nextMs,
                            count,
                            System.currentTimeMillis() - start
                    );
                }

                if (bundle == null) {
                    log.warn("Null ValidBundle found at itemCount={}", count);
                } else if (count < 5) {
                    log.info(
                            "Sample validbundle loaded. index={}, id={}, idPsp={}, paymentType={}, touchpoint={}, type={}, minAmount={}, maxAmount={}",
                            count,
                            bundle.getId(),
                            bundle.getIdPsp(),
                            bundle.getPaymentType(),
                            bundle.getTouchpoint(),
                            bundle.getType(),
                            bundle.getMinPaymentAmount(),
                            bundle.getMaxPaymentAmount()
                    );
                }

                result.add(bundle);
                count++;

                long now = System.currentTimeMillis();

                if (count % 100 == 0) {
                    log.info(
                            "Validbundles materialization progress. count={}, elapsedMs={}, last100ElapsedMs={}, avgItemsPerSecond={}",
                            count,
                            now - start,
                            now - lastProgressLogTime,
                            count * 1000.0 / Math.max(1, now - start)
                    );
                    lastProgressLogTime = now;
                }

                if (now - lastItemTime > 10_000) {
                    log.warn(
                            "Long gap during validbundles materialization. count={}, gapMs={}, elapsedMs={}",
                            count,
                            now - lastItemTime,
                            now - start
                    );
                }

                lastItemTime = now;
            }

            long end = System.currentTimeMillis();

            log.info(
                    "Completed validbundles materialization. totalItems={}, totalTimeMs={}, avgItemsPerSecond={}, maxHasNextMs={}, maxNextMs={}, slowHasNextCount={}, slowNextCount={}, totalHasNextMs={}, totalNextMs={}",
                    result.size(),
                    end - start,
                    result.size() * 1000.0 / Math.max(1, end - start),
                    maxHasNextMs,
                    maxNextMs,
                    slowHasNextCount,
                    slowNextCount,
                    totalHasNextMs,
                    totalNextMs
            );

            if (result.isEmpty()) {
                log.error("Validbundles materialization completed with empty result");
                throw new IllegalStateException("Valid bundles full load returned an empty dataset");
            }

            return List.copyOf(result);

        } catch (Exception e) {
            log.error(
                    "Failed full validbundles load from Cosmos container '{}'. elapsedMs={}",
                    VALID_BUNDLES_CONTAINER,
                    System.currentTimeMillis() - start,
                    e
            );
            throw e;
        }
    }
    
   

    @Cacheable(value = "findValidBundles")
    public List<ValidBundle> findByPaymentOption(PaymentOption paymentOption, boolean allCcp) {
        Iterable<ValidBundle> validBundles = findValidBundles(paymentOption, allCcp);
        return getFilteredBundles(paymentOption, validBundles);
    }

    @Cacheable(value = "findValidBundlesMulti")
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
    private Iterable<ValidBundle> findValidBundlesMulti(PaymentOptionMulti paymentOptionMulti, Boolean allCcp) {

        // add filter by Payment Amount: minPaymentAmount <= paymentAmount < maxPaymentAmount
        var minFilter =
                CriteriaBuilder.lessThan("minPaymentAmount", paymentOptionMulti.getPaymentAmount());
        var maxFilter =
                CriteriaBuilder.greaterThanEqual("maxPaymentAmount", paymentOptionMulti.getPaymentAmount());
        var queryResult = and(minFilter, maxFilter);
        // add filter by Touch Point: touchpoint=<value> || touchpoint==null
        if (paymentOptionMulti.getTouchpoint() != null
                && !paymentOptionMulti.getTouchpoint().equalsIgnoreCase("any")) {
            Optional<Touchpoint> touchpoint = touchpointRepository.findByName(paymentOptionMulti.getTouchpoint());

            if (touchpoint.isEmpty()) {
                throw new AppException(
                        HttpStatus.NOT_FOUND,
                        "Touchpoint not found",
                        "Cannot find touchpoint with name: '" + paymentOptionMulti.getTouchpoint() + "'");
            }
            var touchpointFilter = isEqualOrAny("touchpoint", touchpoint.get().getName());
            queryResult = and(queryResult, touchpointFilter);
        }

        // add filter by Payment Method: paymentMethod=<value> || paymentMethod==null
        if (paymentOptionMulti.getPaymentMethod() != null
                && !paymentOptionMulti.getPaymentMethod().equalsIgnoreCase("any")) {
            Optional<PaymentType> paymentType = paymentTypeRepository.findByName(paymentOptionMulti.getPaymentMethod());

            if (paymentType.isEmpty()) {
                throw new AppException(
                        HttpStatus.NOT_FOUND,
                        "PaymentType not found",
                        "Cannot find payment type with name: '" + paymentOptionMulti.getPaymentMethod() + "'");
            }

            var paymentTypeFilter = isEqualOrNull("paymentType", paymentType.get().getName());
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
            } else {
                queryResult = and(queryResult, isNull(TRANSFER_CATEGORY_LIST));
            }
        }

        // add filter for Poste bundles
        if (Boolean.FALSE.equals(allCcp)) {
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
        return cosmosTemplate.find(new CosmosQuery(queryResult), ValidBundle.class, VALID_BUNDLES_CONTAINER);
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
            Optional<Touchpoint> touchpoint = touchpointRepository.findByName(paymentOption.getTouchpoint());

            if (touchpoint.isEmpty()) {
                throw new AppException(
                        HttpStatus.NOT_FOUND,
                        "Touchpoint not found",
                        "Cannot find touchpont with name: '" + paymentOption.getTouchpoint() + "'");
            }
            var touchpointFilter = isEqualOrAny("touchpoint", touchpoint.get().getName());
            queryResult = and(queryResult, touchpointFilter);
        }

        // add filter by Payment Method: paymentMethod=<value> || paymentMethod==null
        if (paymentOption.getPaymentMethod() != null
                && !paymentOption.getPaymentMethod().equalsIgnoreCase("any")) {
            Optional<PaymentType> paymentType = paymentTypeRepository.findByName(paymentOption.getPaymentMethod());

            if (paymentType.isEmpty()) {
                throw new AppException(
                        HttpStatus.NOT_FOUND,
                        "PaymentType not found",
                        "Cannot find payment type with name: '" + paymentOption.getPaymentMethod() + "'");
            }

            var paymentTypeFilter = isEqualOrNull("paymentType", paymentType.get().getName());
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
            } else {
                queryResult = and(queryResult, isNull(TRANSFER_CATEGORY_LIST));
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
        return cosmosTemplate.find(new CosmosQuery(queryResult), ValidBundle.class, VALID_BUNDLES_CONTAINER);
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
     * Criteria an AND/OR concatenation of the global psp filter criteria
     *
     * @param queryResult query to modify
     * @param iterator    an iterator of PspSearchCriteria objects to generate filter criteria for the
     *                    psp
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
