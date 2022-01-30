# Base container with amazon corretto
FROM amazoncorretto:17.0.2-alpine3.15

# Copy sabis executable jar. You need to replace the jar from the assets dir after a new build
# copyjars.sh will do it for you
COPY assets/ /

# Fix the timezone
# RUN apt-get update ;\
#    apt-get install -y tzdata ;\
#    ln -fs /usr/share/zoneinfo/Europe/Berlin /etc/localtime ;\
#    dpkg-reconfigure -f noninteractive tzdata;\

RUN chmod 600 /opt/config/application.properties

# http port
EXPOSE 8080

# Enable JVM debug port
EXPOSE 8880
ENV JAVA_TOOL_OPTIONS -Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8880

WORKDIR /opt

# Start our app
ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/sabi-service-1.0-SNAPSHOT.jar"]

