package it.gov.pagopa.afm.calculator.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@Builder
public class TransferListItem {
  private String creditorInstitution;
  private String transferCategory;
  private Boolean digitalStamp;
}
