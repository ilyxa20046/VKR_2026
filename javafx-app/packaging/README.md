# Packaging Guide

Этот каталог содержит материалы для подготовки мультиплатформенных desktop-сборок JavaFX-приложения **LDPC Research Studio**.

## Целевые платформы

- Windows
- Linux
- RED OS (как Linux-совместимая среда)

## Общий подход

Исходный код приложения кроссплатформенный, но финальный пакет рекомендуется собирать **на целевой ОС**.

То есть:
- `.exe` / `.msi` — собирать на Windows;
- `.deb` / `.rpm` / app-image-like пакет — собирать на Linux / RED OS.

## Что нужно установить

- JDK 17 с утилитой `jpackage`
- Maven

Проверка:

```bash
java --version
jpackage --version
mvn --version
```

## Шаг 1. Собрать jar-файл

```bash
cd javafx-app
mvn clean package
```

После этого jar появится в `target/`.

## Шаг 2. Собрать runtime image (опционально)

Если нужно:

```bash
mvn javafx:jlink
```

## Шаг 3. Упаковка через jpackage

### Windows

```bash
jpackage \
  --type app-image \
  --name "LDPC Research Studio" \
  --input target \
  --main-jar ldpc-javafx-app-1.0-SNAPSHOT.jar \
  --main-class ru.vkr.ldpcapp.Launcher \
  --dest dist \
  --vendor "VKR Research Project" \
  --app-version 1.0.0 \
  --win-shortcut \
  --win-menu
```

### Linux / RED OS

```bash
jpackage \
  --type app-image \
  --name "LDPC Research Studio" \
  --input target \
  --main-jar ldpc-javafx-app-1.0-SNAPSHOT.jar \
  --main-class ru.vkr.ldpcapp.Launcher \
  --dest dist \
  --vendor "VKR Research Project" \
  --app-version 1.0.0
```

Если в системе доступны нужные инструменты упаковки, можно собирать:
- `--type deb`
- `--type rpm`
- `--type exe`
- `--type msi`

## Скрипты

- `package-windows.bat`
- `package-linux.sh`

Скрипты используют `app-image` как наиболее универсальный стартовый вариант для дипломного проекта.

## Практический совет для ВКР

Для защиты обычно достаточно подготовить:
- одну рабочую сборку под Windows;
- одну рабочую сборку под Linux / RED OS;
- скриншоты и описание мультиплатформенности в тексте диплома.
