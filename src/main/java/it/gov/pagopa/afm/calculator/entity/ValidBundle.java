package it.gov.pagopa.afm.calculator.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Container(containerName = "validbundles")
public class ValidBundle extends Bundle {

    private List<CiBundle> ciBundleList;
}
