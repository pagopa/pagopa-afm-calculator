package it.gov.pagopa.afm.calculator.service;

import com.azure.data.tables.models.TableEntity;
import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.entity.PaymentType;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.initializer.Initializer;
import it.gov.pagopa.afm.calculator.model.BundleType;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.calculator.BundleOption;
import it.gov.pagopa.afm.calculator.repository.PaymentTypeRepository;
import it.gov.pagopa.afm.calculator.repository.TouchpointRepository;
import org.json.JSONException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.afm.calculator.TestUtil.getTableEntity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = {Initializer.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class CalculatorServiceTest {

    @Autowired
    CalculatorService calculatorService;
    
    @MockBean
    ValidBundleCacheService validBundleCacheService;
    @MockBean
    TouchpointRepository touchpointRepository;
    @MockBean
    PaymentTypeRepository paymentTypeRepository;
    
    @BeforeEach
    void initDefaultLookups() {
        when(touchpointRepository.findByName(anyString()))
                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString()))
                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
    }

    @BeforeAll
    void setup() {
        Initializer.table.createEntity(getTableEntity("1", "4030270000000000000", "4030279999999999999", "1030"));

        // two records with same BIN but different ABI --> error
        Initializer.table.createEntity(getTableEntity("2", "5043170000000000000", "5043179999999999999", "80006"));
        Initializer.table.createEntity(getTableEntity("3", "5043170000000000000", "5043179999999999999", "80007"));

        // two records with same BIN and same ABI
        Initializer.table.createEntity(getTableEntity("4", "1005066000000000000", "1005066999999999999", "14156"));
        Initializer.table.createEntity(getTableEntity("5", "1005066000000000000", "1005066999999999999", "14156"));

        Initializer.table.createEntity(getTableEntity("6", "3400000000000000000", "3499999999999999999", "AMREX"));
    }

    @ParameterizedTest
    @CsvSource({
        "requests/getFees.json, responses/getFees.json",
        "requests/getFeesBinNull.json, responses/getFeesBinNull.json",
        "requests/getFeesPspList.json, responses/getFeesBinNotFound.json",
        "requests/getFeesBinNotFound.json, responses/getFeesBinNotFound.json"
    })
    @Order(1)
    void calculate(String input, String output) throws IOException, JSONException {
        Touchpoint touchpoint = TestUtil.getMockTouchpoint();
        PaymentType paymentType = TestUtil.getMockPaymentType();

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(TestUtil.getMockValidBundle())));

        var paymentOption = TestUtil.readObjectFromFile(input, PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10, true);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile(output);
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(2)
    void calculate2() throws IOException, JSONException {
        ValidBundle validBundle = TestUtil.getMockValidBundle();
        validBundle.setIdPsp("77777777777");
        Touchpoint touchpoint = TestUtil.getMockTouchpoint();
        PaymentType paymentType = TestUtil.getMockPaymentType();

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(Collections.singletonList(validBundle));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10, true);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFees2.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(3)
    void calculate3() throws IOException, JSONException {
        ValidBundle validBundle = TestUtil.getMockValidBundle();
        validBundle.setIdPsp("77777777777");

        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(Collections.singletonList(validBundle));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees2.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10, true);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFees3.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(4)
    void calculate_noInTransfer() throws IOException, JSONException {
    	List<ValidBundle> list = new ArrayList<>();
    	list.add(TestUtil.getMockGlobalValidBundle());
    	list.add(TestUtil.getMockValidBundle());

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockPaymentType()));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(list);

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesNoInTransfer.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10, true);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesNoInTransfer.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(5)
    void calculate_invalidTouchpoint() throws IOException {
        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(TestUtil.getMockValidBundle())));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

        AppException exception =
                assertThrows(
                        AppException.class, () -> calculatorService.calculate(paymentOption, 10, true));

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    @Order(6)
    void calculate_invalidPaymentMethod() throws IOException {
        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(TestUtil.getMockValidBundle())));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

        AppException exception =
                assertThrows(
                        AppException.class, () -> calculatorService.calculate(paymentOption, 10, true));

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    @Order(7)
    void calculate_digitalStamp() throws IOException, JSONException {
        Touchpoint touchpoint = TestUtil.getMockTouchpoint();
        PaymentType paymentType = TestUtil.getMockPaymentType();
        ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
        mockValidBundle.setDigitalStamp(true);
        mockValidBundle.setDigitalStampRestriction(true);

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(mockValidBundle)));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesDigitalStamp.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10, true);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFees.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(8)
    void calculate_digitalStamp2() throws IOException, JSONException {
        Touchpoint touchpoint = TestUtil.getMockTouchpoint();
        PaymentType paymentType = TestUtil.getMockPaymentType();
        ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
        mockValidBundle.setDigitalStamp(true);

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(mockValidBundle)));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesDigitalStamp2.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10, true);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFees.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

//  @Test
//  @Order(9)
//  void calculate_BIN_with_different_ABI_error() throws IOException, JSONException {
//    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
//    PaymentType paymentType = TestUtil.getMockPaymentType();
//
//    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
//        .thenReturn(
//            Collections.singleton(touchpoint),
//            Collections.singleton(paymentType),
//            Collections.singleton(TestUtil.getMockValidBundle()));
//
//    var paymentOption =
//        TestUtil.readObjectFromFile("requests/getFeesBINwithMultipleABI.json", PaymentOption.class);
//
//    AppException exception =
//        assertThrows(
//            AppException.class, () -> calculatorService.calculate(paymentOption, 10, true));
//
//    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getHttpStatus());
//  }

    @Test
    @Order(10)
    void calculate_SubThreshold() throws IOException, JSONException {
        Touchpoint touchpoint = TestUtil.getMockTouchpoint();
        PaymentType paymentType = TestUtil.getMockPaymentType();
        ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
        mockValidBundle.setMinPaymentAmount(-10L);
        mockValidBundle.setPaymentAmount(-5L);
        mockValidBundle.setIdPsp("111111111111");
        mockValidBundle.setType(BundleType.GLOBAL);

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(mockValidBundle)));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesSubThreshold.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10, true);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesSubThreshold.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(11)
    void calculate_paymentType_Null() throws IOException, JSONException {
        Touchpoint touchpoint = TestUtil.getMockTouchpoint();
        PaymentType paymentType = TestUtil.getMockPaymentType();
        ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
        mockValidBundle.setPaymentType(null);

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(mockValidBundle)));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10, true);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesPaymentTypeNull.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(12)
    void calculate_digitalStamp3() throws IOException, JSONException {
        Touchpoint touchpoint = TestUtil.getMockTouchpoint();
        PaymentType paymentType = TestUtil.getMockPaymentType();
        ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
        mockValidBundle.setDigitalStamp(true);

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(mockValidBundle)));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesDigitalStamp3.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10, true);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFees.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(11)
    void calculate_allCcpFlagDown() throws IOException {
        Touchpoint touchpoint = TestUtil.getMockTouchpoint();
        PaymentType paymentType = TestUtil.getMockPaymentType();

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(TestUtil.getMockValidBundle())));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
        BundleOption result = calculatorService.calculate(paymentOption, 10, false);
        assertEquals(1, result.getBundleOptions().size());
    }

    @Test
    @Order(12)
    void calculate_amexPayment() throws IOException, JSONException {
        ValidBundle validBundle = TestUtil.getMockAmexValidBundle();
        Touchpoint touchpoint = TestUtil.getMockTouchpoint();
        PaymentType paymentType = TestUtil.getMockPaymentType();

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(Collections.singletonList(validBundle));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getAmexFees.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10, true);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getAmexFees.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @ParameterizedTest
    @CsvSource({
        "requests/getFeesMulti.json, responses/getFeesMulti.json",
        "requests/getFeesMultiBinNull.json, responses/getFeesMultiBinNull.json",
        "requests/getFeesMultiPspList.json, responses/getFeesMultiBinNotFound.json",
        "requests/getFeesMultiBinNotFound.json, responses/getFeesMultiBinNotFound.json"
    })
    @Order(13)
    void calculateMulti(String input, String output) throws IOException, JSONException {
        Touchpoint touchpoint = TestUtil.getMockTouchpoint();
        PaymentType paymentType = TestUtil.getMockPaymentType();

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(TestUtil.getMockValidBundle())));

        var paymentOption = TestUtil.readObjectFromFile(input, PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile(output);
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(14)
    void calculateMulti2() throws IOException, JSONException {
        ValidBundle validBundle = TestUtil.getMockValidBundle();
        validBundle.setIdPsp("77777777777");
        Touchpoint touchpoint = TestUtil.getMockTouchpoint();
        PaymentType paymentType = TestUtil.getMockPaymentType();

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(Collections.singletonList(validBundle));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesMulti2.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(15)
    void calculateMulti3() throws IOException, JSONException {
        ValidBundle validBundle = TestUtil.getMockValidBundle();
        validBundle.setIdPsp("77777777777");

        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(Collections.singletonList(validBundle));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti2.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesMulti3.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(16)
    void calculateMulti_invalidTouchpoint() throws IOException {
        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(TestUtil.getMockValidBundle())));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);

        AppException exception =
                assertThrows(
                        AppException.class, () -> calculatorService.calculateMulti(paymentOption, 10, true, true, "random"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    @Order(17)
    void calculateMulti_invalidPaymentMethod() throws IOException {
        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(TestUtil.getMockValidBundle())));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);

        AppException exception =
                assertThrows(
                        AppException.class, () -> calculatorService.calculateMulti(paymentOption, 10, true, true, "random"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    @Order(18)
    void calculateMulti_digitalStamp() throws IOException, JSONException {
        ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
        mockValidBundle.setDigitalStamp(true);
        mockValidBundle.setDigitalStampRestriction(true);

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockPaymentType()));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(mockValidBundle)));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMultiDigitalStamp.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesMulti.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(19)
    void calculateMulti_digitalStamp2() throws IOException, JSONException {
        ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
        mockValidBundle.setDigitalStamp(true);

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockPaymentType()));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(mockValidBundle)));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMultiDigitalStamp2.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesMulti.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

//  @Test
//  @Order(20)
//  void calculateMulti_BIN_with_different_ABI_error() throws IOException, JSONException {
//    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
//    PaymentType paymentType = TestUtil.getMockPaymentType();
//
//    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
//        .thenReturn(
//            Collections.singleton(touchpoint),
//            Collections.singleton(paymentType),
//            Collections.singleton(TestUtil.getMockValidBundle()));
//
//    var paymentOption =
//        TestUtil.readObjectFromFile("requests/getFeesMultiBINwithMultipleABI.json", PaymentOptionMulti.class);
//
//    AppException exception =
//        assertThrows(
//            AppException.class, () -> calculatorService.calculateMulti(paymentOption, 10, true));
//
//    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getHttpStatus());
//  }

    @Test
    @Order(21)
    void calculateMulti_SubThreshold() throws IOException, JSONException {
    	ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
    	mockValidBundle.setMinPaymentAmount(-10L);
    	mockValidBundle.setPaymentAmount(-5L);
    	mockValidBundle.setIdPsp("111111111111");
    	mockValidBundle.setType(BundleType.GLOBAL);
    	mockValidBundle.setCart(true);

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockPaymentType()));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(mockValidBundle)));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMultiSubThreshold.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesMultiSubThreshold.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(22)
    void calculateMulti_paymentType_Null() throws IOException, JSONException {
        ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
        mockValidBundle.setPaymentType(null);

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockPaymentType()));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(mockValidBundle)));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesMultiPaymentTypeNull.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(23)
    void calculateMulti_digitalStamp3() throws IOException, JSONException {
        ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
        mockValidBundle.setDigitalStamp(true);

        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockPaymentType()));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(mockValidBundle)));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMultiDigitalStamp3.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesMulti.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(24)
    void calculateMulti_allCcpFlagDown() throws IOException {
        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockPaymentType()));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(new ArrayList<>(List.of(TestUtil.getMockValidBundle())));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
        it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption result =
                calculatorService.calculateMulti(paymentOption, 10, false, true, "random");
        assertEquals(1, result.getBundleOptions().size());
    }

    @Test
    @Order(25)
    void calculateMulti_amexPayment() throws IOException, JSONException {
        ValidBundle validBundle = TestUtil.getMockAmexValidBundle();
        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockPaymentType()));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(Collections.singletonList(validBundle));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getAmexFeesMulti.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getAmexFeesMulti.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(26)
    void calculateMultiHighCommission() throws IOException, JSONException {
        ValidBundle validBundle = TestUtil.getHighCommissionValidBundle();
        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockPaymentType()));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(Collections.singletonList(validBundle));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesMultiHighCommission.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(27)
    void calculateMultiFullCommission() throws IOException, JSONException {
    	ValidBundle validBundle = TestUtil.getMockValidBundle();
    	validBundle.setCart(true);
        when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
        when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(TestUtil.getMockPaymentType()));
        when(validBundleCacheService.getAllValidBundles())
        .thenReturn(Collections.singletonList(validBundle));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMultiWrongEC.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesMultiWrongEC.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(28)
    void calculateMultiMultipleBundlesOneOutput() throws IOException, JSONException {
    	ValidBundle worseBundle = TestUtil.getMockValidBundle();
    	worseBundle.setId("1");
    	worseBundle.setName("bundle1");
    	worseBundle.setIdPsp("ABC");
    	worseBundle.setIdChannel("13212880150_01_ONUS");
    	worseBundle.setPspBusinessName("psp business name");
    	worseBundle.setPaymentAmount(60L);
    	worseBundle.setDescription(null);

    	ValidBundle betterBundle = TestUtil.getMockValidBundle();
    	betterBundle.setId("2");
    	betterBundle.setName("bundle2");
    	betterBundle.setIdPsp("ABC");
    	betterBundle.setIdChannel("13212880150_02_ONUS");
    	betterBundle.setPspBusinessName("psp business name");
    	betterBundle.setPaymentAmount(55L);
    	betterBundle.setDescription(null);

        List<ValidBundle> bundles = new ArrayList<>();
        bundles.add(worseBundle);
        bundles.add(betterBundle);

        when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMultiSamePsp.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFeesMultiSamePsp.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }


    @Test
    @Order(29)
    void calculateMultiRandomOrderOnUsFirstFlagDoesNotPromoteAnyTransfer() throws IOException {
    	List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
    	when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        assertEquals(6, result.getBundleOptions().size());

        // In this dataset/request combination no resulting transfer is actually marked onUs,
        // so onUsFirst does not move any element to the first position.
        for (int i = 0; i < result.getBundleOptions().size(); i++) {
           assertEquals(false, result.getBundleOptions().get(i).getOnUs());
        }
    }

    @Test
    @Order(30)
    void calculateMultipleBundlesFeeOrder() throws IOException {
    	List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
    	when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, false, "fee");
        assertEquals(6, result.getBundleOptions().size());

        // Check that the fees are in ascending order
        var options = result.getBundleOptions();
        for (int i = 1; i < options.size(); i++) {
            long prevFee = options.get(i - 1).getActualPayerFee();
            long currFee = options.get(i).getActualPayerFee();
            assertTrue(prevFee <= currFee, "Fees are not in ascending order: " + prevFee + " > " + currFee);
        }
    }

    @Test
    @Order(31)
    void calculateMultiFeeOrderOnUsFirstFlagDoesNotCreateOnUsResults() throws IOException {
    	List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
    	when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "fee");
        assertEquals(6, result.getBundleOptions().size());

        // Check that the fees are in ascending order
        var options = result.getBundleOptions();
        for (int i = 1; i < options.size(); i++) {
            long prevFee = options.get(i - 1).getActualPayerFee();
            long currFee = options.get(i).getActualPayerFee();
            assertTrue(prevFee <= currFee, "Fees are not in ascending order: " + prevFee + " > " + currFee);

            if (prevFee == currFee) {
                String prevPspName = options.get(i - 1).getPspBusinessName();
                String currPspName = options.get(i).getPspBusinessName();
                assertTrue(prevPspName.compareTo(currPspName) <= 0,
                        "Psp Names are not in ascending order: " + prevPspName + " > " + currPspName);
            }
        }

        // With this request all returned transfers have onUs=false.
        assertTrue(options.stream().noneMatch(it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getOnUs));
    }

    @Test
    @Order(32)
    void calculateMultipleBundlesPspNameOrder() throws IOException {
    	List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
    	when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, false, "pspname");
        assertEquals(6, result.getBundleOptions().size());

        // Check that the Psp Names are in ascending order
        var options = result.getBundleOptions();
        for (int i = 1; i < options.size(); i++) {
            int prevFeeIndex = i - 1;
            String prevPspName = options.get(prevFeeIndex).getPspBusinessName();
            String currPspName = options.get(i).getPspBusinessName();
            assertTrue(prevPspName.compareTo(currPspName) <= 0, "Psp Names are not in ascending order: " + prevPspName + " > " + currPspName);
        }
    }

    @Test
    @Order(33)
    void calculateMultiPspNameOrderOnUsFirstFlagDoesNotCreateOnUsResults() throws IOException {
    	List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
    	when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "pspname");
        assertEquals(6, result.getBundleOptions().size());

        // Check that the Psp Names are in ascending order and the first element has onUs == true
        var options = result.getBundleOptions();
        for (int i = 1; i < options.size(); i++) {
            String prevPspName = options.get(i - 1).getPspBusinessName();
            String currPspName = options.get(i).getPspBusinessName();
            assertTrue(prevPspName.compareTo(currPspName) <= 0,
                    "Psp Names are not in ascending order: " + prevPspName + " > " + currPspName);
        }

        // With this request all returned transfers have onUs=false.
        assertTrue(options.stream().noneMatch(it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getOnUs));
    }

    @Test
    @Order(34)
    void calculateMultipleBundlesBinNotParsable() throws IOException {
    	List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
    	when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
        paymentOption.setBin("test");
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        var options = result.getBundleOptions();
        for (it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer option : options) {
            assertFalse(option.getOnUs());
        }
    }

    @Test
    @Order(35)
    void calculateMultipleBundlesMultipleIssuers() throws IOException {
        TableEntity duplicateIssuer = getTableEntity("7", "1005066000000000000", "1005066999999999999", "11111");
        Initializer.table.createEntity(duplicateIssuer);

        List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
        when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
        var options = result.getBundleOptions();
        for (it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer option : options) {
            assertFalse(option.getOnUs());
        }
        Initializer.table.deleteEntity(duplicateIssuer);
    }

    @Test
    @Order(36)
    void calculateMultipleBundlesFeeRandomOrder() throws IOException {
    	List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
    	when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption =
            TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);

        for (int i = 0; i < 100; i++) {
            var result = calculatorService.calculateMulti(paymentOption, 10, true, false, "feerandom");
            assertEquals(6, result.getBundleOptions().size());

            var options = result.getBundleOptions();
            for (int j = 1; j < options.size(); j++) {
                long prevFee = options.get(j - 1).getActualPayerFee();
                long currFee = options.get(j).getActualPayerFee();
                assertTrue(prevFee <= currFee,
                    "Fees are not in ascending order at iteration " + i + ": " + prevFee + " > " + currFee);
            }
        }
    }

    @Test
    @Order(37)
    void calculateMultiFeeRandomOrderOnUsFirstFlagDoesNotCreateOnUsResults() throws IOException {
    	List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
    	when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption =
            TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);

        for (int i = 0; i < 100; i++) {
            var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "feerandom");
            assertEquals(6, result.getBundleOptions().size());

            var options = result.getBundleOptions();
            assertTrue(options.stream().noneMatch(it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getOnUs),
                    "No resulting transfer should be onUs in this dataset/request combination");

            for (int j = 1; j < options.size(); j++) {
                long prevFee = options.get(j - 1).getActualPayerFee();
                long currFee = options.get(j).getActualPayerFee();
                assertTrue(prevFee <= currFee,
                    "Fees are not in ascending order at iteration " + i + ": " + prevFee + " > " + currFee);
            }
        }
    }
    
    @Test
    @Order(38)
    void calculateMultiOnUsFirstMovesRealOnUsTransferToFirstPosition() throws IOException {
        List<ValidBundle> bundles = TestUtil.getMockMultiBundlesWithRealOnUsAndOffUs();
        when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);

        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "feerandom");

        assertFalse(result.getBundleOptions().isEmpty());
        assertTrue(
                result.getBundleOptions().stream()
                        .anyMatch(it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getOnUs)
        );
        assertTrue(result.getBundleOptions().get(0).getOnUs());
    }
    
    @Test
    @Order(39)
    void calculateMultiWithoutOnUsFirstDoesNotRequireFirstResultToBeOnUs() throws IOException {
        List<ValidBundle> bundles = TestUtil.getMockMultiBundlesWithRealOnUsAndOffUs();
        when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);

        var result = calculatorService.calculateMulti(paymentOption, 10, true, false, "random");

        assertFalse(result.getBundleOptions().isEmpty());
        assertTrue(
                result.getBundleOptions().stream()
                        .anyMatch(it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer::getOnUs)
        );
    }
    
    @Test
    @Order(40)
    void calculateMultiSamePspKeepsOnlyBestBundle() throws IOException {
        ValidBundle worseBundle = TestUtil.getMockValidBundle();
        worseBundle.setId("1");
        worseBundle.setName("bundle1");
        worseBundle.setIdPsp("ABC");
        worseBundle.setPaymentAmount(60L);

        ValidBundle betterBundle = TestUtil.getMockValidBundle();
        betterBundle.setId("2");
        betterBundle.setName("bundle2");
        betterBundle.setIdPsp("ABC");
        betterBundle.setPaymentAmount(55L);

        when(validBundleCacheService.getAllValidBundles())
                .thenReturn(new ArrayList<>(List.of(worseBundle, betterBundle)));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMultiSamePsp.json", PaymentOptionMulti.class);
        var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");

        assertEquals(1, result.getBundleOptions().size());
        assertEquals("bundle2", result.getBundleOptions().get(0).getBundleName());
    }
    
    @Test
    @Order(41)
    void calculateExcludesBundleMarkedOffUsWhenAbiMatchesIssuer() throws IOException {
        ValidBundle bundle = TestUtil.getMockValidBundle();
        bundle.setOnUs(false);
        bundle.setAbi("14156");

        when(validBundleCacheService.getAllValidBundles())
                .thenReturn(new ArrayList<>(List.of(bundle)));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

        var result = calculatorService.calculate(paymentOption, 10, true);

        assertEquals(0, result.getBundleOptions().size());
    }
    
    @Test
    @Order(42)
    void calculateKeepsOffUsBundleWhenAbiDoesNotMatchIssuer() throws IOException {
        ValidBundle bundle = TestUtil.getMockValidBundle();
        bundle.setOnUs(false);
        bundle.setAbi("99991");
        bundle.setIdChannel("13212880150_04");

        when(validBundleCacheService.getAllValidBundles())
                .thenReturn(new ArrayList<>(List.of(bundle)));

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

        var result = calculatorService.calculate(paymentOption, 10, true);

        assertEquals(1, result.getBundleOptions().size());
        assertFalse(result.getBundleOptions().get(0).getOnUs());
    }
    
    
    // This test uses a custom valid bundles dataset from the in-memory cache mock.
    @Test
    @Order(Integer.MAX_VALUE)
    void calculateWhenThreeOnUsAndTwoOffUsBundlesMatch() throws IOException {
    	ValidBundle onUs1 = TestUtil.getMockValidBundle();
    	onUs1.setId("1");
    	onUs1.setIdPsp("PSP1");
    	onUs1.setOnUs(true);
    	onUs1.setAbi("14156");
    	onUs1.setIdChannel("13212880150_01_ONUS");
    	onUs1.setName("bundle1");

    	ValidBundle onUs2 = TestUtil.getMockValidBundle();
    	onUs2.setId("2");
    	onUs2.setIdPsp("PSP2");
    	onUs2.setOnUs(true);
    	onUs2.setAbi("14156");
    	onUs2.setIdChannel("13212880150_02_ONUS");
    	onUs2.setName("bundle2");

    	ValidBundle onUs3 = TestUtil.getMockValidBundle();
    	onUs3.setId("3");
    	onUs3.setIdPsp("PSP3");
    	onUs3.setOnUs(true);
    	onUs3.setAbi("14156");
    	onUs3.setIdChannel("13212880150_03_ONUS");
    	onUs3.setName("bundle3");

        ValidBundle offUs1 = TestUtil.getMockValidBundle();
        offUs1.setId("4");
        offUs1.setIdPsp("PSP4");
        offUs1.setOnUs(false);
        offUs1.setAbi("99991");
        offUs1.setIdChannel("13212880150_04");
        offUs1.setName("bundle4");

        ValidBundle offUs2 = TestUtil.getMockValidBundle();
        offUs2.setId("5");
        offUs2.setIdPsp("PSP5");
        offUs2.setOnUs(false);
        offUs2.setAbi("99992");
        offUs2.setIdChannel("13212880150_05");
        offUs2.setName("bundle5");

        List<ValidBundle> bundles = new ArrayList<>();
        bundles.add(onUs1);
        bundles.add(onUs2);
        bundles.add(onUs3);
        bundles.add(offUs1);
        bundles.add(offUs2);

        when(validBundleCacheService.getAllValidBundles()).thenReturn(bundles);

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMultipleTransfer.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10, true);

        assertEquals(5, result.getBundleOptions().size());
        assertEquals(true, result.getBundleOptions().get(0).getOnUs());
        assertEquals(true, result.getBundleOptions().get(1).getOnUs());
        assertEquals(true, result.getBundleOptions().get(2).getOnUs());
        assertEquals(false, result.getBundleOptions().get(3).getOnUs());
        assertEquals(false, result.getBundleOptions().get(4).getOnUs());
    }
}
