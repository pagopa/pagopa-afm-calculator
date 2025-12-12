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
        // build the comparator based on request parameters
        Comparator<PaymentMethodsItem> comparator = buildPaymentMethodsItemComparator(request);

        // sort the payment methods items
        paymentMethodsItems.sort(comparator);
    }

    /**
     * This method builds a comparator for PaymentMethodsItem based on the sorting criteria specified in the request.
     *
     * @param request the payment method request containing sorting criteria
     * @return a comparator for PaymentMethodsItem
     */
    private static Comparator<PaymentMethodsItem> buildPaymentMethodsItemComparator(PaymentMethodRequest request) {
        return (a, b) -> {

            // check priority groups first
            Integer priority = comparePriorityGroup(request, a, b);
            if (priority != null) {
                return priority;
            }

            // neither a nor b have priority
            return compareAndSort(request, a, b);
        };
    }

    /**
     *
     * This method compares two PaymentMethodsItem based on the sorting criteria specified in the request.
     *
     * @param request the payment method request containing sorting criteria
     * @param a       the first payment methods item to compare
     * @param b       the second payment methods item to compare
     * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second
     */
    private static int compareAndSort(PaymentMethodRequest request, PaymentMethodsItem a, PaymentMethodsItem b) {
        int result = 0;

        SortBy sortBy = request.getSortBy() != null ? request.getSortBy() : SortBy.DESCRIPTION;
        SortOrder sortOrder = request.getSortOrder() != null ? request.getSortOrder() : SortOrder.ASC;
        Language language = request.getLanguage() != null ? request.getLanguage() : Language.IT;

        if (sortBy == SortBy.NAME) {
            // comparator for name in the requested language, defaulting to IT if not available
            Map<Language, String> nameA = a.getName();
            Map<Language, String> nameB = b.getName();
            String fallbackA = nameA.get(Language.IT);
            String fallbackB = nameB.get(Language.IT);
            String nameALocalized = nameA.getOrDefault(language, fallbackA);
            String nameBLocalized = nameB.getOrDefault(language, fallbackB);

            result = nameALocalized.compareTo(nameBLocalized);

        } else if (sortBy == SortBy.DESCRIPTION) {
            // comparator for description in the requested language, defaulting to IT if not available
            Map<Language, String> descriptionA = a.getDescription();
            Map<Language, String> descriptionB = b.getDescription();
            String fallbackA = descriptionA.get(Language.IT);
            String fallbackB = descriptionB.get(Language.IT);
            String descriptionALocalized = descriptionA.getOrDefault(language, fallbackA);
            String descriptionBLocalized = descriptionB.getOrDefault(language, fallbackB);

            result = descriptionALocalized.compareTo(descriptionBLocalized);

        } else if (sortBy == SortBy.FEE) {
            // comparator for fee range min value, placing items without fee range at the end
            Long descriptionA = a.getFeeRange() != null ? a.getFeeRange().getMin() : Long.MAX_VALUE;
            Long descriptionB = b.getFeeRange() != null ? b.getFeeRange().getMin() : Long.MAX_VALUE;

            result = descriptionA.compareTo(descriptionB);
        }

        // adjust for sort order
        if (sortOrder == SortOrder.DESC) {
            result = result * -1;
        }

        return result;
    }

    /**
     * This method compares two PaymentMethodsItem based on their priority groups defined in the request.
     *
     * @param request the payment method request containing priority groups
     * @param a       the first payment methods item to compare
     * @param b       the second payment methods item to compare
     * @return -1 if a has higher priority, 1 if b has higher priority, or null if neither has priority
     */
    private static Integer comparePriorityGroup(PaymentMethodRequest request, PaymentMethodsItem a, PaymentMethodsItem b) {
        List<PaymentMethodGroup> priorityGroups = request.getPriorityGroups() != null ? request.getPriorityGroups() : List.of(PaymentMethodGroup.CP);

        PaymentMethodGroup groupA = a.getGroup();
        PaymentMethodGroup groupB = b.getGroup();

        boolean aIsPriority = priorityGroups.contains(groupA);
        boolean bIsPriority = priorityGroups.contains(groupB);

        // Check if both groups are in the priority list
        if (aIsPriority && bIsPriority) {
            // Both are priority, compare their indices in the priority list
            int indexA = priorityGroups.indexOf(groupA);
            int indexB = priorityGroups.indexOf(groupB);

            // Both are priority but have different indices, sort by their index in the priority list
            return Integer.compare(indexA, indexB);
        }

        if (aIsPriority) {
            // a has priority over b
            return -1;
        }

        if (bIsPriority) {
            // b has priority over a
            return 1;
        }
        return null;
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
