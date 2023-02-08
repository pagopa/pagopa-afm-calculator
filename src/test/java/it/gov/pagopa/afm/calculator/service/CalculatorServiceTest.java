package it.gov.pagopa.afm.calculator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

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
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.repository.CosmosRepository;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = {Initializer.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class CalculatorServiceTest {

    @Autowired
    CalculatorService calculatorService;

    @MockBean
    CosmosTemplate cosmosTemplate;
    
    
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
    	e.setLowRange("5143170000000000000");
    	e.setHighRange("5143179999999999999");
    	e.setCircuit("MAST");
    	e.setProductCode("CIR");
    	e.setProductType("1");
    	e.setProductCategory("D");
    	e.setIssuerId("329");
    	e.setAbi("80007");
		Initializer.table.execute(TableOperation.insert(e));
		
		
		// two records with same BIN and same ABI
		e = new IssuerRangeEntity("1005066", "300000");
		e.setLowRange("3000000000000000000");
    	e.setHighRange("3059999999999999999");
    	e.setCircuit("DINERS");
    	e.setProductCode("N");
    	e.setProductType("2");
    	e.setProductCategory("C");
    	e.setIssuerId("100");
    	e.setAbi("14156");
		Initializer.table.execute(TableOperation.insert(e));
		
		e = new IssuerRangeEntity("1005066", "300001");
		e.setLowRange("3100000000000000000");
    	e.setHighRange("3159999999999999999");
    	e.setCircuit("DINERS");
    	e.setProductCode("N");
    	e.setProductType("2");
    	e.setProductCategory("C");
    	e.setIssuerId("100");
    	e.setAbi("14156");
		Initializer.table.execute(TableOperation.insert(e));
	}

    @Test
    @Order(1)
    void calculate() throws IOException, JSONException {
        Touchpoint touchpoint = TestUtil.getMockTouchpoints();
        PaymentType paymentType = TestUtil.getMockPaymentType();

        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(
                Collections.singleton(touchpoint), Collections.singleton(paymentType), Collections.singleton(TestUtil.getMockValidBundle()));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFees.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(2)
    void calculate2() throws IOException, JSONException {
        ValidBundle validBundle = TestUtil.getMockValidBundle();
        validBundle.setIdPsp("77777777777");
        Touchpoint touchpoint = TestUtil.getMockTouchpoints();
        PaymentType paymentType = TestUtil.getMockPaymentType();

        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(
                Collections.singleton(touchpoint), Collections.singleton(paymentType), Collections.singleton(validBundle));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFees2.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(3)
    void calculate3() throws IOException, JSONException {
        ValidBundle validBundle = TestUtil.getMockValidBundle();
        validBundle.setIdPsp("77777777777");

        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(
                Collections.singleton(validBundle)
        );

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees2.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10);
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

        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(
                Collections.singleton(TestUtil.getMockTouchpoints()), Collections.singleton(TestUtil.getMockPaymentType()), list);

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees_noInTransfer.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFees_noInTransfer.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    @Order(5)
    void calculate_invalidTouchpoint() throws IOException {
        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(
                Collections.emptyList(), Collections.singleton(TestUtil.getMockValidBundle()));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

        AppException exception = assertThrows(AppException.class, () ->
                calculatorService.calculate(paymentOption, 10));

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    @Order(6)
    void calculate_invalidPaymentMethod() throws IOException {
        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(
                Collections.singleton(TestUtil.getMockTouchpoints()), Collections.emptyList(), Collections.singleton(TestUtil.getMockValidBundle()));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees.json", PaymentOption.class);

        AppException exception = assertThrows(AppException.class, () ->
                calculatorService.calculate(paymentOption, 10));

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

        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(
                Collections.singleton(touchpoint), Collections.singleton(paymentType), Collections.singleton(mockValidBundle));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees_digitalStamp.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10);
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

        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(
                Collections.singleton(touchpoint), Collections.singleton(paymentType), Collections.singleton(mockValidBundle));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFees_digitalStamp2.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10);
        String actual = TestUtil.toJson(result);

        String expected = TestUtil.readStringFromFile("responses/getFees.json");
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }
    
    @Test
    @Order(9)
    void calculate_BIN_with_different_ABI_error() throws IOException, JSONException {
        Touchpoint touchpoint = TestUtil.getMockTouchpoints();
        PaymentType paymentType = TestUtil.getMockPaymentType();

        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(
                Collections.singleton(touchpoint), Collections.singleton(paymentType), Collections.singleton(TestUtil.getMockValidBundle()));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFeesBINwithMultipleABI.json", PaymentOption.class);
        
        
        AppException exception = assertThrows(AppException.class, () ->
        calculatorService.calculate(paymentOption, 10));
        
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getHttpStatus());
    }
    
    @Test
    @Order(10)
    void calculate_BinNotFound() throws IOException, JSONException {
        Touchpoint touchpoint = TestUtil.getMockTouchpoints();
        PaymentType paymentType = TestUtil.getMockPaymentType();

        when(cosmosTemplate.find(any(CosmosQuery.class), any(), anyString())).thenReturn(
                Collections.singleton(touchpoint), Collections.singleton(paymentType), Collections.singleton(TestUtil.getMockValidBundle()));

        var paymentOption = TestUtil.readObjectFromFile("requests/getFeesBinNotFound.json", PaymentOption.class);
        AppException exception = assertThrows(AppException.class, () ->
        calculatorService.calculate(paymentOption, 10));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }
    
    // This must be the last test to run - it needs to mock the cosmosRepository in the service
    @Test
    @Order(Integer.MAX_VALUE)
    void calculate_multipleTransferCreation() throws IOException, JSONException {
    	
    	CosmosRepository cosmosRepository = Mockito.mock(CosmosRepository.class);
    	
    	calculatorService.setCosmosRepository(cosmosRepository);
    	
    	List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundle();
        
        Mockito.doReturn(bundles).when(cosmosRepository).findByPaymentOption(any());

        var paymentOption = TestUtil.readObjectFromFile("requests/getFeesMultipleTransfer.json", PaymentOption.class);
        var result = calculatorService.calculate(paymentOption, 10);
        assertEquals(5, result.size());
        // check order
        assertEquals("1", result.get(0).getIdBundle());
        assertEquals("2", result.get(1).getIdBundle());
        assertEquals("5", result.get(2).getIdBundle());
        assertEquals("3", result.get(3).getIdBundle());
        assertEquals("4", result.get(4).getIdBundle());
    }
}
