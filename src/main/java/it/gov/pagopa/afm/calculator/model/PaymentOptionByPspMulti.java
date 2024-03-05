package it.gov.pagopa.afm.calculator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentOptionByPspMulti {
  private String idChannel;
  private String idBrokerPsp;
  private String paymentMethod;
  private String touchpoint;
  private String bin;
  @Valid @NotNull @NotEmpty private ArrayList<PaymentNoticeItem> paymentNotice;
}
