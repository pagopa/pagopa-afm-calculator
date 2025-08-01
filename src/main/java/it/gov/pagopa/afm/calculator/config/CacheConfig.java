package it.gov.pagopa.afm.calculator.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static it.gov.pagopa.afm.calculator.util.Constant.DEFAULT_CACHE_KEY;
import static it.gov.pagopa.afm.calculator.util.Constant.ISSUER_RANGE_TABLE_CACHE_KEY;

@Configuration
@ConditionalOnExpression("'${cache.enabled}'=='true'")
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${cache.default.evict.seconds}") long defaultCacheEvictSeconds,
            @Value("${cache.default.maximumSize}") long defaultCacheMaximumSize,
            @Value("${cache.issuerRange.evict.seconds}") long issuerRangeTableCacheEvictSeconds,
            @Value("${cache.issuerRange.maximumSize}") long issuerRangeTableCacheMaximumSize
    ) {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        CaffeineCache defaultCache = new CaffeineCache(DEFAULT_CACHE_KEY,
                Caffeine.newBuilder()
                        .expireAfterWrite(defaultCacheEvictSeconds, TimeUnit.SECONDS)
                        .maximumSize(defaultCacheMaximumSize)
                        .build());

        CaffeineCache issuerRangeTableCache = new CaffeineCache(ISSUER_RANGE_TABLE_CACHE_KEY,
                Caffeine.newBuilder()
                        .expireAfterWrite(issuerRangeTableCacheEvictSeconds, TimeUnit.SECONDS)
                        .maximumSize(issuerRangeTableCacheMaximumSize)
                        .build());

        cacheManager.setCaches(Arrays.asList(defaultCache, issuerRangeTableCache));
        return cacheManager;
    }
}
