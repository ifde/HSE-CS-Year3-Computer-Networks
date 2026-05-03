#!/bin/bash

# DMA Performance Test
# Сравнивает скорость передачи с DMA и без DMA

PORT=9997
TEST_DIR="/tmp/dma_test"
STORAGE_DIR="/tmp/dma_storage"
FILE_SIZE=${1:-100}  # Size in MB, default 100

echo "=== DMA Performance Test ==="
echo "File size: ${FILE_SIZE}MB"
echo ""

# Clean up
rm -rf "$TEST_DIR" "$STORAGE_DIR"
mkdir -p "$TEST_DIR" "$STORAGE_DIR"
lsof -ti :$PORT | xargs kill -9 2>/dev/null || true
sleep 1

# Create test files
echo "Creating test files (${FILE_SIZE}MB each)..."
dd if=/dev/zero of="$TEST_DIR/file_1.bin" bs=1M count=$FILE_SIZE 2>/dev/null
dd if=/dev/zero of="$TEST_DIR/file_2.bin" bs=1M count=$FILE_SIZE 2>/dev/null
echo "Done."
echo ""

# Start server
echo "Starting server on port $PORT..."
java -jar target/mydrive-server.jar $PORT "$STORAGE_DIR" > /tmp/dma_server.log 2>&1 &
SERVER_PID=$!
sleep 2

# Create config
cat > dma_config.properties << EOF
server.host=localhost
server.port=$PORT
local.dir=$TEST_DIR
client.id=dma_test
EOF

# Test WITHOUT DMA
echo "=== Test 1: WITHOUT DMA ==="
TIME_NO_DMA=$( { time java -cp target/mydrive-server.jar mydrive.client.MyDriveClientDMA dma_config.properties false > /tmp/dma_client_no.log 2>&1; } 2>&1 | grep real | awk '{print $2}' )
sleep 1

# Clear storage for next test
rm -rf "$STORAGE_DIR"/*

# Test WITH DMA
echo "=== Test 2: WITH DMA ==="
TIME_DMA=$( { time java -cp target/mydrive-server.jar mydrive.client.MyDriveClientDMA dma_config.properties true > /tmp/dma_client_yes.log 2>&1; } 2>&1 | grep real | awk '{print $2}' )
sleep 1

# Kill server
kill $SERVER_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null

echo ""
echo "=== Results ==="
echo "Without DMA: $TIME_NO_DMA"
echo "With DMA:    $TIME_DMA"
echo ""

# Verify files
echo "=== Files on Server ==="
find "$STORAGE_DIR" -type f -exec ls -lh {} \;

echo ""
echo "=== Logs ==="
echo "Client without DMA:"
tail -5 /tmp/dma_client_no.log
echo ""
echo "Client with DMA:"
tail -5 /tmp/dma_client_yes.log

echo ""
echo "Done."
