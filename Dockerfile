FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# 2-qadam: Runtime bosqichi
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN mkdir -p /app/logs && chown -R spring:spring /app

# JAR faylni nusxalash
COPY --from=build /app/target/*.jar app.jar

# MUHIM: Google Key faylini konteyner ildiziga nusxalash
# Bu fayl Build bosqichida Jenkins tomonidan yetkazib beriladi
COPY google-key.json /google-key.json

# Faylga spring useri uchun ruxsat berish
RUN chown spring:spring /google-key.json && chmod 444 /google-key.json
RUN chown spring:spring app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]