package it.gov.pagopa.afm.calculator.controller;

import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.calculator.BundleOption;
import it.gov.pagopa.afm.calculator.service.CalculatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CalculatorControllerTest {

  @MockBean CalculatorService calculatorService;

  @Autowired private MockMvc mvc;

  @BeforeEach
  void setup() throws IOException {
    BundleOption result = TestUtil.readObjectFromFile("responses/getFees.json", BundleOption.class);
    it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption resultMulti =
        TestUtil.readObjectFromFile("responses/getFeesMulti.json", it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption.class);
    when(calculatorService.calculate(any(), anyInt(), any(Boolean.class))).thenReturn(result);
    when(calculatorService.calculateMulti(any(), anyInt(), any(Boolean.class),any(Boolean.class),any())).thenReturn(resultMulti);
  }

  @Test
  @CsvSource({"/psps/12345/fees", "/psps/12345/fees?allCcp=false", "/psps/12345/fees?allCcp=%20"})
  void getFeesByPsp() throws Exception {
    var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

    mvc.perform(
            post("/psps/12345/fees")
                .content(TestUtil.toJson(paymentOption))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  @CsvSource({"/fees", "/fees?allCcp=false", "/fees?allCcp=%20"})
  void getFees() throws Exception {
    var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

    mvc.perform(
            post("/fees")
                .content(TestUtil.toJson(paymentOption))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @ParameterizedTest
  @CsvSource({"/fees/multi", "/psps/12345/fees/multi",
              "/fees/multi?maxOccurrences=10", "/psps/12345/fees/multi?maxOccurrences=10",
              "/fees/multi?maxOccurrences=10&onUsFirst=true",
              "/psps/12345/fees/multi?maxOccurrences=10&&onUsFirst=true",
              "/fees/multi?maxOccurrences=10&onUsFirst=true&orderBy=fee",
              "/psps/12345/fees/multi?maxOccurrences=10&&onUsFirst=true&orderBy=fee",
              "/fees/multi?onUsFirst=&orderBy=","/psps/12345/fees/multi?onUsFirst=&orderBy="})
  void getFeesMulti(String uri) throws Exception {
    var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);

    mvc.perform(
                    post(uri)
                            .content(TestUtil.toJson(paymentOption))
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

}
