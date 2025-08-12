FROM maven:3.9.6-amazoncorretto-17 AS deps

WORKDIR /app

# Copy only pom.xml to cache dependencies
COPY pom.xml .

# Download all dependencies and plugins (cached unless pom.xml changes)
RUN mvn dependency:resolve dependency:resolve-sources \
    dependency:download-sources \
    clean:clean compiler:compile \
    -B -q --fail-never

FROM maven:3.9.6-amazoncorretto-17 AS build

WORKDIR /app

# Copy cached dependencies from previous stage
COPY --from=deps /root/.m2 /root/.m2

# Copy project files
COPY pom.xml .
COPY src ./src

# Fast build with pre-cached dependencies
RUN mvn package -DskipTests -B -o \
    -Dmaven.main.skip \
    -Dspring-boot.repackage.skip=false \
    -Dspring.profiles.active=docker

FROM amazoncorretto:17-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Environment variables
ENV DISCORD_TOKEN=""
ENV DISCORD_GUILD_ID=""

# Expose port
EXPOSE 8085

# Create startup script for better control
COPY <<EOF /app/start.sh
#!/bin/sh
set -e

# Wait for potential file system sync
sleep 1

# Start application with optimizations
exec java \
    -server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:TieredStopAtLevel=1 \
    -XX:+TieredCompilation \
    -Xverify:none \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=docker \
    -Dspring.jmx.enabled=false \
    -Dfile.encoding=UTF-8 \
    -jar app.jar
EOF

RUN chmod +x /app/start.sh

# Health check to verify application starts
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD pgrep -f "java.*app.jar" > /dev/null || exit 1

ENTRYPOINT ["/app/start.sh"]
