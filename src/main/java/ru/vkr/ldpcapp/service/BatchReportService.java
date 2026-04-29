package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.BatchScenarioResult;
import ru.vkr.ldpcapp.model.ExperimentSummary;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.config.SimulationConfigFormatter;


import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class BatchReportService {
    private final SimulationConfigFormatter configFormatter = new SimulationConfigFormatter();

    public String buildBatchNarrative(SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) {
        if (scenarios == null || scenarios.isEmpty()) {
            return "После запуска batch-режима здесь появится автоматический сравнительный вывод по нескольким сценариям.";
        }

        BatchScenarioResult leader = scenarios.stream()
                .max(Comparator.comparingDouble(this::scenarioScore))
                .orElse(scenarios.get(0));

        BatchScenarioResult bestBerEnergy = scenarios.stream()
                .filter(item -> item.getSummary().getBerEnergyGainDb() != null)
                .max(Comparator.comparingDouble(item -> item.getSummary().getBerEnergyGainDb()))
                .orElse(null);

        BatchScenarioResult bestBlerEnergy = scenarios.stream()
                .filter(item -> item.getSummary().getBlerEnergyGainDb() != null)
                .max(Comparator.comparingDouble(item -> item.getSummary().getBlerEnergyGainDb()))
                .orElse(null);

        StringBuilder builder = new StringBuilder();
        builder.append("Batch-анализ нескольких сценариев\n\n");
        builder.append("Количество рассчитанных сценариев: ").append(scenarios.size()).append("\n");
        if (baseConfig != null) {
            builder.append("Базовая численная конфигурация:\n");
            builder.append(configFormatter.toSummaryText(baseConfig)).append("\n\n");
        }

        builder.append("Наиболее сильный сценарий по интегральной оценке:\n");
        builder.append("• ").append(leader.getScenarioLabel()).append("\n");
        builder.append("• лучший BER gain: ").append(formatGain(leader.getSummary().getBestBerGain())).append("\n");
        builder.append("• лучший BLER gain: ").append(formatGain(leader.getSummary().getBestBlerGain())).append("\n");
        builder.append("• BER energy gain: ").append(formatDb(leader.getSummary().getBerEnergyGainDb())).append("\n");
        builder.append("• BLER energy gain: ").append(formatDb(leader.getSummary().getBlerEnergyGainDb())).append("\n");
        builder.append("• peak throughput: ").append(formatThroughput(leader.getSummary().getPeakThroughputMbps())).append("\n");
        builder.append("• peak spectral efficiency: ").append(formatSpectralEfficiency(leader.getSummary().getPeakSpectralEfficiency())).append("\n");
        builder.append("• required SNR (BER): ").append(formatDb(leader.getSummary().getRequiredSnrBerDb())).append("\n");
        builder.append("• required SNR (BLER): ").append(formatDb(leader.getSummary().getRequiredSnrBlerDb())).append("\n\n");

        if (bestBerEnergy != null) {
            builder.append("Лучший сценарий по целевому BER = 10^-3: ")
                    .append(bestBerEnergy.getScenarioLabel())
                    .append(" (")
                    .append(formatDb(bestBerEnergy.getSummary().getBerEnergyGainDb()))
                    .append(")\n");
        }
        if (bestBlerEnergy != null) {
            builder.append("Лучший сценарий по целевому BLER = 10^-1: ")
                    .append(bestBlerEnergy.getScenarioLabel())
                    .append(" (")
                    .append(formatDb(bestBlerEnergy.getSummary().getBlerEnergyGainDb()))
                    .append(")\n");
        }

        builder.append("\nКраткие выводы по всем сценариям:\n");
        for (BatchScenarioResult item : scenarios) {
            ExperimentSummary summary = item.getSummary();
            builder.append("• ").append(item.getScenarioLabel())
                    .append(": выигрыш BER ").append(formatGain(summary.getBestBerGain()))
                    .append(", выигрыш BLER ").append(formatGain(summary.getBestBlerGain()))
                    .append(", энергия BER ").append(formatDb(summary.getBerEnergyGainDb()))
                    .append(", энергия BLER ").append(formatDb(summary.getBlerEnergyGainDb()))
                    .append(", Пиковая пропускная способность ").append(formatThroughput(summary.getPeakThroughputMbps()))
                    .append(", Пиковая спектральная эффективность ").append(formatSpectralEfficiency(summary.getPeakSpectralEfficiency()))
                    .append(", требуемый SNR BER ").append(formatDb(summary.getRequiredSnrBerDb()))
                    .append(", требуемый SNR BLER ").append(formatDb(summary.getRequiredSnrBlerDb()))
                    .append("\n");
        }

        return builder.toString();
    }

    public String buildBatchReport(SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) {
        if (scenarios == null || scenarios.isEmpty()) {
            return "Нет данных batch-эксперимента для формирования сравнительного отчёта.";
        }

        String narrative = buildBatchNarrative(baseConfig, scenarios);
        StringBuilder builder = new StringBuilder();
        builder.append("Сравнительный отчёт по batch-эксперименту\n\n");
        builder.append("1. Назначение эксперимента\n");
        builder.append("Данный batch-эксперимент предназначен для сопоставления нескольких сценариев передачи данных в цифровом канале связи по показателям BER, BLER и энергетического выигрыша. Сравнение проводится между комбинациями модуляции, типа канала и профиля LDPC-кодирования при единой базовой численной конфигурации.\n\n");

        builder.append("2. Базовая конфигурация\n");
        builder.append(
                baseConfig == null
                        ? "Базовая конфигурация отсутствует.\n\n"
                        : configFormatter.toSummaryText(baseConfig) + "\n\n"
        );

        builder.append("3. Перечень сценариев\n");
        for (int i = 0; i < scenarios.size(); i++) {
            BatchScenarioResult item = scenarios.get(i);
            builder.append(i + 1)
                    .append(") ")
                    .append(item.getScenarioLabel())
                    .append(" | blocks=")
                    .append(item.getConfig().getBlocks())
                    .append(", SNR=")
                    .append(formatSnrRange(item.getConfig()))
                    .append("\n");
        }
        builder.append("\n");

        builder.append("4. Сводная сравнительная таблица\n");
        builder.append(buildScenarioTable(scenarios)).append("\n\n");

        builder.append("4.1. OFDM/MIMO-like контекст\n");
        builder.append("При анализе сценариев учитываются также выбранный waveform (single-carrier / OFDM-64 / OFDM-128) и spatial mode (SISO / 2x2 diversity), что позволяет интерпретировать результаты ближе к радиоинтерфейсу 5G.\n\n");

        builder.append("5. Аналитический вывод\n");
        builder.append(narrative).append("\n\n");

        builder.append("6. Рекомендации для главы 3\n");
        builder.append(buildRecommendations(scenarios)).append("\n");

        return builder.toString();
    }

    public String buildBatchCsv(List<BatchScenarioResult> scenarios) {
        if (scenarios == null || scenarios.isEmpty()) {
            return "scenario,modulation,channel,ldpc_profile,best_ber_gain,best_bler_gain,ber_energy_gain_db,bler_energy_gain_db,peak_throughput_mbps,peak_spectral_efficiency,required_snr_ber_db,required_snr_bler_db\n";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("scenario,modulation,channel,ldpc_profile,best_ber_gain,best_bler_gain,ber_energy_gain_db,bler_energy_gain_db,peak_throughput_mbps,peak_spectral_efficiency,required_snr_ber_db,required_snr_bler_db\n");
        for (BatchScenarioResult item : scenarios) {
            ExperimentSummary summary = item.getSummary();
            builder.append(csv(item.getScenarioLabel())).append(',')
                    .append(csv(item.getModulation())).append(',')
                    .append(csv(item.getChannel())).append(',')
                    .append(csv(item.getLdpcProfileName())).append(',')
                    .append(summary.getBestBerGain()).append(',')
                    .append(summary.getBestBlerGain()).append(',')
                    .append(summary.getBerEnergyGainDb() == null ? "" : String.format(Locale.US, "%.6f", summary.getBerEnergyGainDb())).append(',')
                    .append(summary.getBlerEnergyGainDb() == null ? "" : String.format(Locale.US, "%.6f", summary.getBlerEnergyGainDb())).append(',')
                    .append(String.format(Locale.US, "%.6f", summary.getPeakThroughputMbps())).append(',')
                    .append(String.format(Locale.US, "%.6f", summary.getPeakSpectralEfficiency())).append(',')
                    .append(summary.getRequiredSnrBerDb() == null ? "" : String.format(Locale.US, "%.6f", summary.getRequiredSnrBerDb())).append(',')
                    .append(summary.getRequiredSnrBlerDb() == null ? "" : String.format(Locale.US, "%.6f", summary.getRequiredSnrBlerDb()))
                    .append('\n');
        }
        return builder.toString();
    }

    private String buildScenarioTable(List<BatchScenarioResult> scenarios) {
        String header = String.format(
                Locale.US,
                "%-36s | %-7s | %-8s | %-18s | %-9s | %-9s | %-10s | %-10s | %-12s | %-10s | %-10s | %-10s%n",
                "Сценарий", "Мод.", "Канал", "LDPC", "BER g", "BLER g", "BER дБ", "BLER дБ", "Thrpt", "η", "SNR BER", "SNR BLER"
        );
        String separator = "-".repeat(Math.max(170, header.length() - 1)) + System.lineSeparator();

        String rows = scenarios.stream()
                .map(item -> String.format(
                        Locale.US,
                        "%-36s | %-7s | %-8s | %-18s | %-9s | %-9s | %-10s | %-10s | %-12s | %-10s | %-10s | %-10s",
                        trim(item.getScenarioLabel(), 36),
                        trim(item.getModulation(), 7),
                        trim(item.getChannel(), 8),
                        trim(item.getLdpcProfileName(), 18),
                        formatGain(item.getSummary().getBestBerGain()),
                        formatGain(item.getSummary().getBestBlerGain()),
                        formatDb(item.getSummary().getBerEnergyGainDb()),
                        formatDb(item.getSummary().getBlerEnergyGainDb()),
                        trim(formatThroughput(item.getSummary().getPeakThroughputMbps()), 12),
                        trim(formatSpectralEfficiency(item.getSummary().getPeakSpectralEfficiency()), 10),
                        trim(formatDb(item.getSummary().getRequiredSnrBerDb()), 10),
                        trim(formatDb(item.getSummary().getRequiredSnrBlerDb()), 10)
                ))
                .collect(Collectors.joining(System.lineSeparator()));

        return header + separator + rows;
    }

    private String buildRecommendations(List<BatchScenarioResult> scenarios) {
        BatchScenarioResult bestRobust = scenarios.stream()
                .max(Comparator.comparingDouble(this::scenarioScore))
                .orElse(scenarios.get(0));

        List<BatchScenarioResult> rayleighScenarios = scenarios.stream()
                .filter(item -> SimulationConfig.CHANNEL_RAYLEIGH.equals(item.getChannel()))
                .toList();

        BatchScenarioResult bestRayleigh = rayleighScenarios.stream()
                .max(Comparator.comparingDouble(this::scenarioScore))
                .orElse(null);

        StringBuilder builder = new StringBuilder();
        builder.append("• В качестве наиболее устойчивого сценария по суммарной оценке рекомендуется выделить режим «")
                .append(bestRobust.getScenarioLabel())
                .append("».\n");

        if (bestRayleigh != null) {
            builder.append("• Для условий замираний Rayleigh целесообразно отдельно отметить сценарий «")
                    .append(bestRayleigh.getScenarioLabel())
                    .append("» как наиболее устойчивый среди fading-режимов.\n");
        }

        builder.append("• В тексте главы 3 рекомендуется сопоставить энергетический выигрыш по целевым BER = 10^-3 и BLER = 10^-1, а также показать, как выбор модуляции и профиля LDPC влияет на требуемый SNR.\n");
        builder.append("• Дополнительно стоит сравнить peak throughput и peak spectral efficiency, чтобы показать компромисс между надёжностью и производительностью системы.\n");
        builder.append("• Batch-результаты удобно использовать как основу для сравнительной таблицы и краткого аналитического вывода по нескольким сценариям сразу.");
        return builder.toString();
    }

    private double scenarioScore(BatchScenarioResult item) {
        ExperimentSummary summary = item.getSummary();
        double score = bounded(summary.getBestBerGain()) + bounded(summary.getBestBlerGain());
        score += 3.0 * (summary.getBerEnergyGainDb() == null ? 0.0 : summary.getBerEnergyGainDb());
        score += 3.0 * (summary.getBlerEnergyGainDb() == null ? 0.0 : summary.getBlerEnergyGainDb());
        return score;
    }

    private double bounded(double value) {
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

    private String formatDb(Double value) {
        return value == null ? "н/д" : String.format(Locale.US, "%.2f дБ", value);
    }

    private String formatThroughput(double value) {
        return String.format(Locale.US, "%.2f Мбит/с", value);
    }

    private String formatSpectralEfficiency(double value) {
        return String.format(Locale.US, "%.2f бит/с/Гц", value);
    }

    private String formatSnrRange(SimulationConfig config) {
        return String.format(Locale.US, "%.1f..%.1f step %.1f dB", config.getSnrStart(), config.getSnrEnd(), config.getSnrStep());
    }

    private String trim(String value, int length) {
        String safe = Objects.toString(value, "");
        return safe.length() <= length ? safe : safe.substring(0, Math.max(0, length - 1)) + "…";
    }

    private String csv(String value) {
        String safe = Objects.toString(value, "").replace("\"", "\"\"");
        return '"' + safe + '"';
    }
}
