# syntax=docker/dockerfile:1.7

# ──────────── Stage 1: build ────────────
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace

COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY buildSrc buildSrc
COPY apps apps
COPY modules modules

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle :apps:bff:installDist --no-daemon

# ──────────── Stage 2: runtime ────────────
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /workspace/apps/bff/build/install/bff /app
RUN chown -R app:app /app
USER app

EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx1g -XX:+UseG1GC"
ENTRYPOINT ["/bin/sh", "-c", "exec /app/bin/bff $JAVA_OPTS"]
