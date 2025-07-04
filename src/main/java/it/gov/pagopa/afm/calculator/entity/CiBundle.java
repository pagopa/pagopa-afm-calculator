package it.gov.pagopa.afm.calculator.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.annotation.Id;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Container(containerName = "cibundles")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CiBundle {

    @Id
    @NotBlank
    private String id;

    @NotBlank
    private String ciFiscalCode;

    @Valid
    private List<CiBundleAttribute> attributes;
}
