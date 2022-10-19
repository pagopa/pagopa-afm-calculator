//package it.gov.pagopa.afm.calculator.service;
//
//import it.gov.pagopa.afm.calculator.TestUtil;
//import it.gov.pagopa.afm.calculator.initializer.Initializer;
//import it.gov.pagopa.afm.calculator.model.PaymentOption;
//import org.json.JSONException;
//import org.junit.jupiter.api.Test;
//import org.skyscreamer.jsonassert.JSONAssert;
//import org.skyscreamer.jsonassert.JSONCompareMode;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ContextConfiguration;
//
//import java.io.IOException;
//
//@SpringBootTest
//@ContextConfiguration(initializers = {Initializer.class})
//class CalculatorServiceTest {
//
//    @Autowired
//    CalculatorService calculatorService;
//
////    @MockBean
////    CosmosTemplate cosmosTemplate;
//
//    @Test
//    void calculate() throws IOException, JSONException {
////        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(Collections.singleton(TestUtil.getMockBundleList()));
//        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
//
//        var result = calculatorService.calculate(paymentOption, 10);
//
//        String actual = TestUtil.toJson(result);
//        String expected = TestUtil.readStringFromFile("responses/getFees.json");
//        System.out.println(actual);
//        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
//    }
//
//
//}
