FROM eclipse-temurin:21-jdk

# Use environment variables for easier configuration
ENV VERTICLE_HOME=/usr/verticles
ENV VERTICLE_FILE=piveau-consus-importing-csw-1.0.0-SNAPSHOT.jar

# Set up work directory and a non-root user for security
EXPOSE 8080

RUN mkdir -p $VERTICLE_HOME && groupadd vertx && useradd -g vertx vertx && chown -R vertx $VERTICLE_HOME && chmod -R g+w $VERTICLE_HOME


# Copy the fat jar
COPY target/$VERTICLE_FILE $VERTICLE_HOME/

# Switch to the non-root user
USER vertx

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java $JAVA_OPTS -jar $VERTICLE_FILE"]

