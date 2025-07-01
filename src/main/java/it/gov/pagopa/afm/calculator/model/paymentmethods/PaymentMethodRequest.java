package it.gov.pagopa.afm.calculator.model.paymentmethods;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodRequest {

    @NotNull
    UserTouchpoint userTouchpoint;
    @NotNull
    UserDevice userDevice;
    @NotNull
    Integer amount;
    Boolean digitalStamp;
    String targetKey;

}
