package it.gov.pagopa.afm.calculator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentOptionByPspV2 {
  private String bin;
  private String idBrokerPsp;
  private String idChannel;
  private String paymentMethod;
  private String touchpoint;
  @NotNull @Valid private ArrayList<PaymentNoticeItem> transferList;
}
