FROM maven:3.9.6-amazoncorretto-17 AS build

WORKDIR /app

# Copy dependency files first for better Docker layer caching
COPY pom.xml .

# Download dependencies first (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build with optimizations
RUN mvn clean package -DskipTests -B -q \
    -Dmaven.compile.fork=true \
    -Dmaven.compiler.maxmem=1024m

FROM amazoncorretto:17-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Environment variables
ENV DISCORD_TOKEN=""
ENV DISCORD_GUILD_ID=""

# Expose port
EXPOSE 8085

# Add JVM optimizations for containerized environment and fast startup
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-XX:TieredStopAtLevel=1", \
    "-Xverify:none", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.jmx.enabled=false", \
    "-jar", "app.jar"]
