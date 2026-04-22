package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.service.dto.ValidBundleCacheStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidBundleCacheService {

    private final CosmosRepository cosmosRepository;

    // AtomicReference variable holding the currently active snapshot of valid bundles in memory.
    private final AtomicReference<List<ValidBundle>> cacheRef = new AtomicReference<>();
    private final AtomicReference<Instant> lastSuccessfulRefreshRef = new AtomicReference<>();
    private final AtomicReference<Instant> lastFailedRefreshRef = new AtomicReference<>();

    @Value("${validbundles.cache.warmup-on-startup:true}")
    private boolean warmupOnStartup;

    // The cache is preloaded on pod startup.
    // Without this warmup, the first user request would incur the latency of a full load from Cosmos.
    @PostConstruct
    public void warmup() {
        if (!warmupOnStartup) {
            log.info("Valid bundles cache warmup on startup disabled");
            return;
        }

        try {
            refreshAndGetAllValidBundles();
            log.info("Valid bundles cache warmup completed");
        } catch (Exception e) {
            log.error("Valid bundles cache warmup failed. First request will retry lazy loading", e);
        }
    }

   /*
    * Lazy loading logic:
    * If the cache exists, return it immediately.
    * Otherwise, enter a synchronized block to:
    * 1. Load data from Cosmos.
    * 2. Save the snapshot into memory.
    * All subsequent requests will use this cached snapshot.
    */
    public List<ValidBundle> getAllValidBundles() {
        List<ValidBundle> cached = cacheRef.get();
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            cached = cacheRef.get();
            if (cached == null) {
                cached = loadAllValidBundles();
                cacheRef.set(cached);
                lastSuccessfulRefreshRef.set(Instant.now());
            }
            return cached;
        }
    }

    /*
     * Refresh logic:
     * - On success: Replace the current snapshot.
     * - On failure: Retain the existing snapshot.
     * - If no previous snapshot exists: Propagate the exception.
     * * Example:
     * 00:20 scheduled refresh fails due to Cosmos error -> Keep using current bundles.
     */
    public List<ValidBundle> refreshAndGetAllValidBundles() {
        synchronized (this) {
            List<ValidBundle> previousSnapshot = cacheRef.get();

            try {
                List<ValidBundle> reloaded = loadAllValidBundles();
                cacheRef.set(reloaded);
                lastSuccessfulRefreshRef.set(Instant.now());

                log.info(
                        "Valid bundles cache refreshed successfully. Previous size: {}, new size: {}",
                        previousSnapshot != null ? previousSnapshot.size() : null,
                        reloaded.size()
                );

                return reloaded;
            } catch (Exception e) {
                lastFailedRefreshRef.set(Instant.now());

                if (previousSnapshot != null && !previousSnapshot.isEmpty()) {
                    log.error("Valid bundles cache refresh failed. Keeping previous snapshot with {} bundles",
                            previousSnapshot.size(), e);
                    return previousSnapshot;
                }

                log.error("Valid bundles cache refresh failed and no previous snapshot is available", e);
                throw e;
            }
        }
    }

    
    // Scheduled to refresh the snapshot from Cosmos daily at 00:20.
    // This ensures data consistency as the AFM world updates validbundles periodically.
    // A 15-minute buffer (post-00:05 upstream reconstruction) provides a safety margin.
    @Scheduled(
            cron = "${validbundles.cache.refresh.cron:0 20 0 * * *}",
            zone = "${validbundles.cache.refresh.time-zone:Europe/Rome}"
    )
    public void scheduledRefresh() {
        log.info("Starting scheduled valid bundles cache refresh");

        try {
            refreshAndGetAllValidBundles();
        } catch (Exception e) {
            log.error("Scheduled valid bundles cache refresh failed", e);
        }
    }

    public void evictValidBundlesCache() {
        cacheRef.set(null);
        log.info("Evicted valid bundles in-memory cache");
    }

    public ValidBundleCacheStatus getStatus() {
        List<ValidBundle> cached = cacheRef.get();

        return ValidBundleCacheStatus.builder()
                .loaded(cached != null)
                .size(cached != null ? cached.size() : 0)
                .lastSuccessfulRefresh(lastSuccessfulRefreshRef.get())
                .lastFailedRefresh(lastFailedRefreshRef.get())
                .build();
    }

    private List<ValidBundle> loadAllValidBundles() {
        List<ValidBundle> bundles = cosmosRepository.findAllValidBundles();

        if (bundles == null || bundles.isEmpty()) {
            log.error("Valid bundles cache reload returned an empty dataset");
            throw new IllegalStateException("Valid bundles cache reload returned an empty dataset");
        }

        log.info("Loaded {} valid bundles into in-memory cache", bundles.size());
        return List.copyOf(bundles);
    }
}