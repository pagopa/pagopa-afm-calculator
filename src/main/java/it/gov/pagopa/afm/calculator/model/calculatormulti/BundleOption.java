package it.gov.pagopa.afm.calculator.model.calculatormulti;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BundleOption implements Serializable {
    /**
     * generated serialVersionUID
     */
    private static final long serialVersionUID = -7404184031676587394L;

    @Schema(
            description =
                    "if true (the payment amount is lower than the threshold value) the bundles onus is not"
                            + " calculated (always false)")
    private Boolean belowThreshold;

    private List<Transfer> bundleOptions;
}
