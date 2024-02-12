package it.gov.pagopa.afm.calculator.model.calculatorMulti;

import lombok.*;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

@Builder
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Transfer implements Comparable<Transfer>, Serializable {
  /** generated serialVersionUID */
  private static final long serialVersionUID = 1287710978645388173L;

  private Long taxPayerFee;
  private String paymentMethod;
  private String touchpoint;
  private String idBundle;
  private String bundleName;
  private String bundleDescription;
  private String idCiBundle;
  private String idPsp;
  private String idChannel;
  private String idBrokerPsp;
  private Boolean onUs;
  private String abi;
  private String pspBusinessName;
  @Valid @NotNull @NotEmpty private List<Fee> fees;

  @Override
  public int compareTo(Transfer t) {
    // order by onUs
    return Comparator.comparing((Transfer tr) -> tr.onUs).reversed().compare(this, t);
  }
}
