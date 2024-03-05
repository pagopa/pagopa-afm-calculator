package it.gov.pagopa.afm.calculator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.afm.calculator.entity.CiBundle;
import it.gov.pagopa.afm.calculator.entity.CiBundleAttribute;
import it.gov.pagopa.afm.calculator.entity.PaymentType;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.model.BundleType;
import it.gov.pagopa.afm.calculator.model.TransferCategoryRelation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtil {

  /**
   * @param relativePath Path from source root of the json file
   * @return the Json string read from the file
   * @throws IOException if an I/O error occurs reading from the file or a malformed or unmappable
   *     byte sequence is read
   */
  public String readStringFromFile(String relativePath) throws IOException {
    ClassLoader classLoader = TestUtil.class.getClassLoader();
    File file = new File(Objects.requireNonNull(classLoader.getResource(relativePath)).getPath());
    return Files.readString(file.toPath());
  }

  /**
   * @param relativePath Path from source root of the file
   * @return the requested file
   */
  public File readFile(String relativePath) {
    ClassLoader classLoader = TestUtil.class.getClassLoader();
    return new File(Objects.requireNonNull(classLoader.getResource(relativePath)).getFile());
  }

  /**
   * @param object to map into the Json string
   * @return object as Json string
   * @throws JsonProcessingException if there is an error during the parsing of the object
   */
  public String toJson(Object object) throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(object);
  }

  /**
   * @param relativePath a relative path of valid JSON file
   * @param valueType class to convert the JSON
   * @param <T> a Java Class
   * @return an object of type T using the JSON
   * @throws IOException if an IO error occurs
   */
  public <T> T readObjectFromFile(String relativePath, Class<T> valueType) throws IOException {
    var jsonFile = readFile(relativePath);
    return new ObjectMapper().readValue(jsonFile, valueType);
  }

  public static ValidBundle getMockGlobalValidBundle() {
    return ValidBundle.builder()
        .id("2")
        .name("bundle2")
        .idPsp("123")
        .pspBusinessName("psp business name 2")
        .paymentAmount(2L)
        .minPaymentAmount(0L)
        .maxPaymentAmount(1000L)
        .type(BundleType.GLOBAL)
        .touchpoint("1")
        .paymentType("CP")
        .build();
  }

  public static ValidBundle getMockValidBundle() {
    return ValidBundle.builder()
        .id("1")
        .idChannel("13212880150_07_ONUS")
        .name("bundle1")
        .idPsp("ABC")
        .abi("14156")
        .pspBusinessName("psp business name 1")
        .paymentAmount(1L)
        .minPaymentAmount(0L)
        .maxPaymentAmount(1000L)
        .type(BundleType.PUBLIC)
        .touchpoint("1")
        .paymentType("CP")
        .transferCategoryList(List.of("TAX1"))
        .ciBundleList(Collections.singletonList(getMockCiBundle()))
        .digitalStamp(false)
        .digitalStampRestriction(false)
        .onUs(true)
        .build();
  }

  public static ValidBundle getMockAmexValidBundle() {
    return ValidBundle.builder()
        .id("1")
        .idChannel("AMEX_ONUS")
        .name("bundle1")
        .idPsp("AMEX")
        .pspBusinessName("psp business name amex")
        .abi("AMREX")
        .paymentAmount(1L)
        .minPaymentAmount(0L)
        .maxPaymentAmount(1000L)
        .type(BundleType.PUBLIC)
        .touchpoint("1")
        .paymentType("CP")
        .transferCategoryList(List.of("TAX1"))
        .ciBundleList(Collections.singletonList(getMockCiBundle()))
        .digitalStamp(false)
        .digitalStampRestriction(false)
        .onUs(true)
        .build();
  }

  public static ValidBundle getHighCommissionValidBundle() {
    return ValidBundle.builder()
        .id("1")
        .idChannel("13212880150_07_ONUS")
        .name("bundle1")
        .idPsp("ABC")
        .abi("14156")
        .pspBusinessName("psp business name 1")
        .paymentAmount(50L)
        .minPaymentAmount(0L)
        .maxPaymentAmount(1000L)
        .type(BundleType.PUBLIC)
        .touchpoint("1")
        .paymentType("CP")
        .transferCategoryList(List.of("TAX1"))
        .ciBundleList(Collections.singletonList(getMockCiBundle()))
        .digitalStamp(false)
        .digitalStampRestriction(false)
        .onUs(true)
        .build();
  }

  public static List<ValidBundle> getMockMultipleValidBundle() {
    List<ValidBundle> bundles = new ArrayList<>();
    bundles.add(
        ValidBundle.builder()
            .id("1")
            .idChannel("13212880150_01")
            .name("bundle1")
            .idPsp("XYZ")
            .abi("14160")
            .paymentAmount(1L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.PUBLIC)
            .touchpoint("1")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX1"))
            .ciBundleList(Collections.singletonList(getMockCiBundle()))
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(true)
            .build());
    bundles.add(
        ValidBundle.builder()
            .id("2")
            .idChannel("13212880150_02_ONUS")
            .name("bundle1")
            .idPsp("ABC")
            .abi("14156")
            .paymentAmount(2L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.PUBLIC)
            .touchpoint("1")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX2"))
            .ciBundleList(Collections.singletonList(getMockCiBundle()))
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(true)
            .build());
    bundles.add(
        ValidBundle.builder()
            .id("3")
            .idChannel("13212880150_03_ONUS")
            .name("bundle2")
            .idPsp("DEF")
            .abi("14156")
            .paymentAmount(3L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.PUBLIC)
            .touchpoint("1")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX3"))
            .ciBundleList(Collections.singletonList(getMockCiBundle()))
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(true)
            .build());
    bundles.add(
        ValidBundle.builder()
            .id("4")
            .idChannel("13212880150_04_ONUS")
            .name("bundle3")
            .idPsp("GHI")
            .abi("14157")
            .paymentAmount(4L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.PUBLIC)
            .touchpoint("1")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX4"))
            .ciBundleList(Collections.singletonList(getMockCiBundle()))
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(true)
            .build());
    bundles.add(
        ValidBundle.builder()
            .id("5")
            .idChannel("13212880150_05_ONUS")
            .name("bundle4")
            .idPsp("LMN")
            .abi("14158")
            .paymentAmount(5L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.PUBLIC)
            .touchpoint("1")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX5"))
            .ciBundleList(Collections.singletonList(getMockCiBundle()))
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(true)
            .build());
    bundles.add(
        ValidBundle.builder()
            .id("6")
            .idChannel("13212880150_06_ONUS")
            .name("bundle5")
            .idPsp("OPQ")
            .abi("14156")
            .paymentAmount(6L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.PUBLIC)
            .touchpoint("1")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX6"))
            .ciBundleList(Collections.singletonList(getMockCiBundle()))
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(true)
            .build());
    bundles.add(
        ValidBundle.builder()
            .id("7")
            .idChannel("13212880150_07")
            .name("bundle6")
            .idPsp("RST")
            .abi("14159")
            .paymentAmount(7L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.PUBLIC)
            .touchpoint("1")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX7"))
            .ciBundleList(Collections.singletonList(getMockCiBundle()))
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(true)
            .build());
    return bundles;
  }

  public static Touchpoint getMockTouchpoints() {
    return Touchpoint.builder().id("1").name("CHECKOUT").creationDate(LocalDateTime.now()).build();
  }

  public static PaymentType getMockPaymentType() {
    return PaymentType.builder().id("1").name("CP").build();
  }

  private static CiBundle getMockCiBundle() {
    return CiBundle.builder()
        .id("1")
        .ciFiscalCode("77777777777")
        .attributes(List.of(getMockCiBundleAttribute()))
        .build();
  }

  private static CiBundleAttribute getMockCiBundleAttribute() {
    return CiBundleAttribute.builder()
        .id("1")
        .maxPaymentAmount(10L)
        .transferCategory("TAX1")
        .transferCategoryRelation(TransferCategoryRelation.EQUAL)
        .build();
  }
}
