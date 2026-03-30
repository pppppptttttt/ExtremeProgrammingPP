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
├── .github/
│   ├── workflows/          # CI, CodeQL, релизы по тегам v*
│   └── dependabot.yml      # обновления Gradle и GitHub Actions
├── chat-api/               # protobuf / gRPC контракт
├── docs/                   # требования, архитектура, план тестов, декомпозиция
├── src/main/kotlin/
│   ├── cli/
│   ├── domain/
│   ├── grpc/
│   └── Main.kt
├── src/test/kotlin/        # unit, integration, endtoend (тег e2e)
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

## Как устроено

- **`cli`** — разбор аргументов, консольный ввод-вывод;
- **`domain`** — модель сообщений, команды, порт [`ChatTransport`](src/main/kotlin/domain/port/ChatTransport.kt);
- **`grpc`** — реализация транспорта поверх gRPC;
- **`chat-api`** — protobuf-контракт и codegen для провода между процессами;
- подробности, диаграммы и обоснование решений — в **`docs/Архитектура.md`**;
- кто за что отвечал в паре — в **`docs/Декомпозиция задач.md`**.

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

Юнит- и интеграционные тесты (без сценариев с тегом `e2e`):

### Linux

```bash
./gradlew test
```

### Windows

```powershell
.\gradlew.bat test
```

## E2E-тесты

Два процесса собранного дистрибутива (`installDist`), обмен строкой в консоли. Класс: `endtoend.CliTwoProcessE2ETest`, тег JUnit **`e2e`**.

### Linux

```bash
./gradlew e2eTest
```

### Windows

```powershell
.\gradlew.bat e2eTest
```

Задача сама собирает бинарь и передаёт путь через системное свойство `e2e.binary`.

## Линтер и статический анализ

| Задача Gradle | Назначение |
|---------------|------------|
| `./gradlew ktlintCheck` | проверка стиля Kotlin (ktlint) |
| `./gradlew ktlintFormat` | автоформатирование под ktlint |
| `./gradlew detekt` | статический анализ (Detekt) |

Те же проверки выполняются в **CI** перед сборкой (см. `.github/workflows/ci.yml`).

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

Дополнительные документы в каталоге `docs/`:

* [`docs/Архитектура.md`](docs/Архитектура.md) — уровни, диаграммы, ключевые решения;
* [`docs/Требования.md`](docs/Требования.md) — формализованные требования к системе;
* [`docs/testPlan.md`](docs/testPlan.md) — план тестирования и чеклист автотестов;
* [Декомпозиция задач](docs/Декомпозиция%20задач.md) — распределение зон ответственности между участниками команды.

## Используемые технологии

* **Kotlin/JVM**, **Gradle**
* **gRPC**, **Protocol Buffers**
* **JUnit / Kotlin test**,
* **ktlint**, **Detekt**
* **GitHub Actions** (сборка, тесты, ktlint, Detekt, E2E, отдельно **CodeQL**, релизы по тегам `v*`)
* **Dependabot** (обновления зависимостей)
