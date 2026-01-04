FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# JAR va Google Key-ni nusxalaymiz
COPY --from=build /app/target/*.jar app.jar
COPY google-key.json /google-key.json

# Loglar uchun papka va ruxsatlar
RUN mkdir -p /app/logs && chmod -R 777 /app/logs && chmod 444 /google-key.json

# MUHIM: Ruxsat muammosi chiqmasligi uchun USER qatori olib tashlandi.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]