FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# User yaratamiz
RUN addgroup --system spring && adduser --system spring --ingroup spring

# LOG papkasini yaratib, egasini tayinlaymiz
RUN mkdir -p /app/logs && chown -R spring:spring /app/logs

COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar

# MUHIM: Ilovani vaqtinchalik ROOT sifatida qoldiramiz (pastga qarang)
# Yoki UID ni aniq belgilaymiz.
#USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]