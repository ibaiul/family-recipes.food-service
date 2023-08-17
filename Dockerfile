FROM eclipse-temurin:17-alpine

ENV TZ="Europe/Madrid"

RUN mkdir -p /usr/local/newrelic
ARG NEWRELIC_AGENT_VERSION=8.4.0
ADD https://download.newrelic.com/newrelic/java-agent/newrelic-agent/${NEWRELIC_AGENT_VERSION}/newrelic-agent-${NEWRELIC_AGENT_VERSION}.jar /usr/local/newrelic/newrelic.jar
RUN chmod 444 /usr/local/newrelic/newrelic.jar
ENV JAVA_OPTS="$JAVA_OPTS -javaagent:/usr/local/newrelic/newrelic.jar"
ADD ./newrelic/newrelic-query.yml /usr/local/newrelic/newrelic.yml

RUN addgroup -S app --gid 10001 && adduser -S app -G app --uid 10001
USER app

ARG MODULE_NAME
ARG APP_NAME=food-service-${MODULE_NAME}
ARG JAR_FILE=${MODULE_NAME}/build/libs/${APP_NAME}-1.0-SNAPSHOT.jar
ADD ${JAR_FILE} /app/${APP_NAME}.jar
ENV APP_NAME=${APP_NAME}

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/${APP_NAME}.jar ${APP_OPTS}"]
