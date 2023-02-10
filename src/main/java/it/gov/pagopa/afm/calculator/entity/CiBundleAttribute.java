package it.gov.pagopa.afm.calculator.entity;

import it.gov.pagopa.afm.calculator.model.TransferCategoryRelation;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CiBundleAttribute {

  @Id @NotNull private String id;

  private Long maxPaymentAmount;

  private String transferCategory;

  private TransferCategoryRelation transferCategoryRelation;
}
