package it.gov.pagopa.afm.calculator.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.gov.pagopa.afm.calculator.TestUtil;
import it.gov.pagopa.afm.calculator.entity.IssuerRangeEntity;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.repository.PaymentTypeRepository;
import it.gov.pagopa.afm.calculator.repository.TouchpointRepository;

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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Slf4j
class CalculatorServiceFeeRandomTest {

    @Mock
    UtilityComponent utilityComponent;

    @Mock
    IssuersService issuersService;

    @Mock
    ValidBundleCacheService validBundleCacheService;

    @Mock
    TouchpointRepository touchpointRepository;

    @Mock
    PaymentTypeRepository paymentTypeRepository;

    CalculatorService calculatorService;

    @BeforeEach
    void setup() {
        calculatorService = new CalculatorService(
                "1",
                utilityComponent,
                issuersService,
                "AMREX",
                "POSTE_ID",
                List.of(),
                validBundleCacheService,
                touchpointRepository,
                paymentTypeRepository
        );

        IssuerRangeEntity issuer = new IssuerRangeEntity();
        issuer.setLowRange(1005066000000000000L);
        issuer.setHighRange(1005066999999999999L);
        issuer.setAbi("14156");

        when(issuersService.getIssuerRangeTableCached())
                .thenReturn(List.of(issuer));
    }

    @Test
    void calculateMultiBundlesAndVerifyRandomOrderOnSameFee() throws IOException {
        List<ValidBundle> bundles = TestUtil.getMockFeeRandomBundlesSameFeeOffUs();

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMultiFeeRandom.json", PaymentOptionMulti.class);

        Set<String> observedOrders = new HashSet<>();
        int maxIterations = 1000;
        int i = 0;

        while (observedOrders.size() < 2 && i < maxIterations) {
            var result = calculatorService.calculateForPaymentMethods(
                    bundles,
                    paymentOption,
                    10,
                    false,
                    "feerandom"
            );

            var options = result.getBundleOptions();

            assertFalse(options.isEmpty(), "Expected at least one resulting bundle option");

            for (int j = 1; j < options.size(); j++) {
                long prevFee = options.get(j - 1).getActualPayerFee();
                long currFee = options.get(j).getActualPayerFee();
                assertTrue(prevFee <= currFee,
                        "Fees are not in ascending order at iteration " + i + ": " + prevFee + " > " + currFee);
            }

            String orderSignature = options.stream()
                    .map(t -> t.getIdPsp())
                    .collect(Collectors.joining(","));

            observedOrders.add(orderSignature);
            log.info(orderSignature);
            i++;
        }

        assertTrue(observedOrders.size() > 1,
                "PSP order for equal fees never changed after " + maxIterations + " iterations");
    }

    @Test
    void calculateMultiBundlesAndVerifyRandomOrderOnSameFeeOnusFirst() throws IOException {
        List<ValidBundle> bundles = TestUtil.getMockFeeRandomBundlesSameFeeWithOnUs();

        var paymentOption =
                TestUtil.readObjectFromFile("requests/getFeesMultiFeeRandom.json", PaymentOptionMulti.class);

        Set<String> observedOrders = new HashSet<>();
        int maxIterations = 1000;
        int i = 0;

        while (observedOrders.size() < 2 && i < maxIterations) {
            var result = calculatorService.calculateForPaymentMethods(
                    bundles,
                    paymentOption,
                    10,
                    true,
                    "feerandom"
            );

            var options = result.getBundleOptions();

            assertFalse(options.isEmpty(), "Expected at least one resulting bundle option");
            assertTrue(Boolean.TRUE.equals(options.get(0).getOnUs()),
                    "First element should be onUs at iteration " + i);

            for (int j = 2; j < options.size(); j++) {
                long prevFee = options.get(j - 1).getActualPayerFee();
                long currFee = options.get(j).getActualPayerFee();
                assertTrue(prevFee <= currFee,
                        "Fees are not in ascending order at iteration " + i + ": " + prevFee + " > " + currFee);
            }

            String orderSignature = options.stream()
                    .filter(t -> !Boolean.TRUE.equals(t.getOnUs()))
                    .map(t -> t.getIdPsp())
                    .collect(Collectors.joining(","));

            observedOrders.add(orderSignature);
            log.info(orderSignature);
            i++;
        }

        assertTrue(observedOrders.size() > 1,
                "PSP order for equal fees never changed after " + maxIterations + " iterations");
    }
}