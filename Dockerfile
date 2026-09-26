FROM eclipse-temurin:25.0.4_7-jre-alpine

WORKDIR /terminpilot

RUN adduser -D -s /bin/sh spring

COPY --chown=spring:spring build/libs/*.jar terminpilot-backend.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "terminpilot-backend.jar"]
