package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.TransferListItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UtilityComponentTest {

    @Autowired
    UtilityComponent utilityComponent;


    @ParameterizedTest
    @ValueSource(strings = {
            "123ABC987",
            "9/123ABC987",
            "9/123ABC987/",
            "/123ABC987/",
            "123ABC987/"})
    void getPrimaryTransferCategoryListMulti(String input) {
        var res = utilityComponent.getTransferCategoryList(PaymentOption.builder()
                        .transferList(List.of(TransferListItem.builder()
                                        .transferCategory(input)
                                .build()))
                .build());
        assertEquals("123ABC987", res.get(0));
    }

    @Test
    void getPrimaryTransferCategoryListMulti2() {
        var res = utilityComponent.getTransferCategoryList(PaymentOption.builder()
                        .transferList(List.of(TransferListItem.builder()
                                        .transferCategory(null)
                                .build()))
                .build());
        assertEquals(0, res.size());
    }
}
