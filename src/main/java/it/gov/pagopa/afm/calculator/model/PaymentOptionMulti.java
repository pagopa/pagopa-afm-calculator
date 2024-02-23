package it.gov.pagopa.afm.calculator.model;

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
  @Valid @NotNull @NotEmpty private List<PaymentNoticeItem> paymentNotice;

  public Long getPaymentAmount () {
    return this.getPaymentNotice().stream().mapToLong(PaymentNoticeItem::getPaymentAmount).sum();
  }
}
