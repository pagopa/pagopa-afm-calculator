package it.gov.pagopa.afm.calculator.model.paymentmethods;


import it.gov.pagopa.afm.calculator.model.PaymentNoticeItem;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.UserDevice;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.UserTouchpoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodRequest {

    @NotNull
    UserTouchpoint userTouchpoint;

    UserDevice userDevice;

    String bin;

    @NotNull
    Integer totalAmount;

    @NotNull
    @NotEmpty
    @Valid
    List<PaymentNoticeItem> paymentNotice;

    Boolean allCCp;

    String targetKey;

}
