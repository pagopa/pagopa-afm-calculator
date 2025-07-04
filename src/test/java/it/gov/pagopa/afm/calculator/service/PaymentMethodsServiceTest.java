package it.gov.pagopa.afm.calculator.service;

import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.TableOperation;
import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.entity.IssuerRangeEntity;
import it.gov.pagopa.afm.calculator.entity.PaymentMethod;
import it.gov.pagopa.afm.calculator.initializer.Initializer;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.calculatormulti.Transfer;
import it.gov.pagopa.afm.calculator.model.paymentmethods.*;
import it.gov.pagopa.afm.calculator.repository.PaymentMethodRepository;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = {Initializer.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
