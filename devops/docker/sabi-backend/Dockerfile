# Base container with amazon corretto 8

FROM amazoncorretto:8u212

# Copy sabis executable jar. You need to replace the jar from the assets dir after a new build
# copyjars.sh will do it for you
COPY assets/ /

RUN chmod 600 /opt/config/application.properties

# http port
EXPOSE 8181

WORKDIR /opt

# Sabi starten
ENTRYPOINT ["/bin/java", "-jar", "/opt/sabi-service-1.0-SNAPSHOT.jar"]
