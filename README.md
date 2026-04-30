# LDPC Research Studio

> **Выпускная квалификационная работа (ВКР), 2026**
> Тема: «Исследование и моделирование системы помехоустойчивого кодирования в зашумлённом канале мобильной сети 5G»

---

## О проекте

**LDPC Research Studio** — это мультиплатформенное desktop-приложение на базе Java 17 + JavaFX, разработанное в рамках ВКР для исследования и моделирования системы помехоустойчивого LDPC-кодирования в зашумлённом канале мобильной сети 5G.

Приложение реализует полный стек физического уровня: от генерации информационных бит и LDPC-кодирования до передачи через канал с шумом (AWGN / Rayleigh fading), модуляции (BPSK / QPSK / 16-QAM), OFDM-форм сигнала и MIMO-режимов — с расчётом BER, BLER, throughput, spectral efficiency и энергетического выигрыша.

---

## Технологический стек

| Компонент        | Версия      |
|-----------------|-------------|
| Java            | 17 LTS      |
| JavaFX          | 17.0.10     |
| Maven           | 3.8+        |
| commons-math3   | 3.6.1       |
| Целевые ОС      | Windows, Linux, RED OS |

---

## Архитектура приложения

```
src/main/java/ru/vkr/ldpcapp/
│
├── MainApplication.java           # Точка входа JavaFX
├── Launcher.java                  # Безопасный запуск из IDE
│
├── controller/                    # MVC-контроллеры экранов
│   ├── MainController.java        # Навигация по экранам
│   ├── SimulationController.java  # Управление параметрами и запуском
│   ├── ResultsController.java     # Графики, таблицы, summary, экспорт
│   ├── CompareController.java     # A/B-сравнение двух сценариев
│   └── BatchController.java       # Пакетный анализ нескольких сценариев
│
├── model/                         # Модели данных
│   ├── SimulationConfig.java      # Конфигурация эксперимента
│   ├── ResultPoint.java           # Точка BER/BLER(SNR)
│   ├── ExperimentSummary.java     # Summary и энергетический выигрыш
│   └── BatchScenarioResult.java   # Результат одного batch-сценария
│
└── service/                       # Бизнес-логика и сервисы
    │
    ├── phy/                       # Физический уровень (PHY)
    │   ├── codec/
    │   │   ├── CodecEngine.java           # Диспетчер кодеков (LDPC-профили)
    │   │   └── ldpc/
    │   │       └── LdpcCodec.java         # LDPC кодер/декодер (belief propagation)
    │   ├── channel/
    │   │   └── ChannelEngine.java         # Модели канала: AWGN, Rayleigh fading
    │   ├── modulation/
    │   │   └── ModulationEngine.java      # BPSK, QPSK, 16-QAM
    │   ├── runner/
    │   │   └── ExperimentRunner.java      # Запуск полного цикла эксперимента
    │   ├── transport/
    │   │   └── BitTransport.java          # Транспортный блок (TB) и CRC
    │   ├── metrics/
    │   │   └── PhyMetricsEngine.java      # BER, BLER, throughput, spectral efficiency
    │   ├── stats/
    │   │   └── StatsMath.java             # Вспомогательная статистика
    │   ├── math/
    │   │   └── Complex.java               # Комплексные числа для OFDM/Rayleigh
    │   └── NrBaseGraphLoader.java         # Загрузчик 5G NR base graph (NR_1_0_8.txt)
    │
    ├── config/                    # Конфигурационные утилиты
    │   ├── SimulationConfigFactory.java   # Фабрика конфигураций и готовых профилей
    │   ├── SimulationConfigProfiles.java  # Библиотека профилей для защиты
    │   ├── SimulationConfigFormatter.java # Форматирование конфигурации в текст
    │   └── SimulationConfigValidator.java # Валидация параметров эксперимента
    │
    ├── view/
    │   └── ViewManagerService.java        # Управление переходами между экранами
    │
    ├── SimulationService.java     # Оркестратор моделирования (JavaFX Task)
    ├── ExperimentSession.java     # Хранение результатов текущей сессии
    ├── CompareSession.java        # A/B-сессия для сравнения
    ├── BatchService.java          # Пакетный запуск нескольких сценариев
    ├── BatchSession.java          # Хранение результатов batch-режима
    ├── ExportService.java         # Централизованный экспорт (CSV, PNG, TXT, HTML)
    ├── ReportService.java         # Автогенерация текстового отчёта
    ├── BatchReportService.java    # Сравнительный batch-отчёт и CSV-сводка
    ├── BatchFileService.java      # Сохранение/загрузка batch-проектов на диск
    ├── ChapterThreeMaterialsService.java  # Генерация материалов для Главы 3 ВКР
    ├── PresentationSummaryService.java    # Экспорт для защиты (defense mode)
    ├── RichDocumentService.java   # DOCX/HTML-friendly отчёты
    ├── CsvWriter.java             # Запись CSV-файлов
    ├── TextReportWriter.java      # Запись TXT-отчётов
    ├── ChartExportService.java    # Экспорт графиков в PNG
    ├── ChartInteractionService.java  # Интерактивность графиков (zoom, tooltip)
    ├── ConfigFileService.java     # Сохранение/загрузка конфигурации эксперимента
    ├── ParameterHelpService.java  # Встроенная справка по параметрам
    ├── BerTheoryService.java      # Теоретические кривые BER
    ├── IconFactory.java           # Генерация иконок для UI
    └── AppMetadata.java           # Метаданные приложения (версия, vendor, стек)
```

---

## Что реализовано

### Модели канала
- **AWGN** (Additive White Gaussian Noise)
- **Rayleigh fading** (замирания в мобильном канале)

### Схемы модуляции
- **BPSK** — 1 бит/символ
- **QPSK** — 2 бит/символ
- **16-QAM** — 4 бит/символ

### Типы волновых форм (waveform)
- **Single-carrier** — одиночная несущая
- **OFDM-64** — OFDM с 64 поднесущими
- **OFDM-128** — OFDM с 128 поднесущими

### Пространственные режимы (spatial mode)
- **SISO** — Single Input Single Output
- **2×2 Diversity** — пространственное разнесение

### LDPC-профили
| Профиль | Параметры | Описание |
|---------|-----------|----------|
| Учебный LDPC | (24, 12), R=1/2 | Базовая учебная модель |
| QC-inspired / 5G-like | (96, 48), R=1/2 | Приближение к 5G NR |
| 5G NR BG1 | Z=8, из таблицы 3GPP | Base Graph 1 (NR_1_0_8.txt) |

### Алгоритм декодирования
- **Normalized Min-Sum** (итеративное BP-декодирование)

### Рассчитываемые метрики
| Метрика | Описание |
|---------|----------|
| BER | Bit Error Rate — вероятность ошибки на бит |
| BLER | Block Error Rate — вероятность ошибки на блок |
| Средние итерации | Среднее количество итераций декодера |
| Сходимость | Доля сходящихся блоков |
| Effective Throughput | Эффективная пропускная способность |
| Spectral Efficiency | Спектральная эффективность (бит/с/Гц) |
| Required SNR | Требуемое Eb/N0 при BER = 10⁻³ и BLER = 10⁻¹ |
| Coding Gain | Энергетический выигрыш относительно некодированной передачи |

---

## Экраны приложения

| Экран | Назначение |
|-------|-----------|
| **Главная** | Обзор приложения, назначение, архитектура системы, роль в ВКР, рекомендуемые сценарии для защиты |
| **Моделирование** | Настройка параметров эксперимента, выбор профиля, запуск расчёта |
| **Результаты** | BER/BLER-графики, KPI-карточки, summary-таблица, Defence Mode |
| **Сравнение** | A/B-сравнение двух сценариев по BER, BLER, throughput, spectral efficiency |
| **Пакетный анализ** | Batch-сравнение нескольких сценариев, выбор победителя, общие графики |

---

## Возможности экспорта

| Формат | Описание |
|--------|----------|
| **CSV** | Таблица результатов BER/BLER по точкам SNR |
| **PNG** | Графики BER и BLER |
| **TXT** | Текстовый отчёт эксперимента |
| **HTML** | HTML-отчёт для печати, PDF-конвертации и вставки в диплом |
| **DOCX-friendly** | Word-совместимый формат отчёта |
| **Batch TXT/CSV** | Сводный отчёт и CSV по всем batch-сценариям |
| **Presentation Summary** | Материалы для защиты ВКР (Defence Mode) |
| **Глава 3** | Материалы для третьей главы ВКР |
| **Полный пакет** | Комплект всех материалов эксперимента одним действием |

---

## Быстрый старт

### Предварительные требования

- **JDK 17** (рекомендуется Microsoft Build of OpenJDK 17 или Eclipse Temurin 17)
- **Maven 3.8+**
- IntelliJ IDEA (рекомендуется)

### Запуск через Maven

```bash
cd javafx-app
mvn javafx:run
```

> ⚠️ **Важно:** Maven должен запускаться под JDK 17. Проверьте: `mvn -version` должен показывать `Java version: 17`.

### Запуск из IntelliJ IDEA

Запускайте класс:

```
ru.vkr.ldpcapp.Launcher
```

Это рекомендуемый способ запуска из IDE — он обходит проблемы с `--module-path` и отсутствием JavaFX-runtime на classpath.

---

## Типичные ошибки и их решение

### `UnsupportedClassVersionError: class file version 61.0 ... recognizes up to 52.0`

Maven запущен под Java 8, хотя проект скомпилирован под Java 17.

**Решение:** В IntelliJ IDEA → `Settings → Build Tools → Maven → JDK for importer` → выбрать JDK 17.

Или в терминале (Windows):
```cmd
set JAVA_HOME=C:\Users\<user>\.jdks\ms-17.0.15
set PATH=%JAVA_HOME%\bin;%PATH%
mvn javafx:run
```

### `JavaFX runtime components are missing`

При запуске через `MainApplication` напрямую.

**Решение:** Используйте `ru.vkr.ldpcapp.Launcher` вместо `MainApplication`.

### `Unrecognized option: --module-path`

IDE передаёт модульные параметры в старый JVM.

**Решение:** Убедитесь, что Maven Runner в IDEA использует JDK 17 (`Settings → Build Tools → Maven → Runner`).

---

## Сборка и упаковка

### Сборка JAR

```bash
cd javafx-app
mvn package
```

### Упаковка desktop-приложения через jpackage

#### Windows
```cmd
cd javafx-app
packaging\package-windows.bat
```

#### Linux / RED OS
```bash
cd javafx-app
chmod +x packaging/package-linux.sh
./packaging/package-linux.sh
```

### Результат упаковки

По умолчанию создаётся `app-image` — каталог с готовым к запуску приложением без установщика. Подходит для демонстрации на защите.

При необходимости можно собрать установщик:
- Windows: `.exe`, `.msi`
- Linux / RED OS: `.deb`, `.rpm`

Подробнее — в [`packaging/README.md`](packaging/README.md).

---

## Мультиплатформенность

Приложение проектировалось как кроссплатформенное desktop-решение. Благодаря стеку **Java 17 + JavaFX + Maven** оно собирается и запускается на:

- ✅ **Windows** (основная среда разработки)
- ✅ **Linux** (Ubuntu, Fedora и совместимые)
- ✅ **RED OS** (как Linux-совместимая ОС при наличии JDK 17 / JavaFX runtime)

Для каждой ОС рекомендуется собирать отдельный дистрибутив через `jpackage`.

---

## Структура файлов проекта

```
VKR_2026/
├── src/
│   └── main/
│       ├── java/ru/vkr/ldpcapp/       # Java-исходники (см. архитектуру выше)
│       └── resources/ru/vkr/ldpcapp/
│           ├── view/                   # FXML-экраны
│           │   ├── MainLayout.fxml
│           │   ├── DashboardView.fxml
│           │   ├── SimulationView.fxml
│           │   ├── ResultsView.fxml
│           │   ├── CompareView.fxml
│           │   └── BatchView.fxml
│           ├── styles/
│           │   └── app.css             # JavaFX-стили (soft UI / neumorphism)
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

## Рекомендуемые сценарии для защиты ВКР

| № | Название сценария | Модуляция | Канал | LDPC-профиль |
|---|-----------------|-----------|-------|-------------|
| 1 | Базовый AWGN | BPSK | AWGN | Учебный (24,12) |
| 2 | QAM в Rayleigh | 16-QAM | Rayleigh | QC-inspired (96,48) |
| 3 | 5G NR BG1 OFDM | QPSK | AWGN | 5G NR BG1 (Z=8) |
| 4 | MIMO Diversity | QPSK | Rayleigh | 5G NR BG1 (Z=8), 2×2 |
| 5 | A/B Compare | Любые два | — | — |
| 6 | Batch Analysis | Все профили | Все каналы | Все LDPC |

Готовые профили доступны прямо в интерфейсе вкладки **«Моделирование»**.

---

## Автор

| | |
|---|---|
| **Проект** | Выпускная квалификационная работа, 2026 |
| **Тема** | Исследование и моделирование системы помехоустойчивого кодирования в зашумлённом канале мобильной сети 5G |
| **Приложение** | LDPC Research Studio v1.0.0 |
| **Репозиторий** | [github.com/ilyxa20046/VKR_2026](https://github.com/ilyxa20046/VKR_2026) |

---

*LDPC Research Studio — исследовательское приложение для ВКР по теме помехоустойчивого кодирования в 5G NR.*
