package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.PaymentMethod;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.exception.AppError;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.model.PaymentMethodResponse;
import it.gov.pagopa.afm.calculator.model.PaymentNoticeItem;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption;
import it.gov.pagopa.afm.calculator.model.paymentmethods.*;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.*;
import it.gov.pagopa.afm.calculator.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.repository.PaymentMethodRepository;
import it.gov.pagopa.afm.calculator.util.PaymentMethodComparatorUtil;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.internal.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class PaymentMethodsService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final CosmosRepository cosmosRepository;
    private final CalculatorService calculatorService;
    private final ModelMapper modelMapper;
    
    // Holds method-specific bundles separately from null-paymentType bundles used as wildcards.
    private static class GroupedValidBundles {
        private final Map<String, List<ValidBundle>> bundlesByPaymentType;
        private final List<ValidBundle> wildcardBundles;

        private GroupedValidBundles(
            Map<String, List<ValidBundle>> bundlesByPaymentType,
            List<ValidBundle> wildcardBundles
        ) {
            this.bundlesByPaymentType = bundlesByPaymentType;
            this.wildcardBundles = wildcardBundles;
        }

        public Map<String, List<ValidBundle>> getBundlesByPaymentType() {
            return bundlesByPaymentType;
        }

        public List<ValidBundle> getWildcardBundles() {
            return wildcardBundles;
        }
    }

    public PaymentMethodsResponse searchPaymentMethods(PaymentMethodRequest request) {
        List<PaymentMethodsItem> paymentMethodsItems = new ArrayList<>();
        
        // Map payment notices once and reuse them in all downstream calls.
        List<PaymentNoticeItem> paymentNoticeItems = request.getPaymentNotice().stream()
                .map(el -> modelMapper.map(el, PaymentNoticeItem.class))
                .toList();

        List<PaymentMethod> candidates = getPaymentMethodsCandidates(request);

        // Load bundles once without filtering by payment method.
        // The candidate-specific filtering is applied later in memory.
        List<ValidBundle> bundlesAllPaymentMethods = cosmosRepository.findByPaymentOption(
                PaymentOptionMulti.builder()
                        .paymentMethod(null)
                        .touchpoint(request.getUserTouchpoint().name())
                        .idPspList(null)
                        .paymentNotice(paymentNoticeItems)
                        .build(),
                request.getAllCCp()
        );

        GroupedValidBundles groupedBundles = groupingBundlesByPaymentMethods(bundlesAllPaymentMethods);

        for (PaymentMethod candidate : candidates) {
            Pair<PaymentMethodDisabledReason, PaymentMethodStatus> filterReason = filterByCandidateProperties(candidate, request);

            // Skip bundle calculation when the payment method is already excluded by static candidate properties.
            if (filterReason.getLeft() != null) {
                PaymentMethodsItem item = PaymentMethodsItem.builder()
                        .paymentMethodId(candidate.getPaymentMethodId())
                        .name(candidate.getName())
                        .description(candidate.getDescription())
                        .validityDateFrom(candidate.getValidityDateFrom())
                        .group(candidate.getGroup())
                        .paymentMethodTypes(candidate.getPaymentMethodTypes())
                        .metadata(candidate.getMetadata())
                        .feeRange(null)
                        .paymentMethodAsset(candidate.getPaymentMethodAsset())
                        .methodManagement(candidate.getMethodManagement())
                        .paymentMethodsBrandAssets(candidate.getPaymentMethodsBrandAssets())
                        .disabledReason(filterReason.getLeft())
                        .status(filterReason.getRight())
                        .build();
                paymentMethodsItems.add(item);
                continue;
            }

            // Merge method-specific bundles with null-paymentType bundles only for the current candidate.
            List<ValidBundle> candidateBundles = resolveBundlesForCandidate(candidate, groupedBundles);
            
            // Skip fee calculation when no candidate bundle is available after grouping.
            if (candidateBundles.isEmpty()) {
            	PaymentMethodsItem item = PaymentMethodsItem.builder()
            			.paymentMethodId(candidate.getPaymentMethodId())
            			.name(candidate.getName())
            			.description(candidate.getDescription())
            			.validityDateFrom(candidate.getValidityDateFrom())
            			.group(candidate.getGroup())
            			.paymentMethodTypes(candidate.getPaymentMethodTypes())
            			.metadata(candidate.getMetadata())
            			.feeRange(null)
            			.paymentMethodAsset(candidate.getPaymentMethodAsset())
            			.methodManagement(candidate.getMethodManagement())
            			.paymentMethodsBrandAssets(candidate.getPaymentMethodsBrandAssets())
            			.disabledReason(PaymentMethodDisabledReason.NO_BUNDLE_AVAILABLE)
            			.status(PaymentMethodStatus.DISABLED)
            			.build();
            	paymentMethodsItems.add(item);
            	continue;
            }

            BundleOption bundles = calculatorService.calculateForPaymentMethods(
                    candidateBundles,
                    PaymentOptionMulti.builder()
                            .paymentMethod(candidate.getGroup())
                            .touchpoint(request.getUserTouchpoint().name())
                            .idPspList(null)
                            .paymentNotice(paymentNoticeItems)
                            .build(),
                    Integer.MAX_VALUE, false, "fee");

            FeeRange feeRange = null;
            if (bundles == null || bundles.getBundleOptions() == null || bundles.getBundleOptions().isEmpty()) {
                filterReason = Pair.of(PaymentMethodDisabledReason.NO_BUNDLE_AVAILABLE, PaymentMethodStatus.DISABLED);
            } else {
                int last = bundles.getBundleOptions().size() - 1;
                Long minFee = bundles.getBundleOptions().get(0).getTaxPayerFee();
                Long maxFee = bundles.getBundleOptions().get(last).getTaxPayerFee();
                feeRange = FeeRange.builder()
                        .min(minFee)
                        .max(maxFee)
                        .build();
            }

            PaymentMethodsItem item = PaymentMethodsItem.builder()
                    .paymentMethodId(candidate.getPaymentMethodId())
                    .name(candidate.getName())
                    .description(candidate.getDescription())
                    .validityDateFrom(candidate.getValidityDateFrom())
                    .group(candidate.getGroup())
                    .paymentMethodTypes(candidate.getPaymentMethodTypes())
                    .metadata(candidate.getMetadata())
                    .feeRange(feeRange)
                    .paymentMethodAsset(candidate.getPaymentMethodAsset())
                    .methodManagement(candidate.getMethodManagement())
                    .paymentMethodsBrandAssets(candidate.getPaymentMethodsBrandAssets())
                    .disabledReason(filterReason.getLeft())
                    .status(filterReason.getRight())
                    .build();
            paymentMethodsItems.add(item);
        }

        // sorting
        PaymentMethodComparatorUtil.sortMethods(request, paymentMethodsItems);

        return PaymentMethodsResponse.builder()
                .paymentMethods(paymentMethodsItems)
                .build();
    }



    public PaymentMethodResponse getPaymentMethod(String paymentMethodId) {
        List<PaymentMethod> result = paymentMethodRepository.findByPaymentMethodId(paymentMethodId);
        if (result.isEmpty()) {
            throw new AppException(AppError.PAYMENT_METHOD_NOT_FOUND, paymentMethodId);
        }
        if (result.size() > 1) {
            throw new AppException(AppError.PAYMENT_METHOD_MULTIPLE_FOUND, paymentMethodId);
        }
        return modelMapper.map(result.get(0), PaymentMethodResponse.class);
    }

    private List<PaymentMethod> getPaymentMethodsCandidates(PaymentMethodRequest request) {
        if (request.getUserDevice() == null) {
            return paymentMethodRepository
                    .findByTouchpoint(request.getUserTouchpoint().name());
        }

        return paymentMethodRepository
                .findByTouchpointAndDevice(request.getUserTouchpoint().name(), request.getUserDevice().name());
    }

    private Pair<PaymentMethodDisabledReason, PaymentMethodStatus> filterByCandidateProperties(PaymentMethod candidate, PaymentMethodRequest request) {
        // Target filtering
        List<String> targetRegex = candidate.getTarget();
        if (targetRegex != null && !targetRegex.contains(request.getTargetKey())) {
            return Pair.of(PaymentMethodDisabledReason.TARGET_PREVIEW, PaymentMethodStatus.DISABLED);
        }

        // amount filtering
        if (request.getTotalAmount() < candidate.getRangeAmount().getMin() || request.getTotalAmount() > candidate.getRangeAmount().getMax()) {
            return Pair.of(PaymentMethodDisabledReason.AMOUNT_OUT_OF_BOUND, PaymentMethodStatus.DISABLED);
        }

        // validity date filtering
        if (candidate.getValidityDateFrom().isAfter(LocalDate.now())) {
            return Pair.of(PaymentMethodDisabledReason.NOT_YET_VALID, PaymentMethodStatus.DISABLED);
        }

        // disabled filtering
        if (candidate.getStatus() == PaymentMethodStatus.DISABLED) {
            return Pair.of(PaymentMethodDisabledReason.METHOD_DISABLED, PaymentMethodStatus.DISABLED);
        }

        // maintenance filtering
        if (candidate.getStatus() == PaymentMethodStatus.MAINTENANCE) {
            return Pair.of(PaymentMethodDisabledReason.MAINTENANCE_IN_PROGRESS, PaymentMethodStatus.MAINTENANCE);
        }

        return Pair.of(null, candidate.getStatus());
    }

    private GroupedValidBundles groupingBundlesByPaymentMethods(List<ValidBundle> bundleList) {
        Map<String, List<ValidBundle>> groupedMap = new HashMap<>();
        List<ValidBundle> wildcardBundles = new ArrayList<>();

        for (ValidBundle bundle : bundleList) {
            // Only null paymentType is considered a wildcard.
            if (bundle.getPaymentType() == null) {
                wildcardBundles.add(bundle);
            } else {
                groupedMap.computeIfAbsent(bundle.getPaymentType(), key -> new ArrayList<>()).add(bundle);
            }
        }

        return new GroupedValidBundles(groupedMap, wildcardBundles);
    }
    
    private List<ValidBundle> resolveBundlesForCandidate(
    		PaymentMethod candidate,
    		GroupedValidBundles groupedBundles
    		) {
    	// only exact paymentType matches and null-paymentType bundles are allowed.
    	String paymentType = candidate.getGroup();

    	List<ValidBundle> specificBundles = paymentType == null
    			? Collections.emptyList()
    					: groupedBundles.getBundlesByPaymentType().getOrDefault(paymentType, Collections.emptyList());

    	List<ValidBundle> wildcardBundles = groupedBundles.getWildcardBundles();

    	if (specificBundles.isEmpty()) {
    		return wildcardBundles;
    	}

    	if (wildcardBundles.isEmpty()) {
    		return specificBundles;
    	}

    	List<ValidBundle> resolvedBundles = new ArrayList<>(specificBundles.size() + wildcardBundles.size());
    	resolvedBundles.addAll(specificBundles);
    	resolvedBundles.addAll(wildcardBundles);

    	return resolvedBundles;
    }
    
}