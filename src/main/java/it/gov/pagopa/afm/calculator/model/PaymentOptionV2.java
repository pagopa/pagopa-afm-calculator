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
public class PaymentOptionV2 {
  private String bin;
  private List<PspSearchCriteria> idPspList;
  private String paymentMethod;
  private String touchpoint;
  @NotNull @Valid private List<PaymentNoticeItem> paymentNotice;
}
