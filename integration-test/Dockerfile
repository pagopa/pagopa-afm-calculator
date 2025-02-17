FROM alpine:latest@sha256:a8560b36e8b8210634f77d9f7f9efd7ffa463e380b75e2e74aff4511df3ef88c

ARG ALLURE_RELEASE=2.25.0
ARG ALLURE_REPO=https://repo.maven.apache.org/maven2/io/qameta/allure/allure-commandline
COPY generate_allure_report.sh generate_allure_report.sh

RUN apk update && \
	apk add ca-certificates && \
	update-ca-certificates && \
	apk add openssl && \
	apk add unzip && \
    apk add --no-cache openjdk8-jre-base

RUN wget ${ALLURE_REPO}/${ALLURE_RELEASE}/allure-commandline-${ALLURE_RELEASE}.tgz -O allure-commandline.tgz
RUN tar -zxvf allure-commandline.tgz -C /opt/
RUN ln -s /opt/allure-${ALLURE_RELEASE}/bin/allure /usr/bin/allure
RUN rm allure-commandline.tgz

RUN allure --version
CMD ["sh", "generate_allure_report.sh"]






