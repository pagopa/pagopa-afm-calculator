package it.gov.pagopa.afm.calculator.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.gov.pagopa.afm.calculator.entity.CiBundleAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class CiBundle {

    @Id
    private String id;

    private String ciFiscalCode;

    private String idBundle;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CiBundleAttribute> attributes;

}
