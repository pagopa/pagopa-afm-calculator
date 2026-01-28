package it.gov.pagopa.afm.calculator.util;

import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodRequest;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsItem;
import it.gov.pagopa.afm.calculator.model.paymentmethods.SortOrder;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.Language;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodGroup;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.SortBy;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class PaymentMethodComparatorUtil {


    /**
     * Sort payment methods items based on request parameters
     *
     * @param request             the payment method request with sorting parameters
     * @param paymentMethodsItems the list of payment methods items to sort
     */
    public static void sortMethods(PaymentMethodRequest request, List<PaymentMethodsItem> paymentMethodsItems) {
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
            Long feeA = a.getFeeRange() != null ? a.getFeeRange().getMin() : Long.MAX_VALUE;
            Long feeB = b.getFeeRange() != null ? b.getFeeRange().getMin() : Long.MAX_VALUE;

            result = feeA.compareTo(feeB);
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
        List<String> priorityGroups = request.getPriorityGroups() != null ? request.getPriorityGroups() : List.of("CP");

        String groupA = a.getGroup();
        String groupB = b.getGroup();

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
}
