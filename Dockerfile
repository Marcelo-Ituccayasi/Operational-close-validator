# syntax=docker/dockerfile:1.7

ARG BUILD_IMAGE=eclipse-temurin:25-jdk-noble
ARG RUNTIME_IMAGE=eclipse-temurin:25-jre-noble

FROM ${BUILD_IMAGE} AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

RUN chmod +x mvnw \
    && ./mvnw -B -DskipTests package \
    && test -f target/operational-close-validator-0.0.1-SNAPSHOT.jar

FROM ${RUNTIME_IMAGE} AS runtime

ARG APP_VERSION=0.0.1-SNAPSHOT
ARG VCS_REF=unknown

LABEL org.opencontainers.image.title="Operational Close Validator" \
      org.opencontainers.image.description="Operational close validation application" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.source="https://github.com/Marcelo-Ituccayasi/Operational-close-validator"

ENV TZ=UTC \
    OCV_BUILD_VERSION="${APP_VERSION}" \
    OCV_BUILD_COMMIT="${VCS_REF}" \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.io.tmpdir=/tmp"

RUN groupadd --system --gid 10001 ocv \
    && useradd --system --uid 10001 --gid ocv --home-dir /opt/ocv --shell /usr/sbin/nologin ocv \
    && mkdir -p /opt/ocv /var/lib/ocv/evidence \
    && chown -R ocv:ocv /opt/ocv /var/lib/ocv/evidence

WORKDIR /opt/ocv

COPY --from=build --chown=ocv:ocv /workspace/target/operational-close-validator-0.0.1-SNAPSHOT.jar /opt/ocv/app.jar

USER 10001:10001

EXPOSE 8080
VOLUME ["/var/lib/ocv/evidence"]

STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "-jar", "/opt/ocv/app.jar"]
