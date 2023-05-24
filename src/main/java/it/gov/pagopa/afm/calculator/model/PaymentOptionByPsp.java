package it.gov.pagopa.afm.calculator.model;

import java.util.ArrayList;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentOptionByPsp {
  private String idChannel;
  private String idBrokerPsp;
  private Long paymentAmount;
  private String primaryCreditorInstitution;
  private String paymentMethod;
  private String touchpoint;
  private String bin;
  @Valid private ArrayList<TransferListItem> transferList;
}
