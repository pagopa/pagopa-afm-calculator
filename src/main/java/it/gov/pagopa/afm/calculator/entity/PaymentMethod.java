package it.gov.pagopa.afm.calculator.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.GeneratedValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@SuperBuilder(toBuilder = true)
@Container(containerName = "paymentmethods")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentMethod {

    @Id
    @GeneratedValue
    private String id;

    @JsonProperty("payment_method_id")
    @NotNull
    private String paymentMethodId;
    @NotNull
    private PaymentMethodGroup group;
    @NotNull
    private Map<Language, String> name;
    @NotNull
    private Map<Language, String> description;
    @NotNull
    @JsonProperty("user_touchpoint")
    private List<UserTouchpoint> userTouchpoint;
    @NotNull
    @JsonProperty("user_device")
    private List<UserDevice> userDevice;
    @NotNull
    private PaymentMethodStatus status;
    @NotNull
    @JsonProperty("types")
    private List<PaymentMethodType> types;

    @JsonProperty("validity_date_from")
    @NotNull
    private LocalDate validityDateFrom;

    private List<String> target;

    @JsonProperty("range_amount")
    @NotNull
    private FeeRange rangeAmount;

    private Map<String, String> metadata;

    @NotNull
    @JsonProperty("payment_method_asset")
    private String paymentMethodAsset;

    @NotNull
    @JsonProperty("method_management")
    private MethodManagement methodManagement;

    @JsonProperty("payment_methods_brand_assets")
    private Map<String, String> paymentMethodsBrandAssets;


}
