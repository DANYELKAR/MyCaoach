FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spark && adduser -S spark -G spark && apk add --no-cache curl
WORKDIR /app
RUN mkdir -p /app/inputs && chown spark:spark /app/inputs
COPY --from=build --chown=spark:spark /workspace/target/spark-api-0.0.1-SNAPSHOT.jar app.jar
USER spark
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 CMD curl --fail --silent http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
