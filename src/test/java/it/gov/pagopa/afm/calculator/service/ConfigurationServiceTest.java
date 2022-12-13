package it.gov.pagopa.afm.calculator.service;

import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.query.CosmosQuery;
import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.entity.PaymentType;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.repository.PaymentTypeRepository;
import it.gov.pagopa.afm.calculator.repository.TouchpointRepository;
import it.gov.pagopa.afm.calculator.repository.ValidBundleRepository;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
    void addValidBundles()  {
        configurationService.addValidBundles(new ArrayList<>());
    }

    @Test
    void deleteValidBundles()  {
        configurationService.deleteValidBundles(new ArrayList<>());
    }

    @Test
    void addTouchpoints()  {
        configurationService.addTouchpoints(new ArrayList<>());
    }

    @Test
    void deleteTouchpoints()  {
        configurationService.deleteTouchpoints(new ArrayList<>());
    }

    @Test
    void addPaymentTypes()  {
        configurationService.addPaymentTypes(new ArrayList<>());
    }

    @Test
    void deletePaymentTypes()  {
        configurationService.deletePaymentTypes(new ArrayList<>());
    }


}
