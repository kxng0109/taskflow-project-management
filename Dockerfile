# Use the full JDK here to build the application
FROM eclipse-temurin:25-jdk-jammy AS builder

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper and pom.xml to leverage Docker's layer caching
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download all the dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the application, skipping the tests
RUN ./mvnw package -DskipTests


# Start fresh with a much smaller JRE-only image
FROM eclipse-temurin:25-jre-jammy

# Set the working directory
WORKDIR /app

# Copy only the compiled .jar file from the "builder" stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Define the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

