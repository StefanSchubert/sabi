# Base container with amazon corretto 8

FROM amazoncorretto:8u212

# Copy Captcha executable jar. You need to replace the jar from the assets dir after a new build
COPY assets/ /

# http port
EXPOSE 8181

# Captcha starten
ENTRYPOINT ["/bin/java", "-jar", "/opt/captcha-service.jar"]
