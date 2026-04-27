package ru.vkr.ldpcapp.service.phy.metrics;

import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.phy.modulation.ModulationEngine;

import java.util.Random;

public class PhyMetricsEngine {
    private static final double BASE_SYMBOL_RATE_MBAUD = 20.0;
    private final ModulationEngine modulationEngine = new ModulationEngine();

    public int[] randomBits(int length, Random random) {
        int[] bits = new int[length];
        for (int i = 0; i < length; i++) {
            bits[i] = random.nextBoolean() ? 1 : 0;
        }
        return bits;
    }

    public double sigmaFromEbN0(double snrDb, String modulation, double codeRate) {
        double ebN0 = Math.pow(10.0, snrDb / 10.0);
        int bitsPerSymbol = modulationEngine.buildConstellation(modulation).bitsPerSymbol();
        return Math.sqrt(1.0 / (2.0 * bitsPerSymbol * codeRate * ebN0));
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