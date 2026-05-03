#!/bin/bash

set -e

JAR_PATH="target/mydrive-server.jar"
CONFIG_FILE="${1:-config.properties}"

if [ ! -f "$JAR_PATH" ]; then
    echo "JAR not found. Building..."
    mvn clean package -DskipTests
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Config file not found: $CONFIG_FILE"
    exit 1
fi

echo "Starting MyDrive Client..."
echo "Config: $CONFIG_FILE"
echo ""

java -cp "$JAR_PATH" mydrive.client.MyDriveClient "$CONFIG_FILE"
