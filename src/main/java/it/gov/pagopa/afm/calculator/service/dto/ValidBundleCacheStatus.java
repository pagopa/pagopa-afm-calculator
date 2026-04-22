package it.gov.pagopa.afm.calculator.service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ValidBundleCacheStatus {

    private final boolean loaded;
    private final int size;
    private final Instant lastSuccessfulRefresh;
    private final Instant lastFailedRefresh;
}
