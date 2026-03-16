package it.gov.pagopa.afm.calculator.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.repository.CosmosRepository;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
@Slf4j
class CalculatorServiceFeeRandomTest {

    @Mock
    UtilityComponent utilityComponent;

    @Mock
    CosmosRepository cosmosRepository;

    @Mock
    IssuersService issuersService;

    CalculatorService calculatorService;

    @BeforeEach
    void setup() {
        calculatorService = new CalculatorService(
            "100000",
            cosmosRepository,
            utilityComponent,
            issuersService,
            "AMREX"
        );
    }

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
            log.info(orderSignature);
            i++;
        }

        assertTrue(observedOrders.size() > 1,
            "PSP order for equal fees never changed after " + maxIterations + " iterations");
    }
}
