package it.gov.pagopa.afm.calculator.model.paymentmethods;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodsResponse {
    String paymentMethodId;
    Map<Language, String> name;
    Map<Language, String> description;
    PaymentMethodStatus status;
    LocalDateTime validityDateFrom;
    PaymentMethodGroup group;
    Map<String, String> metadata;
    FeeRange feeRange;
    PaymentMethodDisabledReason disabledReason;
    String paymentMethodAsset;
    MethodManagement methodManagement;
    Map<String, String> paymentMethodsBrandAssets;
}
