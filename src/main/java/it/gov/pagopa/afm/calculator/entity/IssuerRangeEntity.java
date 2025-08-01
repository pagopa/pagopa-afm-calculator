package it.gov.pagopa.afm.calculator.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssuerRangeEntity {

    @JsonProperty("LOW_RANGE")
    private Long lowRange;
    @JsonProperty("HIGH_RANGE")
    private Long highRange;
    @JsonProperty("ABI")
    private String abi;


}
