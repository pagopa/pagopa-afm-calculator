package it.gov.pagopa.afm.calculator.model;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
public class PaymentNoticeItem {
    @NotNull
    private Long paymentAmount;
    @NotNull
    private String primaryCreditorInstitution;
    private List<TransferListItem> transferList;
}
