# ---- build ----------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies before copying the sources.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- run ------------------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin app
USER app

COPY --from=build /build/target/accommodation-finder.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
