package it.gov.pagopa.afm.calculator.model;

import it.gov.pagopa.afm.calculator.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum Touchpoint {
    ANY("ANY"),
    IO("IO"),
    WISP("WISP"),
    CHECKOUT("CHECKOUT"),
    PSP("PSP");

    private String value;

    public static Touchpoint fromValue(String value) {
        return Arrays.stream(Touchpoint.values())
                .filter(elem -> elem.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Touchpoint not found", "Cannot convert string '" + value + "' into enum"));
    }
}
