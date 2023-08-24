package it.gov.pagopa.afm.calculator.model.calculator;

import lombok.*;

@Builder
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Fees {
    private String creditorInstitution;
    private String primaryCiIncurredFee;
    private String actualCiIncurredFee;
}
