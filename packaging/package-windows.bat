@echo off
setlocal EnableExtensions

REM Go to project root (works even when started from packaging\ folder)
pushd "%~dp0.."
if not exist "pom.xml" (
  echo ERROR: pom.xml not found in %CD%
  goto :fail
)

REM =========================
REM Fixed local tool paths
REM =========================
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "MAVEN_HOME=C:\jetbra\apache-maven-3.9.15"
set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

REM =========================
REM Release metadata
REM =========================
set "APP_NAME=LDPC Research Studio"
set "APP_VERSION=1.0.0"
set "MAIN_CLASS=ru.vkr.ldpcapp.Launcher"
set "VENDOR=VKR Research Project"

echo.
echo [1/8] Checking required tools...
if not exist "%MVN_CMD%" (echo ERROR: Maven not found at %MVN_CMD% & goto :fail)
if not exist "%JAVA_HOME%\bin\java.exe" (echo ERROR: Java not found at %JAVA_HOME% & goto :fail)
where jpackage >nul 2>nul || (echo ERROR: jpackage not found in PATH & goto :fail)

echo.
echo [2/8] Tool versions:
"%JAVA_HOME%\bin\java.exe" -version
call "%MVN_CMD%" -version
jpackage --version

echo.
echo [3/8] Cleaning previous build folders...
if exist target rmdir /s /q target
if exist dist rmdir /s /q dist

echo.
echo [4/8] Building Maven package...
call "%MVN_CMD%" clean package
if errorlevel 1 goto :fail

set "MAIN_JAR="
for %%f in (target\ldpc-javafx-app-*.jar) do (
  set "MAIN_JAR=%%~nxf"
  goto :jar_found
)
echo ERROR: no target\ldpc-javafx-app-*.jar found
goto :fail

:jar_found
echo Using JAR: %MAIN_JAR%

echo.
echo [5/8] Copying runtime dependencies...
if exist target\deps rmdir /s /q target\deps
call "%MVN_CMD%" -q dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\deps
if errorlevel 1 goto :fail

echo.
echo [6/8] Preparing jpackage input...
if exist target\jpackage-input rmdir /s /q target\jpackage-input
mkdir target\jpackage-input
copy /Y "target\%MAIN_JAR%" "target\jpackage-input\" >nul
xcopy /Y /I "target\deps\*.jar" "target\jpackage-input\" >nul

echo.
echo [7/8] Detecting packaging type...
set "PKG_TYPE=app-image"
where candle >nul 2>nul
if %errorlevel%==0 (
  where light >nul 2>nul
  if %errorlevel%==0 set "PKG_TYPE=exe"
)
echo Packaging type: %PKG_TYPE%

echo.
echo [8/8] Packaging with jpackage...
if /I "%PKG_TYPE%"=="exe" (
  jpackage ^
    --type exe ^
    --name "%APP_NAME%" ^
    --input target\jpackage-input ^
    --main-jar "%MAIN_JAR%" ^
    --main-class %MAIN_CLASS% ^
    --dest dist ^
    --vendor "%VENDOR%" ^
    --app-version %APP_VERSION% ^
    --java-options "-cp $APPDIR\*" ^
    --win-shortcut ^
    --win-menu ^
    --win-dir-chooser ^
    --win-per-user-install
) else (
  jpackage ^
    --type app-image ^
    --name "%APP_NAME%" ^
    --input target\jpackage-input ^
    --main-jar "%MAIN_JAR%" ^
    --main-class %MAIN_CLASS% ^
    --dest dist ^
    --vendor "%VENDOR%" ^
    --app-version %APP_VERSION% ^
    --java-options "-cp $APPDIR\*"
)
if errorlevel 1 goto :fail

echo.
echo SUCCESS
if /I "%PKG_TYPE%"=="exe" (
  echo Installer:
  dir /b dist\*.exe
) else (
  echo Portable app-image:
  echo dist\%APP_NAME%\%APP_NAME%.exe
)

popd
pause
exit /b 0

:fail
echo.
echo BUILD FAILED
echo If EXE build fails, install WiX Toolset 3.x and add candle/light to PATH.
popd
pause
exit /b 1