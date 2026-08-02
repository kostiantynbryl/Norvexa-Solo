@echo off
setlocal EnableExtensions
set "GRADLE_VERSION=9.5.0"
set "APP_HOME=%~dp0"
set "DIST_DIR=%APP_HOME%.gradle-dist"
set "GRADLE_HOME=%DIST_DIR%\gradle-%GRADLE_VERSION%"
set "ZIP_FILE=%DIST_DIR%\gradle-%GRADLE_VERSION%-bin.zip"
set "URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"

if exist "%GRADLE_HOME%\bin\gradle.bat" goto run
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
if not exist "%ZIP_FILE%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%URL%' -OutFile '%ZIP_FILE%'"
  if errorlevel 1 exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force -Path '%ZIP_FILE%' -DestinationPath '%DIST_DIR%'"
if errorlevel 1 exit /b 1

:run
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
