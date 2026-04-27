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
                SimulationConfig.PROFILE_POLAR
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
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) {
            int z = (liftingSize == 8 || liftingSize == 16 || liftingSize == 32) ? liftingSize : 8;
            boolean bg2 = SimulationConfig.NR_BG2.equals(nrBaseGraph);
            int infoCols = bg2 ? 10 : 22; // BG2 vs BG1
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
        if (SimulationConfig.PROFILE_EDU.equals(profile)) return 12.0 / 24.0;
        if (SimulationConfig.PROFILE_QC.equals(profile)) return 48.0 / 96.0;
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return 64.0 / 128.0;
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return 22.0 / 68.0; // BG1 default
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
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return "3GPP NR / Base Graph";
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return "Polar-like / SC";
        return SimulationConfig.PROFILE_QC.equals(profile) ? "QC-Inspired / 5G-like" : "Educational";
    }

    public static String getProfileDescription(String profile) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) {
            return "5G NR-oriented profile with selectable BG1/BG2 for research scenarios.";
        }
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) {
            return "Simplified Polar-like profile (N=128, K=64).";
        }
        return SimulationConfig.PROFILE_QC.equals(profile)
                ? "Longer quasi-cyclic inspired LDPC profile."
                : "Compact educational LDPC profile.";
    }

    public static String getProfileName(String profile) {
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return "5G NR Base Graph (selectable)";
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return "Polar-like (128,64)";
        return SimulationConfig.PROFILE_QC.equals(profile) ? "QC-inspired LDPC (96,48)" : "Educational LDPC (24,12)";
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
        if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) return "5G NR LDPC (BG1/BG2 selectable)";
        if (SimulationConfig.PROFILE_POLAR.equals(profile)) return "Polar-like (128,64) · Comparison";
        return SimulationConfig.PROFILE_QC.equals(profile)
                ? "QC-LDPC (96,48) · Baseline"
                : "Educational LDPC (24,12)";
    }

    public static String getWaveformDisplayName(String waveform) {
        return switch (waveform) {
            case SimulationConfig.WAVEFORM_SC -> "Single-carrier";
            case SimulationConfig.WAVEFORM_OFDM64 -> "OFDM-64";
            case SimulationConfig.WAVEFORM_OFDM128 -> "OFDM-128";
            default -> waveform;
        };
    }

    public static String getSpatialModeDisplayName(String spatialMode) {
        return switch (spatialMode) {
            case SimulationConfig.SPATIAL_SISO -> "SISO (1x1)";
            case SimulationConfig.SPATIAL_2X2 -> "2x2 diversity";
            default -> spatialMode;
        };
    }

    public static String getEqualizerDisplayName(String equalizerMode) {
        return switch (equalizerMode) {
            case SimulationConfig.EQUALIZER_NONE -> "No equalization";
            case SimulationConfig.EQUALIZER_ZF -> "One-tap ZF";
            default -> equalizerMode;
        };
    }
}