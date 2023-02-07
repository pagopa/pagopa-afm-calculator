package it.gov.pagopa.afm.calculator.model.calculator;

import java.util.Comparator;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Builder
@Data
@ToString
public class Transfer implements Comparable<Transfer> {
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


    @Override
    public int compareTo(Transfer t) {
        //return this.getTaxPayerFee().compareTo(t.getTaxPayerFee());
    	// order by onUs and taxPayerFee
    	return Comparator.comparing((Transfer tr)->tr.onUs).reversed().thenComparingLong(tr->tr.taxPayerFee).compare(this, t);
        
    }
}

