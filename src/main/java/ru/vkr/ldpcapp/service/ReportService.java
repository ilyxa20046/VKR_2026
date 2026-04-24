package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.ExperimentSummary;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ReportService {

    public String buildShortNarrative(SimulationConfig config, List<ResultPoint> points) {
        if (points == null || points.isEmpty()) {
            return "Результаты эксперимента отсутствуют. Для формирования аналитического вывода необходимо выполнить моделирование.";
        }

        SimulationConfig safeConfig = config == null ? SimulationConfig.recommendedProfile() : config;
        ExperimentSummary summary = ExperimentSummary.from(points);
        ResultPoint bestBlerPoint = findBestBlerPoint(points);

        String requiredSnrText = summary.getRequiredSnrBlerDb() == null
                ? "требуемый SNR по критерию BLER = 10^-1 в исследованном диапазоне не достигнут"
                : "требуемый SNR по критерию BLER = 10^-1 составляет около " + formatFixed(summary.getRequiredSnrBlerDb()) + " дБ";

        return "По результатам текущего эксперимента для сценария "
                + safeConfig.getModulation()
                + " / "
                + safeConfig.getChannelModel()
                + " / "
                + safeConfig.getWaveform()
                + " / "
                + safeConfig.getSpatialMode()
                + " / "
                + SimulationConfig.getProfileName(safeConfig.getLdpcProfile())
                + " применение LDPC-кодирования обеспечивает заметное снижение вероятности блочной ошибки по сравнению с передачей без кодирования. "
                + "Наибольший выигрыш по BLER наблюдается вблизи SNR = "
                + formatFixed(bestBlerPoint.getSnr())
                + " дБ и составляет около "
                + formatGain(summary.getBestBlerGain())
                + ". Энергетический выигрыш по критерию BLER = 10^-1 равен "
                + formatEnergy(summary.getBlerEnergyGainDb())
                + ", а "
                + requiredSnrText
                + ". Средний effective throughput составляет "
                + formatFixed(summary.getAverageThroughputMbps())
                + " Мбит/с, средняя спектральная эффективность — "
                + formatFixed(summary.getAverageSpectralEfficiency())
                + " бит/с/Гц. Среднее число итераций декодирования равно "
                + formatFixed(summary.getAverageIterations())
                + ", доля успешной сходимости составляет "
                + formatPercent(summary.getAverageSuccessRatio())
                + ".";
    }

    public String buildDetailedReport(SimulationConfig config, List<ResultPoint> points) {
        if (points == null || points.isEmpty()) {
            return "Отчёт по эксперименту не может быть сформирован, так как список результатов пуст.";
        }

        SimulationConfig safeConfig = config == null ? SimulationConfig.recommendedProfile() : config;
        ExperimentSummary summary = ExperimentSummary.from(points);
        ResultPoint bestBerPoint = findBestBerPoint(points);
        ResultPoint bestBlerPoint = findBestBlerPoint(points);
        ResultPoint bestThroughputPoint = findBestThroughputPoint(points);
        ResultPoint bestEfficiencyPoint = findBestEfficiencyPoint(points);

        return String.join(System.lineSeparator(),
                "Аналитический отчёт по результатам исследовательского моделирования LDPC-кодирования",
                "",
                "1. Параметры эксперимента",
                buildParameterLine("Профиль LDPC", SimulationConfig.getProfileName(safeConfig.getLdpcProfile())),
                buildParameterLine("Семейство кода", SimulationConfig.getProfileFamily(safeConfig.getLdpcProfile())),
                buildParameterLine("Модуляция", safeConfig.getModulation()),
                buildParameterLine("Канал", safeConfig.getChannelModel()),
                buildParameterLine("Waveform", safeConfig.getWaveform()),
                buildParameterLine("Spatial mode", safeConfig.getSpatialMode()),
                buildParameterLine("Cyclic prefix", String.valueOf(safeConfig.getCyclicPrefix())),
                buildParameterLine("Equalizer", safeConfig.getEqualizerMode()),
                buildParameterLine("Информационный блок", safeConfig.getInfoBlockLength() + " бит"),
                buildParameterLine("Диапазон SNR", formatFixed(Math.min(safeConfig.getSnrStart(), safeConfig.getSnrEnd()))
                        + " ... "
                        + formatFixed(Math.max(safeConfig.getSnrStart(), safeConfig.getSnrEnd()))
                        + " дБ"),
                buildParameterLine("Шаг SNR", formatFixed(safeConfig.getSnrStep()) + " дБ"),
                buildParameterLine("Количество блоков на точку", String.valueOf(safeConfig.getBlocks())),
                buildParameterLine("Максимум итераций декодера", String.valueOf(safeConfig.getMaxIterations())),
                buildParameterLine("Коэффициент нормализации", formatFixed(safeConfig.getNormalization())),
                "",
                "2. Основные результаты",
                buildParameterLine("Лучший выигрыш по BER", formatGain(summary.getBestBerGain())
                        + " при SNR = " + formatFixed(bestBerPoint.getSnr()) + " дБ"),
                buildParameterLine("Лучший выигрыш по BLER", formatGain(summary.getBestBlerGain())
                        + " при SNR = " + formatFixed(bestBlerPoint.getSnr()) + " дБ"),
                buildParameterLine("Пиковый effective throughput", formatFixed(summary.getPeakThroughputMbps())
                        + " Мбит/с при SNR = " + formatFixed(bestThroughputPoint.getSnr()) + " дБ"),
                buildParameterLine("Пиковая спектральная эффективность", formatFixed(summary.getPeakSpectralEfficiency())
                        + " бит/с/Гц при SNR = " + formatFixed(bestEfficiencyPoint.getSnr()) + " дБ"),
                buildParameterLine("Средний effective throughput", formatFixed(summary.getAverageThroughputMbps()) + " Мбит/с"),
                buildParameterLine("Средняя спектральная эффективность", formatFixed(summary.getAverageSpectralEfficiency()) + " бит/с/Гц"),
                buildParameterLine("Требуемый SNR при BER = 10^-3", formatDb(summary.getRequiredSnrBerDb())),
                buildParameterLine("Требуемый SNR при BLER = 10^-1", formatDb(summary.getRequiredSnrBlerDb())),
                buildParameterLine("Энергетический выигрыш по BER = 10^-3", formatEnergy(summary.getBerEnergyGainDb())),
                buildParameterLine("Энергетический выигрыш по BLER = 10^-1", formatEnergy(summary.getBlerEnergyGainDb())),
                buildParameterLine("Среднее число итераций декодирования", formatFixed(summary.getAverageIterations())),
                buildParameterLine("Средняя доля успешной сходимости", formatPercent(summary.getAverageSuccessRatio())),
                "",
                "3. Интерпретация результатов",
                "Результаты показывают, что при увеличении SNR не только уменьшаются BER и BLER, но и растёт effective throughput, поскольку снижается доля потерянных блоков. Это позволяет анализировать LDPC-кодирование не только с позиции надёжности, но и с позиции полезной скорости передачи данных.",
                "Спектральная эффективность определяется как произведение числа бит на символ, скорости кода и доли успешно доставленных блоков. Поэтому для более высоких порядков модуляции она потенциально выше, но в неблагоприятных условиях канала может снижаться из-за возрастания BLER.",
                "Требуемый SNR по целевым порогам BER и BLER является удобной инженерной характеристикой, показывающей, какой энергетический запас необходим системе для достижения заданного качества обслуживания. Разность между этим порогом для режима без кодирования и режима с LDPC образует энергетический выигрыш кодирования.",
                "Для рассматриваемого сценария пиковый throughput достигается в точке SNR = " + formatFixed(bestThroughputPoint.getSnr()) + " дБ, а пиковая спектральная эффективность — в точке SNR = " + formatFixed(bestEfficiencyPoint.getSnr()) + " дБ. Эти значения полезны для инженерной интерпретации компромисса между надёжностью и производительностью канала.",
                "",
                "4. Итоговый вывод",
                buildShortNarrative(safeConfig, points)
        );
    }

    private ResultPoint findBestBerPoint(List<ResultPoint> points) {
        return points.stream()
                .max(Comparator.comparingDouble(point -> gain(point.getBerUncoded(), point.getBerLdpc())))
                .orElse(points.get(0));
    }

    private ResultPoint findBestBlerPoint(List<ResultPoint> points) {
        return points.stream()
                .max(Comparator.comparingDouble(point -> gain(point.getBlerUncoded(), point.getBlerLdpc())))
                .orElse(points.get(0));
    }

    private ResultPoint findBestThroughputPoint(List<ResultPoint> points) {
        return points.stream()
                .max(Comparator.comparingDouble(ResultPoint::getEffectiveThroughputMbps))
                .orElse(points.get(0));
    }

    private ResultPoint findBestEfficiencyPoint(List<ResultPoint> points) {
        return points.stream()
                .max(Comparator.comparingDouble(ResultPoint::getSpectralEfficiency))
                .orElse(points.get(0));
    }

    private String buildParameterLine(String key, String value) {
        return "- " + key + ": " + value;
    }

    private double gain(double base, double improved) {
        if (improved <= 0.0 && base > 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        if (improved <= 0.0) {
            return 1.0;
        }
        return base / improved;
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

    private String formatPercent(double ratio) {
        return formatFixed(ratio * 100.0) + "%";
    }

    private String formatFixed(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
