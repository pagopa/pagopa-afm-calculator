package it.gov.pagopa.afm.calculator.model.calculator;

import it.gov.pagopa.afm.calculator.model.PaymentMethod;
import it.gov.pagopa.afm.calculator.model.Touchpoint;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Transfer implements Comparable<Transfer> {
    private Long taxPayerFee;
    private long primaryCiIncurredFee;
    private PaymentMethod paymentMethod;
    private Touchpoint touchpoint;
    private String idBundle;
    private String idCiBundle;
    private String idPsp;

    @Override
    public int compareTo(Transfer t) {
        return this.getTaxPayerFee().compareTo(t.getTaxPayerFee());
    }
}
