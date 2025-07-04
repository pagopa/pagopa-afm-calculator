package it.gov.pagopa.afm.calculator.model;

import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@Builder
public class PspSearchCriteria {
    @NotNull
    @NotEmpty
    private String idPsp;
    private String idChannel;
    private String idBrokerPsp;
}
