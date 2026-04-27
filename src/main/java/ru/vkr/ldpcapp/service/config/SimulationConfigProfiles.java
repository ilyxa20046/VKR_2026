package ru.vkr.ldpcapp.service.config;

import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.List;

public class SimulationConfigProfiles {

    public SimulationConfig recommendedProfile() {
        return profile5gNrBg1Reference();
    }

    public SimulationConfig profileResearchReference() {
        return new SimulationConfig(
                240, 0.0, 8.0, 1.0, 80, 12, 0.85, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_OFDM64,
                SimulationConfig.SPATIAL_2X2,
                8,
                SimulationConfig.EQUALIZER_ZF
        );
    }

    public SimulationConfig profileAwgnBaseline() {
        return new SimulationConfig(
                240, 0.0, 8.0, 1.0, 80, 10, 0.85, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_SC,
                SimulationConfig.SPATIAL_SISO,
                0,
                SimulationConfig.EQUALIZER_NONE
        );
    }

    public SimulationConfig profileRayleighOfdmBaseline() {
        return new SimulationConfig(
                240, -2.0, 10.0, 1.0, 100, 12, 0.85, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_RAYLEIGH,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_OFDM64,
                SimulationConfig.SPATIAL_SISO,
                8,
                SimulationConfig.EQUALIZER_ZF
        );
    }

    public SimulationConfig profileDiversityRobust() {
        return new SimulationConfig(
                240, -2.0, 8.0, 1.0, 100, 14, 0.80, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_RAYLEIGH,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_OFDM64,
                SimulationConfig.SPATIAL_2X2,
                8,
                SimulationConfig.EQUALIZER_ZF
        );
    }

    public SimulationConfig profileHighRate16Qam() {
        return new SimulationConfig(
                240, 4.0, 16.0, 1.0, 100, 12, 0.85, 2025,
                SimulationConfig.MOD_16QAM,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_OFDM128,
                SimulationConfig.SPATIAL_SISO,
                16,
                SimulationConfig.EQUALIZER_ZF
        );
    }

    public SimulationConfig profileEducationalCalibration() {
        return new SimulationConfig(
                120, 0.0, 8.0, 1.0, 60, 10, 0.85, 2025,
                SimulationConfig.MOD_BPSK,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_EDU,
                SimulationConfig.WAVEFORM_SC,
                SimulationConfig.SPATIAL_SISO,
                0,
                SimulationConfig.EQUALIZER_NONE
        );
    }

    public SimulationConfig profileDeepFadeOfdm() {
        return new SimulationConfig(
                240, 0.0, 14.0, 1.0, 120, 14, 0.80, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_RAYLEIGH,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_OFDM128,
                SimulationConfig.SPATIAL_2X2,
                16,
                SimulationConfig.EQUALIZER_ZF
        );
    }

    public SimulationConfig profileDefenseAwgnReference() {
        return new SimulationConfig(
                240, 0.0, 8.0, 1.0, 80, 10, 0.85, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_SC,
                SimulationConfig.SPATIAL_SISO,
                0,
                SimulationConfig.EQUALIZER_NONE
        );
    }

    public SimulationConfig profileDefenseRayleighReference() {
        return new SimulationConfig(
                240, 0.0, 10.0, 1.0, 90, 12, 0.85, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_RAYLEIGH,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_OFDM64,
                SimulationConfig.SPATIAL_SISO,
                8,
                SimulationConfig.EQUALIZER_ZF
        );
    }

    public SimulationConfig profileDefenseDiversityGain() {
        return new SimulationConfig(
                240, 0.0, 10.0, 1.0, 90, 12, 0.85, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_RAYLEIGH,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_OFDM64,
                SimulationConfig.SPATIAL_2X2,
                8,
                SimulationConfig.EQUALIZER_ZF
        );
    }

    public SimulationConfig profileDefenseHighRateTradeoff() {
        return new SimulationConfig(
                240, 4.0, 16.0, 1.0, 100, 12, 0.85, 2025,
                SimulationConfig.MOD_16QAM,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_OFDM128,
                SimulationConfig.SPATIAL_SISO,
                16,
                SimulationConfig.EQUALIZER_ZF
        );
    }

    public SimulationConfig profileDefenseOfdmShowcase() {
        return new SimulationConfig(
                240, 0.0, 10.0, 1.0, 90, 12, 0.85, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_RAYLEIGH,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_OFDM128,
                SimulationConfig.SPATIAL_SISO,
                16,
                SimulationConfig.EQUALIZER_ZF
        );
    }

    public SimulationConfig profileDefenseLdpcVsPolarLdpcRef() {
        SimulationConfig config = new SimulationConfig(
                256, 0.0, 8.0, 1.0, 120, 14, 0.85, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_QC,
                SimulationConfig.WAVEFORM_SC,
                SimulationConfig.SPATIAL_SISO,
                0,
                SimulationConfig.EQUALIZER_NONE
        );
        config.setCrcEnabled(true);
        config.setCrcBits(SimulationConfig.CRC_16);
        config.setSegmentationEnabled(true);
        config.setRateMatchingEnabled(true);
        config.setTargetCodewordLength(192);
        config.setBlerCriterion(SimulationConfig.BLER_BY_CRC_FAIL);
        return config;
    }

    public SimulationConfig profileDefenseLdpcVsPolarPolarRef() {
        SimulationConfig config = profileDefenseLdpcVsPolarLdpcRef();
        config.setLdpcProfile(SimulationConfig.PROFILE_POLAR);
        config.setInfoBlockLength(
                SimulationConfigFactory.normalizeInfoBlockLength(
                        config.getInfoBlockLength(),
                        SimulationConfig.PROFILE_POLAR,
                        config.getLiftingSize()
                )
        );
        return config;
    }

    public SimulationConfig profile5gNrBg1Reference() {
        return new SimulationConfig(
                176, 0.0, 10.0, 1.0, 100, 18, 0.80, 2025,
                SimulationConfig.MOD_QPSK,
                SimulationConfig.CHANNEL_AWGN,
                SimulationConfig.PROFILE_5GNR_BG1,
                SimulationConfig.WAVEFORM_OFDM64,
                SimulationConfig.SPATIAL_SISO,
                8,
                SimulationConfig.EQUALIZER_ZF
        );
    }

    public List<String> researchProfileNames() {
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

    public List<String> defenseProfileNames() {
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

    public SimulationConfig byResearchProfileName(String name) {
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

    public SimulationConfig byDefenseProfileName(String name) {
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
}