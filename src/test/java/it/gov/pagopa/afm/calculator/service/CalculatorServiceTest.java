package it.gov.pagopa.afm.calculator.service;

import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.query.CosmosQuery;
import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class CalculatorServiceTest {

    @Autowired
    CalculatorService calculatorService;

    @MockBean
    CosmosTemplate cosmosTemplate;

    @Test
    void calculate() throws IOException, JSONException {
        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(Collections.singleton(TestUtil.getMockValidBundle()));
        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

        var result = calculatorService.calculate(paymentOption, 10);

        String actual = TestUtil.toJson(result);
        String expected = TestUtil.readStringFromFile("responses/getFees.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    void calculate_noInTransfer() throws IOException, JSONException {
        var list = new ArrayList<>();
        list.add(TestUtil.getMockGlobalValidBundle());
        list.add(TestUtil.getMockValidBundle());

        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(list);
        var paymentOption = TestUtil.readObjectFromFile("requests/getFees_noInTransfer.json", PaymentOption.class);

        var result = calculatorService.calculate(paymentOption, 10);

        String actual = TestUtil.toJson(result);
        String expected = TestUtil.readStringFromFile("responses/getFees_noInTransfer.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }


}
