package it.gov.pagopa.afm.calculator.model.calculator;

import it.gov.pagopa.afm.calculator.model.PaymentMethod;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Builder
@Data
@ToString
public class Transfer implements Comparable<Transfer> {
    private Long taxPayerFee;
    private long primaryCiIncurredFee;
    private PaymentMethod paymentMethod;
    private String idTouchpoint;
    private String idBundle;
    private String bundleName;
    private String bundleDescription;
    private String idCiBundle;
    private String idPsp;

    @Override
    public int compareTo(Transfer t) {
        return this.getTaxPayerFee().compareTo(t.getTaxPayerFee());
    }
}

