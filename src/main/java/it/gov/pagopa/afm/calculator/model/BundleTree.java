package it.gov.pagopa.afm.calculator.model;

import it.gov.pagopa.afm.calculator.model.bundle.BundleInfo;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class BundleTree {

    private HashMap<String, BundleTreeTaxonomy> psp;

    @Data
    public static class BundleTreeTaxonomy {
        private HashMap<String, BundleTreePaymentMethod> paymentMethod;

    }

    @Data
    public static class BundleTreePaymentMethod {
        private HashMap<String, BundleTreeTouchpoint> taxonomy;
    }

    @Data
    public static class BundleTreeTouchpoint {
        private HashMap<String, List<BundleInfo>> touchpoint;
    }
}
