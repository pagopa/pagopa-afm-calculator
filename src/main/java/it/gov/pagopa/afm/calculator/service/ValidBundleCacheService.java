package it.gov.pagopa.afm.calculator.service;

import com.azure.spring.data.cosmos.core.CosmosTemplate;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Service for caching all valid bundles from Cosmos DB.
 * This service provides a cached version of all valid bundles to improve
 * performance
 * by avoiding repeated queries to the Cosmos DB container.
 */
@Service
public class ValidBundleCacheService {

    private final CosmosTemplate cosmosTemplate;

    @Autowired
    public ValidBundleCacheService(CosmosTemplate cosmosTemplate) {
        this.cosmosTemplate = cosmosTemplate;
    }

    /**
     * Retrieve all valid bundles from Cosmos DB and cache them.
     * This method is cached to avoid repeated queries to the validbundles
     * container.
     * The cache is refreshed based on the configured TTL.
     *
     * @return all valid bundles from the database
     */
    @Cacheable(value = "validBundles")
    public List<ValidBundle> getAllValidBundles() {
        Iterable<ValidBundle> result = cosmosTemplate.findAll(ValidBundle.class);
        return StreamSupport.stream(result.spliterator(), false)
                .toList();
    }
}
