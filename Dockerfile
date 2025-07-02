# Use a base image with Java
FROM eclipse-temurin:24-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the built jar into the image
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Expose the port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
