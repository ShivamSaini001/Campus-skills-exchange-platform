@echo off
setlocal
title Campus Skill Exchange - Run Project

set "PROJECT_DIR=%~dp0"
set "TOMCAT_HOME=C:\Tomcat"
set "WAR_FILE=%PROJECT_DIR%target\CampusSkillExchange.war"
set "DEPLOY_DIR=%TOMCAT_HOME%\webapps\CampusSkillExchange"

echo.
echo -----------------------------------------
echo     Campus Skill Exchange Platform
echo -----------------------------------------
echo.

where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven was not found. Install Maven and add it to PATH.
    pause
    exit /b 1
)

if not exist "%TOMCAT_HOME%\bin\startup.bat" (
    echo Tomcat was not found at C:\Tomcat.
    echo Edit TOMCAT_HOME near the top of this file if needed.
    pause
    exit /b 1
)

cd /d "%PROJECT_DIR%"
echo Building the project...
call mvn clean package
if errorlevel 1 (
    echo Build failed. Read the error above.
    pause
    exit /b 1
)

echo.
echo Copying the new website to Tomcat...
if exist "%DEPLOY_DIR%" rmdir /s /q "%DEPLOY_DIR%"
if exist "%TOMCAT_HOME%\webapps\CampusSkillExchange.war" del /q "%TOMCAT_HOME%\webapps\CampusSkillExchange.war"
copy /y "%WAR_FILE%" "%TOMCAT_HOME%\webapps\CampusSkillExchange.war" >nul
if errorlevel 1 (
    echo Could not copy the WAR file. Check the Tomcat folder permissions.
    pause
    exit /b 1
)

echo.
echo Starting Tomcat...
call "%TOMCAT_HOME%\bin\startup.bat"

echo Waiting for the website to start...
timeout /t 8 /nobreak >nul
start "" "http://localhost:8080/CampusSkillExchange/"

echo.
echo Browser opened. Keep Tomcat running while using the website.
echo To stop Tomcat, run C:\Tomcat\bin\shutdown.bat
echo.
pause
