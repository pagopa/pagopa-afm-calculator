package it.gov.pagopa.afm.calculator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class TransferListItem {
    private String creditorInstitution;
    private String transferCategory;
    private Boolean digitalStamp;
}
