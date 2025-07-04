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
    private String idChannel;
    private String idBrokerPsp;
    private Long paymentAmount;
    private String primaryCreditorInstitution;
    private String paymentMethod;
    private String touchpoint;
    private String bin;
    @Valid
    private ArrayList<TransferListItem> transferList;
}
