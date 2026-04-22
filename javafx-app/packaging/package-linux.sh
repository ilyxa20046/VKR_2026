#!/usr/bin/env bash
set -e

APP_NAME="LDPC Research Studio"
APP_VERSION="1.0.0"
MAIN_JAR="ldpc-javafx-app-1.0-SNAPSHOT.jar"
MAIN_CLASS="ru.vkr.ldpcapp.Launcher"
VENDOR="VKR Research Project"

echo "[1/2] Building Maven package..."
mvn clean package

echo "[2/2] Packaging Linux app-image..."
jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --input target \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --dest dist \
  --vendor "$VENDOR" \
  --app-version "$APP_VERSION"
