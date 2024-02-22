package it.gov.pagopa.afm.calculator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.query.CosmosQuery;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.TableOperation;
import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.entity.IssuerRangeEntity;
import it.gov.pagopa.afm.calculator.entity.PaymentType;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.initializer.Initializer;
import it.gov.pagopa.afm.calculator.model.BundleType;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.calculator.BundleOption;
import it.gov.pagopa.afm.calculator.repository.CosmosRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = {Initializer.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class CalculatorServiceTest {

  @Autowired CalculatorService calculatorService;

  @MockBean CosmosTemplate cosmosTemplate;

  @BeforeAll
  void setup() throws StorageException {
    IssuerRangeEntity e = new IssuerRangeEntity("403027", "335106");
    e.setLowRange("4030270000000000000");
    e.setHighRange("4030279999999999999");
    e.setCircuit("VISA");
    e.setProductCode("L");
    e.setProductType("1");
    e.setProductCategory("P");
    e.setIssuerId("453997");
    e.setAbi("1030");
    Initializer.table.execute(TableOperation.insert(e));

    // two records with same BIN but different ABI --> error
    e = new IssuerRangeEntity("504317", "321133");
    e.setLowRange("5043170000000000000");
    e.setHighRange("5043179999999999999");
    e.setCircuit("MAST");
    e.setProductCode("CIR");
    e.setProductType("1");
    e.setProductCategory("D");
    e.setIssuerId("329");
    e.setAbi("80006");
    Initializer.table.execute(TableOperation.insert(e));

    e = new IssuerRangeEntity("504317", "321134");
    e.setLowRange("5043170000000000000");
    e.setHighRange("5043179999999999999");
    e.setCircuit("MAST");
    e.setProductCode("CIR");
    e.setProductType("1");
    e.setProductCategory("D");
    e.setIssuerId("329");
    e.setAbi("80007");
    Initializer.table.execute(TableOperation.insert(e));

    // two records with same BIN and same ABI
    e = new IssuerRangeEntity("1005066", "300000");
    e.setLowRange("1005066000000000000");
    e.setHighRange("1005066999999999999");
    e.setCircuit("DINERS");
    e.setProductCode("N");
    e.setProductType("2");
    e.setProductCategory("C");
    e.setIssuerId("100");
    e.setAbi("14156");
    Initializer.table.execute(TableOperation.insert(e));

    e = new IssuerRangeEntity("1005066", "300001");
    e.setLowRange("1005066000000000000");
    e.setHighRange("1005066999999999999");
    e.setCircuit("DINERS");
    e.setProductCode("N");
    e.setProductType("2");
    e.setProductCategory("C");
    e.setIssuerId("100");
    e.setAbi("14156");
    Initializer.table.execute(TableOperation.insert(e));

    e = new IssuerRangeEntity("340000", "321087");
    e.setLowRange("3400000000000000000");
    e.setHighRange("3499999999999999999");
    e.setCircuit("AMEX");
    e.setProductCode("99");
    e.setProductType("3");
    e.setProductCategory("C");
    e.setIssuerId("999999");
    e.setAbi("AMREX");
    Initializer.table.execute(TableOperation.insert(e));
  }

  @ParameterizedTest
  @CsvSource({
    "requests/getFees.json, responses/getFees.json",
    "requests/getFeesBinNull.json, responses/getFeesBinNull.json",
    "requests/getFeesPspList.json, responses/getFees.json",
    "requests/getFeesBinNotFound.json, responses/getFeesBinNotFound.json"
  })
  @Order(1)
  void calculate(String input, String output) throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(TestUtil.getMockValidBundle()));

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
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(validBundle));

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

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(Collections.singleton(validBundle));

    var paymentOption = TestUtil.readObjectFromFile("requests/getFees2.json", PaymentOption.class);
    var result = calculatorService.calculate(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getFees3.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  @Test
  @Order(4)
  void calculate_noInTransfer() throws IOException, JSONException {
    var list = new ArrayList<>();
    list.add(TestUtil.getMockGlobalValidBundle());
    list.add(TestUtil.getMockValidBundle());

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(TestUtil.getMockTouchpoints()),
            Collections.singleton(TestUtil.getMockPaymentType()),
            list);

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
    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(Collections.emptyList(), Collections.singleton(TestUtil.getMockValidBundle()));

    var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

    AppException exception =
        assertThrows(
            AppException.class, () -> calculatorService.calculate(paymentOption, 10, true));

    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  @Order(6)
  void calculate_invalidPaymentMethod() throws IOException {
    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(TestUtil.getMockTouchpoints()),
            Collections.emptyList(),
            Collections.singleton(TestUtil.getMockValidBundle()));

    var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

    AppException exception =
        assertThrows(
            AppException.class, () -> calculatorService.calculate(paymentOption, 10, true));

    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  @Order(7)
  void calculate_digitalStamp() throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();
    ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
    mockValidBundle.setDigitalStamp(true);
    mockValidBundle.setDigitalStampRestriction(true);

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(mockValidBundle));

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
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();
    ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
    mockValidBundle.setDigitalStamp(true);

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(mockValidBundle));

    var paymentOption =
        TestUtil.readObjectFromFile("requests/getFeesDigitalStamp2.json", PaymentOption.class);
    var result = calculatorService.calculate(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getFees.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  @Test
  @Order(9)
  void calculate_BIN_with_different_ABI_error() throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(TestUtil.getMockValidBundle()));

    var paymentOption =
        TestUtil.readObjectFromFile("requests/getFeesBINwithMultipleABI.json", PaymentOption.class);

    AppException exception =
        assertThrows(
            AppException.class, () -> calculatorService.calculate(paymentOption, 10, true));

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getHttpStatus());
  }

  @Test
  @Order(10)
  void calculate_SubThreshold() throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();
    ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
    mockValidBundle.setMinPaymentAmount(-10L);
    mockValidBundle.setPaymentAmount(-5L);
    mockValidBundle.setIdPsp("111111111111");
    mockValidBundle.setType(BundleType.GLOBAL);

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(mockValidBundle));

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
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();
    ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
    mockValidBundle.setPaymentType(null);

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(mockValidBundle));

    var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
    var result = calculatorService.calculate(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getFeesPaymentTypeNull.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  @Test
  @Order(12)
  void calculate_digitalStamp3() throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();
    ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
    mockValidBundle.setDigitalStamp(true);

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(mockValidBundle));

    var paymentOption =
        TestUtil.readObjectFromFile("requests/getFeesDigitalStamp3.json", PaymentOption.class);
    var result = calculatorService.calculate(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getFees.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  @Test
  @Order(11)
  void calculate_allCcpFlagDown() throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();
    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(TestUtil.getMockValidBundle()));

    var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
    BundleOption result = calculatorService.calculate(paymentOption, 10, false);
    assertEquals(1, result.getBundleOptions().size());
  }

  @Test
  @Order(12)
  void calculate_amexPayment() throws IOException, JSONException {
    ValidBundle validBundle = TestUtil.getMockAmexValidBundle();
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(validBundle));

    var paymentOption =
        TestUtil.readObjectFromFile("requests/getAmexFees.json", PaymentOption.class);
    var result = calculatorService.calculate(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getAmexFees.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  // This must be the last test to run - it needs to mock the cosmosRepository in the service
  @Test
  @Order(Integer.MAX_VALUE)
  void calculate_multipleTransferCreation() throws IOException, JSONException {

    CosmosRepository cosmosRepository = Mockito.mock(CosmosRepository.class);

    calculatorService.setCosmosRepository(cosmosRepository);

    List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundle();

    Mockito.doReturn(bundles).when(cosmosRepository).findByPaymentOption(any(PaymentOption.class), any(Boolean.class));

    var paymentOption =
        TestUtil.readObjectFromFile("requests/getFeesMultipleTransfer.json", PaymentOption.class);
    var result = calculatorService.calculate(paymentOption, 10, true);
    assertEquals(5, result.getBundleOptions().size());
    // check order
    assertEquals(true, result.getBundleOptions().get(0).getOnUs());
    assertEquals(true, result.getBundleOptions().get(1).getOnUs());
    assertEquals(true, result.getBundleOptions().get(2).getOnUs());
    assertEquals(false, result.getBundleOptions().get(3).getOnUs());
    assertEquals(false, result.getBundleOptions().get(4).getOnUs());
  }

  @ParameterizedTest
  @CsvSource({
      "requests/getFeesMulti.json, responses/getFeesMulti.json",
      "requests/getFeesMultiBinNull.json, responses/getFeesMultiBinNull.json",
      "requests/getFeesMultiPspList.json, responses/getFeesMulti.json",
      "requests/getFeesMultiBinNotFound.json, responses/getFeesMultiBinNotFound.json"
  })
  @Order(13)
  void calculateMulti(String input, String output) throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(TestUtil.getMockValidBundle()));

    var paymentOption = TestUtil.readObjectFromFile(input, PaymentOptionMulti.class);
    var result = calculatorService.calculateMulti(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile(output);
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  @Test
  @Order(14)
  void calculateMulti2() throws IOException, JSONException {
    ValidBundle validBundle = TestUtil.getMockValidBundle();
    validBundle.setIdPsp("77777777777");
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(validBundle));

    var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
    var result = calculatorService.calculateMulti(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getFeesMulti2.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  @Test
  @Order(15)
  void calculateMulti3() throws IOException, JSONException {
    ValidBundle validBundle = TestUtil.getMockValidBundle();
    validBundle.setIdPsp("77777777777");

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(Collections.singleton(validBundle));

    var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti2.json", PaymentOptionMulti.class);
    var result = calculatorService.calculateMulti(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getFeesMulti3.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  @Test
  @Order(16)
  void calculateMulti_invalidTouchpoint() throws IOException {
    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(Collections.emptyList(), Collections.singleton(TestUtil.getMockValidBundle()));

    var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);

    AppException exception =
        assertThrows(
            AppException.class, () -> calculatorService.calculateMulti(paymentOption, 10, true));

    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  @Order(17)
  void calculateMulti_invalidPaymentMethod() throws IOException {
    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(TestUtil.getMockTouchpoints()),
            Collections.emptyList(),
            Collections.singleton(TestUtil.getMockValidBundle()));

    var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);

    AppException exception =
        assertThrows(
            AppException.class, () -> calculatorService.calculateMulti(paymentOption, 10, true));

    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  @Order(18)
  void calculateMulti_digitalStamp() throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();
    ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
    mockValidBundle.setDigitalStamp(true);
    mockValidBundle.setDigitalStampRestriction(true);

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(mockValidBundle));

    var paymentOption =
        TestUtil.readObjectFromFile("requests/getFeesMultiDigitalStamp.json", PaymentOptionMulti.class);
    var result = calculatorService.calculateMulti(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getFeesMulti.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  @Test
  @Order(19)
  void calculateMulti_digitalStamp2() throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();
    ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
    mockValidBundle.setDigitalStamp(true);

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(mockValidBundle));

    var paymentOption =
        TestUtil.readObjectFromFile("requests/getFeesMultiDigitalStamp2.json", PaymentOptionMulti.class);
    var result = calculatorService.calculateMulti(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getFeesMulti.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  @Test
  @Order(20)
  void calculateMulti_BIN_with_different_ABI_error() throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(TestUtil.getMockValidBundle()));

    var paymentOption =
        TestUtil.readObjectFromFile("requests/getFeesMultiBINwithMultipleABI.json", PaymentOptionMulti.class);

    AppException exception =
        assertThrows(
            AppException.class, () -> calculatorService.calculateMulti(paymentOption, 10, true));

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getHttpStatus());
  }

  @Test
  @Order(21)
  void calculateMulti_SubThreshold() throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();
    ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
    mockValidBundle.setMinPaymentAmount(-10L);
    mockValidBundle.setPaymentAmount(-5L);
    mockValidBundle.setIdPsp("111111111111");
    mockValidBundle.setType(BundleType.GLOBAL);

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(mockValidBundle));

    var paymentOption =
        TestUtil.readObjectFromFile("requests/getFeesMultiSubThreshold.json", PaymentOptionMulti.class);
    var result = calculatorService.calculateMulti(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getFeesMultiSubThreshold.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  @Test
  @Order(22)
  void calculateMulti_paymentType_Null() throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();
    ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
    mockValidBundle.setPaymentType(null);

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(mockValidBundle));

    var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
    var result = calculatorService.calculateMulti(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getFeesMultiPaymentTypeNull.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  @Test
  @Order(23)
  void calculateMulti_digitalStamp3() throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();
    ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
    mockValidBundle.setDigitalStamp(true);

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(mockValidBundle));

    var paymentOption =
        TestUtil.readObjectFromFile("requests/getFeesMultiDigitalStamp3.json", PaymentOptionMulti.class);
    var result = calculatorService.calculateMulti(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getFeesMulti.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }

  @Test
  @Order(24)
  void calculateMulti_allCcpFlagDown() throws IOException, JSONException {
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();
    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(TestUtil.getMockValidBundle()));

    var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
    it.gov.pagopa.afm.calculator.model.calculatorMulti.BundleOption result =
        calculatorService.calculateMulti(paymentOption, 10, false);
    assertEquals(1, result.getBundleOptions().size());
  }

  @Test
  @Order(25)
  void calculateMulti_amexPayment() throws IOException, JSONException {
    ValidBundle validBundle = TestUtil.getMockAmexValidBundle();
    Touchpoint touchpoint = TestUtil.getMockTouchpoints();
    PaymentType paymentType = TestUtil.getMockPaymentType();

    when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString()))
        .thenReturn(
            Collections.singleton(touchpoint),
            Collections.singleton(paymentType),
            Collections.singleton(validBundle));

    var paymentOption =
        TestUtil.readObjectFromFile("requests/getAmexFeesMulti.json", PaymentOptionMulti.class);
    var result = calculatorService.calculateMulti(paymentOption, 10, true);
    String actual = TestUtil.toJson(result);

    String expected = TestUtil.readStringFromFile("responses/getAmexFeesMulti.json");
    JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
  }
}
