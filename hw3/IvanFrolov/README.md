# HW3 (DNS + L7 addressing) — IvanFrolov

This project implements HW3 tasks using **PCAP4J** (PCAP low-level API):
1) DNS traffic capture + interpretation
2) MX lookup in 2 steps: `D -> IPmx`
3) Compare replies from a **root DNS server** vs your **provider DNS server** for: `github.com`, `hse.ru`, `draw.io`

## Config
Fill `config.properties` (project root):
- `my.ip`, `my.mac`, `router.mac`
- `provider.dns.ip`
- `root.dns.ip` (optional)

## Build
```bash
cd hw3/IvanFrolov
mvn -q test
mvn -q package
```

## Run
On macOS you typically need `sudo` for capture/injection:
```bash
cd hw3/IvanFrolov
sudo java -jar target/IvanFrolov-1.0-SNAPSHOT-jar-with-dependencies.jar
```

The app prints supported commands on startup.
