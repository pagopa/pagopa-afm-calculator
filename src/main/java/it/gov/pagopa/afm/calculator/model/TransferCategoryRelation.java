package it.gov.pagopa.afm.calculator.model;

import it.gov.pagopa.afm.calculator.exception.AppException;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

@Getter
@ToString
public enum TransferCategoryRelation {
    EQUAL("EQUAL"),
    NOT_EQUAL("NOT_EQUAL");

    private final String value;

    TransferCategoryRelation(final String transferCategoryRelation) {
        this.value = transferCategoryRelation;
    }

    public static TransferCategoryRelation fromValue(String value) {
        return Arrays.stream(TransferCategoryRelation.values())
                .filter(elem -> elem.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "TransferCategoryRelation not found", "Cannot convert string '" + value + "' into enum"));
    }
}
