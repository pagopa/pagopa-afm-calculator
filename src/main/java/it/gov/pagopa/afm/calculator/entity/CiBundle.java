package it.gov.pagopa.afm.calculator.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "CIBUNDLE", schema = "AFM_CALCULATOR")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CiBundle {

    @Id
    private String id;

    private String ciFiscalCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="bundleId", referencedColumnName = "id")
    private Bundle bundle;

    @OneToMany (cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CiBundleAttribute> attributes;

}
