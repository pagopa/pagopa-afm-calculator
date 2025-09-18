package it.gov.pagopa.afm.calculator.service;

import com.azure.spring.data.cosmos.core.CosmosTemplate;
import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.entity.PaymentMethod;
import it.gov.pagopa.afm.calculator.exception.AppError;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer;
import it.gov.pagopa.afm.calculator.model.paymentmethods.FeeRange;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodRequest;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsResponse;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodDisabledReason;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodGroup;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodStatus;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodType;
import it.gov.pagopa.afm.calculator.repository.PaymentMethodRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                .target(null)
                .validityDateFrom(LocalDate.now().minusDays(1))
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

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
        assertEquals(1, response.getPaymentMethods().size());
        assertEquals(PaymentMethodStatus.ENABLED, response.getPaymentMethods().get(0).getStatus());
    }

    @Test
    void searchPaymentMethods_noUserDevice_OK() throws IOException {
        when(paymentMethodRepository
                .findByTouchpoint(anyString())).thenReturn(List.of(PaymentMethod.builder()
                .paymentMethodId("PAYPAL")
                .status(PaymentMethodStatus.ENABLED)
                .group(PaymentMethodGroup.PPAL)
                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                .target(null)
                .validityDateFrom(LocalDate.now().minusDays(1))
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

        PaymentMethodRequest paymentMethodRequest = TestUtil.readObjectFromFile("requests/paymentOptionsSearchNoUserDevice.json", PaymentMethodRequest.class);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
        assertEquals(1, response.getPaymentMethods().size());
        assertEquals(PaymentMethodStatus.ENABLED, response.getPaymentMethods().get(0).getStatus());
    }

    @Test
    void searchPaymentMethods_OK_EmptyTransferList() throws IOException {
        when(paymentMethodRepository
                .findByTouchpointAndDevice(anyString(), anyString())).thenReturn(List.of(PaymentMethod.builder()
                .paymentMethodId("PAYPAL")
                .status(PaymentMethodStatus.ENABLED)
                .group(PaymentMethodGroup.PPAL)
                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                .target(null)
                .validityDateFrom(LocalDate.now().minusDays(1))
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

        PaymentMethodRequest paymentMethodRequest = TestUtil.readObjectFromFile("requests/paymentOptionsSearchEmptyTransferList.json", PaymentMethodRequest.class);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
        assertEquals(1, response.getPaymentMethods().size());
        assertEquals(PaymentMethodStatus.ENABLED, response.getPaymentMethods().get(0).getStatus());
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
                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                .target(List.of("wrongTarget"))
                .validityDateFrom(LocalDate.now().minusDays(1))
                .rangeAmount(FeeRange.builder()
                        .min(0L)
                        .max(1000L)
                        .build())
                .build()));
        PaymentMethodRequest paymentMethodRequest = TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
        assertEquals(1, response.getPaymentMethods().size());
        assertEquals("PAYPAL", response.getPaymentMethods().get(0).getPaymentMethodId());
        assertEquals(PaymentMethodStatus.DISABLED, response.getPaymentMethods().get(0).getStatus());
        assertEquals(PaymentMethodDisabledReason.TARGET_PREVIEW, response.getPaymentMethods().get(0).getDisabledReason());
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
                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                .target(List.of("user"))
                .validityDateFrom(LocalDate.now().minusDays(1))
                .rangeAmount(FeeRange.builder()
                        .min(5L)
                        .max(1000L)
                        .build())
                .build()));
        PaymentMethodRequest paymentMethodRequest = TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
        assertEquals(1, response.getPaymentMethods().size());
        assertEquals("PAYPAL", response.getPaymentMethods().get(0).getPaymentMethodId());
        assertEquals(PaymentMethodStatus.DISABLED, response.getPaymentMethods().get(0).getStatus());
        assertEquals(PaymentMethodDisabledReason.AMOUNT_OUT_OF_BOUND, response.getPaymentMethods().get(0).getDisabledReason());
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
                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                .target(List.of("user"))
                .validityDateFrom(LocalDate.now().minusDays(1))
                .rangeAmount(FeeRange.builder()
                        .min(0L)
                        .max(1000L)
                        .build())
                .build()));
        PaymentMethodRequest paymentMethodRequest = TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
        assertEquals(1, response.getPaymentMethods().size());
        assertEquals("PAYPAL", response.getPaymentMethods().get(0).getPaymentMethodId());
        assertEquals(PaymentMethodStatus.DISABLED, response.getPaymentMethods().get(0).getStatus());
        assertEquals(PaymentMethodDisabledReason.METHOD_DISABLED, response.getPaymentMethods().get(0).getDisabledReason());
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
                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                .target(List.of("user"))
                .validityDateFrom(LocalDate.now().minusDays(1))
                .rangeAmount(FeeRange.builder()
                        .min(0L)
                        .max(1000L)
                        .build())
                .build()));
        PaymentMethodRequest paymentMethodRequest = TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
        assertEquals(1, response.getPaymentMethods().size());
        assertEquals("PAYPAL", response.getPaymentMethods().get(0).getPaymentMethodId());
        assertEquals(PaymentMethodStatus.MAINTENANCE, response.getPaymentMethods().get(0).getStatus());
        assertEquals(PaymentMethodDisabledReason.MAINTENANCE_IN_PROGRESS, response.getPaymentMethods().get(0).getDisabledReason());
    }

    @Test
    void whenPaymentMethodExists_thenReturnIt() {
        PaymentMethod method = new PaymentMethod();
        method.setId("pm1");

        when(paymentMethodRepository.findByPaymentMethodId("pm1"))
                .thenReturn(List.of(method));

        PaymentMethod result = paymentMethodsService.getPaymentMethod("pm1");

        assertNotNull(result);
        assertEquals("pm1", result.getId());
    }

    @Test
    void whenPaymentMethodNotFound_thenThrowException() {
        when(paymentMethodRepository.findByPaymentMethodId("notFound"))
                .thenReturn(Collections.emptyList());

        AppException ex = assertThrows(AppException.class,
                () -> paymentMethodsService.getPaymentMethod("notFound"));

        assertEquals(AppError.PAYMENT_METHOD_NOT_FOUND.httpStatus, ex.getHttpStatus());
    }

    @Test
    void whenMultiplePaymentMethodsFound_thenThrowException() {
        PaymentMethod m1 = new PaymentMethod();
        PaymentMethod m2 = new PaymentMethod();

        when(paymentMethodRepository.findByPaymentMethodId("dup"))
                .thenReturn(List.of(m1, m2));

        AppException ex = assertThrows(AppException.class,
                () -> paymentMethodsService.getPaymentMethod("dup"));

        assertEquals(AppError.PAYMENT_METHOD_MULTIPLE_FOUND.httpStatus, ex.getHttpStatus());
    }

}
