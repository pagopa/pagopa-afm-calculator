package it.gov.pagopa.afm.calculator.model;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@Builder
public class PspSearchCriteria {
  @NotNull @NotEmpty private String idPsp;
  private String idChannel;
  private String idBrokerPsp;
}
