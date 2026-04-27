package it.gov.pagopa.afm.calculator;

import com.azure.data.tables.models.TableEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.afm.calculator.entity.*;
import it.gov.pagopa.afm.calculator.model.BundleType;
import it.gov.pagopa.afm.calculator.model.TransferCategoryRelation;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@UtilityClass
public class TestUtil {
	
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * @param relativePath Path from source root of the json file
     * @return the Json string read from the file
     * @throws IOException if an I/O error occurs reading from the resource
     */
    public String readStringFromFile(String relativePath) throws IOException {
        ClassLoader classLoader = TestUtil.class.getClassLoader();

        try (InputStream inputStream = Objects.requireNonNull(
                classLoader.getResourceAsStream(relativePath),
                "Resource not found: " + relativePath
        )) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * @param relativePath Path from source root of the file
     * @return the requested file
     */
    public File readFile(String relativePath) {
        ClassLoader classLoader = TestUtil.class.getClassLoader();

        try {
            return new File(Objects.requireNonNull(
                    classLoader.getResource(relativePath),
                    "Resource not found: " + relativePath
            ).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid resource URI: " + relativePath, e);
        }
    }

    /**
     * @param object to map into the Json string
     * @return object as Json string
     * @throws JsonProcessingException if there is an error during the parsing of the object
     */
    public String toJson(Object object) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(object);
    }

    /**
     * @param relativePath a relative path of valid JSON file
     * @param valueType    class to convert the JSON
     * @param <T>          a Java Class
     * @return an object of type T using the JSON
     * @throws IOException if an IO error occurs
     */
    public <T> T readObjectFromFile(String relativePath, Class<T> valueType) throws IOException {
        ClassLoader classLoader = TestUtil.class.getClassLoader();

        try (InputStream inputStream = Objects.requireNonNull(
                classLoader.getResourceAsStream(relativePath),
                "Resource not found: " + relativePath
        )) {
            return OBJECT_MAPPER.readValue(inputStream, valueType);
        }
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
                .touchpoint("CHECKOUT")
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
                .touchpoint("CHECKOUT")
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
                .touchpoint("CHECKOUT")
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
                .touchpoint("CHECKOUT")
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
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX1"))
                        .ciBundleList(Collections.singletonList(getMockCiBundle()))
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(false)
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
                        .touchpoint("CHECKOUT")
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
                        .touchpoint("CHECKOUT")
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
                        .touchpoint("CHECKOUT")
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
                        .touchpoint("CHECKOUT")
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
                        .touchpoint("CHECKOUT")
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
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX7"))
                        .ciBundleList(Collections.singletonList(getMockCiBundle()))
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(false)
                        .build());
        return bundles;
    }

    public static List<ValidBundle> getMockMultipleValidBundleSamePsp() {
        List<ValidBundle> bundles = new ArrayList<>();
        bundles.add(
                ValidBundle.builder()
                        .id("1")
                        .idChannel("13212880150_01")
                        .name("bundle1")
                        .idPsp("ABC")
                        .pspBusinessName("psp business name")
                        .abi("14160")
                        .paymentAmount(50L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.GLOBAL)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX1"))
                        .ciBundleList(new ArrayList<>())
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(true)
                        .build());
        bundles.add(
                ValidBundle.builder()
                        .id("2")
                        .idChannel("13212880150_02_ONUS")
                        .name("bundle2")
                        .idPsp("ABC")
                        .pspBusinessName("psp business name")
                        .abi("14156")
                        .paymentAmount(55L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.PUBLIC)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX2"))
                        .ciBundleList(Collections.singletonList(getMockCiBundle()))
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(true)
                        .build());
        return bundles;
    }

    public static List<ValidBundle> getMockMultipleValidBundlesMultiPsp() {
        List<ValidBundle> bundles = new ArrayList<>();
        bundles.add(
                ValidBundle.builder()
                        .id("3")
                        .idChannel("13212880150_01")
                        .name("bundle3")
                        .idPsp("DEF")
                        .pspBusinessName("psp DEF")
                        .abi("14163")
                        .paymentAmount(50L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.GLOBAL)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX1"))
                        .ciBundleList(new ArrayList<>())
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(false)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("4")
                        .idChannel("13212880150_02")
                        .name("bundle4")
                        .idPsp("GHI")
                        .pspBusinessName("psp GHI")
                        .abi("14164")
                        .paymentAmount(40L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.GLOBAL)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX1"))
                        .ciBundleList(new ArrayList<>())
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(false)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("5")
                        .idChannel("13212880155_01")
                        .name("bundle5")
                        .idPsp("JKL")
                        .pspBusinessName("psp JKL")
                        .abi("14165")
                        .paymentAmount(35L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.GLOBAL)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX1"))
                        .ciBundleList(new ArrayList<>())
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(false)
                        .build());
        bundles.add(
                ValidBundle.builder()
                        .id("6")
                        .idChannel("13212880155_01")
                        .name("bundle6")
                        .idPsp("JKL")
                        .pspBusinessName("psp JKL")
                        .abi("14165")
                        .paymentAmount(30L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.GLOBAL)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX1"))
                        .ciBundleList(new ArrayList<>())
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(false)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("1")
                        .idChannel("13212880150_01")
                        .name("bundle1")
                        .idPsp("ABC")
                        .pspBusinessName("psp ABC")
                        .abi("14160")
                        .paymentAmount(50L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.GLOBAL)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX1"))
                        .ciBundleList(new ArrayList<>())
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(true)
                        .build());
        bundles.add(
                ValidBundle.builder()
                        .id("2")
                        .idChannel("13212880150_02_ONUS")
                        .name("bundle2")
                        .idPsp("ABC")
                        .pspBusinessName("psp ABC")
                        .abi("14156")
                        .paymentAmount(55L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.PUBLIC)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX2"))
                        .ciBundleList(Collections.singletonList(getMockCiBundle()))
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(true)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("7")
                        .idChannel("13212880160_01")
                        .name("bundle7")
                        .idPsp("MNO")
                        .pspBusinessName("psp MNO")
                        .abi("14166")
                        .paymentAmount(60L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.PUBLIC)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX2"))
                        .ciBundleList(Collections.singletonList(getMockCiBundle()))
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(true)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("8")
                        .idChannel("13212880160_02")
                        .name("bundle8")
                        .idPsp("PQR")
                        .pspBusinessName("psp PQR")
                        .abi("14167")
                        .paymentAmount(45L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.GLOBAL)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX1"))
                        .ciBundleList(new ArrayList<>())
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(false)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("9")
                        .idChannel("13212880161_01")
                        .name("bundle9")
                        .idPsp("STU")
                        .pspBusinessName("psp STU")
                        .abi("14168")
                        .paymentAmount(70L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.PUBLIC)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX3"))
                        .ciBundleList(Collections.singletonList(getMockCiBundle()))
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(true)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("10")
                        .idChannel("13212880162_01")
                        .name("bundle10")
                        .idPsp("VWX")
                        .pspBusinessName("psp VWX")
                        .abi("14169")
                        .paymentAmount(65L)
                        .minPaymentAmount(10L)
                        .maxPaymentAmount(1500L)
                        .type(BundleType.PRIVATE)
                        .touchpoint("IO")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX4", "TAX5"))
                        .ciBundleList(Collections.singletonList(getMockCiBundle()))
                        .digitalStamp(true)
                        .digitalStampRestriction(true)
                        .onUs(false)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("11")
                        .idChannel("13212880163_01")
                        .name("bundle11")
                        .idPsp("YZA")
                        .pspBusinessName("psp YZA")
                        .abi("14170")
                        .paymentAmount(80L)
                        .minPaymentAmount(5L)
                        .maxPaymentAmount(2000L)
                        .type(BundleType.PUBLIC)
                        .touchpoint("3")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX6"))
                        .ciBundleList(new ArrayList<>())
                        .digitalStamp(true)
                        .digitalStampRestriction(false)
                        .onUs(true)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("12")
                        .idChannel("13212880164_01")
                        .name("bundle12")
                        .idPsp("BCD")
                        .pspBusinessName("psp BCD")
                        .abi("14171")
                        .paymentAmount(90L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(2500L)
                        .type(BundleType.PUBLIC)
                        .touchpoint("4")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX7", "TAX8"))
                        .ciBundleList(Collections.singletonList(getMockCiBundle()))
                        .digitalStamp(false)
                        .digitalStampRestriction(true)
                        .onUs(false)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("13")
                        .idChannel("13212880165_01")
                        .name("bundle13")
                        .idPsp("EFG")
                        .pspBusinessName("psp EFG")
                        .abi("14172")
                        .paymentAmount(75L)
                        .minPaymentAmount(5L)
                        .maxPaymentAmount(1200L)
                        .type(BundleType.PUBLIC)
                        .touchpoint("IO")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX9"))
                        .ciBundleList(Collections.singletonList(getMockCiBundle()))
                        .digitalStamp(true)
                        .digitalStampRestriction(false)
                        .onUs(true)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("14")
                        .idChannel("13212880166_01")
                        .name("bundle14")
                        .idPsp("HIJ")
                        .pspBusinessName("psp HIJ")
                        .abi("14173")
                        .paymentAmount(85L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1800L)
                        .type(BundleType.PUBLIC)
                        .touchpoint("3")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX10", "TAX11"))
                        .ciBundleList(new ArrayList<>())
                        .digitalStamp(false)
                        .digitalStampRestriction(true)
                        .onUs(false)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("15")
                        .idChannel("13212880160_01")
                        .name("bundle15")
                        .idPsp("MNO")
                        .pspBusinessName("psp MNO")
                        .abi("14166")
                        .paymentAmount(45L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.GLOBAL)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX1"))
                        .ciBundleList(new ArrayList<>())
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(false)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("16")
                        .idChannel("13212880160_02")
                        .name("bundle16")
                        .idPsp("PQR")
                        .pspBusinessName("psp PQR")
                        .abi("14167")
                        .paymentAmount(55L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.GLOBAL)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX1"))
                        .ciBundleList(new ArrayList<>())
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(false)
                        .build());

        bundles.add(
                ValidBundle.builder()
                        .id("17")
                        .idChannel("13212880160_03")
                        .name("bundle17")
                        .idPsp("STU")
                        .pspBusinessName("psp STU")
                        .abi("14168")
                        .paymentAmount(60L)
                        .minPaymentAmount(0L)
                        .maxPaymentAmount(1000L)
                        .type(BundleType.GLOBAL)
                        .touchpoint("CHECKOUT")
                        .paymentType("CP")
                        .transferCategoryList(List.of("TAX1"))
                        .ciBundleList(new ArrayList<>())
                        .digitalStamp(false)
                        .digitalStampRestriction(false)
                        .onUs(false)
                        .build());


        return bundles;
    }

    public static Touchpoint getMockTouchpoint() {
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

    public static TableEntity getTableEntity(String key, String lowRange, String highRange, String abi) {
        return new TableEntity(key, key)
                .addProperty("LOW_RANGE", lowRange)
                .addProperty("HIGH_RANGE", highRange)
                .addProperty("ABI", abi);
    }

    public static List<ValidBundle> getMockMultipleValidBundlesMultiPspSameFee() {
        List<ValidBundle> bundles = new ArrayList<>();

        bundles.add(ValidBundle.builder()
            .id("1")
            .idChannel("channel_01")
            .name("bundle1")
            .idPsp("PSP_A")
            .pspBusinessName("psp A")
            .abi("11111")
            .paymentAmount(30L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.GLOBAL)
            .touchpoint("CHECKOUT")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX1"))
            .ciBundleList(new ArrayList<>())
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(false)
            .build());

        bundles.add(ValidBundle.builder()
            .id("2")
            .idChannel("channel_02")
            .name("bundle2")
            .idPsp("PSP_B")
            .pspBusinessName("psp B")
            .abi("22222")
            .paymentAmount(30L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.GLOBAL)
            .touchpoint("CHECKOUT")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX1"))
            .ciBundleList(new ArrayList<>())
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(false)
            .build());

        bundles.add(ValidBundle.builder()
            .id("3")
            .idChannel("channel_03")
            .name("bundle3")
            .idPsp("PSP_C")
            .pspBusinessName("psp C")
            .abi("33333")
            .paymentAmount(30L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.GLOBAL)
            .touchpoint("CHECKOUT")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX1"))
            .ciBundleList(new ArrayList<>())
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(false)
            .build());

        bundles.add(ValidBundle.builder()
            .id("4")
            .idChannel("channel_04")
            .name("bundle4")
            .idPsp("PSP_D")
            .pspBusinessName("psp D")
            .abi("44444")
            .paymentAmount(50L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.GLOBAL)
            .touchpoint("CHECKOUT")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX1"))
            .ciBundleList(new ArrayList<>())
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(false)
            .build());

        bundles.add(ValidBundle.builder()
            .id("5")
            .idChannel("channel_05")
            .name("bundle5")
            .idPsp("PSP_E")
            .pspBusinessName("psp E")
            .abi("55555")
            .paymentAmount(50L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.GLOBAL)
            .touchpoint("CHECKOUT")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX1"))
            .ciBundleList(new ArrayList<>())
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(false)
            .build());

        bundles.add(ValidBundle.builder()
            .id("6")
            .idChannel("channel_06")
            .name("bundle6")
            .idPsp("PSP_F")
            .pspBusinessName("psp F")
            .abi("66666")
            .paymentAmount(50L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.GLOBAL)
            .touchpoint("CHECKOUT")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX1"))
            .ciBundleList(new ArrayList<>())
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(false)
            .build());

        bundles.add(ValidBundle.builder()
            .id("7")
            .idChannel("channel_07")
            .name("bundle7")
            .idPsp("PSP_G")
            .pspBusinessName("psp G")
            .abi("77777")
            .paymentAmount(70L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.GLOBAL)
            .touchpoint("CHECKOUT")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX1"))
            .ciBundleList(new ArrayList<>())
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(false)
            .build());

        bundles.add(ValidBundle.builder()
            .id("8")
            .idChannel("channel_08")
            .name("bundle8")
            .idPsp("PSP_H")
            .pspBusinessName("psp H")
            .abi("88888")
            .paymentAmount(70L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.GLOBAL)
            .touchpoint("CHECKOUT")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX1"))
            .ciBundleList(new ArrayList<>())
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(false)
            .build());

        bundles.add(ValidBundle.builder()
            .id("9")
            .idChannel("channel_09")
            .name("bundle9")
            .idPsp("PSP_I")
            .pspBusinessName("psp I")
            .abi("99999")
            .paymentAmount(70L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.GLOBAL)
            .touchpoint("CHECKOUT")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX1"))
            .ciBundleList(new ArrayList<>())
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(false)
            .build());

        bundles.add(ValidBundle.builder()
            .id("10")
            .idChannel("channel_10")
            .name("bundle10")
            .idPsp("PSP_J")
            .pspBusinessName("psp J")
            .abi("10101")
            .paymentAmount(70L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.GLOBAL)
            .touchpoint("CHECKOUT")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX1"))
            .ciBundleList(new ArrayList<>())
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(false)
            .build());

        bundles.add(ValidBundle.builder()
            .id("11")
            .idChannel("channel_11_ONUS")
            .name("bundle11")
            .idPsp("PSP_K")
            .pspBusinessName("psp K")
            .abi("14156")
            .paymentAmount(30L)
            .minPaymentAmount(0L)
            .maxPaymentAmount(1000L)
            .type(BundleType.GLOBAL)
            .touchpoint("CHECKOUT")
            .paymentType("CP")
            .transferCategoryList(List.of("TAX1"))
            .ciBundleList(new ArrayList<>())
            .digitalStamp(false)
            .digitalStampRestriction(false)
            .onUs(true)
            .build());

        return bundles;
    }
    
    public static List<ValidBundle> getMockMultiBundlesWithRealOnUsAndOffUs() {
        List<ValidBundle> bundles = new ArrayList<>();

        ValidBundle onUsBundle = getMockValidBundle();
        onUsBundle.setId("1");
        onUsBundle.setName("bundle-onus");
        onUsBundle.setIdPsp("PSP_ONUS");
        onUsBundle.setPspBusinessName("PSP ONUS");
        onUsBundle.setOnUs(true);
        onUsBundle.setAbi("14156");
        onUsBundle.setIdChannel("13212880150_01_ONUS");
        onUsBundle.setPaymentAmount(30L);

        ValidBundle offUsBundle = getMockValidBundle();
        offUsBundle.setId("2");
        offUsBundle.setName("bundle-offus");
        offUsBundle.setIdPsp("PSP_OFFUS");
        offUsBundle.setPspBusinessName("PSP OFFUS");
        offUsBundle.setOnUs(false);
        offUsBundle.setAbi("99991");
        offUsBundle.setIdChannel("13212880150_02");
        offUsBundle.setPaymentAmount(30L);

        bundles.add(offUsBundle);
        bundles.add(onUsBundle);

        return bundles;
    }
    
    public static List<ValidBundle> getMockFeeRandomBundlesSameFeeOffUs() {
        List<ValidBundle> source = getMockMultipleValidBundlesMultiPsp();
        List<ValidBundle> bundles = new ArrayList<>();

        ValidBundle b1 = source.get(0); // DEF - GLOBAL - TAX1
        b1.setPaymentAmount(30L);
        b1.setOnUs(false);
        b1.setAbi("99991");
        b1.setIdChannel("13212880150_01");

        ValidBundle b2 = source.get(1); // GHI - GLOBAL - TAX1
        b2.setPaymentAmount(30L);
        b2.setOnUs(false);
        b2.setAbi("99992");
        b2.setIdChannel("13212880150_02");

        ValidBundle b3 = source.get(7); // PQR - GLOBAL - TAX1
        b3.setPaymentAmount(30L);
        b3.setOnUs(false);
        b3.setAbi("99993");
        b3.setIdChannel("13212880160_02");

        bundles.add(b1);
        bundles.add(b2);
        bundles.add(b3);

        return bundles;
    } 
    
    public static List<ValidBundle> getMockFeeRandomBundlesSameFeeWithOnUs() {
        List<ValidBundle> source = getMockMultipleValidBundlesMultiPsp();
        List<ValidBundle> bundles = new ArrayList<>();

        ValidBundle onUs = source.get(4); // ABC - GLOBAL - TAX1 - onUs=true
        onUs.setPaymentAmount(30L);
        onUs.setOnUs(true);
        onUs.setAbi("14156");
        onUs.setIdChannel("13212880150_01_ONUS");

        ValidBundle offUs1 = source.get(0); // DEF - GLOBAL - TAX1
        offUs1.setPaymentAmount(30L);
        offUs1.setOnUs(false);
        offUs1.setAbi("99991");
        offUs1.setIdChannel("13212880150_01");

        ValidBundle offUs2 = source.get(1); // GHI - GLOBAL - TAX1
        offUs2.setPaymentAmount(30L);
        offUs2.setOnUs(false);
        offUs2.setAbi("99992");
        offUs2.setIdChannel("13212880150_02");

        ValidBundle offUs3 = source.get(7); // PQR - GLOBAL - TAX1
        offUs3.setPaymentAmount(30L);
        offUs3.setOnUs(false);
        offUs3.setAbi("99993");
        offUs3.setIdChannel("13212880160_02");

        bundles.add(onUs);
        bundles.add(offUs1);
        bundles.add(offUs2);
        bundles.add(offUs3);

        return bundles;
    }
    
    public static List<ValidBundle> getMockRepositoryFilteredMultipleValidBundlesMultiPsp() {
        List<ValidBundle> source = getMockMultipleValidBundlesMultiPsp();

        return new ArrayList<>(List.of(
                source.get(0),  // DEF - GLOBAL - CHECKOUT - TAX1
                source.get(1),  // GHI - GLOBAL - CHECKOUT - TAX1
                source.get(2),  // JKL - GLOBAL - CHECKOUT - TAX1
                source.get(3),  // JKL - GLOBAL - CHECKOUT - TAX1 cheaper
                source.get(7),  // PQR - GLOBAL - CHECKOUT - TAX1
                source.get(14), // MNO - GLOBAL - CHECKOUT - TAX1
                source.get(15), // PQR - GLOBAL - CHECKOUT - TAX1 worse
                source.get(16)  // STU - GLOBAL - CHECKOUT - TAX1
        ));
    }
}