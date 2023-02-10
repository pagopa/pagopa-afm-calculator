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
  private Long paymentAmount;
  private String primaryCreditorInstitution;
  private String paymentMethod;
  private String touchpoint;
  @Valid private ArrayList<TransferListItem> transferList;
}
