package it.gov.pagopa.afm.calculator.model;

import lombok.*;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@Builder
public class TransferListItem {
    @NotNull
    private String creditorInstitution;
    private String transferCategory;
    private Boolean digitalStamp;
}
