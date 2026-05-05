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
    private static final String PAYMENT_METHOD_ID = "paymentMethodId";
    private static final String GROUP = "group";
    private static final String DISABLED_REASON = "disabledReason";
    private static final String FEE_RANGE = "feeRange";
    private static final String PAYMENT_METHOD_TYPES = "paymentMethodTypes";
    private static final String METHOD_MANAGEMENT = "methodManagement";
    private static final String VALIDITY_DATE_FROM = "validityDateFrom";

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
        } else return AppError.UNKNOWN.getDetails();
    }

    private static String getTitle(ResponseEntity<ProblemJson> result) {
        if (result != null && result.getBody() != null && result.getBody().getTitle() != null) {
            return result.getBody().getTitle();
        } else return AppError.UNKNOWN.getTitle();
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
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .writeValueAsString(param);
        } catch (JsonProcessingException e) {
            log.warn("An error occurred when trying to parse a parameter", e);
            return "parsing error";
        }
    }

    private static Object toSafeLogValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof ResponseEntity<?> responseEntity) {
            Map<String, Object> response = new HashMap<>();
            response.put(STATUS, responseEntity.getStatusCodeValue());
            response.put(BODY, toSafeLogValue(responseEntity.getBody()));
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

        List<Map<String, Object>> safePaymentMethods = paymentMethods.stream()
                .map(LoggingAspect::toSafePaymentMethodsItem)
                .toList();

        safeResponse.put(PAYMENT_METHODS, safePaymentMethods);
        safeResponse.put(PAYMENT_METHODS_SIZE, safePaymentMethods.size());

        return safeResponse;
    }

    private static Map<String, Object> toSafePaymentMethodsItem(PaymentMethodsItem item) {
        Map<String, Object> safeItem = new HashMap<>();

        safeItem.put(PAYMENT_METHOD_ID, item.getPaymentMethodId());
        safeItem.put(STATUS, item.getStatus());
        safeItem.put(GROUP, item.getGroup());
        safeItem.put(DISABLED_REASON, item.getDisabledReason());
        safeItem.put(FEE_RANGE, item.getFeeRange());
        safeItem.put(PAYMENT_METHOD_TYPES, item.getPaymentMethodTypes());
        safeItem.put(METHOD_MANAGEMENT, item.getMethodManagement());
        safeItem.put(VALIDITY_DATE_FROM, item.getValidityDateFrom());

        return safeItem;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toSafeObjectMap(Object value) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        Map<String, Object> rawMap = objectMapper.convertValue(value, Map.class);
        Map<String, Object> safeMap = new HashMap<>();

        rawMap.forEach((key, fieldValue) -> safeMap.put(key, toSafeLogValue(fieldValue)));

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
            var requestId = UUID.randomUUID().toString();
            MDC.put(REQUEST_ID, requestId);
        }

        String params = getParams(joinPoint);
        MDC.put(ARGS, params);

        log.info("Invoking API operation {} - args: {}", joinPoint.getSignature().getName(), params);

        Object result = joinPoint.proceed();
        Object safeResult = toSafeLogValue(result);

        MDC.put(STATUS, "OK");
        MDC.put(CODE, String.valueOf(httpResponse.getStatus()));
        MDC.put(RESPONSE_TIME, getExecutionTime());
        MDC.put(RESPONSE, toJsonString(safeResult));

        log.info(
                "Successful API operation {} - result: {}",
                joinPoint.getSignature().getName(),
                safeResult
        );

        MDC.remove(RESPONSE);
        MDC.remove(STATUS);
        MDC.remove(CODE);
        MDC.remove(RESPONSE_TIME);
        MDC.remove(START_TIME);

        return result;
    }

    @AfterReturning(value = "execution(* *..exception.ErrorHandler.*(..))", returning = "result")
    public void throwingApiInvocation(JoinPoint joinPoint, ResponseEntity<ProblemJson> result) {
        Object safeResult = toSafeLogValue(result);

        MDC.put(STATUS, "KO");
        MDC.put(CODE, result != null ? String.valueOf(result.getStatusCodeValue()) : "-");
        MDC.put(RESPONSE_TIME, getExecutionTime());
        MDC.put(RESPONSE, toJsonString(safeResult));
        MDC.put(FAULT_CODE, getTitle(result));
        MDC.put(FAULT_DETAIL, getDetail(result));

        log.info("Failed API operation {} - error: {}", MDC.get(METHOD), safeResult);

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
                toSafeLogValue(result)
        );

        return result;
    }
}