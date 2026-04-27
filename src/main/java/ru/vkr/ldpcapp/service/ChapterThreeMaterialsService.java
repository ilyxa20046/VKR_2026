package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.ExperimentSummary;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.config.SimulationConfigFactory;
import ru.vkr.ldpcapp.service.config.SimulationConfigProfiles;

import java.util.List;
import java.util.Locale;

public class ChapterThreeMaterialsService {

    private final ReportService reportService;
    private final SimulationConfigProfiles configProfiles = new SimulationConfigProfiles();

    public ChapterThreeMaterialsService() {
        this(new ReportService());
    }

    public ChapterThreeMaterialsService(ReportService reportService) {
        this.reportService = reportService;
    }

    public String buildChapterThreeMaterials(SimulationConfig config, List<ResultPoint> points) {
        if (points == null || points.isEmpty()) {
            return "Материалы для главы 3 не могут быть сформированы, так как результаты моделирования отсутствуют.";
        }

        SimulationConfig safeConfig = config == null ? configProfiles.recommendedProfile() : config;
        ExperimentSummary summary = ExperimentSummary.from(points);

        return String.join(System.lineSeparator(),
                "МАТЕРИАЛЫ ДЛЯ ГЛАВЫ 3",
                "Исследование и анализ результатов моделирования системы передачи данных с LDPC-кодированием в расширенной научной модели",
                "",
                "3.1 Параметры вычислительного эксперимента",
                buildParametersBlock(safeConfig),
                "",
                "3.2 Методика проведения эксперимента",
                buildMethodologyBlock(safeConfig),
                "",
                "3.3 Таблица результатов моделирования",
                buildTableBlock(points),
                "",
                "3.4 Анализ полученных зависимостей BER(SNR) и BLER(SNR)",
                reportService.buildDetailedReport(safeConfig, points),
                "",
                "3.5 Основные выводы по результатам эксперимента",
                buildConclusionsBlock(summary)
        );
    }

    private String buildParametersBlock(SimulationConfig config) {
        return String.join(System.lineSeparator(),
                "В ходе вычислительного эксперимента использовалась расширенная имитационная модель цифрового канала связи, позволяющая учитывать тип модуляции, модель канала, выбранный LDPC-профиль и дополнительные исследовательские метрики производительности.",
                "Исследование проводилось для сценария: " + config.getModulation()
                        + ", канал " + config.getChannelModel()
                        + ", waveform " + config.getWaveform()
                        + ", spatial mode " + config.getSpatialMode()
                        + ", cyclic prefix " + config.getCyclicPrefix()
                        + ", equalizer " + config.getEqualizerMode()
                        + ", профиль " + SimulationConfigFactory.getProfileDisplayName(config.getLdpcProfile(), config.getLiftingSize()) + ".",
                "Параметры эксперимента имели следующие значения:",
                "- длина информационного блока: " + config.getInfoBlockLength() + " бит;",
                "- диапазон SNR: " + formatFixed(Math.min(config.getSnrStart(), config.getSnrEnd())) + " ... "
                        + formatFixed(Math.max(config.getSnrStart(), config.getSnrEnd())) + " дБ;",
                "- шаг по SNR: " + formatFixed(config.getSnrStep()) + " дБ;",
                "- количество блоков на точку SNR: " + config.getBlocks() + ";",
                "- максимальное число итераций декодера: " + config.getMaxIterations() + ";",
                "- коэффициент normalized min-sum: " + formatFixed(config.getNormalization()) + ";",
                "- cyclic prefix: " + config.getCyclicPrefix() + ";",
                "- equalizer: " + config.getEqualizerMode() + ";",
                "- seed генератора псевдослучайных чисел: " + config.getSeed() + "."
        );
    }

    private String buildMethodologyBlock(SimulationConfig config) {
        return String.join(System.lineSeparator(),
                "Моделирование выполнялось по множеству точек отношения сигнал/шум в заданном диапазоне. Для каждой точки SNR генерировалась последовательность информационных блоков, которые затем передавались по двум сценариям: без кодирования и с применением LDPC-кода.",
                "В кодированном режиме выполнялось LDPC-кодирование, выбранная модуляция (" + config.getModulation()
                        + "), передача по каналу " + config.getChannelModel()
                        + ", использование waveform " + config.getWaveform()
                        + ", cyclic prefix " + config.getCyclicPrefix()
                        + ", equalizer " + config.getEqualizerMode()
                        + " и spatial mode " + config.getSpatialMode()
                        + ", а также итерационное soft-decision декодирование методом normalized min-sum.",
                "После этого рассчитывались показатели BER и BLER, среднее число итераций декодирования, доля успешной сходимости декодера, effective throughput, спектральная эффективность, требуемый SNR по целевым порогам BER = 10^-3 и BLER = 10^-1, а также энергетический выигрыш кодирования.",
                "Число исследованных точек SNR составило " + SimulationConfigFactory.getSnrPointCount(config) + ", а суммарное количество обработанных блоков — " + SimulationConfigFactory.getExperimentBlockCount(config) + ".",
                "Общий оценочный объём переданной информационной последовательности составил " + SimulationConfigFactory.getEstimatedInformationBits(config) + " бит."
        );
    }

    private String buildTableBlock(List<ResultPoint> points) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.US,
                "%6s | %12s | %10s | %13s | %11s | %9s | %11s | %12s | %12s%n",
                "SNR", "BER uncoded", "BER LDPC", "BLER uncoded", "BLER LDPC", "Iter.", "Success", "Throughput", "SpecEff"
        ));
        builder.append("-".repeat(124)).append(System.lineSeparator());

        for (ResultPoint point : points) {
            builder.append(String.format(Locale.US,
                    "%6.2f | %12s | %10s | %13s | %11s | %9.2f | %10.2f%% | %11.2f | %12.2f%n",
                    point.getSnr(),
                    formatMetric(point.getBerUncoded()),
                    formatMetric(point.getBerLdpc()),
                    formatMetric(point.getBlerUncoded()),
                    formatMetric(point.getBlerLdpc()),
                    point.getAverageIterations(),
                    point.getSuccessRatio() * 100.0,
                    point.getEffectiveThroughputMbps(),
                    point.getSpectralEfficiency()
            ));
        }

        return builder.toString().trim();
    }

    private String buildConclusionsBlock(ExperimentSummary summary) {
        return String.join(System.lineSeparator(),
                "1. Применение LDPC-кодирования обеспечивает уменьшение BER и BLER по сравнению с передачей без кодирования даже в расширенной исследовательской модели канала.",
                "2. Наибольший выигрыш по BER составил " + formatGain(summary.getBestBerGain()) + ", а по BLER — " + formatGain(summary.getBestBlerGain()) + ".",
                "3. Средний effective throughput равен " + formatFixed(summary.getAverageThroughputMbps()) + " Мбит/с, а пиковое значение достигает " + formatFixed(summary.getPeakThroughputMbps()) + " Мбит/с.",
                "4. Средняя спектральная эффективность составляет " + formatFixed(summary.getAverageSpectralEfficiency()) + " бит/с/Гц, а максимальная — " + formatFixed(summary.getPeakSpectralEfficiency()) + " бит/с/Гц.",
                "5. Требуемый SNR по порогу BER = 10^-3 равен " + formatDb(summary.getRequiredSnrBerDb()) + ", а по порогу BLER = 10^-1 — " + formatDb(summary.getRequiredSnrBlerDb()) + ".",
                "6. Энергетический выигрыш по порогу BER = 10^-3 равен " + formatEnergy(summary.getBerEnergyGainDb()) + ", а по порогу BLER = 10^-1 — " + formatEnergy(summary.getBlerEnergyGainDb()) + ".",
                "7. Полученные результаты подтверждают, что дополнение модели метриками throughput, spectral efficiency и required SNR существенно повышает её инженерную и исследовательскую ценность для задач мобильных систем связи 5G."
        );
    }

    private String formatMetric(double value) {
        if (value == 0.0) {
            return "0";
        }
        if (value >= 0.1) {
            return formatFixed(value, 3);
        }
        if (value >= 0.001) {
            return formatFixed(value, 4);
        }
        return String.format(Locale.US, "%.2e", value);
    }

    private String formatGain(double value) {
        if (Double.isInfinite(value)) {
            return "ошибки не наблюдались";
        }
        if (value >= 100.0) {
            return String.format(Locale.US, "%.0fx", value);
        }
        if (value >= 10.0) {
            return String.format(Locale.US, "%.1fx", value);
        }
        return String.format(Locale.US, "%.2fx", value);
    }

    private String formatEnergy(Double value) {
        return value == null ? "н/д" : formatFixed(value) + " дБ";
    }

    private String formatDb(Double value) {
        return value == null ? "н/д" : formatFixed(value) + " дБ";
    }

    private String formatFixed(double value) {
        return formatFixed(value, 2);
    }

    private String formatFixed(double value, int precision) {
        return String.format(Locale.US, "%1$." + precision + "f", value);
    }
}
