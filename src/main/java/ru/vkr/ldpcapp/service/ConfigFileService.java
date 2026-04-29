package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.config.SimulationConfigValidator;

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
    private static final String KEY_BLER_CRITERION = "blerCriterion";

    private static final String KEY_ADAPTIVE_STOP_ENABLED = "adaptiveStopEnabled";
    private static final String KEY_MIN_ERROR_EVENTS_PER_SNR = "minErrorEventsPerSnr";
    private static final String KEY_MAX_BLOCKS_PER_SNR = "maxBlocksPerSnr";
    private static final String KEY_CONFIDENCE_LEVEL = "confidenceLevel";
    private static final String KEY_SNR_DOMAIN = "snrDomain";
    private static final String KEY_DECODER_TYPE = "decoderType";
    private static final String KEY_NR_BASE_GRAPH = "nrBaseGraph";
    private static final String KEY_LIFTING_SIZE = "liftingSize";
    private static final String KEY_CRC_ENABLED = "crcEnabled";
    private static final String KEY_CRC_BITS = "crcBits";
    private static final String KEY_SEGMENTATION_ENABLED = "segmentationEnabled";
    private static final String KEY_RATE_MATCHING_ENABLED = "rateMatchingEnabled";
    private static final String KEY_TARGET_CODEWORD_LENGTH = "targetCodewordLength";
    private static final String KEY_HARQ_ENABLED = "harqEnabled";
    private static final String KEY_HARQ_MAX_RETX = "harqMaxRetx";

    private final SimulationConfigValidator configValidator = new SimulationConfigValidator();

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
        properties.setProperty(KEY_BLER_CRITERION, config.getBlerCriterion());

        properties.setProperty(KEY_ADAPTIVE_STOP_ENABLED, Boolean.toString(config.isAdaptiveStopEnabled()));
        properties.setProperty(KEY_MIN_ERROR_EVENTS_PER_SNR, Integer.toString(config.getMinErrorEventsPerSnr()));
        properties.setProperty(KEY_MAX_BLOCKS_PER_SNR, Integer.toString(config.getMaxBlocksPerSnr()));
        properties.setProperty(KEY_CONFIDENCE_LEVEL, Double.toString(config.getConfidenceLevel()));
        properties.setProperty(KEY_SNR_DOMAIN, config.getSnrDomain());
        properties.setProperty(KEY_DECODER_TYPE, config.getDecoderType());
        properties.setProperty(KEY_NR_BASE_GRAPH, config.getNrBaseGraph());
        properties.setProperty(KEY_LIFTING_SIZE, Integer.toString(config.getLiftingSize()));
        properties.setProperty(KEY_CRC_ENABLED, Boolean.toString(config.isCrcEnabled()));
        properties.setProperty(KEY_CRC_BITS, Integer.toString(config.getCrcBits()));
        properties.setProperty(KEY_SEGMENTATION_ENABLED, Boolean.toString(config.isSegmentationEnabled()));
        properties.setProperty(KEY_RATE_MATCHING_ENABLED, Boolean.toString(config.isRateMatchingEnabled()));
        properties.setProperty(KEY_TARGET_CODEWORD_LENGTH, Integer.toString(config.getTargetCodewordLength()));
        properties.setProperty(KEY_HARQ_ENABLED, Boolean.toString(config.isHarqEnabled()));
        properties.setProperty(KEY_HARQ_MAX_RETX, Integer.toString(config.getHarqMaxRetx()));

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
        config.setBlerCriterion(properties.getProperty(KEY_BLER_CRITERION, SimulationConfig.BLER_BY_BIT_MISMATCH));

        config.setAdaptiveStopEnabled(
                readOptionalBoolean(properties, KEY_ADAPTIVE_STOP_ENABLED, SimulationConfig.DEFAULT_ADAPTIVE_STOP_ENABLED)
        );
        config.setMinErrorEventsPerSnr(
                readOptionalInt(properties, KEY_MIN_ERROR_EVENTS_PER_SNR, SimulationConfig.DEFAULT_MIN_ERROR_EVENTS_PER_SNR)
        );
        config.setMaxBlocksPerSnr(
                readOptionalInt(properties, KEY_MAX_BLOCKS_PER_SNR, SimulationConfig.DEFAULT_MAX_BLOCKS_PER_SNR)
        );
        config.setConfidenceLevel(
                readOptionalDouble(properties, KEY_CONFIDENCE_LEVEL, SimulationConfig.DEFAULT_CONFIDENCE_LEVEL)
        );
        config.setSnrDomain(readOptionalString(properties, KEY_SNR_DOMAIN, SimulationConfig.SNR_DOMAIN_EB_N0));
        config.setDecoderType(readOptionalString(properties, KEY_DECODER_TYPE, SimulationConfig.DECODER_NMS));
        config.setNrBaseGraph(readOptionalString(properties, KEY_NR_BASE_GRAPH, SimulationConfig.NR_BG_AUTO));
        config.setLiftingSize(readOptionalInt(properties, KEY_LIFTING_SIZE, 8));
        config.setCrcEnabled(readOptionalBoolean(properties, KEY_CRC_ENABLED, false));
        config.setCrcBits(readOptionalInt(properties, KEY_CRC_BITS, SimulationConfig.CRC_NONE));

        boolean hasSegmentationKey = properties.containsKey(KEY_SEGMENTATION_ENABLED);
        config.setSegmentationEnabled(readOptionalBoolean(properties, KEY_SEGMENTATION_ENABLED, false));

        config.setRateMatchingEnabled(readOptionalBoolean(properties, KEY_RATE_MATCHING_ENABLED, false));
        config.setTargetCodewordLength(readOptionalInt(properties, KEY_TARGET_CODEWORD_LENGTH, 0));
        config.setHarqEnabled(readOptionalBoolean(properties, KEY_HARQ_ENABLED, SimulationConfig.DEFAULT_HARQ_ENABLED));
        config.setHarqMaxRetx(readOptionalInt(properties, KEY_HARQ_MAX_RETX, SimulationConfig.DEFAULT_HARQ_MAX_RETX));

        // Backward compatibility for old files:
        // if segmentation flag was absent and TB does not fit one codeword, enable segmentation automatically.
        if (!hasSegmentationKey) {
            int tbBits = config.getInfoBlockLength() + config.getCrcBits();
            int codewordK = ru.vkr.ldpcapp.service.config.SimulationConfigFactory.getCodeInfoLength(config);
            if (tbBits > codewordK) {
                config.setSegmentationEnabled(true);
            }
        }

        configValidator.validate(config);
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
            return Double.parseDouble(value.trim().replace(',', '.'));
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

    private double readOptionalDouble(Properties properties, String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Некорректное вещественное значение параметра: " + key);
        }
    }

    private boolean readOptionalBoolean(Properties properties, String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private String readOptionalString(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("В файле конфигурации отсутствует параметр: " + key);
        }
        return value;
    }
}