package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.TransferListItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UtilityComponentTest {

    @Autowired
    UtilityComponent utilityComponent;


    @Test
    void getPrimaryTransferCategoryListMulti() {
        var res = utilityComponent.getTransferCategoryList(PaymentOption.builder()
                        .transferList(List.of(TransferListItem.builder()
                                        .transferCategory("9/123ABC987")
                                .build()))
                .build());
        assertEquals("123ABC987", res.get(0));
    }
}
