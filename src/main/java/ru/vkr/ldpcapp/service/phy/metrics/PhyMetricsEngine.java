package ru.vkr.ldpcapp.service.phy.metrics;

import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.phy.modulation.ModulationEngine;

import java.util.Random;

public class PhyMetricsEngine {
    private static final double BASE_SYMBOL_RATE_MBAUD = 20.0;
    private static final double EPS = 1e-12;

    private final ModulationEngine modulationEngine = new ModulationEngine();

    public int[] randomBits(int length, Random random) {
        int[] bits = new int[length];
        for (int i = 0; i < length; i++) {
            bits[i] = random.nextBoolean() ? 1 : 0;
        }
        return bits;
    }

    // Старый метод оставлен для совместимости (предполагает, что snrDb уже Eb/N0)
    public double sigmaFromEbN0(double snrDb, String modulation, double codeRate) {
        int bitsPerSymbol = modulationEngine.buildConstellation(modulation).bitsPerSymbol();
        double safeRate = Math.max(EPS, codeRate);
        double ebN0Linear = Math.pow(10.0, snrDb / 10.0);
        return Math.sqrt(1.0 / (2.0 * bitsPerSymbol * safeRate * ebN0Linear));
    }

    // Новый единый вход: учитывает домен SNR из config (Eb/N0 или Es/N0)
    public double sigmaFromSnrDomain(double snrDb, SimulationConfig config, double codeRate) {
        double ebN0Db = toEbN0Db(
                snrDb,
                config.getSnrDomain(),
                config.getModulation(),
                codeRate
        );
        return sigmaFromEbN0(ebN0Db, config.getModulation(), codeRate);
    }

    public double toEbN0Db(double snrDb, String snrDomain, String modulation, double codeRate) {
        if (SimulationConfig.SNR_DOMAIN_EB_N0.equals(snrDomain)) {
            return snrDb;
        }

        if (SimulationConfig.SNR_DOMAIN_ES_N0.equals(snrDomain)) {
            int bitsPerSymbol = modulationEngine.buildConstellation(modulation).bitsPerSymbol();
            double safeRate = Math.max(EPS, codeRate);
            return snrDb - 10.0 * Math.log10(Math.max(EPS, bitsPerSymbol * safeRate));
        }

        // fallback: считаем как Eb/N0
        return snrDb;
    }

    public double estimateThroughputMbps(SimulationConfig config, double codeRate, double blerLdpc) {
        double bitsPerSymbol = modulationEngine.buildConstellation(config.getModulation()).bitsPerSymbol();
        double successFactor = Math.max(0.0, 1.0 - blerLdpc);
        return BASE_SYMBOL_RATE_MBAUD * bitsPerSymbol * codeRate * usefulFraction(config) * successFactor;
    }

    public double estimateSpectralEfficiency(SimulationConfig config, double codeRate, double blerLdpc) {
        double bitsPerSymbol = modulationEngine.buildConstellation(config.getModulation()).bitsPerSymbol();
        double successFactor = Math.max(0.0, 1.0 - blerLdpc);
        return bitsPerSymbol * codeRate * usefulFraction(config) * successFactor;
    }

    private double usefulFraction(SimulationConfig config) {
        if (SimulationConfig.WAVEFORM_OFDM128.equals(config.getWaveform())) {
            double cpPenalty = 128.0 / (128.0 + Math.max(0, config.getCyclicPrefix()));
            double eqFactor = SimulationConfig.EQUALIZER_ZF.equals(config.getEqualizerMode()) ? 0.95 : 0.85;
            return 0.93 * cpPenalty * eqFactor;
        }
        if (SimulationConfig.WAVEFORM_OFDM64.equals(config.getWaveform())) {
            double cpPenalty = 64.0 / (64.0 + Math.max(0, config.getCyclicPrefix()));
            double eqFactor = SimulationConfig.EQUALIZER_ZF.equals(config.getEqualizerMode()) ? 0.94 : 0.82;
            return 0.90 * cpPenalty * eqFactor;
        }
        return 1.0;
    }
}