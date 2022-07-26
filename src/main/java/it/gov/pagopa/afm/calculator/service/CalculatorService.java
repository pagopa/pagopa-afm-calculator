package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.model.BundleTree;
import it.gov.pagopa.afm.calculator.model.bundle.BundleInfo;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class CalculatorService {


    public Long calculate(
            // TODO: add input
    ) {
        // TODO: build bundleTree
        BundleTree bundleTree = new BundleTree();


        // TODO: get filter from request
        String idPsp = "1";
        String paymentMethod = "BP";
        String taxonomy = null;
        String fiscalCode = "ABCD";
        Double amount = 50.0;

        var bundle = bundleTree.getPsp().get(idPsp)
                .getPaymentMethod().get(paymentMethod)
                .getTaxonomy().get(taxonomy)
                .getTouchpoint().get("IO")
                .parallelStream()
                .filter(bundleInfo -> fiscalCode.equals(bundleInfo.getCiFiscalCode()))
                .filter(bundleInfo -> bundleInfo.getMinPaymentAmount() <= amount && bundleInfo.getMaxPaymentAmount() > amount)
                .min(Comparator.comparing(BundleInfo::getPaymentAmount));

        // TODO handle not found
        return bundle.get()
                .getPaymentAmount();
    }
}
