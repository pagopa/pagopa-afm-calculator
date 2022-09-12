package it.gov.pagopa.afm.calculator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentOptionByPsp {
    private Long paymentAmount;
    private String primaryCreditorInstitution;
    private PaymentMethod paymentMethod;
    private Touchpoint touchpoint;
    private ArrayList<TransferListItem> transferList;
}



