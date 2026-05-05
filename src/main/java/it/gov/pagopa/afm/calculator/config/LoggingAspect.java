package it.gov.pagopa.afm.calculator.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.afm.calculator.exception.AppError;
import it.gov.pagopa.afm.calculator.model.ProblemJson;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsItem;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    public static final String START_TIME = "startTime";
    public static final String METHOD = "method";
    public static final String STATUS = "status";
    public static final String CODE = "httpCode";
    public static final String RESPONSE_TIME = "responseTime";
    public static final String RESPONSE = "response";
    public static final String FAULT_CODE = "faultCode";
    public static final String FAULT_DETAIL = "faultDetail";
    public static final String REQUEST_ID = "requestId";
    public static final String OPERATION_ID = "operationId";
    public static final String ARGS = "args";

    private static final String TYPE = "type";
    private static final String SIZE = "size";
    private static final String BODY = "body";
    private static final String PAYMENT_METHODS = "paymentMethods";
    private static final String PAYMENT_METHODS_SIZE = "paymentMethodsSize";
    private static final String PAYMENT_METHODS_STATUS_COUNTERS = "paymentMethodsStatusCounters";
    private static final String PAYMENT_METHODS_DISABLED_REASON_COUNTERS = "paymentMethodsDisabledReasonCounters";
    private static final String SANITIZATION_ERROR = "sanitizationError";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    HttpServletRequest httRequest;

    @Autowired
    HttpServletResponse httpResponse;

    @Value("${info.application.name}")
    private String name;

    @Value("${info.application.version}")
    private String version;

    @Value("${info.properties.environment}")
    private String environment;

    private static String getDetail(ResponseEntity<ProblemJson> result) {
        if (result != null && result.getBody() != null && result.getBody().getDetail() != null) {
            return result.getBody().getDetail();
        }
        return AppError.UNKNOWN.getDetails();
    }

    private static String getTitle(ResponseEntity<ProblemJson> result) {
        if (result != null && result.getBody() != null && result.getBody().getTitle() != null) {
            return result.getBody().getTitle();
        }
        return AppError.UNKNOWN.getTitle();
    }

    public static String getExecutionTime() {
        String startTime = MDC.get(START_TIME);
        if (startTime != null) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - Long.parseLong(startTime);
            return String.valueOf(executionTime);
        }
        return "-";
    }

    private static String getParams(ProceedingJoinPoint joinPoint) {
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        Map<String, Object> params = new HashMap<>();
        int i = 0;

        for (var paramName : codeSignature.getParameterNames()) {
            Object param = joinPoint.getArgs()[i++];
            params.put(paramName, param);
        }

        return toJsonString(params);
    }

    private static String toJsonString(Object param) {
        try {
            return OBJECT_MAPPER.writeValueAsString(param);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("An error occurred when trying to parse a parameter", e);
            return "parsing error";
        }
    }

    private static Object toSafeLogValueSafely(Object value) {
        try {
            return toSafeLogValue(value);
        } catch (RuntimeException e) {
            log.warn("An error occurred when trying to sanitize log value", e);

            Map<String, Object> fallback = new HashMap<>();
            fallback.put(TYPE, value != null ? value.getClass().getSimpleName() : null);
            fallback.put(SANITIZATION_ERROR, true);
            return fallback;
        }
    }

    private static Object toSafeLogValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof ResponseEntity<?> responseEntity) {
            Map<String, Object> response = new HashMap<>();
            response.put(STATUS, responseEntity.getStatusCodeValue());
            response.put(BODY, toSafeLogValueSafely(responseEntity.getBody()));
            return response;
        }

        if (value instanceof PaymentMethodsResponse paymentMethodsResponse) {
            return toSafePaymentMethodsResponse(paymentMethodsResponse);
        }

        if (value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>) {
            return value;
        }

        if (value instanceof Collection<?> collection) {
            Map<String, Object> collectionInfo = new HashMap<>();
            collectionInfo.put(TYPE, value.getClass().getSimpleName());
            collectionInfo.put(SIZE, collection.size());
            return collectionInfo;
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> mapInfo = new HashMap<>();
            mapInfo.put(TYPE, value.getClass().getSimpleName());
            mapInfo.put(SIZE, map.size());
            return mapInfo;
        }

        if (value.getClass().isArray()) {
            Map<String, Object> arrayInfo = new HashMap<>();
            arrayInfo.put(TYPE, value.getClass().getComponentType().getSimpleName() + "[]");
            arrayInfo.put(SIZE, java.lang.reflect.Array.getLength(value));
            return arrayInfo;
        }

        return toSafeObjectMap(value);
    }

    private static Map<String, Object> toSafePaymentMethodsResponse(PaymentMethodsResponse response) {
        Map<String, Object> safeResponse = new HashMap<>();

        List<PaymentMethodsItem> paymentMethods = response.getPaymentMethods();

        if (paymentMethods == null) {
            safeResponse.put(PAYMENT_METHODS, null);
            safeResponse.put(PAYMENT_METHODS_SIZE, 0);
            return safeResponse;
        }

        Map<String, Object> paymentMethodsInfo = new HashMap<>();
        paymentMethodsInfo.put(TYPE, paymentMethods.getClass().getSimpleName());
        paymentMethodsInfo.put(SIZE, paymentMethods.size());

        Map<String, Long> statusCounters = paymentMethods.stream()
                .filter(item -> item != null && item.getStatus() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getStatus().name(),
                        Collectors.counting()
                ));

        Map<String, Long> disabledReasonCounters = paymentMethods.stream()
                .filter(item -> item != null && item.getDisabledReason() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getDisabledReason().name(),
                        Collectors.counting()
                ));

        safeResponse.put(PAYMENT_METHODS, paymentMethodsInfo);
        safeResponse.put(PAYMENT_METHODS_SIZE, paymentMethods.size());
        safeResponse.put(PAYMENT_METHODS_STATUS_COUNTERS, statusCounters);
        safeResponse.put(PAYMENT_METHODS_DISABLED_REASON_COUNTERS, disabledReasonCounters);

        return safeResponse;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toSafeObjectMap(Object value) {
        Map<String, Object> rawMap = OBJECT_MAPPER.convertValue(value, Map.class);
        Map<String, Object> safeMap = new HashMap<>();

        rawMap.forEach((key, fieldValue) -> safeMap.put(key, toSafeLogValueSafely(fieldValue)));

        return safeMap;
    }

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void restController() {
        // all rest controllers
    }

    @Pointcut("@within(org.springframework.stereotype.Repository)")
    public void repository() {
        // all repository methods
    }

    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void service() {
        // all service methods
    }

    @Pointcut("execution(* *..client.*(..))")
    public void client() {
        // all client methods
    }

    /**
     * Log essential info of application during the startup.
     */
    @PostConstruct
    public void logStartup() {
        log.info("-> Starting {} version {} - environment {}", name, version, environment);
    }

    @Around(value = "restController()")
    public Object logApiInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MDC.put(METHOD, joinPoint.getSignature().getName());
        MDC.put(START_TIME, String.valueOf(System.currentTimeMillis()));
        MDC.put(OPERATION_ID, UUID.randomUUID().toString());

        if (MDC.get(REQUEST_ID) == null) {
            MDC.put(REQUEST_ID, UUID.randomUUID().toString());
        }

        String params = getParams(joinPoint);
        MDC.put(ARGS, params);

        log.info("Invoking API operation {} - args: {}", joinPoint.getSignature().getName(), params);

        try {
            Object result = joinPoint.proceed();

            Object safeResult = toSafeLogValueSafely(result);

            MDC.put(STATUS, "OK");
            MDC.put(CODE, String.valueOf(httpResponse.getStatus()));
            MDC.put(RESPONSE_TIME, getExecutionTime());
            MDC.put(RESPONSE, toJsonString(safeResult));

            /*
             * Keep the log message compact.
             * The response details are stored in the MDC field "response".
             */
            log.info("Successful API operation {}", joinPoint.getSignature().getName());

            return result;

        } catch (Throwable throwable) {
            MDC.put(STATUS, "KO");
            MDC.put(CODE, String.valueOf(httpResponse.getStatus()));
            MDC.put(RESPONSE_TIME, getExecutionTime());
            MDC.put(FAULT_CODE, throwable.getClass().getSimpleName());
            MDC.put(FAULT_DETAIL, throwable.getMessage());

            log.error(
                    "API operation {} failed before successful response logging",
                    joinPoint.getSignature().getName(),
                    throwable
            );

            throw throwable;

        } finally {
            MDC.remove(RESPONSE);
            MDC.remove(STATUS);
            MDC.remove(CODE);
            MDC.remove(RESPONSE_TIME);
            MDC.remove(FAULT_CODE);
            MDC.remove(FAULT_DETAIL);
            MDC.remove(START_TIME);
        }
    }

    @AfterReturning(value = "execution(* *..exception.ErrorHandler.*(..))", returning = "result")
    public void throwingApiInvocation(JoinPoint joinPoint, ResponseEntity<ProblemJson> result) {
        Object safeResult = toSafeLogValueSafely(result);

        MDC.put(STATUS, "KO");
        MDC.put(CODE, result != null ? String.valueOf(result.getStatusCodeValue()) : "-");
        MDC.put(RESPONSE_TIME, getExecutionTime());
        MDC.put(RESPONSE, toJsonString(safeResult));
        MDC.put(FAULT_CODE, getTitle(result));
        MDC.put(FAULT_DETAIL, getDetail(result));

        log.info("Failed API operation {}", MDC.get(METHOD));

        MDC.clear();
    }

    @Around(value = "repository() || service() || client()")
    public Object logTrace(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!log.isDebugEnabled()) {
            return joinPoint.proceed();
        }

        String params = getParams(joinPoint);
        log.debug("Call method {} - args: {}", joinPoint.getSignature().toShortString(), params);

        Object result = joinPoint.proceed();

        log.debug(
                "Return method {} - result: {}",
                joinPoint.getSignature().toShortString(),
                toJsonString(toSafeLogValueSafely(result))
        );

        return result;
    }
}