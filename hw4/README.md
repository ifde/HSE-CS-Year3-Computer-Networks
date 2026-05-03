# MyDrive - Multi-User File Storage

Simple TCP client-server file synchronization application using Java/Netty Framework.

## Quick Start

### Build
```bash
mvn clean package -DskipTests
```

### Create test files (100 MB each)
```bash
./create_test_files.sh
```

### Terminal 1: Start Server
```bash
java -jar target/mydrive-server.jar 9999 /tmp/mydrive
```

### Terminal 2: Run Client
```bash
java -cp target/mydrive-server.jar mydrive.client.MyDriveClient config.properties
```

### Verify sync
```bash
ls -lh /tmp/mydrive/user1/
```

## Files Structure

- **protocol/** - TCP message serialization/deserialization
- **server/** - Server handler & Netty bootstrap  
- **client/** - Client synchronization logic
- **util/** - File utilities (MD5, directory ops)

## Protocol

Custom binary format:
- `[MessageType: 1 byte][Data: variable]`
- Message types: CLIENT_ID, FILE_LIST, FILE_RESPONSE, FILE_CHUNK
- Checksums: MD5 (16 bytes per file)

## Configuration

Edit `config.properties`:
- `server.host` - Server hostname
- `server.port` - Server port  
- `local.dir` - Directory to sync
- `client.id` - Unique client identifier
- `max.connections` - Parallel connections (1-32)
