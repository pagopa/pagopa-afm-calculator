package it.gov.pagopa.afm.calculator.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnExpression("'${cache.enabled}'=='true'")
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    @Bean
    public CacheManager caffeineCacheManager(CacheProperties cacheProperties) {
        return new CustomCaffeineCacheManager(cacheProperties);
    }

    public static class CustomCaffeineCacheManager extends CaffeineCacheManager {
        private final CacheProperties cacheProperties;

        public CustomCaffeineCacheManager(CacheProperties cacheProperties) {
            this.cacheProperties = cacheProperties;
        }

        @Override
        protected Cache createCaffeineCache(String name) {
            Map<String, Long> ttlMap = Optional.ofNullable(cacheProperties.getTtl()).orElse(Collections.emptyMap());
            Map<String, Long> maxSizeMap = Optional.ofNullable(cacheProperties.getMaxSize()).orElse(Collections.emptyMap());

            long ttl = ttlMap.getOrDefault(name,
                    ttlMap.getOrDefault("default", 60L));


            long maxSize = maxSizeMap.getOrDefault(name,
                    ttlMap.getOrDefault("default", 100L));

            Caffeine<Object, Object> builder = Caffeine.newBuilder()
                    .expireAfterWrite(ttl, TimeUnit.SECONDS)
                    .maximumSize(maxSize);

            return new CaffeineCache(name, builder.build());
        }
    }


}
