# Stage 1: Build the application
FROM maven:3-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean install
RUN mvn clean package

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jdk-alpine

# Use environment variables for easier configuration
ENV VERTICLE_HOME="/usr/verticles"
ENV VERTICLE_FILE="piveau-consus-patching-fat.jar"

EXPOSE 8080

# Set up work directory and a non-root user for security
RUN mkdir -p $VERTICLE_HOME && addgroup -S vertx && adduser -S vertx -G vertx && chown -R vertx $VERTICLE_HOME && chmod -R g+w $VERTICLE_HOME

# Copy the fat jar
COPY --from=builder /app/target/$VERTICLE_FILE $VERTICLE_HOME/

# Switch to the non-root user
USER vertx

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java $JAVA_OPTS -jar $VERTICLE_FILE"]


