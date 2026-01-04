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

# 1. Avval user va group yaratamiz (root sifatida)
RUN addgroup --system spring && adduser --system spring --ingroup spring

# 2. Log papkasini yaratamiz va HUQUQLARNI SOZLAYMIZ (root sifatida)
# Bu yerda /app papkasiga ham spring useriga ruxsat beramiz
RUN mkdir -p /app/logs && chown -R spring:spring /app

# 3. JAR faylni nusxalaymiz
COPY --from=build /app/target/*.jar app.jar
# JAR faylga ham egalik huquqini beramiz
RUN chown spring:spring app.jar

# 4. ENDI userga o'tamiz (Hamma tayyorgarlikdan keyin)
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]