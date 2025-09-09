# Use a slim-downed version of OpenJDK as a base image
FROM openjdk:17-slim

# Set the working directory
WORKDIR /app

# Copy the Vert.x fat-jar into the container
# The name of the fat-jar is defined in the pom.xml's build configuration
COPY target/piveau-consus-importing-csw-1.0.0-SNAPSHOT-fat.jar piveau-consus-importing-csw.jar

# Run the application when the container starts
CMD ["java", "-jar", "piveau-consus-importing-csw.jar"]
