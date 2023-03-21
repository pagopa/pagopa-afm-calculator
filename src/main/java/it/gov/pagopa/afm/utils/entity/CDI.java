package it.gov.pagopa.afm.utils.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Container(
    containerName = "${azure.cosmos.cdi-container-name}",
    autoCreateContainer = false,
    ru = "1000")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CDI {
  private String id;
  private String idPsp;
  private String idCdi;
  private String abi;
  private Boolean digitalStamp;
  private String validityDateFrom;
  private List<Detail> details;

  // internal management field
  private StatusType cdiStatus;
  private String cdiErrorDesc;
}
