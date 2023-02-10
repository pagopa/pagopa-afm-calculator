package it.gov.pagopa.afm.calculator.model;

import lombok.Getter;

@Getter
public enum BundleType {
  GLOBAL("GLOBAL"),
  PUBLIC("PUBLIC"),
  PRIVATE("PRIVATE");

  private final String value;

  BundleType(final String bundleType) {
    this.value = bundleType;
  }
}
