#!/bin/bash

set -e

JAR_PATH="target/mydrive-server.jar"
SERVER_PORT="${1:-9999}"
STORAGE_DIR="${2:-/tmp/mydrive}"

if [ ! -f "$JAR_PATH" ]; then
    echo "JAR not found. Building..."
    mvn clean package -DskipTests
fi

echo "Starting MyDrive Server..."
echo "Port: $SERVER_PORT"
echo "Storage: $STORAGE_DIR"
echo ""
echo "Stop the server with Ctrl+C"
echo ""

java -jar "$JAR_PATH" "$SERVER_PORT" "$STORAGE_DIR"
