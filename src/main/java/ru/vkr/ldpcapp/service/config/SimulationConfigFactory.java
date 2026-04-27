package ru.vkr.ldpcapp.service.config;

import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.ArrayList;
import java.util.List;

public final class SimulationConfigFactory {
    private SimulationConfigFactory() {}

    public static List<String> supportedModulations() { return List.of(
            SimulationConfig.MOD_BPSK, SimulationConfig.MOD_QPSK,
            SimulationConfig.MOD_16QAM, SimulationConfig.MOD_64QAM, SimulationConfig.MOD_256QAM
    ); }

    public static List<String> supportedChannels() { return List.of(SimulationConfig.CHANNEL_AWGN, SimulationConfig.CHANNEL_RAYLEIGH); }
    public static List<String> supportedLdpcProfiles() { return List.of(
            SimulationConfig.PROFILE_EDU, SimulationConfig.PROFILE_QC, SimulationConfig.PROFILE_5GNR_BG1, SimulationConfig.PROFILE_POLAR
    ); }
    public static List<String> supportedWaveforms() { return List.of(
            SimulationConfig.WAVEFORM_SC, SimulationConfig.WAVEFORM_OFDM64, SimulationConfig.WAVEFORM_OFDM128
    ); }
    public static List<String> supportedSpatialModes() { return List.of(SimulationConfig.SPATIAL_SISO, SimulationConfig.SPATIAL_2X2); }
    public static List<String> supportedEqualizerModes() { return List.of(SimulationConfig.EQUALIZER_NONE, SimulationConfig.EQUALIZER_ZF); }
    public static List<String> supportedBlerCriteria() { return List.of(SimulationConfig.BLER_BY_BIT_MISMATCH, SimulationConfig.BLER_BY_CRC_FAIL); }

    public static int normalizeCyclicPrefix(int cp, String waveform) {
        if (SimulationConfig.WAVEFORM_OFDM128.equals(waveform)) return Math.max(8, Math.min(32, cp));
        if (SimulationConfig.WAVEFORM_OFDM64.equals(waveform)) return Math.max(4, Math.min(16, cp));
        return 0;
    }

    public static int getProfileInfoWordLength(String profile, int liftingSize) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) {
            int z = (liftingSize == 8 || liftingSize == 16 || liftingSize == 32) ? liftingSize : 8;
            return 22 * z;
        }
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return 64;
        return SimulationConfig.PROFILE_QC.equals(profile) ? 48 : 12;
    }

    public static int normalizeInfoBlockLength(int value, String profile, int liftingSize) {
        int k = getProfileInfoWordLength(profile, liftingSize);
        int safeValue = Math.max(k, value);
        int blocks = (int) Math.ceil((double) safeValue / k);
        return Math.max(k, blocks * k);
    }

    public static List<Double> buildSnrPoints(SimulationConfig c) {
        double start = Math.min(c.getSnrStart(), c.getSnrEnd());
        double end = Math.max(c.getSnrStart(), c.getSnrEnd());
        List<Double> values = new ArrayList<>();
        for (double current = start; current <= end + 1e-9; current += c.getSnrStep()) {
            values.add(Math.round(current * 100.0) / 100.0);
        }
        return values;
    }

    public static int getSnrPointCount(SimulationConfig c) { return buildSnrPoints(c).size(); }
    public static int getExperimentBlockCount(SimulationConfig c) {
        int perSnr = c.isAdaptiveStopEnabled() ? c.getMaxBlocksPerSnr() : c.getBlocks();
        return getSnrPointCount(c) * perSnr;
    }
    public static int getEstimatedInformationBits(SimulationConfig c) { return getExperimentBlockCount(c) * c.getInfoBlockLength(); }
    public static int getCodeInfoLength(SimulationConfig c) { return getProfileInfoWordLength(c.getLdpcProfile(), c.getLiftingSize()); }

    public static double getCodeRate(SimulationConfig c) {
        return switch (c.getLdpcProfile()) {
            case SimulationConfig.PROFILE_EDU -> 12.0 / 24.0;
            case SimulationConfig.PROFILE_QC -> 48.0 / 96.0;
            case SimulationConfig.PROFILE_POLAR -> 64.0 / 128.0;
            case SimulationConfig.PROFILE_5GNR_BG1 -> 22.0 / 68.0;
            default -> 0.5;
        };
    }

    public static String getProfileFamily(String profile) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return "3GPP NR / Base Graph 1";
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return "Polar-like / SC";
        return SimulationConfig.PROFILE_QC.equals(profile) ? "QC-Inspired / 5G-like" : "Educational";
    }

    public static String getProfileDescription(String profile) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return "Base Graph 1 (BG1) для 5G NR-ориентированного исследования.";
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return "Упрощённый Polar-like профиль (N=128, K=64).";
        return SimulationConfig.PROFILE_QC.equals(profile)
                ? "Более длинный квазициркулянтный LDPC-профиль."
                : "Компактный учебный LDPC-код.";
    }

    public static int normalizeInfoBlockLength(int value, String profile) {
        int k = getProfileInfoWordLength(profile);
        int safeValue = Math.max(k, value);
        int blocks = (int) Math.ceil((double) safeValue / k);
        return Math.max(k, blocks * k);
    }

    public static int getProfileInfoWordLength(String profile) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return 176;
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return 64;
        return SimulationConfig.PROFILE_QC.equals(profile) ? 48 : 12;
    }

    public static String getProfileName(String profile) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return "5G NR BG1 (68x46)";
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return "Polar-like (128,64)";
        return SimulationConfig.PROFILE_QC.equals(profile) ? "QC-inspired LDPC (96,48)" : "Учебный LDPC (24,12)";
    }

    public static String getProfileDisplayName(String profile, int liftingSize) {
        if (!SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) {
            return getProfileName(profile);
        }
        int z = (liftingSize == 8 || liftingSize == 16 || liftingSize == 32) ? liftingSize : 8;
        int k = 22 * z;
        return "5G NR BG1 (68x46), Z=" + z + ", k=" + k;
    }

    public static String getProfileUiName(String profile) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return "5G NR LDPC BG1 (Z=8)";
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return "Polar-like (128,64) · Сравнение";
        return SimulationConfig.PROFILE_QC.equals(profile) ? "QC-LDPC (96,48) · Базовый" : "Учебный LDPC (24,12)";
    }

    

    public static String getWaveformDisplayName(String waveform) {
        return switch (waveform) {
            case SimulationConfig.WAVEFORM_SC -> "Однонесущая";
            case SimulationConfig.WAVEFORM_OFDM64 -> "OFDM-64";
            case SimulationConfig.WAVEFORM_OFDM128 -> "OFDM-128";
            default -> waveform;
        };
    }

    public static String getSpatialModeDisplayName(String spatialMode) {
        return switch (spatialMode) {
            case SimulationConfig.SPATIAL_SISO -> "SISO (1x1)";
            case SimulationConfig.SPATIAL_2X2 -> "Разнесение 2x2";
            default -> spatialMode;
        };
    }

    public static String getEqualizerDisplayName(String equalizerMode) {
        return switch (equalizerMode) {
            case SimulationConfig.EQUALIZER_NONE -> "Без эквализации";
            case SimulationConfig.EQUALIZER_ZF -> "Однотактный ZF";
            default -> equalizerMode;
        };
    }

    public static List<String> supportedSnrDomains() {
        return List.of(SimulationConfig.SNR_DOMAIN_EB_N0, SimulationConfig.SNR_DOMAIN_ES_N0);
    }

    public static List<String> supportedDecoderTypes() {
        return List.of(
                SimulationConfig.DECODER_SUM_PRODUCT,
                SimulationConfig.DECODER_MIN_SUM,
                SimulationConfig.DECODER_NMS
        );
    }

    public static int bitsPerSymbol(String modulation) {
        return switch (modulation) {
            case SimulationConfig.MOD_BPSK -> 1;
            case SimulationConfig.MOD_QPSK -> 2;
            case SimulationConfig.MOD_16QAM -> 4;
            case SimulationConfig.MOD_64QAM -> 6;
            case SimulationConfig.MOD_256QAM -> 8;
            default -> 1;
        };
    }

    public static double codeRateByProfile(String profile) {
        return switch (profile) {
            case SimulationConfig.PROFILE_EDU -> 12.0 / 24.0;
            case SimulationConfig.PROFILE_QC -> 48.0 / 96.0;
            case SimulationConfig.PROFILE_POLAR -> 64.0 / 128.0;
            case SimulationConfig.PROFILE_5GNR_BG1 -> 22.0 / 68.0;
            default -> 0.5;
        };
    }

    public static double toEbN0Db(
            double snrDb,
            String snrDomain,
            String modulation,
            String ldpcProfile
    ) {
        if (SimulationConfig.SNR_DOMAIN_EB_N0.equals(snrDomain)) {
            return snrDb;
        }
        double r = codeRateByProfile(ldpcProfile);
        double m = bitsPerSymbol(modulation);
        return snrDb - 10.0 * Math.log10(Math.max(1e-12, r * m));
    }

    public static double toEsN0Db(
            double snrDb,
            String snrDomain,
            String modulation,
            String ldpcProfile
    ) {
        if (SimulationConfig.SNR_DOMAIN_ES_N0.equals(snrDomain)) {
            return snrDb;
        }
        double r = codeRateByProfile(ldpcProfile);
        double m = bitsPerSymbol(modulation);
        return snrDb + 10.0 * Math.log10(Math.max(1e-12, r * m));
    }
}