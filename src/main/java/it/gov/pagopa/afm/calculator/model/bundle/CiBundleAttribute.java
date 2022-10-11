package it.gov.pagopa.afm.calculator.model.bundle;

import it.gov.pagopa.afm.calculator.model.TransferCategoryRelation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
public class CiBundleAttribute {

    private String id;
    private Long maxPaymentAmount;
    private String transferCategory;
    private TransferCategoryRelation transferCategoryRelation;
    private LocalDateTime insertedDate;
}
