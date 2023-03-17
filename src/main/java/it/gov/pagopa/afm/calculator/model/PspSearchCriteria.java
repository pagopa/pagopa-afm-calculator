package it.gov.pagopa.afm.calculator.model;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class PspSearchCriteria {
  @NotNull @NotEmpty private String idPsp;
  private String idChannel;
  private String idBrokerPsp;

}
