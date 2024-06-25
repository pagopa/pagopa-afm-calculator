package it.gov.pagopa.afm.calculator.util;

import com.azure.spring.data.cosmos.core.query.Criteria;
import com.azure.spring.data.cosmos.core.query.CriteriaType;
import lombok.experimental.UtilityClass;
import org.springframework.data.repository.query.parser.Part;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class CriteriaBuilder {

  public static Criteria isNull(String param) {
    return Criteria.getInstance(
        CriteriaType.IS_NULL, param, Collections.emptyList(), Part.IgnoreCaseType.NEVER);
  }

  public static <T> Criteria isEqual(String param, T value) {
    return Criteria.getInstance(
        CriteriaType.IS_EQUAL, param, Collections.singletonList(value), Part.IgnoreCaseType.ALWAYS);
  }

  public static <T> Criteria isTrue(String param, T value) {
    var queryCart = Criteria.getInstance(
            CriteriaType.IS_EQUAL, param, Collections.singletonList(value), Part.IgnoreCaseType.NEVER);
    var queryNull = Criteria.getInstance(CriteriaType.IS_NULL,
            param, Collections.emptyList(), Part.IgnoreCaseType.NEVER);
    return or(queryCart, queryNull);
  }

  public static <T> Criteria isNotEqual(String param, T value) {
    return Criteria.getInstance(
        CriteriaType.NOT, param, Collections.singletonList(value), Part.IgnoreCaseType.ALWAYS);
  }

  public static <T> Criteria isEqualOrAny(String param, T value) {
    var queryEquals =
        Criteria.getInstance(
            CriteriaType.IS_EQUAL,
            param,
            Collections.singletonList(value),
            Part.IgnoreCaseType.ALWAYS);
    var queryNull =
        Criteria.getInstance(
            CriteriaType.IS_EQUAL,
            param,
            Collections.singletonList("ANY"),
            Part.IgnoreCaseType.ALWAYS);

    return or(queryEquals, queryNull);
  }

  public static <T> Criteria isEqualOrNull(String param, T value) {
    var queryEquals =
        Criteria.getInstance(
            CriteriaType.IS_EQUAL,
            param,
            Collections.singletonList(value),
            Part.IgnoreCaseType.ALWAYS);
    var queryNull =
        Criteria.getInstance(
            CriteriaType.IS_NULL, param, Collections.emptyList(), Part.IgnoreCaseType.NEVER);

    return or(queryEquals, queryNull);
  }

  public static <T> Criteria in(String param, List<T> values) {
    return Criteria.getInstance(
        CriteriaType.IN, param, Collections.singletonList(values), Part.IgnoreCaseType.ALWAYS);
  }

  public static <T> Criteria notIn(String param, List<T> values) {
    return Criteria.getInstance(
        CriteriaType.NOT_IN, param, Collections.singletonList(values), Part.IgnoreCaseType.ALWAYS);
  }

  public static <T> Criteria arrayContains(String param, T value) {
    return Criteria.getInstance(
        CriteriaType.ARRAY_CONTAINS,
        param,
        Collections.singletonList(value),
        Part.IgnoreCaseType.NEVER);
  }

  public static Criteria greaterThanEqual(String param, Long value) {
    return Criteria.getInstance(
        CriteriaType.GREATER_THAN_EQUAL,
        param,
        Collections.singletonList(value),
        Part.IgnoreCaseType.NEVER);
  }

  public static Criteria lessThan(String param, Long value) {
    return Criteria.getInstance(
        CriteriaType.LESS_THAN,
        param,
        Collections.singletonList(value),
        Part.IgnoreCaseType.NEVER);
  }

  public static Criteria or(@NotNull Criteria query1, @NotNull Criteria query2) {
    return Criteria.getInstance(CriteriaType.OR, query1, query2);
  }

  public static Criteria and(@NotNull Criteria query1, @NotNull Criteria query2) {
    return Criteria.getInstance(CriteriaType.AND, query1, query2);
  }
}
