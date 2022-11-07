package it.gov.pagopa.afm.calculator.model.bundle;

import it.gov.pagopa.afm.calculator.model.BundleType;
import it.gov.pagopa.afm.calculator.model.PaymentMethod;
import it.gov.pagopa.afm.calculator.model.Touchpoint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class BundleInfo {

    // Bundle Info
    private String bundleId;
    private String idPsp;
    private String idChannel;
    private String idBrokerPsp;
    private String name;
    private String description;
    private Boolean digitalStamp;
    private Boolean digitalStampRestriction;
    private Long paymentAmount;
    private Long minPaymentAmount;
    private Long maxPaymentAmount;
    private PaymentMethod paymentMethod;
    private Boolean onUs;
    private Touchpoint touchpoint;
    private BundleType type;
    private List<String> transferCategoryList;
    private LocalDate bundleValidityDateFrom;
    private LocalDate bundleValidityDateTo;
    private LocalDateTime bundleInsertedDate;
    private LocalDateTime lastUpdatedDate;

    // CI-Bundle Info
    private String ciBundleId;
    private String ciFiscalCode;
    private List<CiBundleAttribute> attributes;
    private LocalDate validityDateFrom;
    private LocalDate validityDateTo;
    private LocalDateTime insertedDate;

}
