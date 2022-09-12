package it.gov.pagopa.afm.calculator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PaymentOption {
    private Long paymentAmount;
    private String primaryCreditorInstitution;
    private PaymentMethod paymentMethod;
    private Touchpoint touchpoint;
    private List<String> idPspList;
    private ArrayList<TransferListItem> transferList;
}



