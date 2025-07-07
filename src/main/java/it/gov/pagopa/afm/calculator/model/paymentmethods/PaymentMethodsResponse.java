package it.gov.pagopa.afm.calculator.model.paymentmethods;

import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
public class PaymentMethodsResponse {
    @NotNull
            @Valid
    List<PaymentMethodsItem> paymentMethods;
}
