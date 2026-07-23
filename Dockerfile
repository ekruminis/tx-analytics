# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

ARG MODULE

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

ARG MODULE

COPY --from=build /app/${MODULE}/target/${MODULE}-*.jar /app/app.jar

ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "/app/app.jar"]
