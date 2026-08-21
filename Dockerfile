FROM eclipse-temurin:25.0.4_7-jre-alpine

WORKDIR /terminpilot

RUN adduser -D -s /bin/sh spring

COPY build/libs/*.jar terminpilot-backend.jar

RUN chown spring:spring terminpilot-backend.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "terminpilot-backend.jar"]