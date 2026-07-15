FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

COPY src src

RUN ./gradlew clean bootJar --no-daemon


FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends htop procps \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -g 1001 appuser1 \
    && useradd -m -u 1001 -g 1001 appuser1 \
    && groupadd -g 1002 appuser2 \
    && useradd -m -u 1002 -g 1002 appuser2

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]