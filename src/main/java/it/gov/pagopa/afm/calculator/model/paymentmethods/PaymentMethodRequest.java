package it.gov.pagopa.afm.calculator.model.paymentmethods;


import it.gov.pagopa.afm.calculator.model.PaymentNoticeItem;
import it.gov.pagopa.afm.calculator.model.PspSearchCriteria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodRequest {

    @NotNull
    UserTouchpoint userTouchpoint;

    @NotNull
    UserDevice userDevice;

    String bin;

    Integer totalAmount;

    List<PaymentNoticeItem> paymentNotice;

    Boolean allCCp;

    String targetKey;

}
