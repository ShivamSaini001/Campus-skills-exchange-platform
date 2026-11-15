
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

REM Check whether the database password is configured
if not defined DB_PASSWORD (
    echo ERROR: MySQL database password is not configured.
    echo.
    echo Please set DB_PASSWORD in your Windows User Environment Variables.
    echo Then close and reopen this batch file.
    echo.
    pause
    exit /b 1
)

REM Check Maven installation
where mvn >nul 2>nul
if errorlevel 1 (
    echo ERROR: Maven was not found.
    echo Install Maven and add it to your PATH.
    echo.
    pause
    exit /b 1
)

REM Check Tomcat installation
if not exist "%TOMCAT_HOME%\bin\startup.bat" (
    echo ERROR: Tomcat was not found at C:\Tomcat.
    echo.
    echo Edit TOMCAT_HOME near the top of this file if needed.
    echo.
    pause
    exit /b 1
)

REM Move to the project directory
cd /d "%PROJECT_DIR%"

REM Build the project
echo Building the project...
call mvn clean package

if errorlevel 1 (
    echo.
    echo ERROR: Project build failed.
    echo Check the Maven errors above.
    echo.
    pause
    exit /b 1
)

REM Check whether the WAR file was created
if not exist "%WAR_FILE%" (
    echo.
    echo ERROR: WAR file was not generated.
    echo.
    pause
    exit /b 1
)

REM Deploy the application to Tomcat
echo.
echo Copying the new website to Tomcat...

if exist "%DEPLOY_DIR%" (
    rmdir /s /q "%DEPLOY_DIR%"
)

if exist "%TOMCAT_HOME%\webapps\CampusSkillExchange.war" (
    del /q "%TOMCAT_HOME%\webapps\CampusSkillExchange.war"
)

copy /y "%WAR_FILE%" "%TOMCAT_HOME%\webapps\CampusSkillExchange.war" >nul

if errorlevel 1 (
    echo.
    echo ERROR: Could not copy the WAR file.
    echo Check Tomcat folder permissions.
    echo.
    pause
    exit /b 1
)

REM Start Tomcat
echo.
echo Starting Tomcat...
call "%TOMCAT_HOME%\bin\startup.bat"

REM Give Tomcat time to deploy the application
echo.
echo Waiting for the website to start...
timeout /t 8 /nobreak >nul

REM Open the application
start "" "http://localhost:8080/CampusSkillExchange/"

echo.
echo -----------------------------------------
echo Website launch command completed.
echo -----------------------------------------
echo.
echo Keep Tomcat running while using the website.
echo To stop Tomcat, run:
echo C:\Tomcat\bin\shutdown.bat
echo.
pause
