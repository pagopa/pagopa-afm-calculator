package it.gov.pagopa.afm.calculator.service;

import static it.gov.pagopa.afm.calculator.TestUtil.getTableEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.azure.data.tables.models.TableEntity;
import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.query.CosmosQuery;
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
import it.gov.pagopa.afm.calculator.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.repository.PaymentTypeRepository;
import it.gov.pagopa.afm.calculator.repository.TouchpointRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
class CalculatorServiceFeeRandomTest {

    @Autowired
    CalculatorService calculatorService;

    @Test
    void calculateMultiBundlesAndVerifyRandomOrderOnSameFee() throws IOException {
        CosmosRepository cosmosRepository = Mockito.mock(CosmosRepository.class);
        calculatorService.setCosmosRepository(cosmosRepository);

        List<ValidBundle> bundles = TestUtil.getMockMultipleValidBundlesMultiPspSameFee();
        Mockito.doReturn(bundles).when(cosmosRepository)
            .findByPaymentOption(any(PaymentOptionMulti.class), any(Boolean.class));

        var paymentOption =
            TestUtil.readObjectFromFile("requests/getFeesMulti.json", PaymentOptionMulti.class);

        Set<String> observedOrders = new HashSet<>();
        int maxIterations = 1000;
        int i = 0;

        while (observedOrders.size() < 2 && i < maxIterations) {
            var result = calculatorService.calculateMulti(paymentOption, 10, true, false, "feerandom");
            var options = result.getBundleOptions();

            for (int j = 1; j < options.size(); j++) {
                long prevFee = options.get(j - 1).getActualPayerFee();
                long currFee = options.get(j).getActualPayerFee();
                assertTrue(prevFee <= currFee,
                    "Fees are not in ascending order at iteration " + i + ": " + prevFee + " > " + currFee);
            }

            String orderSignature = options.stream()
                .map(t -> t.getActualPayerFee() + ":" + t.getIdPsp())
                .collect(Collectors.joining(","));
            observedOrders.add(orderSignature);

            i++;
        }

        assertTrue(observedOrders.size() > 1,
            "PSP order for equal fees never changed after " + maxIterations + " iterations");
    }
}
