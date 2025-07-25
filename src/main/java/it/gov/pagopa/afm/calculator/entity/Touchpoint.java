package it.gov.pagopa.afm.calculator.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.persistence.Id;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Container(containerName = "touchpoints")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Touchpoint {

    @Id
    private String id;
    @PartitionKey
    private String name;

    private LocalDateTime creationDate;
}
