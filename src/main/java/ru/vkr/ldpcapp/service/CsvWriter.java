package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.ExperimentSummary;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.config.SimulationConfigFactory;
import ru.vkr.ldpcapp.service.config.SimulationConfigProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class CsvWriter {
    private final SimulationConfigProfiles configProfiles = new SimulationConfigProfiles();

    public String buildResultsCsv(SimulationConfig config, List<ResultPoint> points) {
        SimulationConfig safeConfig = config != null ? config : configProfiles.recommendedProfile();
        List<ResultPoint> safePoints = points != null ? points : List.of();
        ExperimentSummary summary = ExperimentSummary.from(safePoints);

        StringBuilder builder = new StringBuilder();

        appendRow(builder, "Раздел", "Конфигурация эксперимента");
        appendRow(builder, "Параметр", "Значение");
        appendRow(builder, "InfoBlockLength_bits", Integer.toString(safeConfig.getInfoBlockLength()));
        appendRow(builder, "SNR_Start_dB", formatNumber(safeConfig.getSnrStart()));
        appendRow(builder, "SNR_End_dB", formatNumber(safeConfig.getSnrEnd()));
        appendRow(builder, "SNR_Step_dB", formatNumber(safeConfig.getSnrStep()));
        appendRow(builder, "SNR_Point_Count", Integer.toString(SimulationConfigFactory.getSnrPointCount(safeConfig)));
        appendRow(builder, "Blocks_Per_SNR", Integer.toString(safeConfig.getBlocks()));
        appendRow(builder, "Total_Blocks", Integer.toString(SimulationConfigFactory.getExperimentBlockCount(safeConfig)));
        appendRow(builder, "Estimated_Information_Bits", Integer.toString(SimulationConfigFactory.getEstimatedInformationBits(safeConfig)));
        appendRow(builder, "Code_Rate", formatNumber(SimulationConfigFactory.getCodeRate(safeConfig)));
        appendRow(builder, "Max_Iterations", Integer.toString(safeConfig.getMaxIterations()));
        appendRow(builder, "Normalization_Factor", formatNumber(safeConfig.getNormalization()));
        appendRow(builder, "Random_Seed", Integer.toString(safeConfig.getSeed()));
        appendRow(builder, "Modulation", safeConfig.getModulation());
        appendRow(builder, "Channel", safeConfig.getChannelModel());
        appendRow(builder, "Waveform", safeConfig.getWaveform());
        appendRow(builder, "Spatial_Mode", safeConfig.getSpatialMode());
        appendRow(builder, "Equalizer", safeConfig.getEqualizerMode());
        appendRow(builder, "LDPC_Profile", safeConfig.getLdpcProfile());
        appendRow(builder, "Coding", SimulationConfigFactory.getProfileDisplayName(safeConfig.getLdpcProfile(), safeConfig.getLiftingSize()) + ", normalized min-sum");
        appendRow(builder, "Adaptive_Stop_Enabled", Boolean.toString(safeConfig.isAdaptiveStopEnabled()));
        appendRow(builder, "Min_Error_Events_Per_SNR", Integer.toString(safeConfig.getMinErrorEventsPerSnr()));
        appendRow(builder, "Max_Blocks_Per_SNR", Integer.toString(safeConfig.getMaxBlocksPerSnr()));
        appendRow(builder, "Confidence_Level", formatNumber(safeConfig.getConfidenceLevel()));

        builder.append(System.lineSeparator());

        appendRow(builder, "Раздел", "Итоговые метрики");
        appendRow(builder, "Метрика", "Значение");
        appendRow(builder, "Best_BER_Gain", formatNumber(summary.getBestBerGain()));
        appendRow(builder, "Best_BLER_Gain", formatNumber(summary.getBestBlerGain()));
        appendRow(builder, "Min_Coded_BER", formatNumber(summary.getMinCodedBer()));
        appendRow(builder, "Min_Coded_BLER", formatNumber(summary.getMinCodedBler()));
        appendRow(builder, "Average_Iterations", formatNumber(summary.getAverageIterations()));
        appendRow(builder, "Average_Success_Ratio", formatNumber(summary.getAverageSuccessRatio()));
        appendRow(builder, "Average_Throughput_Mbps", formatNumber(summary.getAverageThroughputMbps()));
        appendRow(builder, "Peak_Throughput_Mbps", formatNumber(summary.getPeakThroughputMbps()));
        appendRow(builder, "Average_Spectral_Efficiency", formatNumber(summary.getAverageSpectralEfficiency()));
        appendRow(builder, "Peak_Spectral_Efficiency", formatNumber(summary.getPeakSpectralEfficiency()));
        appendRow(
                builder,
                "First_Stable_BLER_SNR_dB",
                summary.getFirstStableBlerSnr() == null ? "" : formatNumber(summary.getFirstStableBlerSnr())
        );
        appendRow(
                builder,
                "Required_SNR_BER_dB",
                summary.getRequiredSnrBerDb() == null ? "" : formatNumber(summary.getRequiredSnrBerDb())
        );
        appendRow(
                builder,
                "Required_SNR_BLER_dB",
                summary.getRequiredSnrBlerDb() == null ? "" : formatNumber(summary.getRequiredSnrBlerDb())
        );

        builder.append(System.lineSeparator());

        appendRow(builder, "Раздел", "Точки эксперимента");
        appendRow(
                builder,
                "SNR_dB",
                "BER_Uncoded",
                "BER_LDPC",
                "BER_LDPC_CI_Low",
                "BER_LDPC_CI_High",
                "BLER_Uncoded",
                "BLER_LDPC",
                "BLER_LDPC_CI_Low",
                "BLER_LDPC_CI_High",
                "Average_Iterations",
                "Success_Ratio",
                "Effective_Throughput_Mbps",
                "Spectral_Efficiency",
                "Bit_Errors_LDPC",
                "Block_Errors_LDPC",
                "Total_Bits",
                "Total_Blocks",
                "Confidence_Level"
        );

        for (ResultPoint point : safePoints) {
            appendRow(
                    builder,
                    formatNumber(point.getSnr()),
                    formatNumber(point.getBerUncoded()),
                    formatNumber(point.getBerLdpc()),
                    formatNumber(point.getBerLdpcCiLow()),
                    formatNumber(point.getBerLdpcCiHigh()),
                    formatNumber(point.getBlerUncoded()),
                    formatNumber(point.getBlerLdpc()),
                    formatNumber(point.getBlerLdpcCiLow()),
                    formatNumber(point.getBlerLdpcCiHigh()),
                    formatNumber(point.getAverageIterations()),
                    formatNumber(point.getSuccessRatio()),
                    formatNumber(point.getEffectiveThroughputMbps()),
                    formatNumber(point.getSpectralEfficiency()),
                    Integer.toString(point.getBitErrorsLdpc()),
                    Integer.toString(point.getBlockErrorsLdpc()),
                    Integer.toString(point.getTotalBits()),
                    Integer.toString(point.getTotalBlocks()),
                    formatNumber(point.getConfidenceLevel())
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
