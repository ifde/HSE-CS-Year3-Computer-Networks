# MyDrive - Многопользовательское файловое хранилище

## Как использовать

### Шаг 1: Сборка проекта

```bash
cd /Users/admin/Desktop/Computer\ Networks/hw4
mvn clean package -DskipTests
```

Результат: `target/mydrive-server.jar` (4.6 MB с Netty)

### Шаг 2: Создание тестовых файлов (10 файлов по 100 MB)

```bash
./create_test_files.sh
```

Это создаст директорию `test_files/` с 10 файлами для синхронизации.

### Шаг 3: Запуск сервера (Terminal 1)

```bash
./run_server.sh 9999 /tmp/mydrive
```

Или вручную:
```bash
java -jar target/mydrive-server.jar 9999 /tmp/mydrive
```

Вывод:
```
Server started on port 9999, storage: /tmp/mydrive
```

### Шаг 4: Запуск клиента (Terminal 2)

```bash
./run_client.sh config.properties
```

Вывод:
```
Sent client ID: user1
Scanned 10 files
Sent file list with 10 files
Sent file: file_1.bin (104857600 bytes)
...
Sync completed in 15234 ms
```

### Шаг 5: Проверка синхронизации

```bash
ls -lh /tmp/mydrive/user1/
```

Должны быть все 10 файлов.

---

## Структура кода

```
hw4/
├── src/main/java/mydrive/
│   ├── protocol/           # Сериализация TCP сообщений
│   │   ├── Message.java
│   │   ├── MessageType.java
│   │   ├── MessageEncoder.java    (Java объект -> байты)
│   │   ├── MessageDecoder.java    (байты -> Java объект)
│   │   ├── ClientIdMessage.java
│   │   ├── FileListMessage.java
│   │   ├── FileResponseMessage.java
│   │   └── FileChunkMessage.java
│   │
│   ├── server/             # Серверная часть
│   │   ├── MyDriveServer.java     (Netty bootstrap + event loop)
│   │   └── ServerHandler.java     (обработка каждого клиента)
│   │
│   ├── client/             # Клиентская часть
│   │   ├── MyDriveClient.java           (базовый синхронный клиент)
│   │   ├── MyDriveClientParallel.java  (параллельная отправка файлов)
│   │   └── ClientHandler.java           (обработка ответов)
│   │
│   └── util/              # Утилиты
│       └── FileUtils.java (MD5 checksum, создание директорий)
│
├── pom.xml                # Maven конфиг (Netty 4.1.100)
├── config.properties      # Конфиг клиента
├── IvanFrolov.md         # Развернутый отчет
├── README.md             # Краткая документация
├── create_test_files.sh  # Генерация тестовых файлов
├── run_server.sh         # Запуск сервера
└── run_client.sh         # Запуск клиента
```

---

## Протокол обмена сообщениями

**Базовая структура сообщения:**
```
[1 byte: MessageType] [Message specific data]
```

### 1. CLIENT_ID (тип = 1)
Клиент отправляет свой уникальный ID при подключении.
```
[1 byte: 1]
[2 bytes: id_length]
[id_length bytes: id_string]

Пример: [1] [5] ['u','s','e','r','1']
```

### 2. FILE_LIST (тип = 2)
Клиент отправляет список файлов с контрольными суммами.
```
[1 byte: 2]
[2 bytes: file_count]
  для каждого файла:
    [2 bytes: name_length]
    [name_length bytes: filename]
    [8 bytes: file_size]
    [16 bytes: MD5 checksum]
```

### 3. FILE_RESPONSE (тип = 3)
Сервер отвечает, какие файлы нужны.
```
[1 byte: 3]
[2 bytes: missing_files_count]
  для каждого отсутствующего файла:
    [2 bytes: name_length]
    [name_length bytes: filename]
```

### 4. FILE_CHUNK (тип = 4)
Клиент передает содержимое файла по частям.
```
[1 byte: 4]
[2 bytes: filename_length]
[filename_length bytes: filename]
[8 bytes: total_file_size]
[N bytes: chunk_data]
```

---

## Поток взаимодействия

```
1. CONNECT
   Клиент: TCP подключение к серверу

2. CLIENT_ID
   Клиент -> Сервер: отправляет свой ID (user1)
   Сервер: создает директорию /tmp/mydrive/user1

3. FILE_LIST
   Клиент -> Сервер:
     - Сканирует локальную директорию
     - Вычисляет MD5 для каждого файла
     - Отправляет список с контрольными суммами

4. FILE_RESPONSE
   Сервер -> Клиент:
     - Проверяет локальные файлы
     - Сравнивает контрольные суммы
     - Отправляет список файлов, которых нет или они изменились

5. FILE_CHUNK
   Клиент -> Сервер:
     - Отправляет каждый недостающий файл
     - Разбивает на chunks по 64 KB
     - После каждого файла вычисляет MD5 для проверки

6. DONE
   Синхронизация завершена
```

---

## Технические детали

### Использование Netty Framework

**Event Loop Group:**
- Один `bossGroup` принимает входящие подключения
- Несколько `workerGroup` threads обслуживают клиентов
- Каждый клиент = один handler в event loop (non-blocking I/O)

**Channel Pipeline:**
```
Channel
  ├─ MessageDecoder     (ByteBuf -> Message)
  ├─ MessageEncoder     (Message -> ByteBuf)
  └─ ServerHandler      (бизнес-логика)
```

**Non-blocking архитектура:**
- TCP данные приходят в Netty buffer
- MessageDecoder проверяет наличие полного сообщения
- Если не полное - ждет следующего пакета (не блокирует поток)
- Один thread может обслуживать тысячи клиентов

### Сериализация без сторонних framework'ов

Не используем JSON/Protobuf - пишем бинарный протокол прямо в ByteBuf:
```java
out.writeByte(MessageType.CLIENT_ID);
out.writeShort(clientId.length());
out.writeBytes(clientId.getBytes());
```

Десериализация:
```java
int type = in.readByte();
int length = in.readShort();
byte[] data = new byte[length];
in.readBytes(data);
```

### MD5 контрольные суммы

Для проверки целостности файлов при синхронизации:
```java
MessageDigest digest = MessageDigest.getInstance("MD5");
byte[] buffer = new byte[8192];
while ((bytesRead = fis.read(buffer)) != -1) {
    digest.update(buffer, 0, bytesRead);
}
byte[] checksum = digest.digest();  // 16 bytes
```

---

## Параллельная отправка файлов

В классе `MyDriveClientParallel.java` реализована отправка нескольких файлов одновременно:

```bash
java -cp target/mydrive-server.jar mydrive.client.MyDriveClientParallel config.properties
```

Параметр `max.connections=4` в `config.properties` означает:
- Открывается 4 соединения к серверу одновременно
- Каждое соединение отправляет один файл
- После завершения - следующий файл в той же очереди

```
Соединение 1: file_1.bin
Соединение 2: file_2.bin
Соединение 3: file_3.bin
Соединение 4: file_4.bin

Когда соединение 1 заканчивает -> отправляет file_5.bin
И т.д.
```

Профит:
- TCP slow start для каждого соединения разный
- Лучше использование полосы пропускания
- Параллельная обработка на сервере (Netty event loop)

---

## Конфигурация клиента

`config.properties`:
```properties
server.host=localhost        # Адрес сервера
server.port=9999            # Порт сервера
local.dir=./test_files      # Локальная директория для синхронизации
client.id=user1             # Уникальный ID клиента
max.connections=1           # Макс одновременных соединений (1-32)
```

---

## Примеры запуска

### Пример 1: Локальная синхронизация

```bash
# Терминал 1
./run_server.sh 9999 /tmp/mydrive

# Терминал 2
./create_test_files.sh
./run_client.sh config.properties
```

### Пример 2: Параллельная отправка 4 файлов одновременно

```bash
# Изменяем config.properties
sed -i '' 's/max.connections=1/max.connections=4/' config.properties

# Запускаем параллельный клиент
java -cp target/mydrive-server.jar mydrive.client.MyDriveClientParallel config.properties
```

### Пример 3: Несколько клиентов

```bash
# Терминал 1: Сервер
./run_server.sh

# Терминал 2: Клиент 1
sed -i '' 's/client.id=.*/client.id=alice/' config.properties
./run_client.sh config.properties

# Терминал 3: Клиент 2
sed -i '' 's/client.id=.*/client.id=bob/' config.properties
./run_client.sh config.properties

# Результат
ls -lh /tmp/mydrive/
# alice/
# bob/
```

---

## Контроль за TCP уровнем

### Используемые системные утилиты

```bash
# Мониторить сетевые соединения
lsof -i :9999

# Мониторить пакеты в реальном времени
tcpdump -i lo0 -n port 9999

# Проверить TCP статистику
netstat -an | grep 9999

# Отслеживать размер окна (macOS)
netstat -tnl | grep 9999

# На Linux: размер MSS и window
ss -tan | grep 9999
```

### Наблюдения при передаче файлов

1. **MSS (Maximum Segment Size)**: обычно 1460 bytes на Ethernet
2. **Window size**: начинается с ~65536, затем может увеличиться
3. **Slow start**: первые пакеты идут медленно, потом ускорение
4. **Congestion control**: автоматическая регуляция Netty

---

## Возможные проблемы и решения

### Проблема: "Port already in use"
```bash
# Найти процесс на порту
lsof -i :9999

# Убить процесс
kill -9 <PID>

# Или использовать другой порт
./run_server.sh 9998 /tmp/mydrive
```

### Проблема: "Connection refused"
- Убедиться, что сервер запущен
- Проверить параметры в config.properties
- Проверить firewall

### Проблема: "Slow transmission"
- Увеличить `max.connections` для параллельной отправки
- Проверить дисковую скорость (`dd if=/dev/zero bs=1M count=1000 of=test.bin`)
- Проверить сетевую пропускную способность

---

## Структура файлов на сервере

После первой синхронизации:

```
/tmp/mydrive/
├── user1/
│   ├── file_1.bin (100 MB)
│   ├── file_2.bin (100 MB)
│   └── ... (10 файлов)
└── bob/
    └── ... (файлы bob)
```

Каждый клиент полностью изолирован в своей директории.

---

## Ограничения текущей реализации

1. Нет удаления файлов (только добавление/обновление)
2. Нет инкрементальной синхронизации (каждый раз полный скан)
3. Нет версионирования (перезапись при изменении)
4. Нет сжатия данных
5. Нет аутентификации (только ID)

---

## Для полной оценки 10 баллов нужно добавить

1. ✅ **Базовая функциональность** (5-6 баллов) - реализовано
2. **Параллельная отправка** (+2 балла) - есть в `MyDriveClientParallel`
3. **DMA передача** (+2 балла) - использовать `DefaultFileRegion`

Пример использования DMA (не реализовано, но возможно):
```java
File file = new File(path);
FileInputStream fis = new FileInputStream(file);
DefaultFileRegion region = new DefaultFileRegion(fis.getChannel(), 0, file.length());
ctx.channel().writeAndFlush(region);
```

---

## Итого

- **Язык:** Java 11
- **Framework:** Netty 4.1.100 (асинхронная TCP коммуникация)
- **Протокол:** Собственный бинарный протокол с MD5 контрольными суммами
- **Масштабируемость:** Поддерживает неограниченное количество клиентов
- **Производительность:** До 1 ГБ файлы, параллельная передача до 32 соединений одновременно

