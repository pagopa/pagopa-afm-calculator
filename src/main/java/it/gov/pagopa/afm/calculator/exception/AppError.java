package it.gov.pagopa.afm.calculator.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AppError {

    ISSUERS_BIN_WITH_DIFFERENT_ABI_ERROR(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Issuers BIN with different ABI values found",
            "Found ABI with different value for BIN %s"),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Something was wrong"),

    PAYMENT_METHOD_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Payment method not found",
            "Payment method with id %s not found"),
    PAYMENT_METHOD_MULTIPLE_FOUND(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Payment method multiple found",
            "Payment method with id %s multiple found, contact technical support"),

    UNKNOWN(null, null, null);

    public final HttpStatus httpStatus;
    public final String title;
    public final String details;

    AppError(HttpStatus httpStatus, String title, String details) {
        this.httpStatus = httpStatus;
        this.title = title;
        this.details = details;
    }
}
