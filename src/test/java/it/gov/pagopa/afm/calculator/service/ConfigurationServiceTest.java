package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.repository.PaymentTypeRepository;
import it.gov.pagopa.afm.calculator.repository.TouchpointRepository;
import it.gov.pagopa.afm.calculator.repository.ValidBundleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class ConfigurationServiceTest {

    @Autowired
    ConfigurationService configurationService;

    @MockBean
    ValidBundleRepository validBundleRepository;

    @MockBean
    TouchpointRepository touchpointRepository;

    @MockBean
    PaymentTypeRepository paymentTypeRepository;

    @Test
    void addValidBundles() {
        assertDoesNotThrow(() -> configurationService.addValidBundles(new ArrayList<>()));
    }

    @Test
    void deleteValidBundles() {
        assertDoesNotThrow(() -> configurationService.deleteValidBundles(new ArrayList<>()));
    }

    @Test
    void addTouchpoints() {
        assertDoesNotThrow(() -> configurationService.addTouchpoints(new ArrayList<>()));
    }

    @Test
    void deleteTouchpoints() {
        assertDoesNotThrow(() -> configurationService.deleteTouchpoints(new ArrayList<>()));
    }

    @Test
    void addPaymentTypes() {
        assertDoesNotThrow(() -> configurationService.addPaymentTypes(new ArrayList<>()));
    }

    @Test
    void deletePaymentTypes() {
        assertDoesNotThrow(() -> configurationService.deletePaymentTypes(new ArrayList<>()));
    }
}
