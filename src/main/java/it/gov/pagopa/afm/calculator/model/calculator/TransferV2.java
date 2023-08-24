package it.gov.pagopa.afm.calculator.model.calculator;

import lombok.*;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

@Builder
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TransferV2 implements Comparable<TransferV2>, Serializable {
  /** generated serialVersionUID */
  private static final long serialVersionUID = 1287710978645388173L;

  private String abi;
  private String bundleDescription;
  private String bundleName;
  private String idBrokerPsp;
  private String idBundle;
  private String idChannel;
  private String idCiBundle;
  private String idPsp;
  private Boolean onUs;
  private String paymentMethod;
  private Long taxPayerFee;
  private Long actualPayerFee;
  private String touchpoint;
  private List<Fees> fees;

  @Override
  public int compareTo(TransferV2 t) {
    // order by onUs
    return Comparator.comparing((TransferV2 tr) -> tr.onUs).reversed().compare(this, t);
  }
}
