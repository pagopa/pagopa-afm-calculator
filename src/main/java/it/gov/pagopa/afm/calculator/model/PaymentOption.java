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
public class PaymentOption {
    @NotNull
    private Long paymentAmount;
    @NotNull
    private String primaryCreditorInstitution;
    private String bin;
    private String paymentMethod;
    private String touchpoint;
    private List<PspSearchCriteria> idPspList;
    @Valid
    @NotNull
    @NotEmpty
    private List<TransferListItem> transferList;
}
