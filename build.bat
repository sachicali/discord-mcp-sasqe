@echo off
set JAVA_HOME=D:\packages\scoop\apps\openjdk17\current
set PATH=%JAVA_HOME%\bin;%PATH%
echo Using Java from: %JAVA_HOME%
java -version
echo.
echo Building project...
mvn clean package -DskipTests
EOF < /dev/null
