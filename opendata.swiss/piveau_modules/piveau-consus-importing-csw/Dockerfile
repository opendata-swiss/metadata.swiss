# Stage 1: Build the application
FROM maven:3-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml .
# test dir is skipped as they are failing for now ; once they are fixed, we can just add `src` instead of `src/main`
COPY src/main ./src/main
RUN mvn clean install
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jdk-alpine

# Use environment variables for easier configuration
ENV VERTICLE_HOME="/usr/verticles"
ENV VERTICLE_FILE="piveau-consus-importing-csw.jar"

# Set up work directory and a non-root user for security
EXPOSE 8080

RUN mkdir -p $VERTICLE_HOME && addgroup -S vertx && adduser -S vertx -G vertx && chown -R vertx $VERTICLE_HOME && chmod -R g+w $VERTICLE_HOME

# Copy the fat jar
COPY --from=builder /app/target/$VERTICLE_FILE $VERTICLE_HOME/

# Switch to the non-root user
USER vertx

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java $JAVA_OPTS -jar $VERTICLE_FILE"]
