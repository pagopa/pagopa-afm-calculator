package it.gov.pagopa.afm.calculator.controller;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import it.gov.pagopa.afm.calculator.model.PaymentMethodResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import it.gov.pagopa.afm.calculator.entity.PaymentMethod;
import it.gov.pagopa.afm.calculator.exception.AppError;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.service.PaymentMethodsService;

@WebMvcTest(PaymentMethodsController.class)
class PaymentMethodsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentMethodsService paymentMethodsService;

    @Test
    void givenValidId_whenGetPaymentMethod_thenReturn200() throws Exception {
        PaymentMethodResponse method = new PaymentMethodResponse();
        method.setId("pm1");

        when(paymentMethodsService.getPaymentMethod("pm1")).thenReturn(method);

        mockMvc.perform(get("/payment-methods/pm1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void givenNotFound_whenGetPaymentMethod_thenReturn404() throws Exception {
        when(paymentMethodsService.getPaymentMethod("notFound"))
                .thenThrow(new AppException(AppError.PAYMENT_METHOD_NOT_FOUND, "notFound"));

        mockMvc.perform(get("/payment-methods/notFound"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenMultipleFound_whenGetPaymentMethod_thenReturn422() throws Exception {
        when(paymentMethodsService.getPaymentMethod("dup"))
                .thenThrow(new AppException(AppError.PAYMENT_METHOD_MULTIPLE_FOUND, "dup"));

        mockMvc.perform(get("/payment-methods/dup"))
                .andExpect(status().isInternalServerError());
    }
}
