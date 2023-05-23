package it.gov.pagopa.afm.calculator.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.calculator.Transfer;
import it.gov.pagopa.afm.calculator.service.CalculatorService;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CalculatorControllerTest {

  @MockBean CalculatorService calculatorService;

  @Autowired private MockMvc mvc;

  @BeforeEach
  void setup() throws IOException {
    List<Transfer> result = TestUtil.readObjectFromFile("responses/getFees.json", List.class);
    when(calculatorService.calculate(any(), anyInt(), any(Boolean.class))).thenReturn(result);
  }

  @Test
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
  void getFees() throws Exception {
    var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

    mvc.perform(
            post("/fees")
                .content(TestUtil.toJson(paymentOption))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }
}
