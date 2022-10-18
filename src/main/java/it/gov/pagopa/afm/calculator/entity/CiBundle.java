package it.gov.pagopa.afm.calculator.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Id;
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
    private String id;

    @PartitionKey
    private String ciFiscalCode;

    private String idBundle;

    private List<CiBundleAttribute> attributes;

}
