package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.BatchScenarioResult;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.config.SimulationConfigFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class BatchFileService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public Path saveBatchExperiment(Path targetPath, SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("meta.format", "ldpc-batch-v1");
        properties.setProperty("meta.generated", LocalDateTime.now().toString());

        if (baseConfig != null) {
            writeConfig(properties, "base.", baseConfig);
        }

        List<BatchScenarioResult> safeScenarios = scenarios == null ? List.of() : scenarios;
        properties.setProperty("scenario.count", String.valueOf(safeScenarios.size()));

        for (int i = 0; i < safeScenarios.size(); i++) {
            BatchScenarioResult scenario = safeScenarios.get(i);
            String prefix = "scenario." + i + ".";
            properties.setProperty(prefix + "label", scenario.getScenarioLabel());
            writeConfig(properties, prefix + "config.", scenario.getConfig());

            List<ResultPoint> points = scenario.getPoints();
            properties.setProperty(prefix + "points.count", String.valueOf(points.size()));
            for (int j = 0; j < points.size(); j++) {
                ResultPoint point = points.get(j);
                String pointPrefix = prefix + "point." + j + ".";
                properties.setProperty(pointPrefix + "snr", format(point.getSnr()));
                properties.setProperty(pointPrefix + "berUncoded", format(point.getBerUncoded()));
                properties.setProperty(pointPrefix + "berLdpc", format(point.getBerLdpc()));
                properties.setProperty(pointPrefix + "blerUncoded", format(point.getBlerUncoded()));
                properties.setProperty(pointPrefix + "blerLdpc", format(point.getBlerLdpc()));
                properties.setProperty(pointPrefix + "averageIterations", format(point.getAverageIterations()));
                properties.setProperty(pointPrefix + "successRatio", format(point.getSuccessRatio()));
                properties.setProperty(pointPrefix + "throughput", format(point.getEffectiveThroughputMbps()));
                properties.setProperty(pointPrefix + "spectralEfficiency", format(point.getSpectralEfficiency()));
            }
        }

        try (var writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
            writer.write("# LDPC Batch Experiment\n");
            properties.store(writer, "Saved batch experiment");
        }
        return targetPath;
    }

    public BatchFileData loadBatchExperiment(Path sourcePath) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        String format = properties.getProperty("meta.format", "");
        if (!"ldpc-batch-v1".equals(format)) {
            throw new IllegalArgumentException("Файл не является поддерживаемым batch-экспериментом.");
        }

        SimulationConfig baseConfig = readConfig(properties, "base.");
        int scenarioCount = parseInt(properties.getProperty("scenario.count", "0"), 0);
        List<BatchScenarioResult> scenarios = new ArrayList<>();

        for (int i = 0; i < scenarioCount; i++) {
            String prefix = "scenario." + i + ".";
            String label = properties.getProperty(prefix + "label", "Сценарий " + (i + 1));
            SimulationConfig config = readConfig(properties, prefix + "config.");
            int pointCount = parseInt(properties.getProperty(prefix + "points.count", "0"), 0);
            List<ResultPoint> points = new ArrayList<>();
            for (int j = 0; j < pointCount; j++) {
                String pointPrefix = prefix + "point." + j + ".";
                points.add(new ResultPoint(
                        parseDouble(properties.getProperty(pointPrefix + "snr", "0"), 0.0),
                        parseDouble(properties.getProperty(pointPrefix + "berUncoded", "0"), 0.0),
                        parseDouble(properties.getProperty(pointPrefix + "berLdpc", "0"), 0.0),
                        parseDouble(properties.getProperty(pointPrefix + "blerUncoded", "0"), 0.0),
                        parseDouble(properties.getProperty(pointPrefix + "blerLdpc", "0"), 0.0),
                        parseDouble(properties.getProperty(pointPrefix + "averageIterations", "0"), 0.0),
                        parseDouble(properties.getProperty(pointPrefix + "successRatio", "0"), 0.0),
                        parseDouble(properties.getProperty(pointPrefix + "throughput", "0"), 0.0),
                        parseDouble(properties.getProperty(pointPrefix + "spectralEfficiency", "0"), 0.0)
                ));
            }
            scenarios.add(new BatchScenarioResult(label, config, points));
        }

        return new BatchFileData(baseConfig, scenarios);
    }

    public String buildSuggestedBatchProjectFileName() {
        return "batch_project_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".properties";
    }

    private void writeConfig(Properties properties, String prefix, SimulationConfig config) {
        properties.setProperty(prefix + "infoBlockLength", String.valueOf(config.getInfoBlockLength()));
        properties.setProperty(prefix + "snrStart", format(config.getSnrStart()));
        properties.setProperty(prefix + "snrEnd", format(config.getSnrEnd()));
        properties.setProperty(prefix + "snrStep", format(config.getSnrStep()));
        properties.setProperty(prefix + "blocks", String.valueOf(config.getBlocks()));
        properties.setProperty(prefix + "maxIterations", String.valueOf(config.getMaxIterations()));
        properties.setProperty(prefix + "normalization", format(config.getNormalization()));
        properties.setProperty(prefix + "seed", String.valueOf(config.getSeed()));
        properties.setProperty(prefix + "modulation", config.getModulation());
        properties.setProperty(prefix + "channelModel", config.getChannelModel());
        properties.setProperty(prefix + "ldpcProfile", config.getLdpcProfile());
        properties.setProperty(prefix + "waveform", config.getWaveform());
        properties.setProperty(prefix + "spatialMode", config.getSpatialMode());
        properties.setProperty(prefix + "cyclicPrefix", String.valueOf(config.getCyclicPrefix()));
        properties.setProperty(prefix + "equalizerMode", config.getEqualizerMode());
    }

    private SimulationConfig readConfig(Properties properties, String prefix) {
        String modulation = properties.getProperty(prefix + "modulation", SimulationConfig.MOD_QPSK);
        String channelModel = properties.getProperty(prefix + "channelModel", SimulationConfig.CHANNEL_AWGN);
        String ldpcProfile = properties.getProperty(prefix + "ldpcProfile", SimulationConfig.PROFILE_QC);
        String waveform = properties.getProperty(prefix + "waveform", SimulationConfig.WAVEFORM_OFDM64);
        String spatialMode = properties.getProperty(prefix + "spatialMode", SimulationConfig.SPATIAL_SISO);
        int normalizedLength = SimulationConfigFactory.normalizeInfoBlockLength(
                parseInt(properties.getProperty(prefix + "infoBlockLength", "192"), 192),
                ldpcProfile
        );

        return new SimulationConfig(
                normalizedLength,
                parseDouble(properties.getProperty(prefix + "snrStart", "0.0"), 0.0),
                parseDouble(properties.getProperty(prefix + "snrEnd", "8.0"), 8.0),
                parseDouble(properties.getProperty(prefix + "snrStep", "1.0"), 1.0),
                parseInt(properties.getProperty(prefix + "blocks", "80"), 80),
                parseInt(properties.getProperty(prefix + "maxIterations", "12"), 12),
                parseDouble(properties.getProperty(prefix + "normalization", "0.85"), 0.85),
                parseInt(properties.getProperty(prefix + "seed", "2025"), 2025),
                modulation,
                channelModel,
                ldpcProfile,
                waveform,
                spatialMode,
                parseInt(
                        properties.getProperty(
                                prefix + "cyclicPrefix",
                                String.valueOf(SimulationConfigFactory.normalizeCyclicPrefix(0, waveform))
                        ),
                        SimulationConfigFactory.normalizeCyclicPrefix(0, waveform)
                ),
                properties.getProperty(prefix + "equalizerMode", SimulationConfig.WAVEFORM_SC.equals(waveform) ? SimulationConfig.EQUALIZER_NONE : SimulationConfig.EQUALIZER_ZF)
        );
    }

    private String format(double value) {
        return String.format(Locale.US, "%.10f", value);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    public record BatchFileData(SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) {
    }
}
