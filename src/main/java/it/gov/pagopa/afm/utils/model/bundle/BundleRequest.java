package it.gov.pagopa.afm.utils.model.bundle;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class BundleRequest {
    @NotNull
    private String idChannel;
    @NotNull
    private String idBrokerPsp;
    private String name;
    private String description;
    private Long paymentAmount;
    private Long minPaymentAmount;
    private Long maxPaymentAmount;
    private String paymentType;
    private Boolean onUs;
    private Boolean digitalStamp;
    private Boolean digitalStampRestriction;
    private String touchpoint;
    private BundleType type;
    private List<String> transferCategoryList;
    private LocalDate validityDateFrom;
    private LocalDate validityDateTo;
}
