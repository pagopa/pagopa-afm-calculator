package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.repository.ValidBundleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {ValidBundleCacheServiceTest.CacheTestConfig.class})
class ValidBundleCacheServiceTest {

    @Mock
    private ValidBundleRepository validBundleRepository;

    @Mock
    private BundleIndexService bundleIndexService;

    private ValidBundleCacheService validBundleCacheService;

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("validBundles");
        }
    }

    @BeforeEach
    void setUp() {
        validBundleCacheService = new ValidBundleCacheService(validBundleRepository, bundleIndexService);
    }

    @Test
    void getAllValidBundles_shouldReturnBundlesFromCosmosDB() {
        // Arrange
        ValidBundle bundle1 = ValidBundle.builder().idPsp("PSP1").build();
        ValidBundle bundle2 = ValidBundle.builder().idPsp("PSP2").build();
        List<ValidBundle> expectedBundles = Arrays.asList(bundle1, bundle2);
        
        when(validBundleRepository.findAll())
                .thenReturn(expectedBundles);

        // Act
        List<ValidBundle> result = validBundleCacheService.getAllValidBundles();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("PSP1", result.get(0).getIdPsp());
        assertEquals("PSP2", result.get(1).getIdPsp());
        verify(validBundleRepository, times(1)).findAll();
    }

    @Test
    void getAllValidBundles_shouldReturnEmptyListWhenNoData() {
        // Arrange
        when(validBundleRepository.findAll())
                .thenReturn(Arrays.asList());

        // Act
        List<ValidBundle> result = validBundleCacheService.getAllValidBundles();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(validBundleRepository, times(1)).findAll();
    }
}
