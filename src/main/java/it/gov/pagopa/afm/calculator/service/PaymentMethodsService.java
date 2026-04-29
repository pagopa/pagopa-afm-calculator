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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PaymentMethodsService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final CosmosRepository cosmosRepository;
    private final CalculatorService calculatorService;
    private final ModelMapper modelMapper;

    public PaymentMethodsResponse searchPaymentMethods(PaymentMethodRequest request) {
        List<PaymentMethodsItem> paymentMethodsItems = new ArrayList<>();

        List<PaymentNoticeItem> paymentNoticeItems = request.getPaymentNotice().stream()
                .map(el -> modelMapper.map(el, PaymentNoticeItem.class))
                .toList();

        List<PaymentMethod> candidates = getPaymentMethodsCandidates(request);

        boolean allCcp = Boolean.TRUE.equals(request.getAllCCp());

        List<ValidBundle> bundlesAllPaymentMethods = cosmosRepository.findByPaymentOption(
                PaymentOptionMulti.builder()
                        .paymentMethod(null)
                        .touchpoint(request.getUserTouchpoint().name())
                        .idPspList(null)
                        .paymentNotice(paymentNoticeItems)
                        .build(),
                allCcp
        );

        Map<String, List<ValidBundle>> bundleMap = groupingBundlesByPaymentMethods(bundlesAllPaymentMethods);
        List<ValidBundle> wildCardBundle = bundleMap.getOrDefault(null, new ArrayList<>());

        for (PaymentMethod candidate : candidates) {
            paymentMethodsItems.add(buildPaymentMethodsItem(
                    candidate,
                    request,
                    paymentNoticeItems,
                    bundleMap,
                    wildCardBundle
            ));
        }

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

    private Map<String, List<ValidBundle>> groupingBundlesByPaymentMethods(List<ValidBundle> bundleList){
      List<ValidBundle> wildcards = new ArrayList<>();
      List<ValidBundle> specifics = new ArrayList<>();

      for (ValidBundle bundle : bundleList) {
    	  if (bundle.getPaymentType() == null) {
    		  wildcards.add(bundle);
    	  } else {
    		  specifics.add(bundle);
    	  }
      }

      Map<String, List<ValidBundle>> groupedMap = specifics.stream()
          .collect(Collectors.groupingBy(ValidBundle::getPaymentType));

      if (!wildcards.isEmpty() && !groupedMap.isEmpty()) {
    	  for (List<ValidBundle> groupList : groupedMap.values()) {
    		  groupList.addAll(wildcards);
    	  }
      }
      groupedMap.put(null, new ArrayList<>(wildcards));

      return groupedMap;
    }
    
    private PaymentMethodsItem buildPaymentMethodItem(
            PaymentMethod candidate,
            FeeRange feeRange,
            Pair<PaymentMethodDisabledReason, PaymentMethodStatus> filterReason
    ) {
        return PaymentMethodsItem.builder()
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
    }
    
    private PaymentMethodsItem buildPaymentMethodsItem(
            PaymentMethod candidate,
            PaymentMethodRequest request,
            List<PaymentNoticeItem> paymentNoticeItems,
            Map<String, List<ValidBundle>> bundleMap,
            List<ValidBundle> wildCardBundle
    ) {
        Pair<PaymentMethodDisabledReason, PaymentMethodStatus> filterReason =
                filterByCandidateProperties(candidate, request);

        FeeRange feeRange = null;

        if (filterReason.getLeft() == null) {
            List<ValidBundle> candidateBundles = bundleMap.getOrDefault(candidate.getGroup(), wildCardBundle);

            if (candidateBundles.isEmpty()) {
                filterReason = Pair.of(
                        PaymentMethodDisabledReason.NO_BUNDLE_AVAILABLE,
                        PaymentMethodStatus.DISABLED
                );
            } else {
                BundleOption bundles = calculatorService.calculateForPaymentMethods(
                        candidateBundles,
                        PaymentOptionMulti.builder()
                                .paymentMethod(candidate.getGroup())
                                .touchpoint(request.getUserTouchpoint().name())
                                .idPspList(null)
                                .paymentNotice(paymentNoticeItems)
                                .build(),
                        Integer.MAX_VALUE,
                        false,
                        "fee"
                );

                if (bundles == null || bundles.getBundleOptions() == null || bundles.getBundleOptions().isEmpty()) {
                    filterReason = Pair.of(
                            PaymentMethodDisabledReason.NO_BUNDLE_AVAILABLE,
                            PaymentMethodStatus.DISABLED
                    );
                } else {
                    feeRange = buildFeeRange(bundles);
                }
            }
        }

        return buildPaymentMethodItem(candidate, feeRange, filterReason);
    }
    
    private FeeRange buildFeeRange(BundleOption bundles) {
        int last = bundles.getBundleOptions().size() - 1;

        return FeeRange.builder()
                .min(bundles.getBundleOptions().get(0).getTaxPayerFee())
                .max(bundles.getBundleOptions().get(last).getTaxPayerFee())
                .build();
    }
}
