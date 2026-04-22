@echo off
setlocal

set APP_NAME=LDPC Research Studio
set APP_VERSION=1.0.0
set MAIN_JAR=ldpc-javafx-app-1.0-SNAPSHOT.jar
set MAIN_CLASS=ru.vkr.ldpcapp.Launcher
set VENDOR=VKR Research Project

echo [1/2] Building Maven package...
call mvn clean package
if errorlevel 1 exit /b 1

echo [2/2] Packaging Windows app-image...
jpackage ^
  --type app-image ^
  --name "%APP_NAME%" ^
  --input target ^
  --main-jar "%MAIN_JAR%" ^
  --main-class %MAIN_CLASS% ^
  --dest dist ^
  --vendor "%VENDOR%" ^
  --app-version %APP_VERSION% ^
  --win-shortcut ^
  --win-menu

endlocal
