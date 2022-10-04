package it.gov.pagopa.afm.calculator.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.gov.pagopa.afm.calculator.model.BundleType;
import it.gov.pagopa.afm.calculator.model.PaymentMethod;
import it.gov.pagopa.afm.calculator.model.Touchpoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "BUNDLE", schema = "AFM_CALCULATOR")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Bundle {

    @Id
    private String id;
    private String idPsp;

    private String name;
    private String description;

    private Long paymentAmount;
    private Long minPaymentAmount;
    private Long maxPaymentAmount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private Touchpoint touchpoint;

    @Enumerated(EnumType.STRING)
    private BundleType type;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<TransferCategory> transferCategoryList;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "bundle", cascade = CascadeType.REMOVE)
    @ToString.Exclude
    private List<CiBundle> ciBundles = new ArrayList<>();
}
