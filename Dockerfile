#
# Build
#
FROM maven:3.9.9-amazoncorretto-17-al2023 AS buildtime
WORKDIR /build
COPY . .
RUN mvn clean package -Dmaven.test.skip=true

FROM amazoncorretto:17-alpine3.21 AS builder
COPY --from=buildtime /build/target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

#
# Run
#
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /home/spring

ADD --chown=spring:spring https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.12.0/opentelemetry-javaagent.jar .

COPY --chown=spring:spring  --from=builder dependencies/ ./
COPY --chown=spring:spring  --from=builder snapshot-dependencies/ ./
# https://github.com/moby/moby/issues/37965#issuecomment-426853382
RUN true
COPY --chown=spring:spring  --from=builder spring-boot-loader/ ./
COPY --chown=spring:spring  --from=builder application/ ./

EXPOSE 8080

ENTRYPOINT ["java","-javaagent:opentelemetry-javaagent.jar","--enable-preview","org.springframework.boot.loader.JarLauncher"]
