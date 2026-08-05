FROM eclipse-temurin:21-jre

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

RUN mkdir -p /app/firebase

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
