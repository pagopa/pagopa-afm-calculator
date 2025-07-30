package it.gov.pagopa.afm.calculator.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import static it.gov.pagopa.afm.calculator.util.Constant.ISSUER_RANGE_TABLE_CACHE_KEY;

@Configuration
@ConditionalOnExpression("'${cache.enabled}'=='true'")
@EnableCaching
@EnableScheduling
public class CacheConfig {

    @CacheEvict(value = ISSUER_RANGE_TABLE_CACHE_KEY)
    @Scheduled(cron = "${cache.evict.schedule.issuerRange}")
    public void evictIssuerRangeTableCache() {
        // Issuer range table scheduled cache evict
    }
}
