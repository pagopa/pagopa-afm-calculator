package it.gov.pagopa.afm.calculator.model.calculator;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BundleOption implements Serializable {
  /** generated serialVersionUID */
  private static final long serialVersionUID = -7404184031676587394L;

  private Boolean belowThreshold;
  private List<Transfer> bundleOptions;
}
