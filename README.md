# 🛡️ LDPC Research Studio

<div align="center">

![Java](https://img.shields.io/badge/Java-17_LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.10-3E4C8A?style=for-the-badge&logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.8+-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20RED%20OS-0078D6?style=for-the-badge&logo=windows&logoColor=white)

**Выпускная квалификационная работа (ВКР), 2026**

*«Исследование и моделирование системы помехоустойчивого кодирования в зашумлённом канале мобильной сети 5G»*

</div>

---

## 📖 О проекте

**LDPC Research Studio** — мультиплатформенное desktop-приложение для исследования и моделирования системы помехоустойчивого **LDPC-кодирования** в зашумлённом канале мобильной сети **5G NR**.

Приложение реализует полный стек физического уровня (PHY): от генерации информационных бит и LDPC-кодирования до передачи через зашумленный канал, демодуляции и расчёта ключевых метрик качества связи.

### Что умеет приложение

- Моделирование передачи данных через каналы **AWGN** и **Rayleigh fading**
- Поддержка модуляций **BPSK / QPSK / 16-QAM / 64-QAM / 256-QAM**
- Поддержка волновых форм **Single-carrier / OFDM-64 / OFDM-128**
- Пространственные режимы **SISO** и **2×2 Diversity (MIMO)**
- Пять LDPC-профилей: **Учебный LDPC (24,12) / QC-подобный LDPC (96,48) / 5G NR Base Graph (1/2)**(3GPP)** / Polar-подобный (128,64) / Turbo LTE-подобный (R=1/3)**
- Итеративное декодирование методом **Normalized Min-Sum (Belief Propagation)**
- Расчёт **BER, BLER, Throughput, Spectral Efficiency, Coding Gain**
- **A/B-сравнение** двух сценариев и **пакетный (batch) анализ**
- Экспорт результатов в **CSV, PNG, TXT, HTML**
- Режим **Defence Mode**

---

## 🛠️ Технологический стек

| Компонент        | Версия       |
|-----------------|--------------|
| Java            | **17 LTS**   |
| JavaFX          | **17.0.10**  |
| Maven           | **3.8+**     |
| commons-math3   | **3.6.1**    |
| Целевые ОС      | Windows, Linux, RED OS |

---

## 🚀 Быстрый старт

### Предварительные требования

- **JDK 17** (рекомендуется [Microsoft Build of OpenJDK 17](https://learn.microsoft.com/ru-ru/java/openjdk/download) или [Eclipse Temurin 17](https://adoptium.net/))
- **Maven 3.8+**
- **IntelliJ IDEA** (рекомендуется)

### Запуск через Maven

```bash
mvn javafx:run
```

> ⚠️ **Важно:** Maven должен запускаться под **JDK 17**. Проверьте: команда `mvn -version` должна показывать `Java version: 17`.

### Запуск из IntelliJ IDEA

Запускайте класс:

```
ru.vkr.ldpcapp.Launcher
```

> Это рекомендуемый способ запуска из IDE — он обходит проблемы с `--module-path` и отсутствием JavaFX-runtime в classpath.

---

## 📦 Сборка и упаковка

### Сборка JAR

```bash
mvn package
```

### Упаковка desktop-приложения через `jpackage`

#### Windows

```cmd
packaging\package-windows.bat
```

#### Linux / RED OS

```bash
chmod +x packaging/package-linux.sh
./packaging/package-linux.sh
```

### Результат упаковки

По умолчанию создаётся **app-image** — готовый к запуску каталог приложения без установщика (идеально для демонстрации на защите).

При необходимости можно собрать установщик:
- Windows: `.exe`, `.msi`
- Linux / RED OS: `.deb`, `.rpm`

Подробнее — в [`packaging/README.md`](packaging/README.md).

---

## 🏗️ Архитектура приложения

```
src/main/java/ru/vkr/ldpcapp/
│
├── MainApplication.java              # Точка входа JavaFX
├── Launcher.java                     # Безопасный запуск из IDE
│
├── controller/                       # MVC-контроллеры экранов
│   ├── MainController.java           # Навигация между экранами
│   ├── SimulationController.java     # Настройка параметров и запуск
│   ├── ResultsController.java        # Графики, таблицы, экспорт
│   ├── CompareController.java        # A/B-сравнение двух сценариев
│   └── BatchController.java          # Пакетный анализ нескольких сценариев
│
├── model/                            # Модели данных
│   ├── SimulationConfig.java         # Конфигурация эксперимента
│   ├── ResultPoint.java              # Точка BER/BLER(SNR)
│   ├── ExperimentSummary.java        # Summary и энергетический выигрыш
│   └── BatchScenarioResult.java      # Результат одного batch-сценария
│
└── service/                          # Бизнес-логика и сервисы
    ├── phy/                          # Физический уровень (PHY)
    │   ├── codec/
    │   │   ├── CodecEngine.java      # Диспетчер кодеков (LDPC-профили)
    │   │   └── ldpc/
    │   │       └── LdpcCodec.java    # LDPC кодер/декодер (belief propagation)
    │   ├── channel/
    │   │   └── ChannelEngine.java    # Модели канала: AWGN, Rayleigh fading
    │   ├── modulation/
    │   │   └── ModulationEngine.java # BPSK, QPSK, 16-QAM, 64-QAM, 256-QAM
    │   ├── runner/
    │   │   └── ExperimentRunner.java # Запуск полного цикла эксперимента
    │   ├── transport/
    │   │   └── BitTransport.java     # Транспортный блок (TB) и CRC
    │   ├── metrics/
    │   │   └── PhyMetricsEngine.java # BER, BLER, throughput, spectral efficiency
    │   ├── stats/
    │   │   └── StatsMath.java        # Вспомогательная статистика
    │   ├── math/
    │   │   └── Complex.java          # Комплексные числа для OFDM/Rayleigh
    │   └── NrBaseGraphLoader.java    # Загрузчик 5G NR base graph (NR_1_0_8.txt)/(NR_1_0_16.txt)/(NR_1_0_32.txt)/(NR_2_0_8.txt)/(NR_2_0_16.txt)/(NR_2_0_32.txt)
    │
    ├── config/                       # Конфигурационные утилиты
    │   ├── SimulationConfigFactory.java    # Фабрика конфигураций и профилей
    │   ├── SimulationConfigProfiles.java   # Библиотека профилей для защиты
    │   ├── SimulationConfigFormatter.java  # Форматирование конфигурации в текст
    │   └── SimulationConfigValidator.java  # Валидация параметров эксперимента
    │
    ├── view/
    │   └── ViewManagerService.java         # Управление переходами между экранами
    │
    ├── SimulationService.java        # Оркестратор моделирования (JavaFX Task)
    ├── ExperimentSession.java        # Хранение результатов текущей сессии
    ├── CompareSession.java           # A/B-сессия для сравнения
    ├── BatchService.java             # Пакетный запуск нескольких сценариев
    ├── BatchSession.java             # Хранение результатов batch-режима
    ├── ExportService.java            # Централизованный экспорт (CSV, PNG, TXT, HTML)
    ├── ReportService.java            # Автогенерация текстового отчёта
    ├── BatchReportService.java       # Сравнительный batch-отчёт и CSV-сводка
    ├── BatchFileService.java         # Сохранение/загрузка batch-проектов на диск
    ├── PresentationSummaryService.java    # Экспорт для защиты (Defence Mode)
    ├── RichDocumentService.java      # DOCX/HTML-friendly отчёты
    ├── CsvWriter.java                # Запись CSV-файлов
    ├── TextReportWriter.java         # Запись TXT-отчётов
    ├── ChartExportService.java       # Экспорт графиков в PNG
    ├── ChartInteractionService.java  # Интерактивность графиков (zoom, tooltip)
    ├── ConfigFileService.java        # Сохранение/загрузка конфигурации
    ├── ParameterHelpService.java     # Встроенная справка по параметрам
    ├── BerTheoryService.java         # Теоретические кривые BER
    ├── IconFactory.java              # Генерация иконок для UI
    └── AppMetadata.java              # Метаданные приложения (версия, vendor)
```

---

## 🔬 Реализованный физический уровень

### Модели канала

| Модель | Описание |
|--------|----------|
| **AWGN** | Additive White Gaussian Noise — канал с аддитивным белым гауссовым шумом |
| **Rayleigh fading** | Замирания в мобильном канале, характерные для 5G NR |

### Схемы модуляции

| Схема       | Бит/символ | Описание                            |
|-------------|-----------|-------------------------------------|
| **BPSK**    | 1 | Binary Phase Shift Keying           |
| **QPSK**    | 2 | Quadrature Phase Shift Keying       |
| **16-QAM**  | 4 | 16-Quadrature Amplitude Modulation  |
| **64-QAM**  | 4 | 64-Quadrature Amplitude Modulation  |
| **256-QAM** | 4 | 256-Quadrature Amplitude Modulation |

### Волновые формы (Waveform)

| Waveform | Описание |
|----------|----------|
| **Single-carrier** | Одиночная несущая |
| **OFDM-64** | OFDM с 64 поднесущими |
| **OFDM-128** | OFDM с 128 поднесущими |

### Пространственные режимы (MIMO)

| Режим | Описание |
|-------|----------|
| **SISO** | Single Input Single Output |
| **2×2 Diversity** | Пространственное разнесение (MIMO) |

### Алгоритм декодирования

- **Normalized Min-Sum** — итеративное BP-декодирование (Belief Propagation)

---

## 📊 Рассчитываемые метрики

| Метрика | Обозначение | Описание |
|---------|-------------|----------|
| Bit Error Rate | **BER** | Вероятность ошибки на бит |
| Block Error Rate | **BLER** | Вероятность ошибки на блок |
| Средние итерации | — | Среднее количество итераций декодера |
| Сходимость | — | Доля сходящихся блоков |
| Effective Throughput | — | Эффективная пропускная способность |
| Spectral Efficiency | — | Спектральная эффективность (бит/с/Гц) |
| Required SNR | — | Требуемое Eb/N₀ при BER = 10⁻³ и BLER = 10⁻¹ |
| Coding Gain | — | Энергетический выигрыш относительно некодированной передачи |

---

## 🖥️ Экраны приложения

| Экран | Назначение |
|-------|-----------|
| **Главная** | Обзор приложения, архитектура системы, рекомендуемые сценарии для защиты |
| **Моделирование** | Настройка параметров эксперимента, выбор профиля, запуск расчёта |
| **Результаты** | BER/BLER-графики, KPI-карточки, summary-таблица, Defence Mode |
| **Сравнение** | A/B-сравнение двух сценариев по BER, BLER, throughput, spectral efficiency |
| **Пакетный анализ** | Batch-сравнение нескольких сценариев, выбор победителя, сводные графики |

---

## 💾 Возможности экспорта

| Формат | Описание |
|--------|----------|
| **CSV** | Таблица результатов BER/BLER по точкам SNR |
| **PNG** | Графики BER и BLER |
| **TXT** | Текстовый отчёт эксперимента |
| **HTML** | HTML-отчёт для печати и PDF-конвертации |
| **DOCX-friendly** | Word-совместимый формат отчёта |
| **Batch TXT/CSV** | Сводный отчёт по всем batch-сценариям |
| **Presentation Summary** | Материалы для защиты ВКР (Defence Mode) |
| **Полный пакет** | Комплект всех материалов одним действием |

---

## 🎯 Рекомендуемые сценарии для защиты ВКР

Готовые профили доступны прямо в интерфейсе вкладки **«Моделирование»**.

| № | Сценарий | Модуляция | Канал | LDPC-профиль |
|---|----------|-----------|-------|-------------|
| 1 | Базовый AWGN | BPSK | AWGN | Учебный (24,12) |
| 2 | QAM в Rayleigh | 16-QAM | Rayleigh | QC-inspired (96,48) |
| 3 | 5G NR BG1 OFDM | QPSK | AWGN | 5G NR BG1 (Z=8) |
| 4 | MIMO Diversity | QPSK | Rayleigh | 5G NR BG1 (Z=8), 2×2 |
| 5 | A/B Compare | Любые два | — | — |
| 6 | Batch Analysis | Все профили | Все каналы | Все LDPC |

---

## 🔧 Решение типичных проблем

### `UnsupportedClassVersionError: class file version 61.0`

Maven запущен под Java 8, хотя проект скомпилирован под Java 17.

**Решение:** В IntelliJ IDEA → `Settings → Build Tools → Maven → JDK for importer` → выбрать **JDK 17**.

Или в терминале (Windows):
```cmd
set JAVA_HOME=C:\Users\<имя_пользователя>\.jdks\ms-17.0.15
set PATH=%JAVA_HOME%\bin;%PATH%
mvn javafx:run
```

---

### `JavaFX runtime components are missing`

Попытка запуска через `MainApplication` напрямую.

**Решение:** Используйте `ru.vkr.ldpcapp.Launcher` вместо `MainApplication`.

---

### `Unrecognized option: --module-path`

IDE передаёт модульные параметры в старый JVM.

**Решение:** Убедитесь, что Maven Runner в IDEA использует JDK 17 (`Settings → Build Tools → Maven → Runner`).

---

## 📁 Структура репозитория

```
VKR_2026/
├── src/
│   └── main/
│       ├── java/ru/vkr/ldpcapp/        # Java-исходники (см. архитектуру выше)
│       └── resources/ru/vkr/ldpcapp/
│           ├── view/                   # FXML-экраны
│           │   ├── MainLayout.fxml
│           │   ├── DashboardView.fxml
│           │   ├── SimulationView.fxml
│           │   ├── ResultsView.fxml
│           │   ├── CompareView.fxml
│           │   └── BatchView.fxml
│           ├── styles/
│           │   └── app.css             # JavaFX-стили (Soft UI / Neumorphism)
│           └── NR_1_0_8.txt            # 5G NR BG1 base graph shifts (3GPP)
├── packaging/
│   ├── README.md                       # Инструкция по упаковке
│   ├── package-windows.bat             # Сценарий упаковки для Windows
│   ├── package-linux.sh                # Сценарий упаковки для Linux / RED OS
│   └── assets/                         # Иконки приложения (для jpackage)
├── pom.xml                             # Maven-конфигурация (groupId: ru.vkr)
└── README.md                           # Этот файл
```

---

## 🌍 Мультиплатформенность

Приложение проектировалось как кроссплатформенное desktop-решение. Благодаря стеку **Java 17 + JavaFX + Maven** оно собирается и запускается на:

| ОС | Статус |
|----|--------|
| ✅ **Windows** | Основная среда разработки |
| ✅ **Linux** | Ubuntu, Fedora и совместимые |
| ✅ **RED OS** | Linux-совместимая ОС при наличии JDK 17 / JavaFX runtime |

> Для каждой ОС рекомендуется собирать отдельный дистрибутив через `jpackage`.

---

## 👤 Автор

|  |  |
|--|--|
| **Проект** | Выпускная квалификационная работа, 2026 |
| **Тема** | Исследование и моделирование системы помехоустойчивого кодирования в зашумлённом канале мобильной сети 5G |
| **Приложение** | LDPC Research Studio v1.0.0 |
| **Репозиторий** | [github.com/ilyxa20046/VKR_2026](https://github.com/ilyxa20046/VKR_2026) |

---

<div align="center">

*LDPC Research Studio — исследовательское приложение для ВКР по теме помехоустойчивого кодирования в 5G NR*

</div>
