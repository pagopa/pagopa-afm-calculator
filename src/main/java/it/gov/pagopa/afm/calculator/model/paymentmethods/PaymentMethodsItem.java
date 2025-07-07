package it.gov.pagopa.afm.calculator.model.paymentmethods;

import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodsItem {
    String paymentMethodId;
    Map<Language, String> name;
    Map<Language, String> description;
    PaymentMethodStatus status;
    LocalDate validityDateFrom;
    PaymentMethodGroup group;
    Map<String, String> metadata;
    FeeRange feeRange;
    PaymentMethodDisabledReason disabledReason;
    String paymentMethodAsset;
    MethodManagement methodManagement;
    Map<String, String> paymentMethodsBrandAssets;
}
