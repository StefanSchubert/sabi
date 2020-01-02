# Base container with amazon corretto 8

FROM amazoncorretto:8u232

# Copy Captcha executable jar. You need to replace the jar from the assets dir after a new build
# copyjars.sh will do it for you
COPY assets/ /

# http port
EXPOSE 8081

# Captcha starten
ENTRYPOINT ["/bin/java", "-jar", "/opt/captcha-service.jar"]
