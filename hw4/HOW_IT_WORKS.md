# MyDrive - How It Works

Простое приложение для синхронизации файлов между клиентом и сервером по протоколу TCP с использованием Netty Framework.

## Архитектура

### Server
- Слушает на TCP порту (по умолчанию 9999)
- Для каждого клиента отдельный обработчик (handler)
- Каждый клиент имеет свой ID и директорию хранения
- Поддерживает неограниченное количество одновременных подключений

### Client
- Подключается к серверу
- Отправляет список файлов из локальной директории
- Получает список файлов для синхронизации
- Отправляет недостающие файлы по TCP

## Протокол коммуникации

### Шаг 1: Отправка ID клиента
```
[MessageType: CLIENT_ID][ClientID: variable length]
```

### Шаг 2: Отправка списка файлов
```
[MessageType: FILE_LIST][FileCount: 4 bytes]
Для каждого файла:
  [NameLength: 4 bytes][Name: variable]
  [Size: 8 bytes]
  [MD5Checksum: 16 bytes]
```

### Шаг 3: Получение списка для синхронизации
```
[MessageType: FILE_RESPONSE][FileCount: 4 bytes]
Список файлов для отправки (по тому же формату что FILE_LIST)
```

### Шаг 4: Отправка файлов
```
[MessageType: FILE_CHUNK]
[NameLength: 4 bytes][Name: variable]
[Size: 8 bytes]
[Data: Size bytes]
```

## Структура кода

```
src/main/java/mydrive/
├── Server.java              - Точка входа сервера
├── Client.java              - Точка входа клиента
├── protocol/
│   ├── Message.java         - Базовый класс сообщения
│   ├── MessageEncoder.java  - Сериализация в байты
│   ├── MessageDecoder.java  - Десериализация из байт
│   ├── FileInfo.java        - Информация о файле
│   └── MessageType.java     - Типы сообщений
├── server/
│   ├── ServerHandler.java   - Обработка подключений
│   └── FileStorage.java     - Хранение файлов на сервере
├── client/
│   ├── ClientHandler.java   - Логика синхронизации
│   └── FileScanner.java     - Сканирование локальных файлов
└── util/
    ├── FileUtils.java       - Утилиты для работы с файлами
    ├── Checksum.java        - Вычисление контрольных сумм
    └── Config.java          - Загрузка конфигурации
```

## Использование

### 1. Компиляция
```bash
mvn clean package -DskipTests
```

### 2. Создание тестовых файлов (100 МБ каждый)
```bash
./create_test_files.sh
```

### 3. Запуск сервера (терминал 1)
```bash
java -jar target/mydrive-server.jar 9999 /tmp/mydrive
```

### 4. Запуск клиента (терминал 2)
```bash
java -cp target/mydrive-server.jar mydrive.client.MyDriveClient config.properties
```

### 5. Проверка синхронизации
```bash
ls -lh /tmp/mydrive/user1/
md5sum /tmp/mydrive/user1/* | head -3
```

## Конфигурация

`config.properties`:
```properties
client.id=user1
client.directory=test_files
server.host=localhost
server.port=9999
max.connections=8
use.dma=false
```

## Особенности реализации

- **TCP протокол**: Прямая работа с массивами байт через `ByteBuf`
- **Netty Framework**: `ReplayingDecoder` для десериализации
- **MD5 контрольные суммы**: Проверка целостности файлов
- **Многопоточность**: Каждый клиент в отдельном потоке через Netty `EventLoop`
- **Минимальный код**: Без лишних абстракций и шаблонов

## Тестирование

После запуска синхронизации:
```bash
# Проверить файлы на сервере
ls -lh /tmp/mydrive/user1/

# Проверить контрольные суммы
md5sum test_files/* > /tmp/client_checksums.txt
md5sum /tmp/mydrive/user1/* > /tmp/server_checksums.txt
diff /tmp/client_checksums.txt /tmp/server_checksums.txt
```

## Сетевые наблюдения (для отчета)

При использовании WireShark возможно увидеть:
- **MSS (Maximum Segment Size)**: Обычно 1460 байт
- **Window Size**: Размер буфера приема (обычно 64-256 КБ)
- **TCP Three-Way Handshake**: При подключении клиента
- **Slow Start**: Экспоненциальный рост окна в начале передачи
- **TCP Flags**: SYN, ACK, PSH, FIN для управления потоком

## Запуск с параллельными подключениями

Если реализована поддержка нескольких соединений, клиент автоматически создает N подключений (настраивается в `max.connections`).
