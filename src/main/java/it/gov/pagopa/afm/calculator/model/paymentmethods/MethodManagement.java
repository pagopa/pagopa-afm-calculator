package it.gov.pagopa.afm.calculator.model.paymentmethods;

import lombok.Getter;

@Getter
public enum MethodManagement {

    ONBOARDABLE,
    ONBOARDABLE_ONLY,
    NOT_ONBOARDABLE,
    REDIRECT,
}
