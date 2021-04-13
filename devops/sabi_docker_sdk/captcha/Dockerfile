# Base container with amazon corretto
FROM amazoncorretto:11.0.10

# Copy Captcha executable jar. You need to replace the jar from the assets dir after a new build
# copyjars.sh will do it for you
COPY assets/ /

# http port
EXPOSE 8081

# Captcha starten
ENTRYPOINT ["/bin/java", "-jar", "/opt/captcha-service.jar"]
