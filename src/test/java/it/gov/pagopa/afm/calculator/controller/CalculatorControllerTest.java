package it.gov.pagopa.afm.calculator.controller;

import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CalculatorControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void getFeesByPsp() throws Exception {
        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

        mvc.perform(post("/psps/12345/fees")
                        .content(TestUtil.toJson(paymentOption))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void getFees() throws Exception {
        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

        mvc.perform(post("/fees")
                        .content(TestUtil.toJson(paymentOption))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}

