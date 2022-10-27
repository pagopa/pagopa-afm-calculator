package it.gov.pagopa.afm.calculator.model.configuration;

import it.gov.pagopa.afm.calculator.entity.Bundle;
import it.gov.pagopa.afm.calculator.model.Touchpoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Configuration {

    List<Bundle> bundles;
    List<CiBundle> ciBundles;
    List<Touchpoint> touchpoints;
}
