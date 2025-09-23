package it.gov.pagopa.afm.calculator.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.GeneratedValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.gov.pagopa.afm.calculator.model.paymentmethods.FeeRange;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {

    @NotNull
    private String id;
    @NotNull
    private String paymentMethodId;
    @NotNull
    private PaymentMethodGroup group;
    @NotNull
    private Map<Language, String> name;
    @NotNull
    private Map<Language, String> description;
    @NotNull
    private List<UserTouchpoint> userTouchpoint;
    @NotNull
    private List<UserDevice> userDevice;
    @NotNull
    private PaymentMethodStatus status;
    @NotNull
    private List<PaymentMethodType> paymentMethodTypes;

    @NotNull
    private LocalDate validityDateFrom;

    private List<String> target;

    @NotNull
    private FeeRange rangeAmount;

    private Map<Metadata, String> metadata;

    @NotNull
    private String paymentMethodAsset;

    @NotNull
    private MethodManagement methodManagement;

    private Map<String, String> paymentMethodsBrandAssets;


}
