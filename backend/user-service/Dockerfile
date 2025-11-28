FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY build/libs/user-service.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]