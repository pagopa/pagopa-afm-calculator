package it.gov.pagopa.afm.calculator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import java.util.ArrayList;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentOptionByPsp {
    private Long paymentAmount;
    private String primaryCreditorInstitution;
    private PaymentMethod paymentMethod;
    private String touchpoint;
    @Valid
    private ArrayList<TransferListItem> transferList;
}



