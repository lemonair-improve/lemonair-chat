FROM openjdk:17-jdk

VOLUME /tmp

# Add Pinpoint
ADD https://github.com/pinpoint-apm/pinpoint/releases/download/v2.5.3/pinpoint-agent-2.5.3.tar.gz /usr/local
RUN tar -zxvf /usr/local/pinpoint-agent-2.5.3.tar.gz -C /usr/local

ARG PINPOINT_SERVER_IP="52.78.143.61"
# Update the Pinpoint configuration
RUN sed -i 's/profiler.transport.grpc.collector.ip=127.0.0.1/profiler.transport.grpc.collector.ip=${PINPOINT_SERVER_IP}/g' /usr/local/pinpoint-agent-2.5.3/pinpoint-root.config
RUN sed -i 's/profiler.collector.ip=127.0.0.1/profiler.collector.ip=${PINPOINT_SERVER_IP}/g' /usr/local/pinpoint-agent-2.5.3/pinpoint-root.config

ARG JAR_FILE="build/libs/*.jar"
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar",\
"-javaagent:/usr/local/pinpoint-agent-2.5.3/pinpoint-bootstrap-2.5.3.jar", \
"-Dpinpoint.applicationName=chat", \
"-Dpinpoint.config=/usr/local/pinpoint-agent-2.5.3/pinpoint-root.config", \
"-Dspring.profiles.active=common,deploy",
"/app.jar"]

