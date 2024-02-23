package it.gov.pagopa.afm.calculator.model.calculatormulti;

import lombok.*;
import java.io.Serializable;

@Builder
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Fee implements Serializable {
  /** generated serialVersionUID */
  private static final long serialVersionUID = 1287710978645388173L;

  private String creditorInstitution;
  private long primaryCiIncurredFee;
  private long actualCiIncurredFee;
}
