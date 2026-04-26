package ru.vkr.ldpcapp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SimulationConfig {

    public static final String MOD_BPSK = "BPSK";
    public static final String MOD_QPSK = "QPSK";
    public static final String MOD_16QAM = "16-QAM";
    public static final String MOD_64QAM = "64-QAM";
    public static final String MOD_256QAM = "256-QAM";

    public static final String CHANNEL_AWGN = "AWGN";
    public static final String CHANNEL_RAYLEIGH = "Rayleigh";

    public static final String PROFILE_EDU = "edu-24-12";
    public static final String PROFILE_QC = "qcldpc-96-48";
    public static final String PROFILE_POLAR = "polar-128-64";
    public static final String PROFILE_5GNR_BG1 = "5gnr-bg1-z8";

    public static final String WAVEFORM_SC = "Single-carrier";
    public static final String WAVEFORM_OFDM64 = "OFDM-64";
    public static final String WAVEFORM_OFDM128 = "OFDM-128";

    public static final String SPATIAL_SISO = "SISO";
    public static final String SPATIAL_2X2 = "2x2 Diversity";

    public static final String NR_BG_AUTO = "AUTO";
    public static final String NR_BG1 = "BG1";
    public static final String NR_BG2 = "BG2";

    public static final int CRC_NONE = 0;
    public static final int CRC_16 = 16;
    public static final String BLER_BY_BIT_MISMATCH = "Bit mismatch";
    public static final String BLER_BY_CRC_FAIL = "CRC fail";

    public static final String EQUALIZER_NONE = "None";
    public static final String EQUALIZER_ZF = "One-tap ZF";
    public static final boolean DEFAULT_ADAPTIVE_STOP_ENABLED = true;
    public static final int DEFAULT_MIN_ERROR_EVENTS_PER_SNR = 50;
    public static final int DEFAULT_MAX_BLOCKS_PER_SNR = 2000;
    public static final double DEFAULT_CONFIDENCE_LEVEL = 0.95;

    private int infoBlockLength;
    private double snrStart;
    private double snrEnd;
    private double snrStep;
    private int blocks;
    private int maxIterations;
    private double normalization;
    private int seed;
    private String modulation;
    private String channelModel;
    private String ldpcProfile;
    private String waveform;
    private String spatialMode;
    private int cyclicPrefix;
    private String equalizerMode;
    private boolean adaptiveStopEnabled;
    private int minErrorEventsPerSnr;
    private int maxBlocksPerSnr;
    private double confidenceLevel;

    private String nrBaseGraph = NR_BG_AUTO;
    private int liftingSize = 8;
    private boolean crcEnabled = false;
    private int crcBits = CRC_NONE;
    private boolean segmentationEnabled = false;
    private boolean rateMatchingEnabled = false;
    private int targetCodewordLength = 0; // 0 => использовать n кодового слова
    private String blerCriterion = BLER_BY_BIT_MISMATCH;

    public SimulationConfig() {
    }

    public SimulationConfig(
            int infoBlockLength,
            double snrStart,
            double snrEnd,
            double snrStep,
            int blocks,
            int maxIterations,
            double normalization,
            int seed,
            String modulation,
            String channelModel,
            String ldpcProfile,
            String waveform,
            String spatialMode,
            int cyclicPrefix,
            String equalizerMode
    ) {
        this.infoBlockLength = infoBlockLength;
        this.snrStart = snrStart;
        this.snrEnd = snrEnd;
        this.snrStep = snrStep;
        this.blocks = blocks;
        this.maxIterations = maxIterations;
        this.normalization = normalization;
        this.seed = seed;
        this.modulation = modulation;
        this.channelModel = channelModel;
        this.ldpcProfile = ldpcProfile;
        this.waveform = waveform;
        this.spatialMode = spatialMode;
        this.cyclicPrefix = cyclicPrefix;
        this.equalizerMode = equalizerMode;
        this.adaptiveStopEnabled = DEFAULT_ADAPTIVE_STOP_ENABLED;
        this.minErrorEventsPerSnr = DEFAULT_MIN_ERROR_EVENTS_PER_SNR;
        this.maxBlocksPerSnr = DEFAULT_MAX_BLOCKS_PER_SNR;
        this.confidenceLevel = DEFAULT_CONFIDENCE_LEVEL;
    }
    public SimulationConfig(
            int infoBlockLength,
            double snrStart,
            double snrEnd,
            double snrStep,
            int blocks,
            int maxIterations,
            double normalization,
            int seed,
            String modulation,
            String channelModel,
            String ldpcProfile,
            String waveform,
            String spatialMode,
            int cyclicPrefix,
            String equalizerMode,
            boolean adaptiveStopEnabled,
            int minErrorEventsPerSnr,
            int maxBlocksPerSnr,
            double confidenceLevel
    ) {
        this(
                infoBlockLength,
                snrStart,
                snrEnd,
                snrStep,
                blocks,
                maxIterations,
                normalization,
                seed,
                modulation,
                channelModel,
                ldpcProfile,
                waveform,
                spatialMode,
                cyclicPrefix,
                equalizerMode
        );
        this.adaptiveStopEnabled = adaptiveStopEnabled;
        this.minErrorEventsPerSnr = minErrorEventsPerSnr;
        this.maxBlocksPerSnr = maxBlocksPerSnr;
        this.confidenceLevel = confidenceLevel;
    }

    public static SimulationConfig recommendedProfile() {
        return profile5gNrBg1Reference();
    }



    public static SimulationConfig profileResearchReference() {
        return new SimulationConfig(
                240,
                0.0,
                8.0,
                1.0,
                80,
                12,
                0.85,
                2025,
                MOD_QPSK,
                CHANNEL_AWGN,
                PROFILE_QC,
                WAVEFORM_OFDM64,
                SPATIAL_2X2,
                8,
                EQUALIZER_ZF
        );
    }

    public static SimulationConfig profileAwgnBaseline() {
        return new SimulationConfig(
                240,
                0.0,
                8.0,
                1.0,
                80,
                10,
                0.85,
                2025,
                MOD_QPSK,
                CHANNEL_AWGN,
                PROFILE_QC,
                WAVEFORM_SC,
                SPATIAL_SISO,
                0,
                EQUALIZER_NONE
        );
    }

    public static SimulationConfig profileRayleighOfdmBaseline() {
        return new SimulationConfig(
                240,
                -2.0,
                10.0,
                1.0,
                100,
                12,
                0.85,
                2025,
                MOD_QPSK,
                CHANNEL_RAYLEIGH,
                PROFILE_QC,
                WAVEFORM_OFDM64,
                SPATIAL_SISO,
                8,
                EQUALIZER_ZF
        );
    }

    public static SimulationConfig profileDiversityRobust() {
        return new SimulationConfig(
                240,
                -2.0,
                8.0,
                1.0,
                100,
                14,
                0.80,
                2025,
                MOD_QPSK,
                CHANNEL_RAYLEIGH,
                PROFILE_QC,
                WAVEFORM_OFDM64,
                SPATIAL_2X2,
                8,
                EQUALIZER_ZF
        );
    }

    public static SimulationConfig profileHighRate16Qam() {
        return new SimulationConfig(
                240,
                4.0,
                16.0,
                1.0,
                100,
                12,
                0.85,
                2025,
                MOD_16QAM,
                CHANNEL_AWGN,
                PROFILE_QC,
                WAVEFORM_OFDM128,
                SPATIAL_SISO,
                16,
                EQUALIZER_ZF
        );
    }

    public static SimulationConfig profileEducationalCalibration() {
        return new SimulationConfig(
                120,
                0.0,
                8.0,
                1.0,
                60,
                10,
                0.85,
                2025,
                MOD_BPSK,
                CHANNEL_AWGN,
                PROFILE_EDU,
                WAVEFORM_SC,
                SPATIAL_SISO,
                0,
                EQUALIZER_NONE
        );
    }

    public static SimulationConfig profileDeepFadeOfdm() {
        return new SimulationConfig(
                240,
                0.0,
                14.0,
                1.0,
                120,
                14,
                0.80,
                2025,
                MOD_QPSK,
                CHANNEL_RAYLEIGH,
                PROFILE_QC,
                WAVEFORM_OFDM128,
                SPATIAL_2X2,
                16,
                EQUALIZER_ZF
        );
    }

    public static SimulationConfig profileDefenseAwgnReference() {
        return new SimulationConfig(
                240,
                0.0,
                8.0,
                1.0,
                80,
                10,
                0.85,
                2025,
                MOD_QPSK,
                CHANNEL_AWGN,
                PROFILE_QC,
                WAVEFORM_SC,
                SPATIAL_SISO,
                0,
                EQUALIZER_NONE
        );
    }

    public static SimulationConfig profileDefenseRayleighReference() {
        return new SimulationConfig(
                240,
                0.0,
                10.0,
                1.0,
                90,
                12,
                0.85,
                2025,
                MOD_QPSK,
                CHANNEL_RAYLEIGH,
                PROFILE_QC,
                WAVEFORM_OFDM64,
                SPATIAL_SISO,
                8,
                EQUALIZER_ZF
        );
    }

    public static SimulationConfig profileDefenseDiversityGain() {
        return new SimulationConfig(
                240,
                0.0,
                10.0,
                1.0,
                90,
                12,
                0.85,
                2025,
                MOD_QPSK,
                CHANNEL_RAYLEIGH,
                PROFILE_QC,
                WAVEFORM_OFDM64,
                SPATIAL_2X2,
                8,
                EQUALIZER_ZF
        );
    }

    public static SimulationConfig profileDefenseHighRateTradeoff() {
        return new SimulationConfig(
                240,
                4.0,
                16.0,
                1.0,
                100,
                12,
                0.85,
                2025,
                MOD_16QAM,
                CHANNEL_AWGN,
                PROFILE_QC,
                WAVEFORM_OFDM128,
                SPATIAL_SISO,
                16,
                EQUALIZER_ZF
        );
    }

    public static SimulationConfig profileDefenseOfdmShowcase() {
        return new SimulationConfig(
                240,
                0.0,
                10.0,
                1.0,
                90,
                12,
                0.85,
                2025,
                MOD_QPSK,
                CHANNEL_RAYLEIGH,
                PROFILE_QC,
                WAVEFORM_OFDM128,
                SPATIAL_SISO,
                16,
                EQUALIZER_ZF
        );
    }

    public static SimulationConfig profileDefenseLdpcVsPolarLdpcRef() {
        SimulationConfig config = new SimulationConfig(
                256,
                0.0,
                8.0,
                1.0,
                120,
                14,
                0.85,
                2025,
                MOD_QPSK,
                CHANNEL_AWGN,
                PROFILE_QC,
                WAVEFORM_SC,
                SPATIAL_SISO,
                0,
                EQUALIZER_NONE
        );
        config.setCrcEnabled(true);
        config.setCrcBits(CRC_16);
        config.setSegmentationEnabled(true);
        config.setRateMatchingEnabled(true);
        config.setTargetCodewordLength(192);
        config.setBlerCriterion(BLER_BY_CRC_FAIL);
        return config;
    }

    public static SimulationConfig profileDefenseLdpcVsPolarPolarRef() {
        SimulationConfig config = profileDefenseLdpcVsPolarLdpcRef();
        config.setLdpcProfile(PROFILE_POLAR);
        config.setInfoBlockLength(normalizeInfoBlockLength(config.getInfoBlockLength(), PROFILE_POLAR, config.getLiftingSize()));
        return config;
    }

    public static List<String> supportedModulations() {
        return List.of(MOD_BPSK, MOD_QPSK, MOD_16QAM, MOD_64QAM, MOD_256QAM);
    }

    public static List<String> supportedChannels() {
        return List.of(CHANNEL_AWGN, CHANNEL_RAYLEIGH);
    }

    public static List<String> supportedLdpcProfiles() {
        return List.of(PROFILE_EDU, PROFILE_QC, PROFILE_5GNR_BG1, PROFILE_POLAR);
    }

    public static List<String> supportedWaveforms() {
        return List.of(WAVEFORM_SC, WAVEFORM_OFDM64, WAVEFORM_OFDM128);
    }

    public static List<String> supportedSpatialModes() {
        return List.of(SPATIAL_SISO, SPATIAL_2X2);
    }

    public static List<String> supportedEqualizerModes() {
        return List.of(EQUALIZER_NONE, EQUALIZER_ZF);
    }

    public static List<String> supportedBlerCriteria() {
        return List.of(BLER_BY_BIT_MISMATCH, BLER_BY_CRC_FAIL);
    }

    public static List<String> researchProfileNames() {
        return List.of(
                "Опорный 5G-like сценарий",
                "5G NR BG1 (Z=8) — опорный",
                "AWGN baseline / SISO",
                "Rayleigh + OFDM baseline",
                "Diversity-устойчивый профиль",
                "Высокоскоростной 16-QAM",
                "Учебная калибровка",
                "Глубокий fading + OFDM"
        );
    }

    public static List<String> defenseProfileNames() {
        return List.of(
                "Защита · Базовый AWGN сценарий",
                "Защита · Rayleigh vs OFDM",
                "Защита · Diversity gain",
                "Защита · 16-QAM trade-off",
                "Защита · OFDM showcase",
                "Защита · LDPC vs Polar · LDPC ref",
                "Защита · LDPC vs Polar · Polar ref"
        );
    }

    public static SimulationConfig byResearchProfileName(String name) {
        if (name == null || name.isBlank()) {
            return recommendedProfile();
        }
        return switch (name) {
            case "5G NR BG1 (Z=8) — опорный" -> profile5gNrBg1Reference();
            case "AWGN baseline / SISO" -> profileAwgnBaseline();
            case "Rayleigh + OFDM baseline" -> profileRayleighOfdmBaseline();
            case "Diversity-устойчивый профиль" -> profileDiversityRobust();
            case "Высокоскоростной 16-QAM" -> profileHighRate16Qam();
            case "Учебная калибровка" -> profileEducationalCalibration();
            case "Глубокий fading + OFDM" -> profileDeepFadeOfdm();
            default -> profileResearchReference();
        };
    }

    public static SimulationConfig byDefenseProfileName(String name) {
        if (name == null || name.isBlank()) {
            return profileDefenseAwgnReference();
        }
        return switch (name) {
            case "Защита · Rayleigh vs OFDM" -> profileDefenseRayleighReference();
            case "Защита · Diversity gain" -> profileDefenseDiversityGain();
            case "Защита · 16-QAM trade-off" -> profileDefenseHighRateTradeoff();
            case "Защита · OFDM showcase" -> profileDefenseOfdmShowcase();
            case "Защита · LDPC vs Polar · LDPC ref" -> profileDefenseLdpcVsPolarLdpcRef();
            case "Защита · LDPC vs Polar · Polar ref" -> profileDefenseLdpcVsPolarPolarRef();
            default -> profileDefenseAwgnReference();
        };
    }


    public static int normalizeCyclicPrefix(int cp, String waveform) {
        if (WAVEFORM_OFDM128.equals(waveform)) {
            return Math.max(8, Math.min(32, cp));
        }
        if (WAVEFORM_OFDM64.equals(waveform)) {
            return Math.max(4, Math.min(16, cp));
        }
        return 0;
    }

    public static int getProfileInfoWordLength(String profile) {
        if (PROFILE_5GNR_BG1.equals(profile)) {
            return 176; // legacy default: BG1, 22 info cols * Z=8
        }
        if (PROFILE_POLAR.equals(profile)) {
            return 64;
        }
        return PROFILE_QC.equals(profile) ? 48 : 12;
    }

    public static int getProfileInfoWordLength(String profile, int liftingSize) {
        if (PROFILE_5GNR_BG1.equals(profile)) {
            int z = liftingSize;
            if (z != 8 && z != 16 && z != 32) {
                z = 8;
            }
            return 22 * z;
        }
        return getProfileInfoWordLength(profile);
    }

    public static int normalizeInfoBlockLength(int value, String profile) {
        int k = getProfileInfoWordLength(profile);
        int safeValue = Math.max(k, value);
        int blocks = (int) Math.ceil((double) safeValue / k);
        return Math.max(k, blocks * k);
    }

    public static int normalizeInfoBlockLength(int value, String profile, int liftingSize) {
        int k = getProfileInfoWordLength(profile, liftingSize);
        int safeValue = Math.max(k, value);
        int blocks = (int) Math.ceil((double) safeValue / k);
        return Math.max(k, blocks * k);
    }

    public static String getProfileName(String profile) {
        if (PROFILE_5GNR_BG1.equals(profile)) {
            return "5G NR BG1 (68x46)";
        }
        if (PROFILE_POLAR.equals(profile)) {
            return "Polar-like (128,64)";
        }
        return PROFILE_QC.equals(profile)
                ? "QC-inspired LDPC (96,48)"
                : "Учебный LDPC (24,12)";
    }

    public static String getProfileDisplayName(String profile, int liftingSize) {
        if (!PROFILE_5GNR_BG1.equals(profile)) {
            return getProfileName(profile);
        }
        int z = liftingSize;
        if (z != 8 && z != 16 && z != 32) {
            z = 8;
        }
        int k = 22 * z;
        return "5G NR BG1 (68x46), Z=" + z + ", k=" + k;
    }

    public static String getProfileFamily(String profile) {
        if (PROFILE_5GNR_BG1.equals(profile)) {
            return "3GPP NR / Base Graph 1";
        }
        if (PROFILE_POLAR.equals(profile)) {
            return "Polar-like / SC";
        }
        return PROFILE_QC.equals(profile) ? "QC-Inspired / 5G-like" : "Educational";
    }

    public static String getProfileDescription(String profile) {
        if (PROFILE_5GNR_BG1.equals(profile)) {
            return "Base Graph 1 (BG1) для 5G NR-ориентированного исследования (lifting size задаётся параметром Z).";
        }
        if (PROFILE_POLAR.equals(profile)) {
            return "Упрощённый Polar-like профиль (N=128, K=64) со SC-декодированием для сравнения с LDPC-подходом.";
        }
        return PROFILE_QC.equals(profile)
                ? "Более длинный квазициркулянтный LDPC-профиль, ближе по духу к 5G NR, чем учебный компактный код."
                : "Компактный учебный LDPC-код, удобный для быстрой калибровки и базового сравнения.";
    }

    public static SimulationConfig profile5gNrBg1Reference() {
        return new SimulationConfig(
                176,   // кратно 176
                0.0, 10.0, 1.0,
                100,
                18,
                0.80,
                2025,
                MOD_QPSK,
                CHANNEL_AWGN,
                PROFILE_5GNR_BG1,
                WAVEFORM_OFDM64,
                SPATIAL_SISO,
                8,
                EQUALIZER_ZF
        );
    }

    public static String getWaveformDisplayName(String waveform) {
        return switch (waveform) {
            case WAVEFORM_SC -> "Однонесущая";
            case WAVEFORM_OFDM64 -> "OFDM-64";
            case WAVEFORM_OFDM128 -> "OFDM-128";
            default -> waveform;
        };
    }

    public static String getSpatialModeDisplayName(String spatialMode) {
        return switch (spatialMode) {
            case SPATIAL_SISO -> "SISO (1x1)";
            case SPATIAL_2X2 -> "Разнесение 2x2";
            default -> spatialMode;
        };
    }

    public static String getEqualizerDisplayName(String equalizerMode) {
        return switch (equalizerMode) {
            case EQUALIZER_NONE -> "Без эквализации";
            case EQUALIZER_ZF -> "Однотактный ZF";
            default -> equalizerMode;
        };
    }

    public void validate() {
        if (!supportedModulations().contains(modulation)) {
            throw new IllegalArgumentException("Выбрана неподдерживаемая модуляция.");
        }
        if (!supportedChannels().contains(channelModel)) {
            throw new IllegalArgumentException("Выбран неподдерживаемый тип канала.");
        }
        if (!supportedLdpcProfiles().contains(ldpcProfile)) {
            throw new IllegalArgumentException("Выбран неподдерживаемый профиль LDPC.");
        }
        if (!supportedWaveforms().contains(waveform)) {
            throw new IllegalArgumentException("Выбран неподдерживаемый тип waveform/OFDM-представления.");
        }
        if (!supportedSpatialModes().contains(spatialMode)) {
            throw new IllegalArgumentException("Выбран неподдерживаемый пространственный режим.");
        }
        if (!supportedEqualizerModes().contains(equalizerMode)) {
            throw new IllegalArgumentException("Выбран неподдерживаемый режим эквализации.");
        }
        int normalizedCp = normalizeCyclicPrefix(cyclicPrefix, waveform);
        if (cyclicPrefix != normalizedCp) {
            throw new IllegalArgumentException("Длина cyclic prefix не соответствует выбранному waveform/OFDM-режиму.");
        }
        int infoWordLength = getProfileInfoWordLength(ldpcProfile, liftingSize);
        if (infoBlockLength < infoWordLength || infoBlockLength % infoWordLength != 0) {
            throw new IllegalArgumentException("Длина информационного блока должна быть кратна " + infoWordLength + " для выбранного LDPC-профиля.");
        }
        if (snrStep <= 0.0) {
            throw new IllegalArgumentException("Шаг SNR должен быть положительным.");
        }
        if (blocks < 20) {
            throw new IllegalArgumentException("Количество блоков должно быть не меньше 20.");
        }
        if (maxIterations < 2) {
            throw new IllegalArgumentException("Число итераций декодера должно быть не меньше 2.");
        }
        if (normalization < 0.5 || normalization > 1.0) {
            throw new IllegalArgumentException("Коэффициент normalized min-sum должен находиться в диапазоне от 0.50 до 1.00.");
        }
        if (seed < 1) {
            throw new IllegalArgumentException("Seed генератора должен быть положительным.");
        }
        if (minErrorEventsPerSnr < 1) {
            throw new IllegalArgumentException("Минимальное число событий ошибок на точку SNR должно быть не меньше 1.");
        }
        if (maxBlocksPerSnr < blocks) {
            throw new IllegalArgumentException("Лимит блоков на точку SNR должен быть не меньше базового количества блоков.");
        }
        if (confidenceLevel <= 0.0 || confidenceLevel >= 1.0) {
            throw new IllegalArgumentException("Уровень доверия должен находиться в интервале (0; 1).");
        }
        if (adaptiveStopEnabled && maxBlocksPerSnr < 20) {
            throw new IllegalArgumentException("Для адаптивного режима max blocks per SNR должен быть не меньше 20.");
        }
        if (!List.of(NR_BG_AUTO, NR_BG1, NR_BG2).contains(nrBaseGraph)) {
            throw new IllegalArgumentException("Неверный выбор base graph: " + nrBaseGraph);
        }
        if (liftingSize != 8 && liftingSize != 16 && liftingSize != 32) {
            throw new IllegalArgumentException("Lifting size должен быть 8, 16 или 32.");
        }
        if (crcEnabled && crcBits != CRC_16) {
            throw new IllegalArgumentException("Сейчас поддерживается только CRC-16.");
        }
        if (!crcEnabled && crcBits != CRC_NONE) {
            throw new IllegalArgumentException("Если CRC выключен, crcBits должен быть 0.");
        }
        if (targetCodewordLength < 0) {
            throw new IllegalArgumentException("targetCodewordLength не может быть отрицательным.");
        }
        if (rateMatchingEnabled && targetCodewordLength == 0) {
            throw new IllegalArgumentException("Для rate matching задайте targetCodewordLength > 0.");
        }
        if (!supportedBlerCriteria().contains(blerCriterion)) {
            throw new IllegalArgumentException("Неподдерживаемый критерий BLER: " + blerCriterion);
        }
        if (BLER_BY_CRC_FAIL.equals(blerCriterion) && !crcEnabled) {
            throw new IllegalArgumentException("Критерий BLER по CRC доступен только при включенном CRC.");
        }
        if (!segmentationEnabled && (infoBlockLength + crcBits) > getCodeInfoLength()) {
            throw new IllegalArgumentException(
                    "При выключенной segmentation транспортный блок должен помещаться в одно кодовое слово (K="
                            + getCodeInfoLength() + "). Уменьшите infoBlockLength или включите segmentation."
            );
        }
    }

    public List<Double> buildSnrPoints() {
        double start = Math.min(snrStart, snrEnd);
        double end = Math.max(snrStart, snrEnd);
        List<Double> values = new ArrayList<>();

        for (double current = start; current <= end + 1e-9; current += snrStep) {
            values.add(roundTwoDecimals(current));
        }
        return values;
    }

    public int getSnrPointCount() {
        return buildSnrPoints().size();
    }

    public int getExperimentBlockCount() {
        int perSnr = adaptiveStopEnabled ? maxBlocksPerSnr : blocks;
        return getSnrPointCount() * perSnr;
    }

    public int getEstimatedInformationBits() {
        return getExperimentBlockCount() * infoBlockLength;
    }

    public double getCodeRate() {
        return 0.5;
    }

    public int getCodeInfoLength() {
        return getProfileInfoWordLength(ldpcProfile, liftingSize);
    }

    public String toSummaryText() {
        return String.format(
                Locale.US,
                "Конфигурация эксперимента:%n" +
                        "• профиль LDPC: %s%n" +
                        "• семейство кода: %s%n" +
                        "• модуляция: %s%n" +
                        "• канал: %s%n" +
                        "• waveform: %s%n" +
                        "• spatial mode: %s%n" +
                        "• cyclic prefix: %d%n" +
                        "• equalizer: %s%n" +
                        "• информационный блок: %d бит%n" +
                        "• кодовое слово: %d информационных бит на одно слово%n" +
                        "• диапазон SNR: %.2f ... %.2f дБ с шагом %.2f дБ%n" +
                        "• число точек SNR: %d%n" +
                        "• блоков на точку: %d%n" +
                        "• общий объём моделирования: %d блоков%n" +
                        "• оценочное число информационных бит: %d%n" +
                        "• декодер: normalized min-sum, максимум %d итераций%n" +
                        "• коэффициент нормализации: %.2f%n" +
                        "• скорость кода: %.2f%n" +
                        "• описание профиля: %s%n" +
                        "• seed генератора: %d%n" +
                        "• адаптивная остановка: %s%n" +
                        "• min error events per SNR: %d%n" +
                        "• max blocks per SNR: %d%n" +
                        "• confidence level: %.2f%n" +
                        "• base graph (NR): %s%n" +
                        "• lifting size Z: %d%n" +
                        "• CRC: %s%n" +
                        "• segmentation: %s%n" +
                        "• rate matching: %s%n" +
                        "• BLER criterion: %s%n" +
                        "• target codeword length (E): %d%n",
                getProfileDisplayName(ldpcProfile, liftingSize),
                getProfileFamily(ldpcProfile),
                modulation,
                channelModel,
                waveform,
                spatialMode,
                cyclicPrefix,
                equalizerMode,
                infoBlockLength,
                getCodeInfoLength(),
                Math.min(snrStart, snrEnd),
                Math.max(snrStart, snrEnd),
                snrStep,
                getSnrPointCount(),
                blocks,
                getExperimentBlockCount(),
                getEstimatedInformationBits(),
                maxIterations,
                normalization,
                getCodeRate(),
                getProfileDescription(ldpcProfile),
                seed,
                adaptiveStopEnabled ? "включена" : "выключена",
                minErrorEventsPerSnr,
                maxBlocksPerSnr,
                confidenceLevel,
                nrBaseGraph,
                liftingSize,
                crcEnabled ? ("ON (" + crcBits + " bits)") : "OFF",
                segmentationEnabled ? "ON" : "OFF",
                rateMatchingEnabled ? "ON" : "OFF",
                blerCriterion,
                targetCodewordLength
        );
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public int getInfoBlockLength() {
        return infoBlockLength;
    }

    public void setInfoBlockLength(int infoBlockLength) {
        this.infoBlockLength = infoBlockLength;
    }

    public double getSnrStart() {
        return snrStart;
    }

    public void setSnrStart(double snrStart) {
        this.snrStart = snrStart;
    }

    public double getSnrEnd() {
        return snrEnd;
    }

    public void setSnrEnd(double snrEnd) {
        this.snrEnd = snrEnd;
    }

    public double getSnrStep() {
        return snrStep;
    }

    public void setSnrStep(double snrStep) {
        this.snrStep = snrStep;
    }

    public int getBlocks() {
        return blocks;
    }

    public void setBlocks(int blocks) {
        this.blocks = blocks;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public double getNormalization() {
        return normalization;
    }

    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public String getModulation() {
        return modulation;
    }

    public void setModulation(String modulation) {
        this.modulation = modulation;
    }

    public String getChannelModel() {
        return channelModel;
    }

    public void setChannelModel(String channelModel) {
        this.channelModel = channelModel;
    }

    public String getLdpcProfile() {
        return ldpcProfile;
    }

    public void setLdpcProfile(String ldpcProfile) {
        this.ldpcProfile = ldpcProfile;
    }

    public String getWaveform() {
        return waveform;
    }

    public void setWaveform(String waveform) {
        this.waveform = waveform;
    }

    public String getSpatialMode() {
        return spatialMode;
    }

    public void setSpatialMode(String spatialMode) {
        this.spatialMode = spatialMode;
    }

    public int getCyclicPrefix() {
        return cyclicPrefix;
    }

    public void setCyclicPrefix(int cyclicPrefix) {
        this.cyclicPrefix = cyclicPrefix;
    }

    public String getEqualizerMode() {
        return equalizerMode;
    }

    public void setEqualizerMode(String equalizerMode) {
        this.equalizerMode = equalizerMode;
    }
    public boolean isAdaptiveStopEnabled() {
        return adaptiveStopEnabled;
    }

    public void setAdaptiveStopEnabled(boolean adaptiveStopEnabled) {
        this.adaptiveStopEnabled = adaptiveStopEnabled;
    }

    public int getMinErrorEventsPerSnr() {
        return minErrorEventsPerSnr;
    }

    public void setMinErrorEventsPerSnr(int minErrorEventsPerSnr) {
        this.minErrorEventsPerSnr = minErrorEventsPerSnr;
    }

    public int getMaxBlocksPerSnr() {
        return maxBlocksPerSnr;
    }

    public void setMaxBlocksPerSnr(int maxBlocksPerSnr) {
        this.maxBlocksPerSnr = maxBlocksPerSnr;
    }

    public double getConfidenceLevel() {
        return confidenceLevel;
    }

    public String getNrBaseGraph() {
        return nrBaseGraph;
    }

    public void setNrBaseGraph(String nrBaseGraph) {
        this.nrBaseGraph = nrBaseGraph;
    }

    public int getLiftingSize() {
        return liftingSize;
    }

    public void setLiftingSize(int liftingSize) {
        this.liftingSize = liftingSize;
    }

    public boolean isCrcEnabled() {
        return crcEnabled;
    }

    public void setCrcEnabled(boolean crcEnabled) {
        this.crcEnabled = crcEnabled;
    }

    public int getCrcBits() {
        return crcBits;
    }

    public void setCrcBits(int crcBits) {
        this.crcBits = crcBits;
    }

    public boolean isSegmentationEnabled() {
        return segmentationEnabled;
    }

    public void setSegmentationEnabled(boolean segmentationEnabled) {
        this.segmentationEnabled = segmentationEnabled;
    }

    public boolean isRateMatchingEnabled() {
        return rateMatchingEnabled;
    }

    public void setRateMatchingEnabled(boolean rateMatchingEnabled) {
        this.rateMatchingEnabled = rateMatchingEnabled;
    }

    public int getTargetCodewordLength() {
        return targetCodewordLength;
    }

    public void setTargetCodewordLength(int targetCodewordLength) {
        this.targetCodewordLength = targetCodewordLength;
    }
    public String getBlerCriterion() {
        return blerCriterion;
    }

    public void setBlerCriterion(String blerCriterion) {
        this.blerCriterion = blerCriterion;
    }
    public void setConfidenceLevel(double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }
}
