@echo off
echo Building Discord MCP Server...
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven from https://maven.apache.org/download.cgi
    exit /b 1
)

REM Clean and compile
echo Running Maven clean compile...
mvn clean compile
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Compilation failed!
    exit /b 1
)

echo.
echo Compilation successful!
echo.

REM Package the application
echo Running Maven package...
mvn package -DskipTests
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Packaging failed!
    exit /b 1
)

echo.
echo Build completed successfully!
echo JAR file created in target/ directory
echo.
echo To run the application:
echo   java -jar target/discord-mcp-0.0.1.jar
echo.
echo Remember to set environment variables:
echo   DISCORD_TOKEN=your_bot_token
echo   DISCORD_GUILD_ID=optional_guild_id