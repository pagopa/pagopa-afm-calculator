package it.gov.pagopa.afm.calculator.controller;

import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.calculator.Transfer;
import it.gov.pagopa.afm.calculator.service.CalculatorService;
import it.gov.pagopa.afm.calculator.service.ConfigurationService;
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
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConfigurationControllerTest {

    @MockBean
    ConfigurationService configurationService;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setup() {
    }

    @ParameterizedTest
    @CsvSource({
            "/configuration/bundles/add",
            "/configuration/touchpoint/add",
            "/configuration/paymenttypes/add",
    })
    void postAdd(String url) throws Exception {
        mvc.perform(
                post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(new ArrayList<>()))
                )
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @CsvSource({
            "/configuration/bundles/delete",
            "/configuration/touchpoint/delete",
            "/configuration/paymenttypes/delete",
    })
    void postDelete(String url) throws Exception {
        mvc.perform(
                        post(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(TestUtil.toJson(new ArrayList<>()))
                )
                .andExpect(status().isOk());
    }
}

