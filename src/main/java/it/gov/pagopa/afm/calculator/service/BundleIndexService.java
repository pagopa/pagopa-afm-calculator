package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * In-memory composite index for ValidBundles.
 * Groups bundles by (touchpoint, paymentType) for fast lookups.
 * Rebuild is O(N) single pass; query-time starts from a small subset.
 */
@Slf4j
@Service
public class BundleIndexService {

    private static final String NULL_KEY = "_NULL_";
    private static final String ANY_KEY = "ANY";
    private static final String SEPARATOR = "|";

    /** Composite key: "TOUCHPOINT|PAYMENTTYPE" → list of bundles. */
    private final AtomicReference<Map<String, List<ValidBundle>>> compositeIndex =
            new AtomicReference<>(Collections.emptyMap());

    /** All bundles, for fallback when no filters apply. */
    private final AtomicReference<List<ValidBundle>> allBundlesRef =
            new AtomicReference<>(Collections.emptyList());

    /**
     * Rebuild the composite index from the full bundle list.
     * Called on cache miss (once per TTL cycle).
     */
    public void rebuildIndex(List<ValidBundle> bundles) {
        long start = System.currentTimeMillis();

        Map<String, List<ValidBundle>> newIndex = bundles.stream()
                .collect(Collectors.groupingBy(this::compositeKey));

        this.compositeIndex.set(newIndex);
        this.allBundlesRef.set(bundles);

        long elapsed = System.currentTimeMillis() - start;
        log.info("[BUNDLE-INDEX] Rebuilt composite index: {} bundles, {} groups in {}ms",
                bundles.size(), newIndex.size(), elapsed);
    }

    /**
     * Get bundles matching the given touchpoint and paymentType.
     * Mirrors the old Cosmos query logic:
     * - touchpoint: exact match OR "ANY" OR null bundle value
     * - paymentType: exact match OR null bundle value
     */
    public List<ValidBundle> getBundlesByTouchpointAndPaymentType(
            String resolvedTouchpoint, String resolvedPaymentType) {

        if (resolvedTouchpoint == null && resolvedPaymentType == null) {
            return allBundlesRef.get();
        }

        List<String> tpKeys = buildTouchpointKeys(resolvedTouchpoint);
        List<String> ptKeys = buildPaymentTypeKeys(resolvedPaymentType);

        return lookupComposite(tpKeys, ptKeys);
    }

    /** Backward-compatible: touchpoint-only lookup. */
    public List<ValidBundle> getBundlesByTouchpoint(String resolvedTouchpoint) {
        return getBundlesByTouchpointAndPaymentType(resolvedTouchpoint, null);
    }

    private String compositeKey(ValidBundle b) {
        String tp = b.getTouchpoint() != null ? b.getTouchpoint().toUpperCase() : NULL_KEY;
        String pt = b.getPaymentType() != null ? b.getPaymentType().toUpperCase() : NULL_KEY;
        return tp + SEPARATOR + pt;
    }

    private String compositeKey(String tp, String pt) {
        return tp + SEPARATOR + pt;
    }

    private List<String> buildTouchpointKeys(String resolvedTouchpoint) {
        return resolvedTouchpoint == null
                ? Collections.emptyList()
                : List.of(resolvedTouchpoint.toUpperCase(), ANY_KEY, NULL_KEY);
    }

    private List<String> buildPaymentTypeKeys(String resolvedPaymentType) {
        return resolvedPaymentType == null
                ? Collections.emptyList()
                : List.of(resolvedPaymentType.toUpperCase(), NULL_KEY);
    }

    private List<ValidBundle> lookupComposite(List<String> tpKeys, List<String> ptKeys) {
        Map<String, List<ValidBundle>> index = compositeIndex.get();
        boolean hasTp = !tpKeys.isEmpty();
        boolean hasPt = !ptKeys.isEmpty();

        // Both dimensions filtered → targeted key lookups (fastest path)
        if (hasTp && hasPt) {
            return tpKeys.stream()
                    .flatMap(tp -> ptKeys.stream().map(pt -> compositeKey(tp, pt)))
                    .map(index::get)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .toList();
        }

        // Single dimension filtered → scan index entries matching that dimension
        return index.entrySet().stream()
                .filter(e -> matchesDimension(e.getKey(), tpKeys, ptKeys, hasTp))
                .flatMap(e -> e.getValue().stream())
                .toList();
    }

    private boolean matchesDimension(String key, List<String> tpKeys, List<String> ptKeys, boolean filterByTouchpoint) {
        int sep = key.indexOf(SEPARATOR);
        if (filterByTouchpoint) {
            return tpKeys.contains(key.substring(0, sep));
        }
        return ptKeys.contains(key.substring(sep + 1));
    }
}
