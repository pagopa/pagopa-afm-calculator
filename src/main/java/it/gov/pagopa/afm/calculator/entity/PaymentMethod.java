package it.gov.pagopa.afm.calculator.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.afm.calculator.model.paymentmethods.MethodManagement;
import it.gov.pagopa.afm.calculator.model.paymentmethods.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private String id;

    @JsonProperty("payment_method_id")
    private String paymentMethodId;

    private PaymentMethodGroup group;

    private Map<Language, String> name;

    private Map<Language, String> description;

    @JsonProperty("user_touchpoint")
    private List<UserTouchpoint> userTouchpoint;

    @JsonProperty("user_device")
    private List<UserDevice> userDevice;

    private PaymentMethodStatus status;

    @JsonProperty("validity_date_from")
    private LocalDate validityDateFrom;

    private List<String> target;

    @JsonProperty("range_amount")
    @NotNull
    private FeeRange rangeAmount;

    private Map<String, String> metadata;

    @JsonProperty("payment_method_asset")
    private String paymentMethodAsset;

    @JsonProperty("method_management")
    private MethodManagement methodManagement;

    @JsonProperty("payment_methods_brand_assets")
    private Map<String, String> paymentMethodsBrandAssets;


}
