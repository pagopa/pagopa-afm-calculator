package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link Cacheable} methods are ignored when called from within the same class
 */
@Component
@Slf4j
public class UtilityComponent {

    /**
     * @param bundle a valid bundle
     * @return TRUE if is a Global bundle
     */
    public static boolean isGlobal(ValidBundle bundle) {
        return BundleType.GLOBAL.equals(bundle.getType());
    }

    /**
     * @param creditorInstitutionFiscalCode primary CI fiscal code
     * @param transferList                  list of transfers
     * @return Check if creditor institution belongs to transfer list
     */
    public static boolean inTransferList(
            String creditorInstitutionFiscalCode, List<TransferListItem> transferList) {
        return transferList.parallelStream()
                .anyMatch(
                        transferListItem ->
                                transferListItem.getCreditorInstitution().equals(creditorInstitutionFiscalCode));
    }

    /**
     * Extracts and returns the taxonomy value from the given element string.
     * examples:
     * 9/9182ABC/ -> 9182ABC
     * 9182ABC -> 9182ABC
     *
     * @param elem the input string containing taxonomy information, expected to be in the format "category/taxonomy".
     * @return the taxonomy part of the input string if available, otherwise returns the original string.
     */
    private static String getTaxonomyValue(String elem) {
        if (elem == null) {
            return null;
        }

        String[] split = elem.split("/");

        // 12324    split[0]
        // 9/23423  split[1]
        // /12324   split[1]
        // /23423/  split[1]
        // 1212/    split[0]
        // 9/23423/ split[1]

        return split.length > 1 ? split[1] : split[0];
    }

    /**
     * Retrieve the transfer category list from the transfer list of payment option (OR of transfer
     * categories)
     *
     * @param paymentOption request
     * @return list of string about transfer categories
     */
    @Cacheable(value = "getTransferCategoryList")
    public List<String> getTransferCategoryList(PaymentOption paymentOption) {
        log.debug("getTransferCategoryList");
        return paymentOption.getTransferList() != null
                ? paymentOption.getTransferList().parallelStream()
                .map(TransferListItem::getTransferCategory)
                .map(UtilityComponent::getTaxonomyValue)
                .filter(Objects::nonNull)
                .distinct()
                .toList()
                : null;
    }

    /**
     * Retrieve the transfer category list from the transfer list of payment option (OR of transfer
     * categories)
     *
     * @param paymentOptionMulti request
     * @return list of string about transfer categories
     */
    @Cacheable(value = "getTransferCategoryListMulti")
    public List<String> getTransferCategoryList(PaymentOptionMulti paymentOptionMulti) {
        List<TransferListItem> transferList = new ArrayList<>();
        paymentOptionMulti.getPaymentNotice().forEach(paymentNoticeItem -> transferList.addAll(paymentNoticeItem.getTransferList()));
        log.debug("getTransferCategoryList");
        return !transferList.isEmpty()
                ? transferList.parallelStream()
                .map(TransferListItem::getTransferCategory)
                .map(UtilityComponent::getTaxonomyValue)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList())
                : null;
    }

    /**
     * Retrieve the transfer category list of primary creditor institution contained in the transfer
     * list of payment option
     *
     * @param paymentOption              request
     * @param primaryCreditorInstitution fiscal code fo the CI
     * @return list of string about transfer categories of primary creditor institution
     */
    @Cacheable(value = "getPrimaryTransferCategoryList")
    public List<String> getPrimaryTransferCategoryList(
            PaymentOption paymentOption, String primaryCreditorInstitution) {
        log.debug("getPrimaryTransferCategoryList {} ", primaryCreditorInstitution);
        return paymentOption.getTransferList() != null
                ? paymentOption.getTransferList().parallelStream()
                .filter(elem -> primaryCreditorInstitution.equals(elem.getCreditorInstitution()))
                .map(TransferListItem::getTransferCategory)
                .map(UtilityComponent::getTaxonomyValue)
                .filter(Objects::nonNull)
                .distinct()
                .toList()
                : new ArrayList<>();
    }

    /**
     * Retrieve the transfer category list of primary creditor institution contained in the transfer
     * list of payment option
     *
     * @param paymentNoticeItem          request
     * @param primaryCreditorInstitution fiscal code fo the CI
     * @return list of string about transfer categories of primary creditor institution
     */
    @Cacheable(value = "getPrimaryTransferCategoryListMulti")
    public List<String> getPrimaryTransferCategoryListMulti(
            PaymentNoticeItem paymentNoticeItem, String primaryCreditorInstitution) {
        log.debug("getPrimaryTransferCategoryList {} ", primaryCreditorInstitution);
        return paymentNoticeItem.getTransferList() != null
                ? paymentNoticeItem.getTransferList().parallelStream()
                .filter(elem -> primaryCreditorInstitution.equals(elem.getCreditorInstitution()))
                .map(TransferListItem::getTransferCategory)
                .map(UtilityComponent::getTaxonomyValue)
                .filter(Objects::nonNull)
                .distinct()
                .toList()
                : new ArrayList<>();
    }

}
