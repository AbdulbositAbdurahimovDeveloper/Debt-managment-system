# 1-qadam: Build bosqichi
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# 2-qadam: Runtime bosqichi
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# JAR faylni build bosqichidan nusxalaymiz
COPY --from=build /app/target/*.jar app.jar

# Google Key faylini nusxalaymiz (Jenkins build vaqtida yetkazib beradi)
COPY google-key.json /google-key.json

# Loglar papkasini yaratamiz
RUN mkdir -p /app/logs

# Ruxsatlarni sozlash
RUN chmod 444 /google-key.json && chmod -R 777 /app/logs

# MUHIM: Docker-in-Docker muhitida ruxsat muammolari chiqmasligi uchun
# USER qatori olib tashlandi. Ilova root sifatida ishlaydi.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]