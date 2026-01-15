package it.gov.pagopa.afm.calculator.model.paymentmethods;

import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodsItem {
    @NotNull
    String paymentMethodId;
    @NotNull
    Map<Language, String> name;
    @NotNull
    Map<Language, String> description;
    @NotNull
    PaymentMethodStatus status;
    @NotNull
    LocalDate validityDateFrom;
    @NotNull
    String group;
    @NotNull
    List<PaymentMethodType> paymentMethodTypes;
    FeeRange feeRange;
    @NotNull
    String paymentMethodAsset;
    @NotNull
    MethodManagement methodManagement;
    PaymentMethodDisabledReason disabledReason;
    Map<String, String> paymentMethodsBrandAssets;
    Map<String, String> metadata;
}
