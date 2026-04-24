package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.SimulationConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigFileService {

    private static final String KEY_INFO_BLOCK_LENGTH = "infoBlockLength";
    private static final String KEY_SNR_START = "snrStart";
    private static final String KEY_SNR_END = "snrEnd";
    private static final String KEY_SNR_STEP = "snrStep";
    private static final String KEY_BLOCKS = "blocks";
    private static final String KEY_MAX_ITERATIONS = "maxIterations";
    private static final String KEY_NORMALIZATION = "normalization";
    private static final String KEY_SEED = "seed";
    private static final String KEY_MODULATION = "modulation";
    private static final String KEY_CHANNEL_MODEL = "channelModel";
    private static final String KEY_LDPC_PROFILE = "ldpcProfile";
    private static final String KEY_WAVEFORM = "waveform";
    private static final String KEY_SPATIAL_MODE = "spatialMode";
    private static final String KEY_CYCLIC_PREFIX = "cyclicPrefix";
    private static final String KEY_EQUALIZER_MODE = "equalizerMode";

    public void save(Path path, SimulationConfig config) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(KEY_INFO_BLOCK_LENGTH, Integer.toString(config.getInfoBlockLength()));
        properties.setProperty(KEY_SNR_START, Double.toString(config.getSnrStart()));
        properties.setProperty(KEY_SNR_END, Double.toString(config.getSnrEnd()));
        properties.setProperty(KEY_SNR_STEP, Double.toString(config.getSnrStep()));
        properties.setProperty(KEY_BLOCKS, Integer.toString(config.getBlocks()));
        properties.setProperty(KEY_MAX_ITERATIONS, Integer.toString(config.getMaxIterations()));
        properties.setProperty(KEY_NORMALIZATION, Double.toString(config.getNormalization()));
        properties.setProperty(KEY_SEED, Integer.toString(config.getSeed()));
        properties.setProperty(KEY_MODULATION, config.getModulation());
        properties.setProperty(KEY_CHANNEL_MODEL, config.getChannelModel());
        properties.setProperty(KEY_LDPC_PROFILE, config.getLdpcProfile());
        properties.setProperty(KEY_WAVEFORM, config.getWaveform());
        properties.setProperty(KEY_SPATIAL_MODE, config.getSpatialMode());
        properties.setProperty(KEY_CYCLIC_PREFIX, Integer.toString(config.getCyclicPrefix()));
        properties.setProperty(KEY_EQUALIZER_MODE, config.getEqualizerMode());

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        try (OutputStream outputStream = Files.newOutputStream(path)) {
            properties.store(outputStream, "LDPC research simulation configuration");
        }
    }

    public SimulationConfig load(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        }

        SimulationConfig config = new SimulationConfig();
        config.setInfoBlockLength(readInt(properties, KEY_INFO_BLOCK_LENGTH));
        config.setSnrStart(readDouble(properties, KEY_SNR_START));
        config.setSnrEnd(readDouble(properties, KEY_SNR_END));
        config.setSnrStep(readDouble(properties, KEY_SNR_STEP));
        config.setBlocks(readInt(properties, KEY_BLOCKS));
        config.setMaxIterations(readInt(properties, KEY_MAX_ITERATIONS));
        config.setNormalization(readDouble(properties, KEY_NORMALIZATION));
        config.setSeed(readInt(properties, KEY_SEED));
        config.setModulation(require(properties, KEY_MODULATION));
        config.setChannelModel(require(properties, KEY_CHANNEL_MODEL));
        config.setLdpcProfile(require(properties, KEY_LDPC_PROFILE));
        config.setWaveform(require(properties, KEY_WAVEFORM));
        config.setSpatialMode(require(properties, KEY_SPATIAL_MODE));
        config.setCyclicPrefix(readInt(properties, KEY_CYCLIC_PREFIX));
        config.setEqualizerMode(require(properties, KEY_EQUALIZER_MODE));
        config.validate();
        return config;
    }

    public String buildSuggestedFileName() {
        return "ldpc_research_config.properties";
    }

    private int readInt(Properties properties, String key) {
        String value = require(properties, key);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Некорректное целое значение параметра: " + key);
        }
    }

    private double readDouble(Properties properties, String key) {
        String value = require(properties, key);
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Некорректное вещественное значение параметра: " + key);
        }
    }

    private int readOptionalInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Некорректное целое значение параметра: " + key);
        }
    }

    private String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("В файле конфигурации отсутствует параметр: " + key);
        }
        return value;
    }
}
