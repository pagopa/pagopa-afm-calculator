package it.gov.pagopa.afm.calculator.model.paymentmethods;


import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class FeeRange {
    @NotNull
    Long min;

    @NotNull
    Long max;
}
