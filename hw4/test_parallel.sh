#!/bin/bash

NUM_CLIENTS=${1:-2}
USE_PARALLEL=${2:-true}
PORT=${3:-9998}

echo "=== MyDrive Parallel Test ==="
echo "Clients: $NUM_CLIENTS, Parallel: $USE_PARALLEL, Port: $PORT"
echo ""

# Kill old processes on THIS port only
lsof -ti :$PORT | xargs kill -9 2>/dev/null || true
sleep 1

# Clean and start server
rm -rf /tmp/mydrive_test
java -jar target/mydrive-server.jar $PORT /tmp/mydrive_test > /tmp/server.log 2>&1 &
sleep 3

# Start clients
for i in $(seq 1 $NUM_CLIENTS); do
    mkdir -p "./test_files_$i"
    
    for j in $(seq 1 2); do
        dd if=/dev/zero of="./test_files_$i/file_${j}.bin" bs=1M count=50 2>/dev/null
    done
    
    cat > "config_$i.properties" << EOF
server.host=localhost
server.port=$PORT
local.dir=./test_files_$i
client.id=user$i
max.connections=4
EOF
    
    if [ "$USE_PARALLEL" = "true" ]; then
        java -cp target/mydrive-server.jar mydrive.client.MyDriveClientParallel "config_$i.properties" > "/tmp/client_$i.log" 2>&1 &
    else
        java -cp target/mydrive-server.jar mydrive.client.MyDriveClient "config_$i.properties" > "/tmp/client_$i.log" 2>&1 &
    fi
done

# Wait with simple sleep
echo "Waiting 10 seconds..."
sleep 10

# Kill remaining Java processes
sleep 1
lsof -ti :$PORT | xargs kill -9 2>/dev/null || true

# Results
echo ""
echo "=== Files on Server ==="
find /tmp/mydrive_test -type f -exec ls -lh {} \; 2>/dev/null || echo "No files"

echo ""
echo "=== Server Log ==="
tail -30 /tmp/server.log 2>/dev/null || echo "No log"

echo ""
echo "=== Client 1 Log ==="
tail -30 /tmp/client_1.log 2>/dev/null || echo "No log"

echo ""
echo "Done."
