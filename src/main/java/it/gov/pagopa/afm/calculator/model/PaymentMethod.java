package it.gov.pagopa.afm.calculator.model;

import it.gov.pagopa.afm.calculator.exception.AppException;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

@Getter
@ToString
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

    public static PaymentMethod fromValue(String value) {
        return Arrays.stream(PaymentMethod.values())
                .filter(elem -> elem.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "PaymentMethod not found", "Cannot convert string '" + value + "' into enum"));
    }
}
