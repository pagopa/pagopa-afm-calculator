package it.gov.pagopa.afm.utils.model.bundle;

import it.gov.pagopa.afm.utils.exception.AppException;
import java.util.Arrays;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BundleType {
  GLOBAL("GLOBAL"),
  PUBLIC("PUBLIC"),
  PRIVATE("PRIVATE");

  private final String value;

  BundleType(final String bundleType) {
    this.value = bundleType;
  }

  public static BundleType fromValue(String value) {
    return Arrays.stream(BundleType.values())
        .filter(elem -> elem.value.equals(value))
        .findFirst()
        .orElseThrow(
            () ->
                new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "BundleType not found",
                    "Cannot convert string '" + value + "' into enum"));
  }
}
