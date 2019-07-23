# Base container with amazon corretto 8

FROM amazoncorretto:8u212

# Copy sabis executable jar. You need to replace the jar from the assets dir after a new build
# copyjars.sh will do it for you
COPY assets/ /

RUN chmod 600 /opt/config/application.properties

# http port
EXPOSE 8080
# JVM debug port
EXPOSE 8880

WORKDIR /opt

# Start with debugging support
ENTRYPOINT ["/bin/java", "-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,address=8880,suspend=n", "-jar", "/opt/sabi-service-1.0-SNAPSHOT.jar"]

