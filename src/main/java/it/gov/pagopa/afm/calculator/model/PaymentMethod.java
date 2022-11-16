package it.gov.pagopa.afm.calculator.model;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    ANY("ANY"),
    PPAL("PPAL"),
    BPAY("BPAY"),
    PAYBP("PayBP"),
    BBT("BBT"),
    AD("AD"),
    CP("CP"),
    PO("PO"),
    JIF("JIF"),
    MYBK("MYBK");

    private final String value;

    PaymentMethod(final String paymentMethod) {
        this.value = paymentMethod;
    }

}
