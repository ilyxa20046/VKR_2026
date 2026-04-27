package ru.vkr.ldpcapp.service.config;

import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.List;

public class SimulationConfigProfiles {

    public SimulationConfig recommendedProfile() {
        return profileResearchReference();
    }

    public SimulationConfig profileResearchReference() {
        SimulationConfig c = base(
                352, 0.0, 12.0, 1.0, 220, 18, 0.80, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_5GNR_BG1,
                SimulationConfig.WAVEFORM_OFDM64,
                SimulationConfig.SPATIAL_SISO,
                8,
                SimulationConfig.EQUALIZER_ZF
        );
        applyNrChain(c, SimulationConfig.NR_BG1, 8, true, true, true, 384, SimulationConfig.BLER_BY_CRC_FAIL);
        applyAdvanced(c, SimulationConfig.SNR_DOMAIN_EB_N0, SimulationConfig.DECODER_NMS, true, 100, 8000, 0.95);
        applyHarq(c, false, 0);
        normalizeInfo(c);
        return c;
    }

    public SimulationConfig profileAwgnBaseline() {
        SimulationConfig c = base(
                352, -2.0, 14.0, 1.0, 220, 16, 0.80, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_5GNR_BG1,
                SimulationConfig.WAVEFORM_SC,
                SimulationConfig.SPATIAL_SISO,
                0,
                SimulationConfig.EQUALIZER_NONE
        );
        applyNrChain(c, SimulationConfig.NR_BG1, 8, true, true, false, 0, SimulationConfig.BLER_BY_CRC_FAIL);
        applyAdvanced(c, SimulationConfig.SNR_DOMAIN_EB_N0, SimulationConfig.DECODER_NMS, true, 100, 7000, 0.95);
        applyHarq(c, false, 0);
        normalizeInfo(c);
        return c;
    }

    public SimulationConfig profileRayleighOfdmBaseline() {
        SimulationConfig c = base(
                352, 0.0, 16.0, 1.0, 240, 18, 0.80, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_RAYLEIGH,
                SimulationConfig.PROFILE_5GNR_BG1,
                SimulationConfig.WAVEFORM_OFDM64,
                SimulationConfig.SPATIAL_SISO,
                8,
                SimulationConfig.EQUALIZER_ZF
        );
        applyNrChain(c, SimulationConfig.NR_BG1, 8, true, true, true, 384, SimulationConfig.BLER_BY_CRC_FAIL);
        applyAdvanced(c, SimulationConfig.SNR_DOMAIN_EB_N0, SimulationConfig.DECODER_NMS, true, 120, 9000, 0.95);
        applyHarq(c, false, 0);
        normalizeInfo(c);
        return c;
    }

    public SimulationConfig profileDiversityRobust() {
        SimulationConfig c = base(
                352, 0.0, 16.0, 1.0, 240, 20, 0.78, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_RAYLEIGH,
                SimulationConfig.PROFILE_5GNR_BG1,
                SimulationConfig.WAVEFORM_OFDM64,
                SimulationConfig.SPATIAL_2X2,
                8,
                SimulationConfig.EQUALIZER_ZF
        );
        applyNrChain(c, SimulationConfig.NR_BG1, 8, true, true, true, 384, SimulationConfig.BLER_BY_CRC_FAIL);
        applyAdvanced(c, SimulationConfig.SNR_DOMAIN_EB_N0, SimulationConfig.DECODER_NMS, true, 140, 10000, 0.95);
        applyHarq(c, true, 2);
        normalizeInfo(c);
        return c;
    }

    public SimulationConfig profileHighRate16Qam() {
        SimulationConfig c = base(
                352, 4.0, 20.0, 1.0, 260, 20, 0.80, 2025,
                SimulationConfig.MOD_16QAM,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_5GNR_BG1,
                SimulationConfig.WAVEFORM_OFDM128,
                SimulationConfig.SPATIAL_SISO,
                16,
                SimulationConfig.EQUALIZER_ZF
        );
        applyNrChain(c, SimulationConfig.NR_BG1, 16, true, true, true, 768, SimulationConfig.BLER_BY_CRC_FAIL);
        applyAdvanced(c, SimulationConfig.SNR_DOMAIN_EB_N0, SimulationConfig.DECODER_NMS, true, 140, 10000, 0.95);
        applyHarq(c, false, 0);
        normalizeInfo(c);
        return c;
    }

    public SimulationConfig profileEducationalCalibration() {
        SimulationConfig c = base(
                120, 0.0, 10.0, 1.0, 150, 10, 0.85, 2025,
                SimulationConfig.MOD_BPSK,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_EDU,
                SimulationConfig.WAVEFORM_SC,
                SimulationConfig.SPATIAL_SISO,
                0,
                SimulationConfig.EQUALIZER_NONE
        );
        applyNrChain(c, SimulationConfig.NR_BG_AUTO, 8, true, true, false, 0, SimulationConfig.BLER_BY_BIT_MISMATCH);
        applyAdvanced(c, SimulationConfig.SNR_DOMAIN_EB_N0, SimulationConfig.DECODER_MIN_SUM, true, 80, 5000, 0.95);
        applyHarq(c, false, 0);
        return c;
    }

    public SimulationConfig profileDeepFadeOfdm() {
        SimulationConfig c = base(
                240, 0.0, 20.0, 1.0, 280, 22, 0.78, 2025,
                SimulationConfig.MOD_256QAM,
                SimulationConfig.CHANNEL_RAYLEIGH,
                SimulationConfig.PROFILE_5GNR_BG1,
                SimulationConfig.WAVEFORM_OFDM128,
                SimulationConfig.SPATIAL_2X2,
                8,
                SimulationConfig.EQUALIZER_ZF
        );
        applyNrChain(c, SimulationConfig.NR_BG2, 8, true, true, false, 0, SimulationConfig.BLER_BY_CRC_FAIL);
        applyAdvanced(c, SimulationConfig.SNR_DOMAIN_EB_N0, SimulationConfig.DECODER_MIN_SUM, true, 160, 12000, 0.95);
        applyHarq(c, true, 3);
        normalizeInfo(c);
        return c;
    }

    public SimulationConfig profileDefenseAwgnReference() {
        return profileAwgnBaseline();
    }

    public SimulationConfig profileDefenseRayleighReference() {
        return profileRayleighOfdmBaseline();
    }

    public SimulationConfig profileDefenseDiversityGain() {
        return profileDiversityRobust();
    }

    public SimulationConfig profileDefenseHighRateTradeoff() {
        SimulationConfig c = profileHighRate16Qam();
        c.setModulation(SimulationConfig.MOD_16QAM);
        c.setChannelModel(SimulationConfig.CHANNEL_RAYLEIGH);
        c.setWaveform(SimulationConfig.WAVEFORM_OFDM128);
        c.setSpatialMode(SimulationConfig.SPATIAL_2X2);
        c.setSnrStart(4.0);
        c.setSnrEnd(20.0);
        c.setHarqEnabled(true);
        c.setHarqMaxRetx(2);
        return c;
    }

    public SimulationConfig profileDefense256QamStress() {
        SimulationConfig c = profileDeepFadeOfdm(); // здесь уже 256-QAM тяжёлый сценарий
        c.setModulation(SimulationConfig.MOD_256QAM);
        c.setChannelModel(SimulationConfig.CHANNEL_RAYLEIGH);
        c.setWaveform(SimulationConfig.WAVEFORM_OFDM128);
        c.setSpatialMode(SimulationConfig.SPATIAL_2X2);
        c.setSnrStart(8.0);
        c.setSnrEnd(24.0);
        c.setHarqEnabled(true);
        c.setHarqMaxRetx(3);
        return c;
    }

    public SimulationConfig profileDefenseOfdmShowcase() {
        SimulationConfig c = profileRayleighOfdmBaseline();
        c.setWaveform(SimulationConfig.WAVEFORM_OFDM128);
        c.setCyclicPrefix(8);
        c.setModulation(SimulationConfig.MOD_16QAM);
        c.setSpatialMode(SimulationConfig.SPATIAL_2X2);
        c.setSnrStart(2.0);
        c.setSnrEnd(18.0);
        return c;
    }

    public SimulationConfig profileDefenseLdpcVsPolarLdpcRef() {
        SimulationConfig c = base(
                256, 0.0, 12.0, 1.0, 220, 18, 0.80, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_SC,
                SimulationConfig.SPATIAL_SISO,
                0,
                SimulationConfig.EQUALIZER_NONE
        );
        applyNrChain(c, SimulationConfig.NR_BG_AUTO, 8, true, true, false, 0, SimulationConfig.BLER_BY_CRC_FAIL);
        applyAdvanced(c, SimulationConfig.SNR_DOMAIN_EB_N0, SimulationConfig.DECODER_NMS, true, 100, 8000, 0.95);
        applyHarq(c, false, 0);
        return c;
    }

    public SimulationConfig profileDefenseLdpcVsPolarPolarRef() {
        SimulationConfig c = profileDefenseLdpcVsPolarLdpcRef();
        c.setLdpcProfile(SimulationConfig.PROFILE_POLAR);
        c.setInfoBlockLength(256);
        c.setDecoderType(SimulationConfig.DECODER_SUM_PRODUCT);
        return c;
    }

    public SimulationConfig profile5gNrBg1Reference() {
        return profileResearchReference();
    }

    public List<String> researchProfileNames() {
        return List.of(
                "Опорный 5G-like сценарий",
                "AWGN baseline / SISO",
                "Rayleigh + OFDM baseline",
                "Diversity-устойчивый профиль",
                "Высокоскоростной 16-QAM",
                "Учебная калибровка",
                "Глубокий fading + OFDM"
        );
    }

    public List<String> defenseProfileNames() {
        return List.of(
                "Защита · Базовый AWGN сценарий",
                "Защита · Rayleigh vs OFDM",
                "Защита · Diversity gain",
                "Защита · 16-QAM trade-off",
                "Защита · 256-QAM stress",
                "Защита · OFDM showcase",
                "Защита · LDPC vs Polar · LDPC ref",
                "Защита · LDPC vs Polar · Polar ref"
        );
    }

    public SimulationConfig byResearchProfileName(String name) {
        if (name == null || name.isBlank()) {
            return recommendedProfile();
        }
        return switch (name) {
            case "AWGN baseline / SISO" -> profileAwgnBaseline();
            case "Rayleigh + OFDM baseline" -> profileRayleighOfdmBaseline();
            case "Diversity-устойчивый профиль" -> profileDiversityRobust();
            case "Высокоскоростной 16-QAM" -> profileHighRate16Qam();
            case "Учебная калибровка" -> profileEducationalCalibration();
            case "Глубокий fading + OFDM" -> profileDeepFadeOfdm();
            default -> profileResearchReference();
        };
    }

    public SimulationConfig byDefenseProfileName(String name) {
        if (name == null || name.isBlank()) {
            return profileDefenseAwgnReference();
        }
        return switch (name) {
            case "Защита · Rayleigh vs OFDM" -> profileDefenseRayleighReference();
            case "Защита · Diversity gain" -> profileDefenseDiversityGain();
            case "Защита · 16-QAM trade-off" -> profileDefenseHighRateTradeoff();
            case "Защита · 256-QAM stress" -> profileDefense256QamStress();
            case "Защита · OFDM showcase" -> profileDefenseOfdmShowcase();
            case "Защита · LDPC vs Polar · LDPC ref" -> profileDefenseLdpcVsPolarLdpcRef();
            case "Защита · LDPC vs Polar · Polar ref" -> profileDefenseLdpcVsPolarPolarRef();
            default -> profileDefenseAwgnReference();
        };
    }

    private SimulationConfig base(
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
        return new SimulationConfig(
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
    }

    private void applyNrChain(
            SimulationConfig c,
            String baseGraph,
            int liftingSize,
            boolean crcEnabled,
            boolean segmentationEnabled,
            boolean rateMatchingEnabled,
            int targetCodewordLength,
            String blerCriterion
    ) {
        c.setNrBaseGraph(baseGraph);
        c.setLiftingSize(liftingSize);
        c.setCrcEnabled(crcEnabled);
        c.setCrcBits(crcEnabled ? SimulationConfig.CRC_16 : SimulationConfig.CRC_NONE);
        c.setSegmentationEnabled(segmentationEnabled);
        c.setRateMatchingEnabled(rateMatchingEnabled);
        c.setTargetCodewordLength(rateMatchingEnabled ? Math.max(0, targetCodewordLength) : 0);
        c.setBlerCriterion(blerCriterion);
    }

    private void applyAdvanced(
            SimulationConfig c,
            String snrDomain,
            String decoderType,
            boolean adaptiveStopEnabled,
            int minErrorEventsPerSnr,
            int maxBlocksPerSnr,
            double confidenceLevel
    ) {
        c.setSnrDomain(snrDomain);
        c.setDecoderType(decoderType);
        c.setAdaptiveStopEnabled(adaptiveStopEnabled);
        c.setMinErrorEventsPerSnr(minErrorEventsPerSnr);
        c.setMaxBlocksPerSnr(maxBlocksPerSnr);
        c.setConfidenceLevel(confidenceLevel);
    }

    private void applyHarq(SimulationConfig c, boolean enabled, int maxRetx) {
        c.setHarqEnabled(enabled);
        c.setHarqMaxRetx(maxRetx);
    }

    private void normalizeInfo(SimulationConfig c) {
        c.setInfoBlockLength(
                SimulationConfigFactory.normalizeInfoBlockLength(
                        c.getInfoBlockLength(),
                        c.getLdpcProfile(),
                        c.getLiftingSize(),
                        c.getNrBaseGraph()
                )
        );
    }
}