package it.gov.pagopa.afm.calculator.model.calculatormulti;

public enum OrderType {

    RANDOM("random"),    // bundles are sorted randomly
    BYFEE("byfee"),      // sorted by increasing fee, if fees are equal then by PSP name
    BYPSPNAME("bypspname");  // sorted by PSP name

    private final String value;

    OrderType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
