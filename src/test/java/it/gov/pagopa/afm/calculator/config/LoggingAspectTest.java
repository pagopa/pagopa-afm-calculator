package it.gov.pagopa.afm.calculator.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.afm.calculator.model.paymentmethods.FeeRange;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsItem;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsResponse;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodDisabledReason;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodStatus;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoggingAspectTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @AfterEach
    void cleanup() {
        /*
         * MDC is thread-local.
         * Each test must clean it to avoid leaking values into other tests.
         */
        MDC.clear();
    }

    @Test
    void toJsonString_shouldSerializeFullPaymentMethodsResponse() throws Exception {
        /*
         * This test verifies the current expected behavior:
         * PaymentMethodsResponse is serialized completely.
         *
         * This means the paymentMethods list remains an array and each item keeps
         * its useful fields, including paymentMethodId, status, group, feeRange,
         * disabledReason and paymentMethodTypes.
         */
        PaymentMethodsResponse response = PaymentMethodsResponse.builder()
                .paymentMethods(List.of(
                        PaymentMethodsItem.builder()
                                .paymentMethodId("PAYPAL")
                                .status(PaymentMethodStatus.ENABLED)
                                .group("PPAL")
                                .paymentMethodTypes(List.of(PaymentMethodType.APP))
                                .feeRange(FeeRange.builder().min(10L).max(20L).build())
                                .validityDateFrom(LocalDate.of(2000, 1, 1))
                                .build(),
                        PaymentMethodsItem.builder()
                                .paymentMethodId("CARDS")
                                .status(PaymentMethodStatus.DISABLED)
                                .disabledReason(PaymentMethodDisabledReason.NO_BUNDLE_AVAILABLE)
                                .group("CP")
                                .paymentMethodTypes(List.of(PaymentMethodType.CARTE))
                                .feeRange(FeeRange.builder().min(30L).max(40L).build())
                                .validityDateFrom(LocalDate.of(2000, 1, 1))
                                .build()
                ))
                .build();

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                response
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertTrue(root.has("paymentMethods"));
        assertTrue(root.get("paymentMethods").isArray());
        assertEquals(2, root.get("paymentMethods").size());

        JsonNode first = root.get("paymentMethods").get(0);
        assertEquals("PAYPAL", first.get("paymentMethodId").asText());
        assertEquals("ENABLED", first.get("status").asText());
        assertEquals("PPAL", first.get("group").asText());
        assertEquals("APP", first.get("paymentMethodTypes").get(0).asText());
        assertEquals(10L, first.get("feeRange").get("min").asLong());
        assertEquals(20L, first.get("feeRange").get("max").asLong());

        JsonNode second = root.get("paymentMethods").get(1);
        assertEquals("CARDS", second.get("paymentMethodId").asText());
        assertEquals("DISABLED", second.get("status").asText());
        assertEquals("NO_BUNDLE_AVAILABLE", second.get("disabledReason").asText());
        assertEquals("CP", second.get("group").asText());
        assertEquals("CARTE", second.get("paymentMethodTypes").get(0).asText());
    }

    @Test
    void toJsonString_shouldSerializePaymentMethodsResponseWithNullList() throws Exception {
        /*
         * This test verifies that a PaymentMethodsResponse with a null list
         * is still serialized correctly.
         */
        PaymentMethodsResponse response = PaymentMethodsResponse.builder()
                .paymentMethods(null)
                .build();

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                response
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertTrue(root.has("paymentMethods"));
        assertTrue(root.get("paymentMethods").isNull());
    }

    @Test
    void toJsonString_shouldSerializePaymentMethodsResponseWithEmptyList() throws Exception {
        /*
         * This test verifies that an empty paymentMethods list is serialized
         * as an empty JSON array.
         */
        PaymentMethodsResponse response = PaymentMethodsResponse.builder()
                .paymentMethods(List.of())
                .build();

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                response
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertTrue(root.has("paymentMethods"));
        assertTrue(root.get("paymentMethods").isArray());
        assertEquals(0, root.get("paymentMethods").size());
    }

    @Test
    void toJsonString_shouldSerializeCollectionWithAllItems() throws Exception {
        /*
         * All values must be present in the serialized JSON.
         */
        List<String> values = List.of("one", "two", "three");

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                values
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertTrue(root.isArray());
        assertEquals(3, root.size());
        assertEquals("one", root.get(0).asText());
        assertEquals("two", root.get(1).asText());
        assertEquals("three", root.get(2).asText());
    }

    @Test
    void toJsonString_shouldSerializeMapWithAllEntries() throws Exception {
        /*
         * Map keys and values must be serialized.
         */
        Map<String, Object> map = new HashMap<>();
        map.put("first", "value1");
        map.put("second", "value2");

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                map
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertEquals("value1", root.get("first").asText());
        assertEquals("value2", root.get("second").asText());
    }

    @Test
    void toJsonString_shouldSerializeArrayWithAllValues() throws Exception {
    	/*
    	 * This test verifies that arrays are serialized as JSON arrays and all values are included.
    	 */
        String[] values = {"a", "b", "c", "d"};

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                (Object) values
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertTrue(root.isArray());
        assertEquals(4, root.size());
        assertEquals("a", root.get(0).asText());
        assertEquals("b", root.get(1).asText());
        assertEquals("c", root.get(2).asText());
        assertEquals("d", root.get(3).asText());
    }

    @Test
    void toJsonString_shouldSerializePrimitiveLikeValues() throws Exception {
        /*
         * This test verifies that simple scalar values are serialized correctly.
         */
        String stringJson = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                "test-value"
        );

        String numberJson = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                123
        );

        String booleanJson = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                true
        );

        assertEquals("\"test-value\"", stringJson);
        assertEquals("123", numberJson);
        assertEquals("true", booleanJson);
    }

    @Test
    void toJsonString_shouldSerializeResponseEntityWithBody() throws Exception {
        /*
         * This test verifies how ResponseEntity is serialized by the current implementation.
         * The response body is included in the JSON representation.
         */
        PaymentMethodsResponse body = PaymentMethodsResponse.builder()
                .paymentMethods(List.of(
                        PaymentMethodsItem.builder()
                                .paymentMethodId("PAYPAL")
                                .status(PaymentMethodStatus.ENABLED)
                                .group("PPAL")
                                .build()
                ))
                .build();

        ResponseEntity<PaymentMethodsResponse> responseEntity =
                ResponseEntity.status(HttpStatus.OK).body(body);

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                responseEntity
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertTrue(root.has("body"));
        assertTrue(root.get("body").has("paymentMethods"));
        assertEquals("PAYPAL", root.get("body").get("paymentMethods").get(0).get("paymentMethodId").asText());
        assertEquals("ENABLED", root.get("body").get("paymentMethods").get(0).get("status").asText());
        assertEquals("PPAL", root.get("body").get("paymentMethods").get(0).get("group").asText());
    }

    @Test
    void getExecutionTime_shouldReturnElapsedTimeWhenStartTimeIsPresent() throws InterruptedException {
        /*
         * This test verifies that getExecutionTime calculates the elapsed time
         * using the START_TIME value stored in MDC.
         */
        MDC.put(LoggingAspect.START_TIME, String.valueOf(System.currentTimeMillis() - 50));

        String executionTime = LoggingAspect.getExecutionTime();

        assertNotNull(executionTime);
        assertNotEquals("-", executionTime);
        assertTrue(Long.parseLong(executionTime) >= 0);
    }

    @Test
    void getExecutionTime_shouldReturnDashWhenStartTimeIsMissing() {
        /*
         * This test verifies the fallback behavior when START_TIME is not present in MDC.
         */
        MDC.remove(LoggingAspect.START_TIME);

        String executionTime = LoggingAspect.getExecutionTime();

        assertEquals("-", executionTime);
    }

    @Test
    void toJsonString_shouldReturnParsingErrorForNonSerializableObject() throws Exception {
        /*
         * This test verifies the fallback behavior of toJsonString.
         * If Jackson cannot serialize an object, the method must not throw;
         * it must return the expected fallback string.
         */
        Object nonSerializable = new Object() {
            @SuppressWarnings("unused")
            public Object getBrokenValue() {
                throw new IllegalStateException("Cannot serialize this value");
            }
        };

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                nonSerializable
        );

        assertEquals("parsing error", json);
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokePrivateStatic(
            String methodName,
            Class<?>[] parameterTypes,
            Object... args
    ) throws Exception {
        /*
         * The methods under test are private static helpers.
         * Reflection is used here to validate their behavior without changing production visibility.
         */
        Method method = LoggingAspect.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return (T) method.invoke(null, args);
    }
}