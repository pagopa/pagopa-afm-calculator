FROM alpine:latest@sha256:8a1f59ffb675680d47db6337b49d22281a139e9d709335b492be023728e11715

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






