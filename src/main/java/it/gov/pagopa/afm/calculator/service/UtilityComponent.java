package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.TransferListItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link Cacheable} methods are ignored when called from within the same class
 */
@Component
@Slf4j
public class UtilityComponent {

    /**
     * Retrieve the transfer category list from the transfer list of payment option (OR of transfer categories)
     * @param paymentOption
     * @return list of string about transfer categories
     */
    @Cacheable(value = "getTransferCategoryList")
    public List<String> getTransferCategoryList(PaymentOption paymentOption) {
        log.debug("getTransferCategoryList");
        return paymentOption.getTransferList() != null ?
                paymentOption.getTransferList()
                        .parallelStream()
                        .map(TransferListItem::getTransferCategory)
                        .distinct()
                        .collect(Collectors.toList())
                : null;
    }

    /**
     * Retrieve the transfer category list of primary creditor institution contained in the transfer list of payment option
     * @param paymentOption
     * @param primaryCreditorInstitution
     * @return list of string about transfer categories of primary creditor institution
     */
    @Cacheable(value = "getPrimaryTransferCategoryList")
    public List<String> getPrimaryTransferCategoryList(PaymentOption paymentOption, String primaryCreditorInstitution) {
        log.debug("getPrimaryTransferCategoryList {} ", primaryCreditorInstitution);
        return paymentOption.getTransferList() != null ?
                paymentOption.getTransferList()
                        .parallelStream()
                        .filter(elem -> primaryCreditorInstitution.equals(elem.getCreditorInstitution()))
                        .map(TransferListItem::getTransferCategory)
                        .distinct()
                        .collect(Collectors.toList())
                : new ArrayList<>();
    }

    public List<String> getTouchpoints(){
        return null;
    }
}
