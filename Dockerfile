# 1-qadam: Build bosqichi (Maven build)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# 2-qadam: Runtime bosqichi (Ilovani yurgizish)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# JAR faylni nusxalaymiz
COPY --from=build /app/target/*.jar app.jar

# MUHIM: Google Key faylini image ichiga nusxalash
# Bu fayl Build vaqtida Jenkins workspace-da bo'lishi kerak
COPY google-key.json /google-key.json

# Loglar uchun papka ochish
RUN mkdir -p /app/logs && chmod -R 777 /app/logs

# Xavfsizlik uchun fayl ruxsatini sozlash
RUN chmod 444 /google-key.json

EXPOSE 8080

# Ilovani ishga tushirish
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]