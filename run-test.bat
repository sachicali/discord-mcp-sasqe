@echo off
set JAVA_HOME=D:\packages\scoop\apps\openjdk17\current
set PATH=%JAVA_HOME%\bin;%PATH%
set DISCORD_TOKEN=test_token_123
echo Starting MCP server with test token...
java -jar target\discord-mcp-0.0.1.jar
EOF < /dev/null
