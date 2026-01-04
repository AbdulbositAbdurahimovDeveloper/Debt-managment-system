# 1-Bosqich: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# 2-Bosqich: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# JAR faylni nusxalash
COPY --from=build /app/target/*.jar app.jar

# Log papkasini yaratish
RUN mkdir -p /app/logs && chmod -R 777 /app/logs

EXPOSE 8080

# Ilovani ishga tushirish
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]