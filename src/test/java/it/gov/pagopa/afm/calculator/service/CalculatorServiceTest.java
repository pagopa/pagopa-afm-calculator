package it.gov.pagopa.afm.calculator.service;

import com.azure.data.tables.models.TableEntity;
import com.azure.spring.data.cosmos.core.CosmosTemplate;
import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.entity.*;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.initializer.Initializer;
import it.gov.pagopa.afm.calculator.model.BundleType;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.calculator.BundleOption;
import it.gov.pagopa.afm.calculator.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.repository.PaymentTypeRepository;
import it.gov.pagopa.afm.calculator.repository.TouchpointRepository;
import org.json.JSONException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = { Initializer.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class CalculatorServiceTest {

        @Autowired
        CalculatorService calculatorService;
        @SpyBean
        CosmosRepository cosmosRepository;

        @MockBean
        CosmosTemplate cosmosTemplate;
        @MockBean
        TouchpointRepository touchpointRepository;
        @MockBean
        PaymentTypeRepository paymentTypeRepository;

        @BeforeAll
        void setup() {
                Initializer.table.createEntity(
                                getTableEntity("1", "4030270000000000000", "4030279999999999999", "1030"));

                Initializer.table.createEntity(
                                getTableEntity("2", "5043170000000000000", "5043179999999999999", "80006"));
                Initializer.table.createEntity(
                                getTableEntity("3", "5043170000000000000", "5043179999999999999", "80007"));

                Initializer.table.createEntity(
                                getTableEntity("4", "1005066000000000000", "1005066999999999999", "14156"));
                Initializer.table.createEntity(
                                getTableEntity("5", "1005066000000000000", "1005066999999999999", "14156"));

                Initializer.table.createEntity(
                                getTableEntity("6", "3400000000000000000", "3499999999999999999", "AMREX"));
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
                Touchpoint touchpoint = TestUtil.getMockTouchpoint();
                PaymentType paymentType = TestUtil.getMockPaymentType();

                when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
                when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(TestUtil.getMockValidBundle()));

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
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(validBundle));

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

                when(cosmosTemplate.findAll(ValidBundle.class))
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

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                list);

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesNoInTransfer.json",
                                PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                String actual = TestUtil.toJson(result);

                String expected = TestUtil.readStringFromFile("responses/getFeesNoInTransfer.json");
                JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
        }

        @Test
        @Order(5)
        void calculate_invalidTouchpoint() throws IOException {
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(TestUtil.getMockValidBundle()));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

                AppException exception = assertThrows(
                                AppException.class, () -> calculatorService.calculate(paymentOption, 10, true));

                assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @Order(6)
        void calculate_invalidPaymentMethod() throws IOException {
                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.empty());
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(TestUtil.getMockValidBundle()));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

                AppException exception = assertThrows(
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
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesDigitalStamp.json",
                                PaymentOption.class);
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
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesDigitalStamp2.json",
                                PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                String actual = TestUtil.toJson(result);

                String expected = TestUtil.readStringFromFile("responses/getFees.json");
                JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
        }

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
                mockValidBundle.setTouchpoint("CHECKOUT");

                when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
                when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
                when(cosmosTemplate.findAll(ValidBundle.class)).thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesSubThreshold.json",
                                PaymentOption.class);
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
                mockValidBundle.setTouchpoint("CHECKOUT");

                when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
                when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
                when(cosmosTemplate.findAll(ValidBundle.class)).thenReturn(Collections.singletonList(mockValidBundle));

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
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesDigitalStamp3.json",
                                PaymentOption.class);
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
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(TestUtil.getMockValidBundle()));

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
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(validBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getAmexFees.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                String actual = TestUtil.toJson(result);

                String expected = TestUtil.readStringFromFile("responses/getAmexFees.json");
                JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
        }

        @Test
        @Order(Integer.MAX_VALUE)
        void calculate_multipleTransferCreation() throws IOException {
                calculatorService.setCosmosRepository(cosmosRepository);

                List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundle();
                Mockito.doReturn(bundles).when(cosmosRepository).findByPaymentOption(any(PaymentOption.class),
                                any(Boolean.class));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMultipleTransfer.json",
                                PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                assertEquals(5, result.getBundleOptions().size());
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
                Touchpoint touchpoint = TestUtil.getMockTouchpoint();
                PaymentType paymentType = TestUtil.getMockPaymentType();

                when(touchpointRepository.findByName(anyString())).thenReturn(Optional.of(touchpoint));
                when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.of(paymentType));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(TestUtil.getMockValidBundle()));

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
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(validBundle));

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

                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(validBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti2.json",
                                PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
                String actual = TestUtil.toJson(result);

                String expected = TestUtil.readStringFromFile("responses/getFeesMulti3.json");
                JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
        }

        @Test
        @Order(16)
        void calculateMulti_invalidTouchpoint() throws IOException {
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(TestUtil.getMockValidBundle()));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);

                AppException exception = assertThrows(
                                AppException.class,
                                () -> calculatorService.calculateMulti(paymentOption, 10, true, true, "random"));

                assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @Order(17)
        void calculateMulti_invalidPaymentMethod() throws IOException {
                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString())).thenReturn(Optional.empty());
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(TestUtil.getMockValidBundle()));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);

                AppException exception = assertThrows(
                                AppException.class,
                                () -> calculatorService.calculateMulti(paymentOption, 10, true, true, "random"));

                assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @Order(18)
        void calculateMulti_digitalStamp() throws IOException, JSONException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setDigitalStamp(true);
                mockValidBundle.setDigitalStampRestriction(true);

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMultiDigitalStamp.json",
                                PaymentOptionMulti.class);
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

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMultiDigitalStamp2.json",
                                PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
                String actual = TestUtil.toJson(result);

                String expected = TestUtil.readStringFromFile("responses/getFeesMulti.json");
                JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
        }

        @Test
        @Order(21)
        void calculateMulti_SubThreshold() throws IOException, JSONException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setMinPaymentAmount(-10L);
                mockValidBundle.setPaymentAmount(-5L);
                mockValidBundle.setIdPsp("111111111111");
                mockValidBundle.setType(BundleType.GLOBAL);
                mockValidBundle.setTouchpoint("CHECKOUT");
                mockValidBundle.setCiBundleList(Collections.emptyList());

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMultiSubThreshold.json",
                                PaymentOptionMulti.class);
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

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(mockValidBundle));

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

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMultiDigitalStamp3.json",
                                PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
                String actual = TestUtil.toJson(result);

                String expected = TestUtil.readStringFromFile("responses/getFeesMulti.json");
                JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
        }

        @Test
        @Order(24)
        void calculateMulti_allCcpFlagDown() throws IOException {
                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(TestUtil.getMockValidBundle()));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
                it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption result = calculatorService
                                .calculateMulti(paymentOption, 10, false, true, "random");
                assertEquals(1, result.getBundleOptions().size());
        }

        @Test
        @Order(25)
        void calculateMulti_amexPayment() throws IOException, JSONException {
                ValidBundle validBundle = TestUtil.getMockAmexValidBundle();
                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(validBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getAmexFeesMulti.json",
                                PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
                String actual = TestUtil.toJson(result);

                String expected = TestUtil.readStringFromFile("responses/getAmexFeesMulti.json");
                JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
        }

        @Test
        @Order(26)
        void calculateMultiHighCommission() throws IOException, JSONException {
                ValidBundle validBundle = TestUtil.getHighCommissionValidBundle();
                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(
                                                Collections.singletonList(validBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
                String actual = TestUtil.toJson(result);

                String expected = TestUtil.readStringFromFile("responses/getFeesMultiHighCommission.json");
                JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
        }

        @Test
        @Order(28)
        void calculateMultiMultipleBundlesOneOutput() throws IOException, JSONException {
                List<ValidBundle> validBundles = TestUtil.getMockMultipleValidBundleSamePsp();
                Mockito.doReturn(validBundles).when(cosmosRepository).findByPaymentOption(any(PaymentOptionMulti.class),
                                any(Boolean.class));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMultiSamePsp.json",
                                PaymentOptionMulti.class);

                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
                String actual = TestUtil.toJson(result);

                String expected = TestUtil.readStringFromFile("responses/getFeesMultiSamePsp.json");
                JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
        }

        @Test
        @Order(29)
        void calculateMultipleBundlesRandomOrderOnusFirst() throws IOException {
                List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();

                Mockito.doReturn(bundles).when(cosmosRepository).findByPaymentOption(any(PaymentOptionMulti.class),
                                any(Boolean.class));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
                assertEquals(10, result.getBundleOptions().size());
                assertEquals(true, result.getBundleOptions().get(0).getOnUs());
                for (int i = 1; i < result.getBundleOptions().size(); i++) {
                        assertEquals(false, result.getBundleOptions().get(i).getOnUs());
                }
        }

        @Test
        @Order(30)
        void calculateMultipleBundlesFeeOrder() throws IOException {
                List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
                Mockito.doReturn(bundles).when(cosmosRepository).findByPaymentOption(any(PaymentOptionMulti.class),
                                any(Boolean.class));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, false, "fee");
                assertEquals(10, result.getBundleOptions().size());

                var options = result.getBundleOptions();
                for (int i = 1; i < options.size(); i++) {
                        long prevFee = options.get(i - 1).getActualPayerFee();
                        long currFee = options.get(i).getActualPayerFee();
                        assertTrue(prevFee <= currFee, "Fees are not in ascending order: " + prevFee + " > " + currFee);
                }
        }

        @Test
        @Order(31)
        void calculateMultipleBundlesFeeOrderOnusFirst() throws IOException {
                List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
                Mockito.doReturn(bundles).when(cosmosRepository).findByPaymentOption(any(PaymentOptionMulti.class),
                                any(Boolean.class));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "fee");
                assertEquals(10, result.getBundleOptions().size());

                var options = result.getBundleOptions();
                for (int i = 1; i < options.size(); i++) {
                        int prevFeeIndex = i - 1;
                        if (prevFeeIndex == 0) {
                                assertEquals(true, result.getBundleOptions().get(0).getOnUs());
                        } else {
                                long prevFee = options.get(prevFeeIndex).getActualPayerFee();
                                long currFee = options.get(i).getActualPayerFee();
                                assertTrue(prevFee <= currFee,
                                                "Fees are not in ascending order: " + prevFee + " > " + currFee);

                                if (prevFee == currFee) {
                                        String prevPspName = options.get(prevFeeIndex).getPspBusinessName();
                                        String currPspName = options.get(i).getPspBusinessName();
                                        assertTrue(prevPspName.compareTo(currPspName) <= 0,
                                                        "Psp Names are not in ascending order: " + prevPspName + " > "
                                                                        + currPspName);
                                }

                        }
                }
        }

        @Test
        @Order(32)
        void calculateMultipleBundlesPspNameOrder() throws IOException {
                List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
                Mockito.doReturn(bundles).when(cosmosRepository).findByPaymentOption(any(PaymentOptionMulti.class),
                                any(Boolean.class));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, false, "pspname");
                assertEquals(10, result.getBundleOptions().size());

                var options = result.getBundleOptions();
                for (int i = 1; i < options.size(); i++) {
                        int prevFeeIndex = i - 1;
                        String prevPspName = options.get(prevFeeIndex).getPspBusinessName();
                        String currPspName = options.get(i).getPspBusinessName();
                        assertTrue(prevPspName.compareTo(currPspName) <= 0,
                                        "Psp Names are not in ascending order: " + prevPspName + " > " + currPspName);
                }
        }

        @Test
        @Order(33)
        void calculateMultipleBundlesPspNameOrderOnusFirst() throws IOException {
                List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
                Mockito.doReturn(bundles).when(cosmosRepository).findByPaymentOption(any(PaymentOptionMulti.class),
                                any(Boolean.class));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "pspname");
                assertEquals(10, result.getBundleOptions().size());

                var options = result.getBundleOptions();
                for (int i = 1; i < options.size(); i++) {
                        int prevFeeIndex = i - 1;

                        if (prevFeeIndex == 0) {
                                assertEquals(true, result.getBundleOptions().get(0).getOnUs());
                        } else {
                                String prevPspName = options.get(prevFeeIndex).getPspBusinessName();
                                String currPspName = options.get(i).getPspBusinessName();
                                assertTrue(prevPspName.compareTo(currPspName) <= 0,
                                                "Psp Names are not in ascending order: " + prevPspName + " > "
                                                                + currPspName);
                        }
                }
        }

        @Test
        @Order(34)
        void calculateMultipleBundlesBinNotParsable() throws IOException {
                List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
                Mockito.doReturn(bundles).when(cosmosRepository).findByPaymentOption(any(PaymentOptionMulti.class),
                                any(Boolean.class));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
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
                TableEntity duplicateIssuer = getTableEntity("7", "1005066000000000000", "1005066999999999999",
                                "11111");
                Initializer.table.createEntity(duplicateIssuer);

                List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPsp();
                bundles.forEach(b -> b.setOnUs(false));
                Mockito.doReturn(bundles).when(cosmosRepository).findByPaymentOption(any(PaymentOptionMulti.class),
                                any(Boolean.class));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
                var options = result.getBundleOptions();
                for (it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer option : options) {
                        assertFalse(option.getOnUs(), "Bundle " + option.getIdBundle() + " should have onUs=false");
                }
                Initializer.table.deleteEntity(duplicateIssuer);
        }

        @Test
        @Order(36)
        void calculate_touchpointAny() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setTouchpoint("ANY");

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertNotNull(result);
                assertEquals(1, result.getBundleOptions().size());
        }

        @Test
        @Order(37)
        void calculate_transferCategoryNullInRequest() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setTransferCategoryList(List.of("category1", "category2"));

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
                paymentOption.getTransferList().forEach(t -> t.setTransferCategory(null));
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertEquals(0, result.getBundleOptions().size());
        }

        @Test
        @Order(38)
        void calculate_pspListWithChannelAndBroker() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setIdPsp("ABC");
                mockValidBundle.setIdChannel("123_01");
                mockValidBundle.setIdBrokerPsp("123");

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesPspList.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertNotNull(result);
                assertEquals(1, result.getBundleOptions().size());
        }

        @Test
        @Order(39)
        void calculateMulti_cartFilterTrue() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setCart(false);

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
                paymentOption.getPaymentNotice().add(paymentOption.getPaymentNotice().get(0));
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
                
                assertEquals(0, result.getBundleOptions().size());
        }

        @Test
        @Order(40)
        void calculateMulti_cartFilterFalse() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setCart(true);

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertNotNull(result);
                assertTrue(result.getBundleOptions().size() > 0);
        }

        @Test
        @Order(41)
        void calculateMulti_filteredCiBundlesNotAllPresent() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                CiBundle ciBundle = new CiBundle();
                ciBundle.setCiFiscalCode("99999999999");
                mockValidBundle.setCiBundleList(List.of(ciBundle));

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
                
                assertEquals(0, result.getBundleOptions().size());
        }

        @Test
        @Order(42)
        void calculate_transferCategoryEmptyString() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setTransferCategoryList(List.of("category1"));

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
                paymentOption.getTransferList().forEach(t -> t.setTransferCategory(""));
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertEquals(0, result.getBundleOptions().size());
        }

        @Test
        @Order(43)
        void calculate_pspListPartialMatch() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setIdPsp("DIFFERENT_PSP");

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesPspList.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertEquals(0, result.getBundleOptions().size());
        }

        @Test
        @Order(44)
        void calculateMulti_touchpointAny() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setTouchpoint("ANY");

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
                
                assertNotNull(result);
                assertEquals(1, result.getBundleOptions().size());
        }

        @Test
        @Order(45)
        void calculate_paymentAmountOutOfRange() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setMinPaymentAmount(100L);
                mockValidBundle.setMaxPaymentAmount(200L);

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
                paymentOption.setPaymentAmount(50L);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertEquals(0, result.getBundleOptions().size());
        }

        @Test
        @Order(46)
        void calculate_touchpointNullInBundle() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setTouchpoint(null);

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertNotNull(result);
                assertEquals(1, result.getBundleOptions().size());
        }

        @Test
        @Order(47)
        void calculate_paymentTypeNotMatching() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setPaymentType("PO");

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertEquals(0, result.getBundleOptions().size());
        }

        @Test
        @Order(48)
        void calculate_pspListChannelNotMatching() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setIdPsp("ABC");
                mockValidBundle.setIdChannel("DIFFERENT_CHANNEL");
                mockValidBundle.setIdBrokerPsp("123");

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesPspList.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertEquals(0, result.getBundleOptions().size());
        }

        @Test
        @Order(49)
        void calculate_transferCategoryBundleNullRequestNotNull() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setTransferCategoryList(null);

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertNotNull(result);
                assertEquals(1, result.getBundleOptions().size());
        }

        @Test
        @Order(50)
        void calculate_allCcpFalseWithPosteBundle() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setIdPsp("testIdPspPoste");

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, false);
                
                assertEquals(0, result.getBundleOptions().size());
        }

        @Test
        @Order(51)
        void calculateMulti_transferListNull() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setType(BundleType.GLOBAL);
                mockValidBundle.setCiBundleList(null);
                mockValidBundle.setTransferCategoryList(null);

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);
                paymentOption.getPaymentNotice().get(0).setTransferList(null);
                var result = calculatorService.calculateMulti(paymentOption, 10, true, true, "random");
                
                assertNotNull(result);
                assertEquals(1, result.getBundleOptions().size());
        }

        @Test
        @Order(52)
        void calculate_paymentAmountAboveMax() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setMinPaymentAmount(0L);
                mockValidBundle.setMaxPaymentAmount(100L);

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
                paymentOption.setPaymentAmount(150L);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertEquals(0, result.getBundleOptions().size());
        }

        @Test
        @Order(53)
        void calculate_touchpointExactMatch() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setTouchpoint("CHECKOUT");

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertNotNull(result);
                assertEquals(1, result.getBundleOptions().size());
        }

        @Test
        @Order(54)
        void calculate_pspListWithEmptyBroker() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setIdPsp("ABC ");
                mockValidBundle.setIdChannel("");
                mockValidBundle.setIdBrokerPsp("ANY_BROKER");

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesPspList.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertNotNull(result);
                assertEquals(1, result.getBundleOptions().size());
        }

        @Test
        @Order(55)
        void calculate_pspListBrokerNotMatching() throws IOException {
                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setIdPsp("ABC");
                mockValidBundle.setIdChannel("123_01");
                mockValidBundle.setIdBrokerPsp("DIFFERENT_BROKER");

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFeesPspList.json", PaymentOption.class);
                
                paymentOption.getIdPspList().clear();
                paymentOption.getIdPspList().add(new it.gov.pagopa.afm.calculator.model.PspSearchCriteria("ABC", "123_01", "123"));
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertEquals(0, result.getBundleOptions().size());
        }

        @Test
        @Order(56)
        void calculate_pspInBlacklist() throws IOException {
                CosmosRepository cosmosRepositoryWithBlacklist = new CosmosRepository(
                                touchpointRepository,
                                paymentTypeRepository,
                                new UtilityComponent(),
                                new ValidBundleCacheService(cosmosTemplate),
                                "testIdPspPoste",
                                List.of("BLACKLISTED_PSP")
                );
                calculatorService.setCosmosRepository(cosmosRepositoryWithBlacklist);

                ValidBundle mockValidBundle = TestUtil.getMockValidBundle();
                mockValidBundle.setIdPsp("BLACKLISTED_PSP");

                when(touchpointRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockTouchpoint()));
                when(paymentTypeRepository.findByName(anyString()))
                                .thenReturn(Optional.of(TestUtil.getMockPaymentType()));
                when(cosmosTemplate.findAll(ValidBundle.class))
                                .thenReturn(Collections.singletonList(mockValidBundle));

                var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
                var result = calculatorService.calculate(paymentOption, 10, true);
                
                assertEquals(0, result.getBundleOptions().size());
                
                calculatorService.setCosmosRepository(cosmosRepository);
        }
}
