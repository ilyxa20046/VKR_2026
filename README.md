# LDPC Research JavaFX App

Стартовый JavaFX-проект для ВКР по теме исследования и моделирования помехоустойчивого кодирования в зашумлённом цифровом канале мобильной сети 5G.

## Рекомендуемый стек

- Java 17 LTS
- JavaFX 17
- Maven

## Мультиплатформенность

Да, приложение проектируется как мультиплатформенное desktop-решение.

Благодаря стеку **Java 17 + JavaFX + Maven** его можно собирать и запускать на:
- **Windows**;
- **Linux**;
- **RED OS** (как Linux-совместимой среде, при наличии JDK/JavaFX-runtime или собранного пакета);
- при необходимости и на других ОС, где доступен JavaFX.

То есть для ВКР это корректно называть **мультиплатформенным графическим приложением**.

Важно только честно формулировать в тексте диплома:
- логика приложения является кроссплатформенной;
- для каждой ОС может потребоваться своя упаковка/дистрибутив;
- финальную поставку лучше собирать отдельно под Windows и отдельно под Linux/RED OS через jpackage.

## Как запускать

### Рекомендуемый способ

```bash
cd javafx-app
mvn javafx:run
```

В `pom.xml` запуск через `javafx-maven-plugin` уже переведён на `Launcher` и `CLASSPATH`-режим.
Это сделано специально для сред, где модульный запуск JavaFX через `--module-path` может завершаться ошибкой вида:

> Unrecognized option: --module-path

Важно: `mvn javafx:run` будет работать только если сам Maven запускается под JDK 17.
Если Maven в вашей IDE использует другой runtime, временно запускайте приложение через `ru.vkr.ldpcapp.Launcher`.

### Важно: Maven тоже должен работать на Java 17

Если при `mvn javafx:run` появляется ошибка вида:

> UnsupportedClassVersionError ... class file version 61.0 ... recognizes up to 52.0

это означает, что **сам Maven запущен под Java 8**, даже если проект компилируется под Java 17.

Для запуска JavaFX-приложения через Maven нужно, чтобы и `mvn`, и IDE использовали **JDK 17**.

Что проверить:
- в IntelliJ IDEA: **Settings → Build, Execution, Deployment → Build Tools → Maven → JDK = 17**;
- в IntelliJ IDEA также полезно проверить **Settings → Build Tools → Maven → Runner**, чтобы Maven запускался именно на JDK 17;
- в терминале: `mvn -version` должен показывать **Java version: 17**;
- при необходимости указать `JAVA_HOME` на JDK 17 перед запуском Maven.

Если в строке запуска Maven в IDEA вы всё ещё видите старый runtime-стек (`org.codehaus.classworlds.Launcher` с Java 8-подобным поведением), выполните в IDEA:
- **Reload All Maven Projects**;
- затем заново откройте окно Maven;
- и проверьте, что Maven JDK действительно переключился на 17, а не на встроенный runtime IDE.

Пример для Windows CMD:

```bat
set JAVA_HOME=C:\Users\Sitjoi\.jdks\ms-17.0.15
set PATH=%JAVA_HOME%\bin;%PATH%
mvn javafx:run
```

Если не хочется менять Maven-окружение прямо сейчас, используйте запуск из IDE через:
- `ru.vkr.ldpcapp.Launcher`

Это полностью рабочий и поддерживаемый вариант запуска проекта.

### Запуск из IDE

Если при запуске `MainApplication` появляется ошибка:

> JavaFX runtime components are missing, and are required to run this application

используйте класс:

- `ru.vkr.ldpcapp.Launcher`

Именно его удобнее запускать кнопкой Run из IntelliJ IDEA.

## Что уже реализовано

- Maven-каркас JavaFX-приложения;
- обновлённый light soft-ui / neumorphism UI, доведённый до более близкого к референсу mobile-dashboard направления: floating cards, pill-кнопки, icon badges, segmented chips, pastel KPI-карточки, gradient hero-блоки и мягкие объёмные поверхности;
- branding приложения: продуктовое имя `LDPC Research Studio`, версия, vendor и единые метаданные для интерфейса и упаковки;
- окно «О программе» с данными о продукте, стеке, платформах и исследовательских возможностях;
- финально отполированный MainLayout: refined top-bar, облегчённая quickbar, более аккуратный sidebar, восстановленный стиль shell-кнопок и спокойная status-bar shell-части приложения;
- главное окно и FXML-навигация;
- `DashboardView`, `SimulationView`, `ResultsView`, `CompareView`, `BatchView`;
- на вкладке моделирования доступны готовые исследовательские профили для быстрого перехода между базовыми и расширенными сценариями;
- стартовая страница переписана и переосмыслена как экран «Главное»: теперь она не просто декоративный обзор, а понятное описание назначения приложения, исследовательских возможностей, режима работы пользователя, роли системы в ВКР и рекомендуемых сценариев для защиты;
- на главной странице добавлены содержательные блоки: что исследуется в работе, что делает приложение, архитектура системы, ключевые отличия от базовой учебной модели, практическая значимость для ВКР, формируемые метрики и графики, а также типовой сценарий исследования;
- экран моделирования с параметрами эксперимента, ускоренной прокруткой страницы, без лишнего верхнего helper-блока, с вынесенной наверх grouped action-bar, cleaner form-layout, логическим делением формы на секции (кодирование и модуляция / канал, OFDM и spatial mode / диапазон SNR и объём эксперимента), более выразительными тематизированными кнопками запуска, расширенным preview текущей конфигурации, переключением базовый/расширенный режим и отдельными готовыми сценариями для защиты;
- экран результатов с верхней минималистичной action-bar без лишних служебных подписей, компактным hero-блоком, более понятным заголовком итогов моделирования, компактной summary-таблицей параметров текущего сценария вместо chips, многоколоночной summary-таблицей ключевых параметров, цветовым выделением самых важных KPI по смысловым группам, отдельно стилизованной таблицей результатов моделирования, укрупнёнными графиками BER/BLER, scroll-friendly компоновкой, режимом демонстрации защиты и корректным текстовым отображением gain-метрик в случаях, когда ошибки в выборке не наблюдались;
- упрощённая верхняя shell-панель приложения: сохранено только брендирование, «Центр справки» и «О приложении», без лишних верхних быстрых действий и статусных меток;
- экраны Compare и «Пакетный анализ» с Defense Mode для наглядного показа победителя, peak throughput, spectral efficiency и required SNR, а также с приведением верхних параметров сценариев к тому же compact summary-table / matrix-style, что и на Results, без лишних служебных ярлыков и вторичных подпунктов; вкладки «Сравнение» и «Пакетный анализ» дополнительно переведены на compact action-меню по аналогии с Results, где действия сгруппированы в понятные списки по сценариям, документам и копированию, а wording и верхняя структура «Пакетного анализа» упрощены и приведены к стилю страницы «Результаты»;
- compare-вкладка доведена до рабочего A/B-сценария: на Results можно выбрать текущий запуск как сценарий A, затем выполнить второй запуск и сформировать сравнение A/B; compare-кнопки на Results сокращены и стилизованы в soft-ui, победитель Compare и лучший сценарий Batch дополнительно подсвечиваются визуально, а лучший batch-сценарий получает текстовую метку ★ внутри таблицы;
- scroll-friendly компоновка Compare и Batch: крупные графики вынесены вверх, а таблицы и narrative перенесены в раскрывающиеся секции и оформлены как отдельные data-table контейнеры;
- верхние зоны Compare и Batch дополнительно очищены: уменьшен визуальный шум hero-блоков и уплотнены action-панели;
- action-кнопки, helper-блоки и названия сервисных панелей на основных экранах приведены к единому более продуктово-ориентированному стилю;
- нижняя status-bar shell-части приложения дополнительно упрощена и оставляет только основной статус и краткую подпись продукта;
- стиль shell-кнопок верхней quickbar и левой навигации восстановлен через отдельные классы оформления и более жёсткое CSS-применение, а статусы и helper-тексты контроллеров дополнительно выровнены по единому product-style тону;
- декоративные текстовые иконки перед заголовками основных экранов и секций убраны, чтобы интерфейс выглядел чище и единообразнее;
- явное отображение modulation / channel / waveform / spatial mode / LDPC-контекста на аналитических экранах Results, Compare и Batch;
- `SimulationService` с расширенной исследовательской моделью:
  - BPSK;
  - QPSK;
  - 16-QAM;
  - AWGN;
  - Rayleigh fading;
  - Single-carrier;
  - OFDM-64;
  - OFDM-128;
  - SISO;
  - 2x2 Diversity;
  - учебный LDPC (24,12);
  - QC-inspired / 5G-like LDPC (96,48);
  - normalized min-sum;
  - BER, BLER, средние итерации и сходимость;
  - effective throughput;
  - spectral efficiency;
  - требуемый SNR по целевым BER = 10^-3 и BLER = 10^-1;
  - энергетический выигрыш по BER = 10^-3 и BLER = 10^-1;
- запуск расчёта в фоне через `Task`;
- сохранение результатов через `ExperimentSession`;
- экспорт CSV;
- экспорт графиков BER/BLER в PNG;
- экспорт TXT-отчёта;
- режим демонстрации защиты на экране результатов с крупными KPI-карточками для BER, BLER, throughput и required SNR;
- presentation-summary экспорт для защиты на экранах Results, Compare и Batch;
- DOCX-friendly / Word-friendly экспорт single-, compare- и batch-отчётов;
- HTML-friendly экспорт отчётов для быстрой печати, конвертации в PDF и вставки в диплом;
- пакетный экспорт полного комплекта материалов эксперимента;
- генерация материалов для главы 3;
- сохранение и загрузка конфигурации эксперимента;
- встроенная справка по параметрам;
- tooltip-подсказки у ключевых параметров моделирования;
- отдельный Help Center / окно «Справка» с расширенным описанием параметров, метрик и интерпретации результатов;
- batch-режим сравнения нескольких сценариев по модуляции, типу канала и LDPC-профилю;
- общие BER/BLER-графики и автоматический batch-вывод по нескольким сценариям сразу;
- сравнение batch-сценариев не только по BER/BLER и energy gain, но и по peak throughput, peak spectral efficiency и required SNR;
- экспорт batch-сравнения в единый TXT-отчёт, CSV-сводку и пакет материалов;
- сохранение и загрузка batch-экспериментов на диск в переносимом project-формате.

## Ключевые файлы

- `src/main/java/ru/vkr/ldpcapp/MainApplication.java` — точка входа JavaFX;
- `src/main/java/ru/vkr/ldpcapp/Launcher.java` — безопасный запуск из IDE;
- `src/main/java/ru/vkr/ldpcapp/controller/MainController.java` — навигация по экранам;
- `src/main/java/ru/vkr/ldpcapp/controller/SimulationController.java` — управление параметрами исследовательской модели;
- `src/main/java/ru/vkr/ldpcapp/controller/ResultsController.java` — графики, таблицы, summary и экспорт;
- `src/main/java/ru/vkr/ldpcapp/controller/CompareController.java` — сравнение двух сценариев по BER/BLER, throughput, spectral efficiency и required SNR;
- `src/main/java/ru/vkr/ldpcapp/controller/BatchController.java` — batch-сравнение нескольких сценариев;
- `src/main/java/ru/vkr/ldpcapp/model/SimulationConfig.java` — конфигурация эксперимента;
- `src/main/java/ru/vkr/ldpcapp/model/ResultPoint.java` — точка BER/BLER(SNR);
- `src/main/java/ru/vkr/ldpcapp/model/ExperimentSummary.java` — summary и энергетический выигрыш;
- `src/main/java/ru/vkr/ldpcapp/service/SimulationService.java` — ядро моделирования;
- `src/main/java/ru/vkr/ldpcapp/service/ExportService.java` — централизованный экспорт материалов;
- `src/main/java/ru/vkr/ldpcapp/service/ReportService.java` — автогенерация отчёта;
- `src/main/java/ru/vkr/ldpcapp/service/ChapterThreeMaterialsService.java` — материалы для главы 3;
- `src/main/java/ru/vkr/ldpcapp/service/ParameterHelpService.java` — встроенная справка;
- `src/main/java/ru/vkr/ldpcapp/service/BatchService.java` — пакетный расчёт нескольких сценариев;
- `src/main/java/ru/vkr/ldpcapp/service/BatchSession.java` — хранение результатов и базовой конфигурации batch-режима;
- `src/main/java/ru/vkr/ldpcapp/service/BatchReportService.java` — формирование сравнительного batch-отчёта и CSV-сводки;
- `src/main/java/ru/vkr/ldpcapp/service/BatchFileService.java` — сохранение и загрузка batch-проектов на диск;
- `src/main/resources/ru/vkr/ldpcapp/view/*.fxml` — FXML-экраны;
- `src/main/resources/ru/vkr/ldpcapp/styles/app.css` — JavaFX-стили.

## Упаковка desktop-приложения через jpackage

В проект уже добавлен каталог `packaging/` с базовыми сценариями упаковки.

### Быстрый путь

#### Windows

```bash
cd javafx-app
packaging\package-windows.bat
```

#### Linux / RED OS

```bash
cd javafx-app
chmod +x packaging/package-linux.sh
./packaging/package-linux.sh
```

### Что получится

По умолчанию создаётся `app-image` — удобный стартовый формат для демонстрации на защите и для ручного переноса на целевую ОС.

При необходимости дальше можно собрать:
- Windows: `exe`, `msi`;
- Linux / RED OS: `deb`, `rpm`.

### Где смотреть

- `packaging/README.md` — подробная инструкция;
- `packaging/package-windows.bat` — сценарий для Windows;
- `packaging/package-linux.sh` — сценарий для Linux / RED OS;
- `packaging/assets/` — будущие иконки приложения.

Текущее branding-имя приложения уже зафиксировано как **LDPC Research Studio**, поэтому следующий шаг для финальной поставки — просто добавить реальные иконки и, при необходимости, platform-specific packaging assets.

## Что логично делать дальше

- накопление и повторное открытие batch-пакетов сравнения;
- прямой PDF-экспорт через печать/рендеринг HTML-отчётов и оформление platform-specific шаблонов;
- автоматическое сравнение нескольких batch-запусков между собой;
- более глубокое приближение к 5G NR LDPC/transport block workflow;
- переход от OFDM/MIMO-like модели к более детальному OFDM- и diversity-анализу;
- финальная branding-упаковка под Windows и Linux / RED OS с иконками и версионированием.
