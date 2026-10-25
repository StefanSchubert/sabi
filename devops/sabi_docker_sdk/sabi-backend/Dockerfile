# Base container with amazon corretto
FROM amazoncorretto:25-alpine

# Copy sabis executable jar. You need to replace the jar from the assets dir after a new build
# copyjars.sh will do it for you
COPY assets/ /

# Note: externalized /opt/config/application.properties was removed in 1.4.0
# (Config Mmgmt). Configuration now comes from the application.yml inside the
# JAR plus environment variables (see docker-compose.yml).
# Notice: docker-compose.yml sets LOCAL as application profile,
# thus you might add a application-local.yml if needed, which must not be checked
# in and has been listed in the .gitignore

# http port
EXPOSE 8080

# Enable JVM debug port
EXPOSE 8880
ENV JAVA_TOOL_OPTIONS -Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8880

WORKDIR /opt

# Start our app
ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/sabi-service.jar"]

