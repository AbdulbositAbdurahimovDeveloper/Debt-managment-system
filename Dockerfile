# 1-qadam: Build bosqichi
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Keshdan foydalanish uchun pom.xml ni oldin nusxalaymiz
COPY pom.xml .
RUN mvn dependency:go-offline

# Kodni nusxalab, jar faylni quramiz
COPY src ./src
RUN mvn clean package -DskipTests

# 2-qadam: Runtime bosqichi
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Xavfsizlik: Ilovani root bo'lmagan user bilan ishga tushiramiz
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Build bosqichidan faqat kerakli JARni olamiz
COPY --from=build /app/target/*.jar app.jar

# Loglar uchun joy (Jenkins volume bilan bog'lanadi)
RUN mkdir -p /app/logs && chown spring:spring /app/logs

EXPOSE 8080

# Ilovani ishga tushirish
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]