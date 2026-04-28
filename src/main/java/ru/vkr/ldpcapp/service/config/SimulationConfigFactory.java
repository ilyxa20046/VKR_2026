package ru.vkr.ldpcapp.service.config;

import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.ArrayList;
import java.util.List;

public final class SimulationConfigFactory {

    private static final double EPS = 1e-12;

    private SimulationConfigFactory() {
    }

    public static List<String> supportedModulations() {
        return List.of(
                SimulationConfig.MOD_BPSK,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.MOD_16QAM,
                SimulationConfig.MOD_64QAM,
                SimulationConfig.MOD_256QAM
        );
    }

    public static List<String> supportedChannels() {
        return List.of(
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.CHANNEL_RAYLEIGH
        );
    }

    public static List<String> supportedLdpcProfiles() {
        return List.of(
                SimulationConfig.PROFILE_EDU,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.PROFILE_5GNR_BG1,
                SimulationConfig.PROFILE_POLAR,
                SimulationConfig.PROFILE_TURBO_LTE
        );
    }

    public static List<String> supportedWaveforms() {
        return List.of(
                SimulationConfig.WAVEFORM_SC,
                SimulationConfig.WAVEFORM_OFDM64,
                SimulationConfig.WAVEFORM_OFDM128
        );
    }

    public static List<String> supportedSpatialModes() {
        return List.of(
                SimulationConfig.SPATIAL_SISO,
                SimulationConfig.SPATIAL_2X2
        );
    }

    public static List<String> supportedEqualizerModes() {
        return List.of(
                SimulationConfig.EQUALIZER_NONE,
                SimulationConfig.EQUALIZER_ZF
        );
    }

    public static List<String> supportedBlerCriteria() {
        return List.of(
                SimulationConfig.BLER_BY_BIT_MISMATCH,
                SimulationConfig.BLER_BY_CRC_FAIL
        );
    }

    public static List<String> supportedSnrDomains() {
        return List.of(
                SimulationConfig.SNR_DOMAIN_EB_N0,
                SimulationConfig.SNR_DOMAIN_ES_N0
        );
    }

    public static List<String> supportedDecoderTypes() {
        return List.of(
                SimulationConfig.DECODER_SUM_PRODUCT,
                SimulationConfig.DECODER_MIN_SUM,
                SimulationConfig.DECODER_NMS
        );
    }

    public static int normalizeCyclicPrefix(int cp, String waveform) {
        if (SimulationConfig.WAVEFORM_OFDM128.equals(waveform)) {
            return Math.max(8, Math.min(32, cp));
        }
        if (SimulationConfig.WAVEFORM_OFDM64.equals(waveform)) {
            return Math.max(4, Math.min(16, cp));
        }
        return 0;
    }

    public static int bitsPerSymbol(String modulation) {
        if (SimulationConfig.MOD_BPSK.equals(modulation)) return 1;
        if (SimulationConfig.MOD_QPSK.equals(modulation)) return 2;
        if (SimulationConfig.MOD_16QAM.equals(modulation)) return 4;
        if (SimulationConfig.MOD_64QAM.equals(modulation)) return 6;
        if (SimulationConfig.MOD_256QAM.equals(modulation)) return 8;
        return 2;
    }
    public static int getCodeInfoLength(SimulationConfig c) {
        if (c == null) {
            return 0;
        }
        int z = c.getLiftingSize();
        String bg = c.getNrBaseGraph();
        return getProfileInfoWordLength(c.getLdpcProfile(), z, bg);
    }

    public static int getSnrPointCount(SimulationConfig c) {
        if (c == null) {
            return 0;
        }
        return buildSnrPoints(c).size();
    }

    public static int getExperimentBlockCount(SimulationConfig c) {
        if (c == null) {
            return 0;
        }
        int snrPoints = getSnrPointCount(c);
        int blocksPerSnr = c.isAdaptiveStopEnabled() ? c.getMaxBlocksPerSnr() : c.getBlocks();
        return Math.max(0, snrPoints * Math.max(0, blocksPerSnr));
    }

    public static long getEstimatedInformationBits(SimulationConfig c) {
        if (c == null) {
            return 0L;
        }
        return (long) getExperimentBlockCount(c) * (long) Math.max(0, c.getInfoBlockLength());
    }

    public static double getCodeRate(SimulationConfig c) {
        if (c == null) {
            return 0.5;
        }
        return getProfileCodeRate(c.getLdpcProfile(), c.getNrBaseGraph());
    }

    public static List<Double> buildSnrPoints(SimulationConfig c) {
        double start = Math.min(c.getSnrStart(), c.getSnrEnd());
        double end = Math.max(c.getSnrStart(), c.getSnrEnd());
        double step = Math.max(0.01, c.getSnrStep());

        List<Double> values = new ArrayList<>();
        for (double current = start; current <= end + 1e-9; current += step) {
            values.add(roundSnr(current));
        }

        if (values.isEmpty()) {
            values.add(roundSnr(start));
        }
        return values;
    }

    private static double roundSnr(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static int getProfileInfoWordLength(String profile, int liftingSize) {
        return getProfileInfoWordLength(profile, liftingSize, SimulationConfig.NR_BG_AUTO);
    }

    public static int getProfileInfoWordLength(String profile, int liftingSize, String nrBaseGraph) {
        if (SimulationConfig.PROFILE_TURBO_LTE.equals(profile)) {
            return 40; // LTE-like turbo transport segment
        }
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) {
            int z = (liftingSize == 8 || liftingSize == 16 || liftingSize == 32) ? liftingSize : 8;
            boolean bg2 = SimulationConfig.NR_BG2.equals(nrBaseGraph);
            int infoCols = bg2 ? 10 : 22;
            return infoCols * z;
        }
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) {
            return 64;
        }
        return SimulationConfig.PROFILE_QC.equals(profile) ? 48 : 12;
    }

    public static int normalizeInfoBlockLength(int value, String profile, int liftingSize) {
        return normalizeInfoBlockLength(value, profile, liftingSize, SimulationConfig.NR_BG_AUTO);
    }

    public static int normalizeInfoBlockLength(int value, String profile, int liftingSize, String nrBaseGraph) {
        int k = getProfileInfoWordLength(profile, liftingSize, nrBaseGraph);
        int safeValue = Math.max(k, value);
        int blocks = (int) Math.ceil((double) safeValue / k);
        return Math.max(k, blocks * k);
    }

    public static int getProfileInfoWordLength(String profile) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return 176;
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return 64;
        return SimulationConfig.PROFILE_QC.equals(profile) ? 48 : 12;
    }

    public static int normalizeInfoBlockLength(int value, String profile) {
        int k = getProfileInfoWordLength(profile);
        int safeValue = Math.max(k, value);
        int blocks = (int) Math.ceil((double) safeValue / k);
        return Math.max(k, blocks * k);
    }

    public static double getProfileCodeRate(String profile) {
        if (SimulationConfig.PROFILE_TURBO_LTE.equals(profile)) return 1.0 / 3.0;
        if (SimulationConfig.PROFILE_EDU.equals(profile)) return 12.0 / 24.0;
        if (SimulationConfig.PROFILE_QC.equals(profile)) return 48.0 / 96.0;
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return 64.0 / 128.0;
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return 22.0 / 68.0;
        return 0.5;
    }

    public static double getProfileCodeRate(String profile, String nrBaseGraph) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile) && SimulationConfig.NR_BG2.equals(nrBaseGraph)) {
            return 10.0 / 52.0; // BG2 minimal estimate for this model
        }
        return getProfileCodeRate(profile);
    }

    public static double toEbN0Db(double snrDb, String snrDomain, String modulation, String ldpcProfile) {
        return toEbN0Db(snrDb, snrDomain, modulation, ldpcProfile, SimulationConfig.NR_BG_AUTO);
    }

    public static double toEbN0Db(
            double snrDb,
            String snrDomain,
            String modulation,
            String ldpcProfile,
            String nrBaseGraph
    ) {
        if (SimulationConfig.SNR_DOMAIN_EB_N0.equals(snrDomain)) {
            return snrDb;
        }
        int m = bitsPerSymbol(modulation);
        double r = getProfileCodeRate(ldpcProfile, nrBaseGraph);
        return snrDb - 10.0 * Math.log10(Math.max(EPS, m * r));
    }

    public static String getProfileFamily(String profile) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return "3GPP NR / Базовый граф";
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return "Polar-подобный / SC";
        if (SimulationConfig.PROFILE_TURBO_LTE.equals(profile)) return "4G LTE / Turbo";
        return SimulationConfig.PROFILE_QC.equals(profile) ? "QC-подобный / 5G-like" : "Учебный";
    }

    public static String getProfileDescription(String profile) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) {
            return "Профиль, ориентированный на 5G NR, с выбором базового графа BG1/BG2 для исследовательских сценариев.";
        }
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) {
            return "Упрощенный Polar-подобный профиль (N=128, K=64).";
        }
        if (SimulationConfig.PROFILE_TURBO_LTE.equals(profile)) {
            return "Turbo-профиль, соответствующий подходу 4G LTE (R=1/3), для сравнения с 5G NR LDPC.";
        }
        return SimulationConfig.PROFILE_QC.equals(profile)
                ? "Удлиненный квазициклический LDPC-профиль."
                : "Компактный учебный LDPC-профиль.";
    }

    public static String getProfileName(String profile) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return "5G NR LDPC (выбрать BG1/BG2)";
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return "Polar-подобный (128,64)";
        if (SimulationConfig.PROFILE_TURBO_LTE.equals(profile)) return "Turbo LTE-подобный (R=1/3)";
        return SimulationConfig.PROFILE_QC.equals(profile) ? "QC-подобный LDPC (96,48)" : "Учебный LDPC (24,12)";
    }

    public static String getProfileDisplayName(String profile, int liftingSize) {
        return getProfileDisplayName(profile, liftingSize, SimulationConfig.NR_BG_AUTO);
    }

    public static String getProfileDisplayName(String profile, int liftingSize, String nrBaseGraph) {
        if (!SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) {
            return getProfileName(profile);
        }
        int z = (liftingSize == 8 || liftingSize == 16 || liftingSize == 32) ? liftingSize : 8;
        String bg = SimulationConfig.NR_BG2.equals(nrBaseGraph) ? "BG2" : "BG1";
        int k = getProfileInfoWordLength(profile, z, bg);
        return "5G NR " + bg + ", Z=" + z + ", k=" + k;
    }

    public static String getProfileUiName(String profile) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return "5G NR LDPC (выбрать BG1/BG2)";
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return "Polar-подобный (128,64) · Сравнение";
        if (SimulationConfig.PROFILE_TURBO_LTE.equals(profile)) return "4G LTE Turbo (R=1/3) · Базовый";
        return SimulationConfig.PROFILE_QC.equals(profile)
                ? "QC-LDPC (96,48) · Базовый"
                : "Учебный LDPC (24,12)";
    }
    public static List<Double> supportedRateComparisons() {
        return List.of(1.0 / 3.0, 1.0 / 2.0, 2.0 / 3.0, 5.0 / 6.0);
    }

    public static int computeTargetCodewordLengthForRate(SimulationConfig c, double targetRate) {
        int k = getProfileInfoWordLength(c.getLdpcProfile(), c.getLiftingSize(), c.getNrBaseGraph());
        double safeRate = Math.max(0.05, Math.min(0.99, targetRate));
        int e = (int) Math.ceil(k / safeRate);

        // Удобно выравнивать под byte boundary
        if ((e & 7) != 0) {
            e = ((e + 7) / 8) * 8;
        }
        return Math.max(k, e);
    }

    public static double getEffectiveCodeRate(SimulationConfig c) {
        int k = getProfileInfoWordLength(c.getLdpcProfile(), c.getLiftingSize(), c.getNrBaseGraph());
        if (c.isRateMatchingEnabled() && c.getTargetCodewordLength() > 0) {
            double r = (double) k / c.getTargetCodewordLength();
            return Math.max(0.05, Math.min(0.99, r));
        }
        return getProfileCodeRate(c.getLdpcProfile(), c.getNrBaseGraph());
    }

    public static String formatRate(double rate) {
        return String.format(java.util.Locale.US, "%.3f", rate);
    }

    public static String getMcsLikeLabel(SimulationConfig c) {
        return c.getModulation() + " @ R=" + formatRate(getEffectiveCodeRate(c));
    }

    public static String getWaveformUiName(String waveform) {
        return switch (waveform) {
            case SimulationConfig.WAVEFORM_SC -> "Однонесущая";
            case SimulationConfig.WAVEFORM_OFDM64 -> "OFDM-64";
            case SimulationConfig.WAVEFORM_OFDM128 -> "OFDM-128";
            default -> waveform;
        };
    }
    public static String getDecoderTypeUiName(String decoderType) {
        return switch (decoderType) {
            case SimulationConfig.DECODER_SUM_PRODUCT -> "Сумма-произведение";
            case SimulationConfig.DECODER_MIN_SUM -> "минимум-сумма";
            case SimulationConfig.DECODER_NMS -> "Нормализованная минимум-сумма";
            default -> decoderType;
        };
    }
    public static String getNrBaseGraphUiName(String bg) {
        return switch (bg) {
            case SimulationConfig.NR_BG_AUTO -> "Автоматический выбор";
            case SimulationConfig.NR_BG1 -> "BG1";
            case SimulationConfig.NR_BG2 -> "BG2";
            default -> bg;
        };
    }

    public static String getBlerCriterionUiName(String criterion) {
        return switch (criterion) {
            case SimulationConfig.BLER_BY_BIT_MISMATCH -> "Несовпадение битов";
            case SimulationConfig.BLER_BY_CRC_FAIL -> "Ошибка CRC";
            default -> criterion;
        };
    }

    public static String getModulationUiName(String modulation) {
        return switch (modulation) {
            case SimulationConfig.MOD_BPSK -> "BPSK";
            case SimulationConfig.MOD_QPSK -> "QPSK";
            case SimulationConfig.MOD_16QAM -> "16-QAM";
            case SimulationConfig.MOD_64QAM -> "64-QAM";
            case SimulationConfig.MOD_256QAM -> "256-QAM";
            default -> modulation;
        };
    }

    public static String getChannelUiName(String channel) {
        return switch (channel) {
            case SimulationConfig.CHANNEL_AWGN -> "AWGN";
            case SimulationConfig.CHANNEL_RAYLEIGH -> "Рэлеевский канал";
            default -> channel;
        };
    }
    public static String getEqualizerUiName(String equalizerMode) {
        return switch (equalizerMode) {
            case SimulationConfig.EQUALIZER_NONE -> "Без эквализации";
            case SimulationConfig.EQUALIZER_ZF -> "Однотактный ZF";
            default -> equalizerMode;
        };
    }
    public static String getSpatialModeUiName(String spatialMode) {
        return switch (spatialMode) {
            case SimulationConfig.SPATIAL_SISO -> "SISO";
            case SimulationConfig.SPATIAL_2X2 -> "2x2 (разнесение)";
            default -> spatialMode;
        };
    }

    public static String getSnrDomainUiName(String domain) {
        return switch (domain) {
            case SimulationConfig.SNR_DOMAIN_EB_N0 -> "Eb/N0";
            case SimulationConfig.SNR_DOMAIN_ES_N0 -> "Es/N0";
            default -> domain;
        };
    }

    public static String getCrcBitsUiName(Integer bits) {
        if (bits == null) return "";
        return bits == 0 ? "Без CRC" : "CRC-" + bits;
    }
}