package it.gov.pagopa.afm.calculator.controller;

import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodRequest;
import it.gov.pagopa.afm.calculator.service.PaymentMethodsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentMethodsControllerTest {

    @MockBean
    PaymentMethodsService paymentMethodsService;

    @Autowired
    private MockMvc mvc;

    @Test
    void createPaymentMethods() throws Exception {

        mvc.perform(post("/payment-methods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(PaymentMethodRequest.builder().build())))
                .andExpect(status().isCreated());

    }

    @Test
    void getPaymentMethods() throws Exception {

        mvc.perform(get("/payment-methods")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }

    @Test
    void getPaymentMethod() throws Exception {

        mvc.perform(get("/payment-methods/PAYPAL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }

    @Test
    void deletePaymentMethod() throws Exception {

        mvc.perform(delete("/payment-methods/PAYPAL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }

    @Test
    void updatePaymentMethod() throws Exception {

        mvc.perform(get("/payment-methods/PAYPAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(PaymentMethodRequest.builder().build())))
                .andExpect(status().isOk());

    }

}
