FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser
COPY --from=build --chown=appuser:appuser /workspace/target/purpleloop-0.1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
