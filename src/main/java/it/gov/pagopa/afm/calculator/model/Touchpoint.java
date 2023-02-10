package it.gov.pagopa.afm.calculator.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@Builder
public class Touchpoint {
  private String id;
  private String name;
  private LocalDateTime creationDate;
}
