#
# Build
#
FROM maven:3.9.11-eclipse-temurin-21 AS buildtime
WORKDIR /build
COPY . .
RUN mvn clean package -Dmaven.test.skip=true


#
# Extract Spring Boot layers
#
FROM eclipse-temurin:21-jre AS builder
WORKDIR /builder
COPY --from=buildtime /build/target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract


#
# Runtime
#
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd spring && useradd -g spring spring

# OpenTelemetry agent already used by the current microservice Dockerfile
ADD --chown=spring:spring https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.25.1/opentelemetry-javaagent.jar ./opentelemetry-javaagent.jar

COPY --chown=spring:spring --from=builder /builder/dependencies/ ./
COPY --chown=spring:spring --from=builder /builder/snapshot-dependencies/ ./
# https://github.com/moby/moby/issues/37965#issuecomment-426853382
RUN true
COPY --chown=spring:spring --from=builder /builder/spring-boot-loader/ ./
COPY --chown=spring:spring --from=builder /builder/application/ ./

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java","-javaagent:opentelemetry-javaagent.jar","org.springframework.boot.loader.JarLauncher"]