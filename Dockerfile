FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# ... (Build bosqichi o'zgarishsiz)

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# User yaratish
RUN addgroup --system spring && adduser --system spring --ingroup spring

# 1. Log papkasini yaratamiz
# 2. Google Keyni nusxalaymiz
# 3. BUTUN /app papkasiga spring userini ega qilamiz
RUN mkdir -p /app/logs

COPY --from=build /app/target/*.jar app.jar
COPY google-key.json /google-key.json

# BU QATOR JUDA MUHIM: Hammasiga ruxsat berish
RUN chown -R spring:spring /app /app/logs /google-key.json && \
    chmod -R 775 /app/logs

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]