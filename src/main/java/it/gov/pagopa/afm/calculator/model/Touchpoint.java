package it.gov.pagopa.afm.calculator.model;

import lombok.*;

import java.time.LocalDateTime;

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
