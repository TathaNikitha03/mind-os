@echo off
title MIND OS Backend Service (Port 8081)
echo ============================================================
echo  Starting MIND OS Spring Boot Backend Server (Port 8081)
echo ============================================================
cd /d "%~dp0backend"

if not exist "target\backend-0.0.1-SNAPSHOT.jar" (
    echo [*] Building backend package...
    call mvnw.cmd package -DskipTests
)

echo [*] Launching Spring Boot Backend...
java -jar target\backend-0.0.1-SNAPSHOT.jar
pause
