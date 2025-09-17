package it.gov.pagopa.afm.calculator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
public class PaymentOptionMulti {
    private String bin;
    private String paymentMethod;
    private String touchpoint;
    private List<PspSearchCriteria> idPspList;
    private List<PaymentNoticeItem> paymentNotice;

    @JsonIgnore
    public Long getPaymentAmount() {
        return this.getPaymentNotice().stream().mapToLong(PaymentNoticeItem::getPaymentAmount).sum();
    }
}
