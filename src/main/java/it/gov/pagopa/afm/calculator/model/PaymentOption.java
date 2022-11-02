package it.gov.pagopa.afm.calculator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
public class PaymentOption {
    @NotNull
    private Long paymentAmount;
    @NotNull
    private String primaryCreditorInstitution;
    private PaymentMethod paymentMethod;
    private Touchpoint touchpoint;
    private List<String> idPspList;
    @Valid
    private ArrayList<TransferListItem> transferList;
}



