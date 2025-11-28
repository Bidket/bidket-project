FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY build/libs/notification-service.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]