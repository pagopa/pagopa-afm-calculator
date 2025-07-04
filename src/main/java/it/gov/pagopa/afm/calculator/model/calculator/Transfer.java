package it.gov.pagopa.afm.calculator.model.calculator;

import lombok.*;

import java.io.Serializable;
import java.util.Comparator;

@Builder
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Transfer implements Comparable<Transfer>, Serializable {
    /**
     * generated serialVersionUID
     */
    private static final long serialVersionUID = 1287710978645388173L;

    private Long taxPayerFee;
    private long primaryCiIncurredFee;
    private String paymentMethod;
    private String touchpoint;
    private String idBundle;
    private String bundleName;
    private String bundleDescription;
    private String idCiBundle;
    private String idPsp;
    private String idChannel;
    private String idBrokerPsp;
    private Boolean onUs;
    private String abi;
    private String pspBusinessName;

    @Override
    public int compareTo(Transfer t) {
        // order by onUs
        return Comparator.comparing((Transfer tr) -> tr.onUs).reversed().compare(this, t);
    }
}
