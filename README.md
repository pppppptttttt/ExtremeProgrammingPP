# ExtremeProgrammingPP

[![CI](https://github.com/pppppptttttt/ExtremeProgrammingPP/actions/workflows/ci.yml/badge.svg)](https://github.com/pppppptttttt/ExtremeProgrammingPP/actions/workflows/ci.yml)

Консольный **peer-to-peer чат на Kotlin/JVM** с прямым соединением между двумя участниками.  
Для сетевого взаимодействия используется **gRPC**, а контракт сообщений описан в **Protocol Buffers**.

## Возможности

- прямое соединение между двумя peer-ами;
- запуск в режиме ожидания подключения;
- запуск в режиме подключения к уже запущенному peer-у;
- консольный обмен сообщениями;
- отображение:
  - имени отправителя;
  - даты и времени отправки;
  - текста сообщения;

## Структура проекта

```text
ExtremeProgrammingPP/
├── .github/workflows/ci.yml
├── chat-api/                     # protobuf/gRPC контракт
├── docs/                         # архитектура и требования
├── src/main/kotlin/
│   ├── cli/
│   ├── domain/
│   ├── grpc/
│   └── Main.kt
├── src/test/kotlin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

## Требования к окружению

Для локальной сборки нужен:

* **JDK 21**;
* доступный `bash` или `PowerShell` для запуска Gradle Wrapper.

## Сборка проекта

### Linux

```bash
./gradlew clean build
```

### Windows

```powershell
.\gradlew.bat clean build
```

## Запуск тестов

### Linux

```bash
./gradlew test
```

### Windows

```powershell
.\gradlew.bat test
```

## Проверка запуска приложения

Чтобы проверить, что приложение корректно стартует и CLI доступен:

### Linux

```bash
./gradlew run --args="--help"
```

### Windows

```powershell
.\gradlew.bat run --args="--help"
```

## Запуск приложения

Приложение поддерживает два основных сценария:

### 1. Запуск peer-а в режиме ожидания подключения

Пример:

#### Linux

```bash
./gradlew run --args="--name Alice --listen-port 50051"
```

#### Windows

```powershell
.\gradlew.bat run --args="--name Alice --listen-port 50051"
```

* приложение стартует;
* поднимает локальный gRPC server;
* ждёт входящего подключения другого peer-а.

### 2. Подключение ко второму peer-у

Пример:

#### Linux

```bash
./gradlew run --args="--name Bob --peer-host 127.0.0.1 --peer-port 50051"
```

#### Windows

```powershell
.\gradlew.bat run --args="--name Bob --peer-host 127.0.0.1 --peer-port 50051"
```

Ожидаемое поведение:

* приложение создаёт исходящее соединение;
* устанавливает gRPC stream;
* после подключения можно отправлять сообщения в консоль.

## Формат сообщений

```text
[2026-03-30 13:45:12] Alice: Привет!
[2026-03-30 13:45:18] Bob: Привет, соединение работает.
```

## Документация

Дополнительные документы находятся в каталоге `docs/`:

* `docs/Архитектура.md` — описание архитектуры проекта;
* `docs/Требования.md` — формализованные требования к системе.

## Используемые технологии

* **Kotlin/JVM**
* **gRPC**
* **Protocol Buffers**
* **Gradle**
* **JUnit / Kotlin test**
* **GitHub Actions**
