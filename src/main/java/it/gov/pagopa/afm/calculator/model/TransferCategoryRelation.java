package it.gov.pagopa.afm.calculator.model;

import lombok.Getter;

@Getter
public enum TransferCategoryRelation {
  EQUAL("EQUAL"),
  NOT_EQUAL("NOT_EQUAL");

  private final String value;

  TransferCategoryRelation(final String transferCategoryRelation) {
    this.value = transferCategoryRelation;
  }
}
