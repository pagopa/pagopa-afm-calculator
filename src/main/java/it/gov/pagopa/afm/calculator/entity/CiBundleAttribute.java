package it.gov.pagopa.afm.calculator.entity;

import it.gov.pagopa.afm.calculator.model.TransferCategoryRelation;
import lombok.*;

import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CiBundleAttribute {

    @Id
    @NotNull
    private String id;

    private Long maxPaymentAmount;

    private String transferCategory;

    private TransferCategoryRelation transferCategoryRelation;
}
