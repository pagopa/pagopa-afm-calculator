package it.gov.pagopa.afm.calculator.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.afm.calculator.exception.AppError;
import it.gov.pagopa.afm.calculator.model.ProblemJson;
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
            Object body = responseEntity.getBody();
            return "ResponseEntity(status=" + responseEntity.getStatusCodeValue()
                    + ", bodyType=" + (body != null ? body.getClass().getSimpleName() : "null")
                    + ")";
        }

        if (value instanceof Collection<?> collection) {
            return value.getClass().getSimpleName() + "(size=" + collection.size() + ")";
        }

        if (value instanceof Map<?, ?> map) {
            return value.getClass().getSimpleName() + "(size=" + map.size() + ")";
        }

        if (value.getClass().isArray()) {
            return value.getClass().getComponentType().getSimpleName() + "[]";
        }

        return value.getClass().getSimpleName();
    }

    private static String toSafeJsonString(Object value) {
        return toJsonString(toSafeLogValue(value));
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
        // all service methods
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

        MDC.put(STATUS, "OK");
        MDC.put(CODE, String.valueOf(httpResponse.getStatus()));
        MDC.put(RESPONSE_TIME, getExecutionTime());
        MDC.put(RESPONSE, toSafeJsonString(result));
        log.info(
                "Successful API operation {} - result: {}",
                joinPoint.getSignature().getName(),
                toSafeLogValue(result)
        );
        MDC.remove(RESPONSE);
        MDC.remove(STATUS);
        MDC.remove(CODE);
        MDC.remove(RESPONSE_TIME);
        MDC.remove(START_TIME);
        return result;
    }

    @AfterReturning(value = "execution(* *..exception.ErrorHandler.*(..))", returning = "result")
    public void trowingApiInvocation(JoinPoint joinPoint, ResponseEntity<ProblemJson> result) {
        MDC.put(STATUS, "KO");
        MDC.put(CODE, String.valueOf(result.getStatusCodeValue()));
        MDC.put(RESPONSE_TIME, getExecutionTime());
        MDC.put(RESPONSE, toSafeJsonString(result));
        MDC.put(FAULT_CODE, getTitle(result));
        MDC.put(FAULT_DETAIL, getDetail(result));
        log.info("Failed API operation {} - error: {}", MDC.get(METHOD), toSafeLogValue(result));
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
