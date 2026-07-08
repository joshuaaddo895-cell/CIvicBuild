# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

RUN chmod +x mvnw

COPY src src

RUN ./mvnw package -DskipTests -B -q

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system civicbuild \
    && useradd --system --gid civicbuild civicbuild

COPY --from=build /app/target/*.jar app.jar
RUN chown civicbuild:civicbuild app.jar

USER civicbuild

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
