package it.gov.pagopa.afm.calculator.entity;

import it.gov.pagopa.afm.calculator.model.TransferCategoryRelation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Id;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CiBundleAttribute {

    @Id
    private String id;

    private Long maxPaymentAmount;

    private String transferCategory;

    private TransferCategoryRelation transferCategoryRelation;

}
