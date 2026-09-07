package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.PaymentMethod;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.model.PaymentMethodResponse;
import it.gov.pagopa.afm.calculator.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.repository.PaymentMethodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "cache.enabled=true",
        "cache.ttl.paymentMethod=3600",
        "cache.maxSize.paymentMethod=100"
})
class PaymentMethodsServiceCacheTest {

    @Autowired
    private PaymentMethodsService paymentMethodsService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private PaymentMethodRepository paymentMethodRepository;

    @MockBean
    private CosmosRepository cosmosRepository;

    @MockBean
    private CalculatorService calculatorService;

    @BeforeEach
    void clearPaymentMethodCache() {
        Cache cache = cacheManager.getCache("paymentMethod");
        assertNotNull(cache);
        cache.clear();
    }

    @Test
    void whenSamePaymentMethodIsRequestedTwice_thenRepositoryIsCalledOnce() {
        PaymentMethod method = paymentMethod("cosmos-id-1", "pm1");

        when(paymentMethodRepository.findByPaymentMethodId("pm1"))
                .thenReturn(List.of(method));

        PaymentMethodResponse first =
                paymentMethodsService.getPaymentMethod("pm1");

        PaymentMethodResponse second =
                paymentMethodsService.getPaymentMethod("pm1");

        assertEquals("cosmos-id-1", first.getId());
        assertEquals("cosmos-id-1", second.getId());

        verify(paymentMethodRepository, times(1))
                .findByPaymentMethodId("pm1");
    }

    @Test
    void whenDifferentPaymentMethodsAreRequested_thenEachKeyIsCachedIndependently() {
        when(paymentMethodRepository.findByPaymentMethodId("pm1"))
                .thenReturn(List.of(paymentMethod("cosmos-id-1", "pm1")));

        when(paymentMethodRepository.findByPaymentMethodId("pm2"))
                .thenReturn(List.of(paymentMethod("cosmos-id-2", "pm2")));

        paymentMethodsService.getPaymentMethod("pm1");
        paymentMethodsService.getPaymentMethod("pm2");

        paymentMethodsService.getPaymentMethod("pm1");
        paymentMethodsService.getPaymentMethod("pm2");

        verify(paymentMethodRepository, times(1))
                .findByPaymentMethodId("pm1");

        verify(paymentMethodRepository, times(1))
                .findByPaymentMethodId("pm2");
    }

    @Test
    void whenPaymentMethodIsInitiallyNotFound_thenExceptionIsNotCached() {
        PaymentMethod method =
                paymentMethod("cosmos-id-3", "pm-late");

        when(paymentMethodRepository.findByPaymentMethodId("pm-late"))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(method));

        assertThrows(
                AppException.class,
                () -> paymentMethodsService.getPaymentMethod("pm-late")
        );

        PaymentMethodResponse response =
                paymentMethodsService.getPaymentMethod("pm-late");

        assertEquals("cosmos-id-3", response.getId());

        verify(paymentMethodRepository, times(2))
                .findByPaymentMethodId("pm-late");
    }

    @Test
    void whenDuplicatePaymentMethodsAreInitiallyFound_thenExceptionIsNotCached() {
        PaymentMethod first =
                paymentMethod("cosmos-id-4", "dup");

        PaymentMethod second =
                paymentMethod("cosmos-id-5", "dup");

        PaymentMethod corrected =
                paymentMethod("cosmos-id-4", "dup");

        when(paymentMethodRepository.findByPaymentMethodId("dup"))
                .thenReturn(List.of(first, second))
                .thenReturn(List.of(corrected));

        assertThrows(
                AppException.class,
                () -> paymentMethodsService.getPaymentMethod("dup")
        );

        PaymentMethodResponse response =
                paymentMethodsService.getPaymentMethod("dup");

        assertEquals("cosmos-id-4", response.getId());

        verify(paymentMethodRepository, times(2))
                .findByPaymentMethodId("dup");
    }

    private PaymentMethod paymentMethod(
            String id,
            String paymentMethodId
    ) {
        PaymentMethod method = new PaymentMethod();
        method.setId(id);
        method.setPaymentMethodId(paymentMethodId);
        return method;
    }
}