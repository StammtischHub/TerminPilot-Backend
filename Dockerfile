FROM eclipse-temurin:25.0.4_7-jre-alpine

WORKDIR /terminpilot

RUN adduser -D -s /bin/sh spring

COPY build/libs/*.jar terminpilot-backend.jar

RUN mkdir -p /terminpilot/tokens && chown -R spring:spring /terminpilot

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "terminpilot-backend.jar"]
