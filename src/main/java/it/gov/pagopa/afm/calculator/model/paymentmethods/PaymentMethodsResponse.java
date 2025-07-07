package it.gov.pagopa.afm.calculator.model.paymentmethods;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PaymentMethodsResponse {
    List<PaymentMethodsItem> paymentMethods;
}
