FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY build/libs/eureka-server.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]