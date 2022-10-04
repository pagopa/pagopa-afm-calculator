package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.repository.BundleRepository;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.domain.Specification;

import java.io.IOException;

import static org.mockito.Mockito.when;

@SpringBootTest
class CalculatorServiceTest {

    @Autowired
    CalculatorService calculatorService;

    @MockBean
    BundleRepository bundleRepository;

    @Test
    void calculate() throws IOException, JSONException {
        when(bundleRepository.findAll(Mockito.any(Specification.class))).thenReturn(TestUtil.getMockBundleList());
        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

        var result = calculatorService.calculate(paymentOption, 10);

        String actual = TestUtil.toJson(result);
        String expected = TestUtil.readStringFromFile("responses/getFees.json");
        System.out.println(actual);
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }


}
