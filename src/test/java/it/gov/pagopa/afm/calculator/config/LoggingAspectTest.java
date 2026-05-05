package it.gov.pagopa.afm.calculator.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsItem;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoggingAspectTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void toSafeLogValue_shouldSerializePaymentMethodsResponseAsJsonWithPaymentMethodsSize() throws Exception {
        /*
         * This test verifies the main expected behavior for the Kibana dashboard:
         * the logged response must remain a valid JSON object, but large collections
         * must be summarized with their type and size instead of being fully serialized.
         */
        PaymentMethodsResponse response = PaymentMethodsResponse.builder()
                .paymentMethods(List.of(
                        PaymentMethodsItem.builder()
                                .paymentMethodId("PAYPAL")
                                .build(),
                        PaymentMethodsItem.builder()
                                .paymentMethodId("CARDS")
                                .build(),
                        PaymentMethodsItem.builder()
                                .paymentMethodId("SATISPAY")
                                .build()
                ))
                .build();

        Object safeValue = invokePrivateStatic(
                "toSafeLogValue",
                new Class<?>[]{Object.class},
                response
        );

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                safeValue
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertTrue(root.has("paymentMethods"));
        assertTrue(root.get("paymentMethods").get("type").asText().contains("List"));
        assertEquals(3, root.get("paymentMethods").get("size").asInt());

        /*
         * The full list content must not be logged.
         * Only the collection metadata should be present.
         */
        assertFalse(json.contains("PAYPAL"));
        assertFalse(json.contains("CARDS"));
        assertFalse(json.contains("SATISPAY"));
    }

    @Test
    void toSafeLogValue_shouldSerializeCollectionAsTypeAndSizeOnly() throws Exception {
        /*
         * This test verifies that collections are not serialized item by item.
         */
        List<String> values = List.of("one", "two", "three");

        Object safeValue = invokePrivateStatic(
                "toSafeLogValue",
                new Class<?>[]{Object.class},
                values
        );

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                safeValue
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertTrue(root.get("type").asText().contains("List"));
        assertEquals(3, root.get("size").asInt());

        assertFalse(json.contains("one"));
        assertFalse(json.contains("two"));
        assertFalse(json.contains("three"));
    }

    @Test
    void toSafeLogValue_shouldSerializeMapAsTypeAndSizeOnly() throws Exception {
        /*
         * This test verifies that maps are summarized as metadata only.
         * Map entries are intentionally not serialized to avoid logging large or sensitive payloads.
         */
        Map<String, Object> map = new HashMap<>();
        map.put("first", "value1");
        map.put("second", "value2");

        Object safeValue = invokePrivateStatic(
                "toSafeLogValue",
                new Class<?>[]{Object.class},
                map
        );

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                safeValue
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertEquals("HashMap", root.get("type").asText());
        assertEquals(2, root.get("size").asInt());

        assertFalse(json.contains("value1"));
        assertFalse(json.contains("value2"));
    }

    @Test
    void toSafeLogValue_shouldSerializeArrayAsTypeAndSizeOnly() throws Exception {
        /*
         * This test verifies the same compact logging behavior for arrays.
         * The array type and length are preserved, while the array values are omitted.
         */
        String[] values = {"a", "b", "c", "d"};

        Object safeValue = invokePrivateStatic(
                "toSafeLogValue",
                new Class<?>[]{Object.class},
                (Object) values
        );

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                safeValue
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertEquals("String[]", root.get("type").asText());
        assertEquals(4, root.get("size").asInt());

        assertFalse(json.contains("a"));
        assertFalse(json.contains("b"));
        assertFalse(json.contains("c"));
        assertFalse(json.contains("d"));
    }

    @Test
    void toSafeLogValue_shouldPreservePrimitiveLikeValues() throws Exception {
        /*
         * This test verifies that simple scalar values are preserved as-is.
         */
        Object stringValue = invokePrivateStatic(
                "toSafeLogValue",
                new Class<?>[]{Object.class},
                "test-value"
        );

        Object numberValue = invokePrivateStatic(
                "toSafeLogValue",
                new Class<?>[]{Object.class},
                123
        );

        Object booleanValue = invokePrivateStatic(
                "toSafeLogValue",
                new Class<?>[]{Object.class},
                true
        );

        assertEquals("test-value", stringValue);
        assertEquals(123, numberValue);
        assertEquals(true, booleanValue);
    }

    @Test
    void toSafeLogValue_shouldSerializeResponseEntityWithStatusAndSafeBody() throws Exception {
        /*
         * This test verifies that ResponseEntity is still represented as JSON,
         * including the HTTP status and a sanitized version of the body.
         */
        PaymentMethodsResponse body = PaymentMethodsResponse.builder()
                .paymentMethods(List.of(
                        PaymentMethodsItem.builder()
                                .paymentMethodId("PAYPAL")
                                .build(),
                        PaymentMethodsItem.builder()
                                .paymentMethodId("CARDS")
                                .build()
                ))
                .build();

        ResponseEntity<PaymentMethodsResponse> responseEntity =
                ResponseEntity.status(HttpStatus.OK).body(body);

        Object safeValue = invokePrivateStatic(
                "toSafeLogValue",
                new Class<?>[]{Object.class},
                responseEntity
        );

        String json = invokePrivateStatic(
                "toJsonString",
                new Class<?>[]{Object.class},
                safeValue
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertEquals(200, root.get("status").asInt());
        assertTrue(root.has("body"));
        assertTrue(root.get("body").has("paymentMethods"));
        assertTrue(root.get("body").get("paymentMethods").get("type").asText().contains("List"));
        assertEquals(2, root.get("body").get("paymentMethods").get("size").asInt());

        /*
         * The ResponseEntity body must be sanitized recursively:
         * the collection size is logged, but the collection items are not.
         */
        assertFalse(json.contains("PAYPAL"));
        assertFalse(json.contains("CARDS"));
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