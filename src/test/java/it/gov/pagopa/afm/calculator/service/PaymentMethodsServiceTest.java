package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.entity.PaymentMethod;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.exception.AppError;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.model.PaymentMethodResponse;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer;
import it.gov.pagopa.afm.calculator.model.paymentmethods.FeeRange;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodRequest;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsResponse;
import it.gov.pagopa.afm.calculator.model.paymentmethods.SortOrder;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.Language;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodDisabledReason;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodStatus;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodType;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.SortBy;
import it.gov.pagopa.afm.calculator.repository.PaymentMethodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class PaymentMethodsServiceTest {

    @Autowired
    @InjectMocks
    PaymentMethodsService paymentMethodsService;

    @MockBean
    PaymentMethodRepository paymentMethodRepository;

    @MockBean
    CalculatorService calculatorService;

    @ParameterizedTest
    @CsvSource({
            "requests/paymentOptionsSearch.json",
            "requests/paymentOptionsSearchEmptyTransferList.json",
            "requests/paymentOptionsSearchNullFiscalCodeNullTransferList.json",
            "requests/paymentOptionsSearchNullTransferList.json"
    })
    void searchPaymentMethods_OK(String input) throws IOException {
        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(List.of(
                        PaymentMethod.builder()
                                .paymentMethodId("PAYPAL")
                                .status(PaymentMethodStatus.ENABLED)
                                .group("PPAL")
                                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                                .target(null)
                                .validityDateFrom(LocalDate.now().minusDays(1))
                                .rangeAmount(FeeRange.builder()
                                        .min(0L)
                                        .max(1000L)
                                        .build())
                                .build()
                ));

        when(calculatorService.getFilteredValidBundlesForPaymentMethods(any(PaymentOptionMulti.class), anyBoolean()))
                .thenReturn(List.of(bundleForPaymentType("PPAL")));

        when(calculatorService.calculateFeeRangeForPaymentMethods(
                anyList(),
                any(PaymentOptionMulti.class)
        )).thenReturn(FeeRange.builder()
                .min(10L)
                .max(10L)
                .build());

        PaymentMethodRequest paymentMethodRequest =
                TestUtil.readObjectFromFile(input, PaymentMethodRequest.class);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);

        assertEquals(1, response.getPaymentMethods().size());
        assertEquals(PaymentMethodStatus.ENABLED, response.getPaymentMethods().get(0).getStatus());
    }

    @Test
    void searchPaymentMethods_noUserDevice_OK() throws IOException {
        when(paymentMethodRepository.findByTouchpoint(anyString()))
                .thenReturn(List.of(
                        PaymentMethod.builder()
                                .paymentMethodId("PAYPAL")
                                .status(PaymentMethodStatus.ENABLED)
                                .group("PPAL")
                                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                                .target(null)
                                .validityDateFrom(LocalDate.now().minusDays(1))
                                .rangeAmount(FeeRange.builder()
                                        .min(0L)
                                        .max(1000L)
                                        .build())
                                .build()
                ));

        when(calculatorService.getFilteredValidBundlesForPaymentMethods(any(PaymentOptionMulti.class), anyBoolean()))
                .thenReturn(List.of(bundleForPaymentType("PPAL")));

        when(calculatorService.calculateFeeRangeForPaymentMethods(
                anyList(),
                any(PaymentOptionMulti.class)
        )).thenReturn(FeeRange.builder()
                .min(10L)
                .max(10L)
                .build());

        PaymentMethodRequest paymentMethodRequest =
                TestUtil.readObjectFromFile("requests/paymentOptionsSearchNoUserDevice.json", PaymentMethodRequest.class);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);

        assertEquals(1, response.getPaymentMethods().size());
        assertEquals(PaymentMethodStatus.ENABLED, response.getPaymentMethods().get(0).getStatus());
    }

    @Test
    void searchPaymentMethods_Target() throws IOException {
        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(List.of(
                        PaymentMethod.builder()
                                .paymentMethodId("PAYPAL")
                                .status(PaymentMethodStatus.ENABLED)
                                .group("PPAL")
                                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                                .target(List.of("wrongTarget"))
                                .validityDateFrom(LocalDate.now().minusDays(1))
                                .rangeAmount(FeeRange.builder()
                                        .min(0L)
                                        .max(1000L)
                                        .build())
                                .build()
                ));

        PaymentMethodRequest paymentMethodRequest =
                TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);

        assertEquals(1, response.getPaymentMethods().size());
        assertEquals("PAYPAL", response.getPaymentMethods().get(0).getPaymentMethodId());
        assertEquals(PaymentMethodStatus.DISABLED, response.getPaymentMethods().get(0).getStatus());
        assertEquals(PaymentMethodDisabledReason.TARGET_PREVIEW, response.getPaymentMethods().get(0).getDisabledReason());
    }

    @Test
    void searchPaymentMethods_Amount() throws IOException {
        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(List.of(
                        PaymentMethod.builder()
                                .paymentMethodId("PAYPAL")
                                .status(PaymentMethodStatus.ENABLED)
                                .group("PPAL")
                                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                                .target(List.of("user"))
                                .validityDateFrom(LocalDate.now().minusDays(1))
                                .rangeAmount(FeeRange.builder()
                                        .min(5L)
                                        .max(1000L)
                                        .build())
                                .build()
                ));

        PaymentMethodRequest paymentMethodRequest =
                TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);

        assertEquals(1, response.getPaymentMethods().size());
        assertEquals("PAYPAL", response.getPaymentMethods().get(0).getPaymentMethodId());
        assertEquals(PaymentMethodStatus.DISABLED, response.getPaymentMethods().get(0).getStatus());
        assertEquals(PaymentMethodDisabledReason.AMOUNT_OUT_OF_BOUND, response.getPaymentMethods().get(0).getDisabledReason());
    }

    @Test
    void searchPaymentMethods_Disabled() throws IOException {
        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(List.of(
                        PaymentMethod.builder()
                                .paymentMethodId("PAYPAL")
                                .status(PaymentMethodStatus.DISABLED)
                                .group("PPAL")
                                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                                .target(List.of("user"))
                                .validityDateFrom(LocalDate.now().minusDays(1))
                                .rangeAmount(FeeRange.builder()
                                        .min(0L)
                                        .max(1000L)
                                        .build())
                                .build()
                ));

        PaymentMethodRequest paymentMethodRequest =
                TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);

        assertEquals(1, response.getPaymentMethods().size());
        assertEquals("PAYPAL", response.getPaymentMethods().get(0).getPaymentMethodId());
        assertEquals(PaymentMethodStatus.DISABLED, response.getPaymentMethods().get(0).getStatus());
        assertEquals(PaymentMethodDisabledReason.METHOD_DISABLED, response.getPaymentMethods().get(0).getDisabledReason());
    }

    @Test
    void searchPaymentMethods_Maintenance() throws IOException {
        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(List.of(
                        PaymentMethod.builder()
                                .paymentMethodId("PAYPAL")
                                .status(PaymentMethodStatus.MAINTENANCE)
                                .group("PPAL")
                                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                                .target(List.of("user"))
                                .validityDateFrom(LocalDate.now().minusDays(1))
                                .rangeAmount(FeeRange.builder()
                                        .min(0L)
                                        .max(1000L)
                                        .build())
                                .build()
                ));

        PaymentMethodRequest paymentMethodRequest =
                TestUtil.readObjectFromFile("requests/paymentOptionsSearch.json", PaymentMethodRequest.class);

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

        PaymentMethodResponse result = paymentMethodsService.getPaymentMethod("pm1");

        assertNotNull(result);
        assertEquals("pm1", result.getId());
    }

    @Test
    void whenPaymentMethodNotFound_thenThrowException() {
        when(paymentMethodRepository.findByPaymentMethodId("notFound"))
                .thenReturn(Collections.emptyList());

        AppException ex = assertThrows(
                AppException.class,
                () -> paymentMethodsService.getPaymentMethod("notFound")
        );

        assertEquals(AppError.PAYMENT_METHOD_NOT_FOUND.httpStatus, ex.getHttpStatus());
    }

    @Test
    void whenMultiplePaymentMethodsFound_thenThrowException() {
        PaymentMethod m1 = new PaymentMethod();
        PaymentMethod m2 = new PaymentMethod();

        when(paymentMethodRepository.findByPaymentMethodId("dup"))
                .thenReturn(List.of(m1, m2));

        AppException ex = assertThrows(
                AppException.class,
                () -> paymentMethodsService.getPaymentMethod("dup")
        );

        assertEquals(AppError.PAYMENT_METHOD_MULTIPLE_FOUND.httpStatus, ex.getHttpStatus());
    }

    @ParameterizedTest
    @CsvSource({
            "requests/paymentOptionsSearchSorting.json",
            "requests/paymentOptionsSearchSortingDefault.json"
    })
    void searchPaymentMethods_sorting(String input) throws IOException {
        PaymentMethod paypal = PaymentMethod.builder()
                .paymentMethodId("PAYPAL")
                .description(Map.of(Language.IT, "Paypal", Language.EN, "Paypal"))
                .status(PaymentMethodStatus.ENABLED)
                .group("PPAL")
                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                .target(null)
                .validityDateFrom(LocalDate.now().minusDays(1))
                .rangeAmount(FeeRange.builder().min(0L).max(1000L).build())
                .build();

        PaymentMethod google = PaymentMethod.builder()
                .paymentMethodId("GOOGLEPAY")
                .description(Map.of(Language.IT, "Google Pay", Language.EN, "Google Pay"))
                .status(PaymentMethodStatus.ENABLED)
                .group("GOOG")
                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                .target(null)
                .validityDateFrom(LocalDate.now().minusDays(1))
                .rangeAmount(FeeRange.builder().min(0L).max(1000L).build())
                .build();

        PaymentMethod banca = PaymentMethod.builder()
                .paymentMethodId("BBB")
                .description(Map.of(Language.IT, "Banca instesa", Language.EN, "intesa bank"))
                .status(PaymentMethodStatus.ENABLED)
                .group("RBPS")
                .paymentMethodTypes(List.of(PaymentMethodType.CONTO))
                .target(null)
                .validityDateFrom(LocalDate.now().minusDays(1))
                .rangeAmount(FeeRange.builder().min(0L).max(1000L).build())
                .build();

        PaymentMethod cart = PaymentMethod.builder()
                .paymentMethodId("CART")
                .description(Map.of(Language.IT, "Carte", Language.EN, "Cards"))
                .status(PaymentMethodStatus.ENABLED)
                .group("CP")
                .paymentMethodTypes(List.of(PaymentMethodType.CARTE))
                .target(null)
                .validityDateFrom(LocalDate.now().minusDays(1))
                .rangeAmount(FeeRange.builder().min(0L).max(1000L).build())
                .build();

        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(List.of(paypal, google, banca, cart));

        when(calculatorService.getFilteredValidBundlesForPaymentMethods(any(PaymentOptionMulti.class), anyBoolean()))
                .thenReturn(allBundlesForSorting());

        when(calculatorService.calculateFeeRangeForPaymentMethods(
                anyList(),
                any(PaymentOptionMulti.class)
        )).thenReturn(FeeRange.builder()
                .min(10L)
                .max(10L)
                .build());

        PaymentMethodRequest paymentMethodRequest =
                TestUtil.readObjectFromFile(input, PaymentMethodRequest.class);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);

        assertEquals(4, response.getPaymentMethods().size());
        assertEquals(PaymentMethodStatus.ENABLED, response.getPaymentMethods().get(0).getStatus());
        assertEquals("CART", response.getPaymentMethods().get(0).getPaymentMethodId());
        assertEquals("BBB", response.getPaymentMethods().get(1).getPaymentMethodId());
        assertEquals("GOOGLEPAY", response.getPaymentMethods().get(2).getPaymentMethodId());
        assertEquals("PAYPAL", response.getPaymentMethods().get(3).getPaymentMethodId());
    }

    @ParameterizedTest
    @CsvSource({
            "requests/paymentOptionsSearchSorting.json"
    })
    void searchPaymentMethods_sortingAsc(String input) throws IOException {
        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(getMethodList());

        when(calculatorService.getFilteredValidBundlesForPaymentMethods(any(PaymentOptionMulti.class), anyBoolean()))
                .thenReturn(allBundlesForSorting());

        when(calculatorService.calculateFeeRangeForPaymentMethods(
                anyList(),
                any(PaymentOptionMulti.class)
        )).thenReturn(FeeRange.builder()
                .min(10L)
                .max(10L)
                .build());

        PaymentMethodRequest paymentMethodRequest =
                TestUtil.readObjectFromFile(input, PaymentMethodRequest.class);
        paymentMethodRequest.setSortOrder(SortOrder.ASC);
        paymentMethodRequest.setPriorityGroups(List.of("PPAL", "CP"));

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);

        assertEquals(4, response.getPaymentMethods().size());
        assertEquals(PaymentMethodStatus.ENABLED, response.getPaymentMethods().get(0).getStatus());
        assertEquals("PAYPAL", response.getPaymentMethods().get(0).getPaymentMethodId());
        assertEquals("CART", response.getPaymentMethods().get(1).getPaymentMethodId());
        assertEquals("BBB", response.getPaymentMethods().get(2).getPaymentMethodId());
        assertEquals("GOOGLEPAY", response.getPaymentMethods().get(3).getPaymentMethodId());
    }

    @ParameterizedTest
    @CsvSource({
            "requests/paymentOptionsSearchSorting.json"
    })
    void searchPaymentMethods_sortingDesc(String input) throws IOException {
        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(getMethodList());

        when(calculatorService.getFilteredValidBundlesForPaymentMethods(any(PaymentOptionMulti.class), anyBoolean()))
                .thenReturn(allBundlesForSorting());

        when(calculatorService.calculateFeeRangeForPaymentMethods(
                anyList(),
                any(PaymentOptionMulti.class)
        )).thenReturn(FeeRange.builder()
                .min(10L)
                .max(10L)
                .build());

        PaymentMethodRequest paymentMethodRequest =
                TestUtil.readObjectFromFile(input, PaymentMethodRequest.class);
        paymentMethodRequest.setSortOrder(SortOrder.DESC);
        paymentMethodRequest.setPriorityGroups(List.of("PPAL", "CP"));

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);

        assertEquals(4, response.getPaymentMethods().size());
        assertEquals(PaymentMethodStatus.ENABLED, response.getPaymentMethods().get(0).getStatus());
        assertEquals("PAYPAL", response.getPaymentMethods().get(0).getPaymentMethodId());
        assertEquals("CART", response.getPaymentMethods().get(1).getPaymentMethodId());
        assertEquals("GOOGLEPAY", response.getPaymentMethods().get(2).getPaymentMethodId());
        assertEquals("BBB", response.getPaymentMethods().get(3).getPaymentMethodId());
    }

    @ParameterizedTest
    @CsvSource({
            "requests/paymentOptionsSearchSorting.json"
    })
    void searchPaymentMethods_sortingName(String input) throws IOException {
        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(getMethodList());

        when(calculatorService.getFilteredValidBundlesForPaymentMethods(any(PaymentOptionMulti.class), anyBoolean()))
                .thenReturn(allBundlesForSorting());

        when(calculatorService.calculateFeeRangeForPaymentMethods(
                anyList(),
                any(PaymentOptionMulti.class)
        )).thenReturn(FeeRange.builder()
                .min(10L)
                .max(10L)
                .build());

        PaymentMethodRequest paymentMethodRequest =
                TestUtil.readObjectFromFile(input, PaymentMethodRequest.class);
        paymentMethodRequest.setSortBy(SortBy.NAME);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);

        assertEquals(4, response.getPaymentMethods().size());
        assertEquals("CART", response.getPaymentMethods().get(0).getPaymentMethodId());
        assertEquals("BBB", response.getPaymentMethods().get(1).getPaymentMethodId());
        assertEquals("GOOGLEPAY", response.getPaymentMethods().get(2).getPaymentMethodId());
        assertEquals("PAYPAL", response.getPaymentMethods().get(3).getPaymentMethodId());
    }

    @ParameterizedTest
    @CsvSource({
            "requests/paymentOptionsSearchSorting.json"
    })
    void searchPaymentMethods_sortingFee(String input) throws IOException {
        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(getMethodList());

        when(calculatorService.getFilteredValidBundlesForPaymentMethods(any(PaymentOptionMulti.class), anyBoolean()))
                .thenReturn(allBundlesForSorting());

        when(calculatorService.calculateFeeRangeForPaymentMethods(
                anyList(),
                any(PaymentOptionMulti.class)
        )).thenReturn(FeeRange.builder()
                .min(10L)
                .max(10L)
                .build());

        PaymentMethodRequest paymentMethodRequest =
                TestUtil.readObjectFromFile(input, PaymentMethodRequest.class);
        paymentMethodRequest.setSortBy(SortBy.FEE);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(paymentMethodRequest);

        assertEquals(4, response.getPaymentMethods().size());
        assertEquals("CART", response.getPaymentMethods().get(0).getPaymentMethodId());
        assertEquals("PAYPAL", response.getPaymentMethods().get(1).getPaymentMethodId());
        assertEquals("GOOGLEPAY", response.getPaymentMethods().get(2).getPaymentMethodId());
        assertEquals("BBB", response.getPaymentMethods().get(3).getPaymentMethodId());
    }
    
    @Test
    void searchPaymentMethods_allCcpNull_shouldUseFalseAndNotThrow() throws IOException {
        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(List.of(PaymentMethod.builder()
                        .paymentMethodId("PAYPAL")
                        .status(PaymentMethodStatus.ENABLED)
                        .group("PPAL")
                        .paymentMethodTypes(List.of(PaymentMethodType.APP))
                        .target(null)
                        .validityDateFrom(LocalDate.now().minusDays(1))
                        .rangeAmount(FeeRange.builder()
                                .min(0L)
                                .max(100000L)
                                .build())
                        .build()));

        when(calculatorService.getFilteredValidBundlesForPaymentMethods(
                any(PaymentOptionMulti.class),
                eq(false)
        )).thenReturn(List.of(ValidBundle.builder()
                .paymentType("PPAL")
                .build()));

        when(calculatorService.calculateFeeRangeForPaymentMethods(
                anyList(),
                any(PaymentOptionMulti.class)
        )).thenReturn(FeeRange.builder()
                .min(100L)
                .max(100L)
                .build());

        PaymentMethodRequest request = TestUtil.readObjectFromFile(
                "requests/paymentOptionsSearch.json",
                PaymentMethodRequest.class
        );
        request.setAllCCp(null);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(request);

        assertNotNull(response);
        assertEquals(1, response.getPaymentMethods().size());
        assertEquals(PaymentMethodStatus.ENABLED, response.getPaymentMethods().get(0).getStatus());

        verify(calculatorService).getFilteredValidBundlesForPaymentMethods(
                any(PaymentOptionMulti.class),
                eq(false)
        );
    }
    
    @Test
    void searchPaymentMethods_allCcpTrue_shouldPassTrueToCalculatorService() throws IOException {
        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(List.of(PaymentMethod.builder()
                        .paymentMethodId("PAYPAL")
                        .status(PaymentMethodStatus.ENABLED)
                        .group("PPAL")
                        .paymentMethodTypes(List.of(PaymentMethodType.APP))
                        .target(null)
                        .validityDateFrom(LocalDate.now().minusDays(1))
                        .rangeAmount(FeeRange.builder()
                                .min(0L)
                                .max(100000L)
                                .build())
                        .build()));

        when(calculatorService.getFilteredValidBundlesForPaymentMethods(
                any(PaymentOptionMulti.class),
                eq(true)
        )).thenReturn(List.of(ValidBundle.builder()
                .paymentType("PPAL")
                .build()));

        when(calculatorService.calculateFeeRangeForPaymentMethods(
                anyList(),
                any(PaymentOptionMulti.class)
        )).thenReturn(FeeRange.builder()
                .min(100L)
                .max(100L)
                .build());

        PaymentMethodRequest request = TestUtil.readObjectFromFile(
                "requests/paymentOptionsSearch.json",
                PaymentMethodRequest.class
        );
        request.setAllCCp(true);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(request);

        assertNotNull(response);
        assertEquals(1, response.getPaymentMethods().size());

        verify(calculatorService).getFilteredValidBundlesForPaymentMethods(
                any(PaymentOptionMulti.class),
                eq(true)
        );
    } 
    
    @Test
    void searchPaymentMethods_allCcpFalse_shouldPassFalseToCalculatorService() throws IOException {
        when(paymentMethodRepository.findByTouchpointAndDevice(anyString(), anyString()))
                .thenReturn(List.of(PaymentMethod.builder()
                        .paymentMethodId("PAYPAL")
                        .status(PaymentMethodStatus.ENABLED)
                        .group("PPAL")
                        .paymentMethodTypes(List.of(PaymentMethodType.APP))
                        .target(null)
                        .validityDateFrom(LocalDate.now().minusDays(1))
                        .rangeAmount(FeeRange.builder()
                                .min(0L)
                                .max(100000L)
                                .build())
                        .build()));

        when(calculatorService.getFilteredValidBundlesForPaymentMethods(
                any(PaymentOptionMulti.class),
                eq(false)
        )).thenReturn(List.of(ValidBundle.builder()
                .paymentType("PPAL")
                .build()));

        when(calculatorService.calculateFeeRangeForPaymentMethods(
                anyList(),
                any(PaymentOptionMulti.class)
        )).thenReturn(FeeRange.builder()
                .min(100L)
                .max(100L)
                .build());

        PaymentMethodRequest request = TestUtil.readObjectFromFile(
                "requests/paymentOptionsSearch.json",
                PaymentMethodRequest.class
        );
        request.setAllCCp(false);

        PaymentMethodsResponse response = paymentMethodsService.searchPaymentMethods(request);

        assertNotNull(response);
        assertEquals(1, response.getPaymentMethods().size());

        verify(calculatorService).getFilteredValidBundlesForPaymentMethods(
                any(PaymentOptionMulti.class),
                eq(false)
        );
    }

    private static List<PaymentMethod> getMethodList() {
        PaymentMethod paypal = PaymentMethod.builder()
                .paymentMethodId("PAYPAL")
                .name(Map.of(Language.IT, "Paypal", Language.EN, "Paypal"))
                .description(Map.of(Language.IT, "Paypal", Language.EN, "Paypal"))
                .status(PaymentMethodStatus.ENABLED)
                .group("PPAL")
                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                .target(null)
                .validityDateFrom(LocalDate.now().minusDays(1))
                .rangeAmount(FeeRange.builder()
                        .min(0L)
                        .max(1000L)
                        .build())
                .build();

        PaymentMethod google = PaymentMethod.builder()
                .paymentMethodId("GOOGLEPAY")
                .name(Map.of(Language.IT, "Google Pay"))
                .description(Map.of(Language.IT, "Google Pay"))
                .status(PaymentMethodStatus.ENABLED)
                .group("GOOG")
                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                .target(null)
                .validityDateFrom(LocalDate.now().minusDays(1))
                .rangeAmount(FeeRange.builder()
                        .min(1L)
                        .max(1000L)
                        .build())
                .build();

        PaymentMethod banca = PaymentMethod.builder()
                .paymentMethodId("BBB")
                .name(Map.of(Language.IT, "Banca instesa", Language.EN, "intesa bank"))
                .description(Map.of(Language.IT, "Banca instesa", Language.EN, "intesa bank"))
                .status(PaymentMethodStatus.ENABLED)
                .group("RBPS")
                .paymentMethodTypes(List.of(PaymentMethodType.CONTO))
                .target(null)
                .validityDateFrom(LocalDate.now().minusDays(1))
                .rangeAmount(FeeRange.builder()
                        .min(10L)
                        .max(1000L)
                        .build())
                .build();

        PaymentMethod cart = PaymentMethod.builder()
                .paymentMethodId("CART")
                .name(Map.of(Language.IT, "Carte", Language.EN, "Cards"))
                .description(Map.of(Language.IT, "Carte", Language.EN, "Cards"))
                .status(PaymentMethodStatus.ENABLED)
                .group("CP")
                .paymentMethodTypes(List.of(PaymentMethodType.CARTE))
                .target(null)
                .validityDateFrom(LocalDate.now().minusDays(1))
                .rangeAmount(FeeRange.builder()
                        .min(20L)
                        .max(1000L)
                        .build())
                .build();

        return List.of(paypal, google, banca, cart);
    }

    private static ValidBundle bundleForPaymentType(String paymentType) {
        return ValidBundle.builder()
                .id("bundle-" + paymentType)
                .idPsp("PSP1")
                .paymentType(paymentType)
                .touchpoint("CHECKOUT")
                .paymentAmount(10L)
                .minPaymentAmount(0L)
                .maxPaymentAmount(1000L)
                .build();
    }

    private static List<ValidBundle> allBundlesForSorting() {
        return List.of(
                bundleForPaymentType("PPAL"),
                bundleForPaymentType("GOOG"),
                bundleForPaymentType("RBPS"),
                bundleForPaymentType("CP")
        );
    }
}