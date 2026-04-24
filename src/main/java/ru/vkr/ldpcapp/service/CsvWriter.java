package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.ExperimentSummary;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class CsvWriter {

    public String buildResultsCsv(SimulationConfig config, List<ResultPoint> points) {
        SimulationConfig safeConfig = config != null ? config : SimulationConfig.recommendedProfile();
        List<ResultPoint> safePoints = points != null ? points : List.of();
        ExperimentSummary summary = ExperimentSummary.from(safePoints);

        StringBuilder builder = new StringBuilder();

        appendRow(builder, "Раздел", "Конфигурация эксперимента");
        appendRow(builder, "Параметр", "Значение");
        appendRow(builder, "InfoBlockLength_bits", Integer.toString(safeConfig.getInfoBlockLength()));
        appendRow(builder, "SNR_Start_dB", formatNumber(safeConfig.getSnrStart()));
        appendRow(builder, "SNR_End_dB", formatNumber(safeConfig.getSnrEnd()));
        appendRow(builder, "SNR_Step_dB", formatNumber(safeConfig.getSnrStep()));
        appendRow(builder, "SNR_Point_Count", Integer.toString(safeConfig.getSnrPointCount()));
        appendRow(builder, "Blocks_Per_SNR", Integer.toString(safeConfig.getBlocks()));
        appendRow(builder, "Total_Blocks", Integer.toString(safeConfig.getExperimentBlockCount()));
        appendRow(builder, "Estimated_Information_Bits", Integer.toString(safeConfig.getEstimatedInformationBits()));
        appendRow(builder, "Code_Rate", formatNumber(safeConfig.getCodeRate()));
        appendRow(builder, "Max_Iterations", Integer.toString(safeConfig.getMaxIterations()));
        appendRow(builder, "Normalization_Factor", formatNumber(safeConfig.getNormalization()));
        appendRow(builder, "Random_Seed", Integer.toString(safeConfig.getSeed()));
        appendRow(builder, "Modulation", "BPSK");
        appendRow(builder, "Channel", "AWGN");
        appendRow(builder, "Coding", "LDPC (24,12), normalized min-sum");

        builder.append(System.lineSeparator());

        appendRow(builder, "Раздел", "Итоговые метрики");
        appendRow(builder, "Метрика", "Значение");
        appendRow(builder, "Best_BER_Gain", formatNumber(summary.getBestBerGain()));
        appendRow(builder, "Best_BLER_Gain", formatNumber(summary.getBestBlerGain()));
        appendRow(builder, "Min_Coded_BER", formatNumber(summary.getMinCodedBer()));
        appendRow(builder, "Average_Iterations", formatNumber(summary.getAverageIterations()));
        appendRow(builder, "Average_Success_Ratio", formatNumber(summary.getAverageSuccessRatio()));
        appendRow(
                builder,
                "First_Stable_BLER_SNR_dB",
                summary.getFirstStableBlerSnr() == null ? "" : formatNumber(summary.getFirstStableBlerSnr())
        );

        builder.append(System.lineSeparator());

        appendRow(builder, "Раздел", "Точки эксперимента");
        appendRow(
                builder,
                "SNR_dB",
                "BER_Uncoded",
                "BER_LDPC",
                "BLER_Uncoded",
                "BLER_LDPC",
                "Average_Iterations",
                "Success_Ratio"
        );

        for (ResultPoint point : safePoints) {
            appendRow(
                    builder,
                    formatNumber(point.getSnr()),
                    formatNumber(point.getBerUncoded()),
                    formatNumber(point.getBerLdpc()),
                    formatNumber(point.getBlerUncoded()),
                    formatNumber(point.getBlerLdpc()),
                    formatNumber(point.getAverageIterations()),
                    formatNumber(point.getSuccessRatio())
            );
        }

        return builder.toString();
    }

    public void write(Path path, String content) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, '\uFEFF' + content, StandardCharsets.UTF_8);
    }

    private void appendRow(StringBuilder builder, String... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                builder.append(';');
            }
            builder.append(escape(values[index]));
        }
        builder.append(System.lineSeparator());
    }

    private String escape(String value) {
        String safeValue = value == null ? "" : value;
        boolean requiresQuotes = safeValue.contains(";") || safeValue.contains("\n") || safeValue.contains("\r") || safeValue.contains("\"");
        if (!requiresQuotes) {
            return safeValue;
        }
        return '"' + safeValue.replace("\"", "\"\"") + '"';
    }

    private String formatNumber(double value) {
        if (Double.isNaN(value)) {
            return "";
        }
        if (Double.isInfinite(value)) {
            return "INF";
        }

        double abs = Math.abs(value);
        String formatted;
        if (abs == 0.0) {
            formatted = "0";
        } else if (abs >= 0.001 && abs < 1000.0) {
            formatted = String.format(Locale.US, "%.6f", value);
            while (formatted.contains(".") && (formatted.endsWith("0") || formatted.endsWith("."))) {
                formatted = formatted.substring(0, formatted.length() - 1);
            }
        } else {
            formatted = String.format(Locale.US, "%.6e", value);
        }

        return formatted.replace('.', ',');
    }
}
