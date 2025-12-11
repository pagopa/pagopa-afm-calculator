package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.PaymentMethod;
import it.gov.pagopa.afm.calculator.exception.AppError;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.model.PaymentMethodResponse;
import it.gov.pagopa.afm.calculator.model.PaymentNoticeItem;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption;
import it.gov.pagopa.afm.calculator.model.paymentmethods.*;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.*;
import it.gov.pagopa.afm.calculator.repository.PaymentMethodRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.internal.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class PaymentMethodsService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final CalculatorService calculatorService;
    private final ModelMapper modelMapper;

    public PaymentMethodsResponse searchPaymentMethods(PaymentMethodRequest request) {
        List<PaymentMethodsItem> paymentMethodsItems = new ArrayList<>();

        List<PaymentMethod> candidates = getPaymentMethodsCandidates(request);

        for (PaymentMethod candidate : candidates) {
            Pair<PaymentMethodDisabledReason, PaymentMethodStatus> filterReason = filterByCandidateProperties(candidate, request);

            BundleOption bundles = calculatorService.calculateMulti(PaymentOptionMulti.builder()
                            .paymentMethod(candidate.getGroup().name())
                            .touchpoint(request.getUserTouchpoint().name())
                            .idPspList(null)
                            .paymentNotice(request.getPaymentNotice().stream().map(el -> modelMapper.map(el, PaymentNoticeItem.class)).toList())
                            .build(),
                    Integer.MAX_VALUE, request.getAllCCp(), false, "fee");

            // filter by bundles
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
        sortMethods(request, paymentMethodsItems);

        return PaymentMethodsResponse.builder()
                .paymentMethods(paymentMethodsItems)
                .build();
    }

    /**
     * Sort payment methods items based on request parameters
     *
     * @param request             the payment method request with sorting parameters
     * @param paymentMethodsItems the list of payment methods items to sort
     */
    private static void sortMethods(PaymentMethodRequest request, List<PaymentMethodsItem> paymentMethodsItems) {
        // create the comparator based on priority groups
        Comparator<PaymentMethodsItem> comparator = comparatorPriorityGroups(request);
        // adjust the comparator based on sortBy
        comparator = comparatorSortBy(comparator, request);
        // adjust the comparator based on sortOrder
        comparator = comparatorSortOrder(request.getSortOrder(), comparator);

        // sort the payment methods items
        paymentMethodsItems.sort(comparator);
    }

    /**
     * This method adjusts the comparator based on the sort order.
     *
     * @param sortOrder  the desired sort order (ASC or DESC)
     * @param comparator the final comparator
     * @return a comparator that sorts in the specified order
     */
    private static Comparator<PaymentMethodsItem> comparatorSortOrder(SortOrder sortOrder, Comparator<PaymentMethodsItem> comparator) {
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    /**
     * This method returns a comparator based on the sortBy parameter in the request.
     *
     * @param comparator the initial comparator
     * @param request    the payment method request containing the sortBy parameter
     * @return a comparator that sorts payment methods based on the specified attribute
     */
    private static Comparator<PaymentMethodsItem> comparatorSortBy(Comparator<PaymentMethodsItem> comparator, PaymentMethodRequest request) {

        if (request.getSortBy() == SortBy.NAME) {
            // comparator for name in the requested language, defaulting to IT if not available
            comparator = Comparator.comparing(
                    (PaymentMethodsItem a) -> {
                        Map<Language, String> name = a.getName();
                        return name.getOrDefault(request.getLanguage(), name.get(Language.IT));
                    }
            );
        } else if (request.getSortBy() == SortBy.DESCRIPTION) {
            // comparator for description in the requested language, defaulting to IT if not available
            comparator = Comparator.comparing(
                    a -> {
                        Map<Language, String> description = a.getDescription();
                        return description.getOrDefault(request.getLanguage(), description.get(Language.IT));
                    }
            );
        } else if (request.getSortBy() == SortBy.FEE) {
            // comparator for fee range min value, placing items without fee range at the end
            comparator = Comparator.comparingLong(a -> a.getFeeRange() != null ? a.getFeeRange().getMin() : Long.MAX_VALUE);
        }

        return comparator;
    }

    /**
     * Comparator to prioritize specific groups of payment methods
     *
     * @param request the payment method request containing priority groups
     * @return a comparator that prioritizes payment methods based on specified groups
     */
    private static Comparator<PaymentMethodsItem> comparatorPriorityGroups(PaymentMethodRequest request) {
        // get priority groups from request, default to empty list if null
        List<PaymentMethodGroup> priorityGroups = request.getPriorityGroups() != null ? request.getPriorityGroups() : List.of();

        return (a, b) ->
        {
            // a has higher priority than b
            if (priorityGroups.contains(a.getGroup()) && !priorityGroups.contains(b.getGroup())) return -1;

            // b has higher priority than a
            if (!priorityGroups.contains(a.getGroup()) && priorityGroups.contains(b.getGroup())) return 1;

            // both have same priority
            return 0;

        };
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
}
