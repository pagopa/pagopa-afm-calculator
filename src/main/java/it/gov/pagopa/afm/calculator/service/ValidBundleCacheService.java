package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.repository.ValidBundleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Service for caching all valid bundles from Cosmos DB.
 */
@Slf4j
@Service
public class ValidBundleCacheService {

    private final ValidBundleRepository validBundleRepository;

    @Autowired
    public ValidBundleCacheService(ValidBundleRepository validBundleRepository) {
        this.validBundleRepository = validBundleRepository;
    }

    /**
     * Retrieve all valid bundles from Cosmos DB and cache them.
     */
    @Cacheable(value = "validBundles")
    public List<ValidBundle> getAllValidBundles() {
        log.info("[CACHE MISS] Loading all valid bundles from Cosmos DB...");
        Iterable<ValidBundle> result = validBundleRepository.findAll();
        List<ValidBundle> bundles = StreamSupport.stream(result.spliterator(), false).toList();
        log.info("[CACHE MISS] Loaded {} valid bundles from Cosmos DB", bundles.size());
        return bundles;
    }
}
