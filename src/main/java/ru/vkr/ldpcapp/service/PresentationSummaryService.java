package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.BatchScenarioResult;
import ru.vkr.ldpcapp.model.ExperimentSummary;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PresentationSummaryService {

    public String buildExperimentSummary(SimulationConfig config, List<ResultPoint> points) {
        if (points == null || points.isEmpty()) {
            return "Краткая презентационная сводка недоступна: сначала выполните расчёт сценария.";
        }

        SimulationConfig safeConfig = config == null ? SimulationConfig.recommendedProfile() : config;
        ExperimentSummary summary = ExperimentSummary.from(points);

        return String.join(System.lineSeparator(),
                "PRESENTATION SUMMARY · SINGLE SCENARIO",
                "",
                "Сценарий:",
                "• modulation: " + safeConfig.getModulation(),
                "• channel: " + safeConfig.getChannelModel(),
                "• LDPC: " + SimulationConfig.getProfileDisplayName(safeConfig.getLdpcProfile(), safeConfig.getLiftingSize()),
                "• waveform: " + safeConfig.getWaveform(),
                "• spatial mode: " + safeConfig.getSpatialMode(),
                "",
                "Ключевые показатели:",
                "• BER gain: " + formatGain(summary.getBestBerGain()),
                "• BLER gain: " + formatGain(summary.getBestBlerGain()),
                "• peak throughput: " + formatThroughput(summary.getPeakThroughputMbps()),
                "• peak spectral efficiency: " + formatEfficiency(summary.getPeakSpectralEfficiency()),
                "• required SNR @ BER=10^-3: " + formatDb(summary.getRequiredSnrBerDb()),
                "• required SNR @ BLER=10^-1: " + formatDb(summary.getRequiredSnrBlerDb()),
                "",
                "Краткий вывод:",
                "LDPC-кодирование в выбранном waveform/spatial профиле обеспечивает заметное улучшение надёжности передачи и даёт инженерно интерпретируемый выигрыш по качеству и полезной скорости."
        );
    }

    public String buildCompareSummary(
            SimulationConfig leftConfig,
            SimulationConfig rightConfig,
            List<ResultPoint> leftResults,
            List<ResultPoint> rightResults,
            String leftTitle,
            String rightTitle
    ) {
        if (leftResults == null || leftResults.isEmpty() || rightResults == null || rightResults.isEmpty()) {
            return "Краткая презентационная сводка сравнения недоступна: сначала подготовьте два сценария.";
        }

        ExperimentSummary leftSummary = ExperimentSummary.from(leftResults);
        ExperimentSummary rightSummary = ExperimentSummary.from(rightResults);
        double leftScore = compareScore(leftSummary);
        double rightScore = compareScore(rightSummary);
        String winner = Math.abs(leftScore - rightScore) < 1e-9
                ? "Сценарии сопоставимы"
                : (leftScore > rightScore ? leftTitle : rightTitle);

        return String.join(System.lineSeparator(),
                "PRESENTATION SUMMARY · COMPARE",
                "",
                "Scenario A: " + leftTitle,
                "• " + scenarioLine(leftConfig, leftSummary),
                "",
                "Scenario B: " + rightTitle,
                "• " + scenarioLine(rightConfig, rightSummary),
                "",
                "Победитель:",
                "• " + winner,
                "",
                "Вывод:",
                "Попарное сравнение показывает компромисс между BER/BLER, required SNR и throughput. Presentation-режим помогает быстро показать комиссии лучший сценарий и его инженерные преимущества."
        );
    }

    public String buildBatchSummary(SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) {
        if (scenarios == null || scenarios.isEmpty()) {
            return "Краткая презентационная batch-сводка недоступна: сначала выполните batch-расчёт.";
        }

        BatchScenarioResult winner = scenarios.stream()
                .max(Comparator.comparingDouble(item -> compareScore(item.getSummary())))
                .orElse(scenarios.get(0));

        double maxThroughput = scenarios.stream()
                .map(BatchScenarioResult::getSummary)
                .mapToDouble(ExperimentSummary::getPeakThroughputMbps)
                .max()
                .orElse(0.0);
        double maxEfficiency = scenarios.stream()
                .map(BatchScenarioResult::getSummary)
                .mapToDouble(ExperimentSummary::getPeakSpectralEfficiency)
                .max()
                .orElse(0.0);
        Double bestRequiredSnr = scenarios.stream()
                .map(BatchScenarioResult::getSummary)
                .flatMap(summary -> java.util.stream.Stream.of(summary.getRequiredSnrBerDb(), summary.getRequiredSnrBlerDb()))
                .filter(java.util.Objects::nonNull)
                .min(Double::compareTo)
                .orElse(null);

        return String.join(System.lineSeparator(),
                "PRESENTATION SUMMARY · BATCH",
                "",
                "Базовая конфигурация:",
                baseConfig == null ? "• не указана" : "• " + baseConfig.getWaveform() + ", " + baseConfig.getSpatialMode() + ", blocks=" + baseConfig.getBlocks(),
                "",
                "Лучший сценарий batch-анализа:",
                "• " + winner.getScenarioLabel(),
                "• BER gain: " + formatGain(winner.getSummary().getBestBerGain()),
                "• BLER gain: " + formatGain(winner.getSummary().getBestBlerGain()),
                "",
                "Лучшие показатели среди всех сценариев:",
                "• peak throughput: " + formatThroughput(maxThroughput),
                "• peak spectral efficiency: " + formatEfficiency(maxEfficiency),
                "• best required SNR: " + formatDb(bestRequiredSnr),
                "",
                "Вывод:",
                "Batch-режим позволяет быстро выбрать наиболее подходящий modulation/channel/LDPC/waveform/spatial профиль для демонстрации и включения в главу 3 диплома."
        );
    }

    private String scenarioLine(SimulationConfig config, ExperimentSummary summary) {
        SimulationConfig safeConfig = config == null ? SimulationConfig.recommendedProfile() : config;
        return safeConfig.getModulation() + ", "
                + safeConfig.getChannelModel() + ", "
                + safeConfig.getWaveform() + ", "
                + safeConfig.getSpatialMode()
                + " | BER gain=" + formatGain(summary.getBestBerGain())
                + ", BLER gain=" + formatGain(summary.getBestBlerGain())
                + ", thr=" + formatThroughput(summary.getPeakThroughputMbps())
                + ", η=" + formatEfficiency(summary.getPeakSpectralEfficiency())
                + ", req SNR=" + formatDb(bestNullable(summary.getRequiredSnrBerDb(), summary.getRequiredSnrBlerDb()));
    }

    private Double bestNullable(Double a, Double b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }

    private double compareScore(ExperimentSummary summary) {
        double berGain = safe(summary.getBestBerGain());
        double blerGain = safe(summary.getBestBlerGain());
        double berEnergy = summary.getBerEnergyGainDb() == null ? 0.0 : summary.getBerEnergyGainDb();
        double blerEnergy = summary.getBlerEnergyGainDb() == null ? 0.0 : summary.getBlerEnergyGainDb();
        double throughput = summary.getPeakThroughputMbps() / 10.0;
        double spectral = summary.getPeakSpectralEfficiency() * 4.0;
        double requiredBerPenalty = summary.getRequiredSnrBerDb() == null ? 0.0 : -summary.getRequiredSnrBerDb();
        double requiredBlerPenalty = summary.getRequiredSnrBlerDb() == null ? 0.0 : -summary.getRequiredSnrBlerDb();
        return berGain + blerGain + 3.0 * berEnergy + 3.0 * blerEnergy + throughput + spectral + requiredBerPenalty + requiredBlerPenalty;
    }

    private double safe(double value) {
        return Double.isInfinite(value) ? 1000.0 : value;
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

    private String formatThroughput(double value) {
        return String.format(Locale.US, "%.2f Мбит/с", value);
    }

    private String formatEfficiency(double value) {
        return String.format(Locale.US, "%.2f бит/с/Гц", value);
    }

    private String formatDb(Double value) {
        return value == null ? "н/д" : String.format(Locale.US, "%.2f дБ", value);
    }
}
