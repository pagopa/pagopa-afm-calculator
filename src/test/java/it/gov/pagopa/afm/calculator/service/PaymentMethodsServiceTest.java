package it.gov.pagopa.afm.calculator.service;

import com.azure.spring.data.cosmos.core.CosmosTemplate;
import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.entity.PaymentMethod;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer;
import it.gov.pagopa.afm.calculator.model.paymentmethods.FeeRange;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodRequest;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsResponse;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodDisabledReason;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodGroup;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodStatus;
import it.gov.pagopa.afm.calculator.repository.PaymentMethodRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class PaymentMethodsServiceTest {

    @Autowired
    @InjectMocks
    PaymentMethodsService paymentMethodsService;

    @MockBean
    CosmosTemplate cosmosTemplate;

    @MockBean
    PaymentMethodRepository paymentMethodRepository;

    @MockBean
    CalculatorService calculatorService;


    @Test
    void searchPaymentMethods_OK() throws IOException {
        when(paymentMethodRepository
                .findByTouchpointAndDevice(anyString(), anyString())).thenReturn(List.of(PaymentMethod.builder()
                .paymentMethodId("PAYPAL")
                .status(PaymentMethodStatus.ENABLED)
                .group(PaymentMethodGroup.PPAL)
                .target(null)
                .rangeAmount(FeeRange.builder()
                        .min(0L)
                        .max(1000L)
                        .build())
                .build()));
        when(calculatorService.calculateMulti(any(PaymentOptionMulti.class), anyInt(), anyBoolean(), anyBoolean(), anyString()))
                .thenReturn(it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption.builder()
                        .belowThreshold(false)
                        .bundleOptions(List.of(Transfer.builder().build()))
                        .build());

        PaymentMethodRequest paymentMethodRequest = TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

        List<PaymentMethodsResponse> response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
        assertEquals(1, response.size());
        assertEquals(PaymentMethodStatus.ENABLED, response.get(0).getStatus());
    }

    @Test
    void searchPaymentMethods_Target() throws IOException {

        when(calculatorService.calculateMulti(any(PaymentOptionMulti.class), anyInt(), anyBoolean(), anyBoolean(), anyString()))
                .thenReturn(it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption.builder()
                        .belowThreshold(false)
                        .bundleOptions(List.of(Transfer.builder().build()))
                        .build());

        when(paymentMethodRepository
                .findByTouchpointAndDevice(anyString(), anyString())).thenReturn(List.of(PaymentMethod.builder()
                .paymentMethodId("PAYPAL")
                .status(PaymentMethodStatus.ENABLED)
                .group(PaymentMethodGroup.PPAL)
                .target(List.of("user"))
                .rangeAmount(FeeRange.builder()
                        .min(0L)
                        .max(1000L)
                        .build())
                .build()));
        PaymentMethodRequest paymentMethodRequest = TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

        List<PaymentMethodsResponse> response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
        assertEquals(1, response.size());
        assertEquals("PAYPAL", response.get(0).getPaymentMethodId());
        assertEquals(PaymentMethodStatus.DISABLED, response.get(0).getStatus());
        assertEquals(PaymentMethodDisabledReason.TARGET_PREVIEW, response.get(0).getDisabledReason());
    }

    @Test
    void searchPaymentMethods_Amount() throws IOException {

        when(calculatorService.calculateMulti(any(PaymentOptionMulti.class), anyInt(), anyBoolean(), anyBoolean(), anyString()))
                .thenReturn(it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption.builder()
                        .belowThreshold(false)
                        .bundleOptions(List.of(Transfer.builder().build()))
                        .build());

        when(paymentMethodRepository
                .findByTouchpointAndDevice(anyString(), anyString())).thenReturn(List.of(PaymentMethod.builder()
                .paymentMethodId("PAYPAL")
                .status(PaymentMethodStatus.ENABLED)
                .group(PaymentMethodGroup.PPAL)
                .target(List.of("user"))
                .rangeAmount(FeeRange.builder()
                        .min(5L)
                        .max(1000L)
                        .build())
                .build()));
        PaymentMethodRequest paymentMethodRequest = TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

        List<PaymentMethodsResponse> response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
        assertEquals(1, response.size());
        assertEquals("PAYPAL", response.get(0).getPaymentMethodId());
        assertEquals(PaymentMethodStatus.DISABLED, response.get(0).getStatus());
        assertEquals(PaymentMethodDisabledReason.AMOUNT_OUT_OF_BOUND, response.get(0).getDisabledReason());
    }

    @Test
    void searchPaymentMethods_Disabled() throws IOException {

        when(calculatorService.calculateMulti(any(PaymentOptionMulti.class), anyInt(), anyBoolean(), anyBoolean(), anyString()))
                .thenReturn(it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption.builder()
                        .belowThreshold(false)
                        .bundleOptions(List.of(Transfer.builder().build()))
                        .build());

        when(paymentMethodRepository
                .findByTouchpointAndDevice(anyString(), anyString())).thenReturn(List.of(PaymentMethod.builder()
                .paymentMethodId("PAYPAL")
                .status(PaymentMethodStatus.DISABLED)
                .group(PaymentMethodGroup.PPAL)
                .target(List.of("user"))
                .rangeAmount(FeeRange.builder()
                        .min(5L)
                        .max(1000L)
                        .build())
                .build()));
        PaymentMethodRequest paymentMethodRequest = TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

        List<PaymentMethodsResponse> response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
        assertEquals(1, response.size());
        assertEquals("PAYPAL", response.get(0).getPaymentMethodId());
        assertEquals(PaymentMethodStatus.DISABLED, response.get(0).getStatus());
        assertEquals(PaymentMethodDisabledReason.METHOD_DISABLED, response.get(0).getDisabledReason());
    }

    @Test
    void searchPaymentMethods_Maintenance() throws IOException {

        when(calculatorService.calculateMulti(any(PaymentOptionMulti.class), anyInt(), anyBoolean(), anyBoolean(), anyString()))
                .thenReturn(it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption.builder()
                        .belowThreshold(false)
                        .bundleOptions(List.of(Transfer.builder().build()))
                        .build());

        when(paymentMethodRepository
                .findByTouchpointAndDevice(anyString(), anyString())).thenReturn(List.of(PaymentMethod.builder()
                .paymentMethodId("PAYPAL")
                .status(PaymentMethodStatus.MAINTENANCE)
                .group(PaymentMethodGroup.PPAL)
                .target(List.of("user"))
                .rangeAmount(FeeRange.builder()
                        .min(5L)
                        .max(1000L)
                        .build())
                .build()));
        PaymentMethodRequest paymentMethodRequest = TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

        List<PaymentMethodsResponse> response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
        assertEquals(1, response.size());
        assertEquals("PAYPAL", response.get(0).getPaymentMethodId());
        assertEquals(PaymentMethodStatus.DISABLED, response.get(0).getStatus());
        assertEquals(PaymentMethodDisabledReason.MAINTENANCE_IN_PROGRESS, response.get(0).getDisabledReason());
    }
}
