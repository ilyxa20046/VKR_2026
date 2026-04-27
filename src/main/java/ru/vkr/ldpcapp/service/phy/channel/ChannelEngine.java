package ru.vkr.ldpcapp.service.phy.channel;

import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.phy.math.Complex;
import ru.vkr.ldpcapp.service.phy.modulation.ModulationEngine;

import java.util.Random;

public class ChannelEngine {
    private static final double INV_SQRT2 = 1.0 / Math.sqrt(2.0);
    private final ModulationEngine modulationEngine = new ModulationEngine();

    public Transmission transmitBits(int[] bits, SimulationConfig config, double sigma, Random random) {
        ModulationEngine.Constellation constellation = modulationEngine.buildConstellation(config.getModulation());
        int bitsPerSymbol = constellation.bitsPerSymbol();
        int symbolCount = (int) Math.ceil((double) bits.length / bitsPerSymbol);

        Complex[][] received = new Complex[symbolCount][];
        Complex[][] gains = new Complex[symbolCount][];

        WaveformProfile waveformProfile = getWaveformProfile(config);
        int branches = getSpatialBranches(config.getSpatialMode());
        Complex[] blockGains = createChannelGains(config.getChannelModel(), branches, random);
        int remainingSpan = 0;

        for (int symbolIndex = 0; symbolIndex < symbolCount; symbolIndex++) {
            if (SimulationConfig.CHANNEL_RAYLEIGH.equals(config.getChannelModel())
                    && SimulationConfig.WAVEFORM_SC.equals(config.getWaveform())
                    && remainingSpan <= 0) {
                blockGains = createChannelGains(config.getChannelModel(), branches, random);
                remainingSpan = waveformProfile.fadingSpan;
            }

            int from = symbolIndex * bitsPerSymbol;
            int to = Math.min(bits.length, from + bitsPerSymbol);
            int[] symbolBits = new int[bitsPerSymbol];
            System.arraycopy(bits, from, symbolBits, 0, to - from);
            Complex symbol = constellation.lookup(symbolBits);

            Complex[] rxBranches = new Complex[branches];
            Complex[] hBranches = new Complex[branches];

            for (int branch = 0; branch < branches; branch++) {
                Complex h = SimulationConfig.CHANNEL_AWGN.equals(config.getChannelModel())
                        ? new Complex(1.0, 0.0)
                        : SimulationConfig.WAVEFORM_SC.equals(config.getWaveform())
                        ? blockGains[branch]
                        : createOfdmSubcarrierGain(waveformProfile.taps, random);

                Complex noise = new Complex(sigma * random.nextGaussian(), sigma * random.nextGaussian());
                rxBranches[branch] = Complex.multiply(h, symbol).add(noise);
                hBranches[branch] = h;
            }

            if (SimulationConfig.CHANNEL_RAYLEIGH.equals(config.getChannelModel())
                    && SimulationConfig.WAVEFORM_SC.equals(config.getWaveform())) {
                remainingSpan--;
            }

            received[symbolIndex] = rxBranches;
            gains[symbolIndex] = hBranches;
        }

        return new Transmission(received, gains);
    }

    public double[] demapToLlr(Transmission transmission, double sigma, SimulationConfig config) {
        ModulationEngine.Constellation constellation = modulationEngine.buildConstellation(config.getModulation());
        double noiseVariance = 2.0 * sigma * sigma;
        double[] llr = new double[transmission.received.length * constellation.bitsPerSymbol()];
        int llrIndex = 0;

        boolean useOneTapEqualizer = !SimulationConfig.WAVEFORM_SC.equals(config.getWaveform())
                && SimulationConfig.EQUALIZER_ZF.equals(config.getEqualizerMode());

        for (int symbolIndex = 0; symbolIndex < transmission.received.length; symbolIndex++) {
            Complex[] yBranches = transmission.received[symbolIndex];
            Complex[] hBranches = transmission.gains[symbolIndex];

            for (int bitIndex = 0; bitIndex < constellation.bitsPerSymbol(); bitIndex++) {
                double min0 = Double.POSITIVE_INFINITY;
                double min1 = Double.POSITIVE_INFINITY;

                for (ModulationEngine.ConstellationEntry entry : constellation.entries()) {
                    double distance = 0.0;

                    for (int branch = 0; branch < yBranches.length; branch++) {
                        Complex observed = yBranches[branch];
                        Complex channel = hBranches[branch];

                        if (useOneTapEqualizer) {
                            observed = Complex.divide(observed, channel);
                            channel = new Complex(1.0, 0.0);
                        }

                        Complex expected = Complex.multiply(channel, entry.symbol());
                        Complex diff = observed.subtract(expected);
                        distance += diff.abs2();
                    }

                    if (entry.bits()[bitIndex] == 0) {
                        min0 = Math.min(min0, distance);
                    } else {
                        min1 = Math.min(min1, distance);
                    }
                }

                llr[llrIndex++] = (min1 - min0) / noiseVariance;
            }
        }

        return llr;
    }

    private int getSpatialBranches(String spatialMode) {
        return SimulationConfig.SPATIAL_2X2.equals(spatialMode) ? 4 : 1;
    }

    private Complex[] createChannelGains(String channelModel, int branches, Random random) {
        Complex[] gains = new Complex[branches];
        for (int branch = 0; branch < branches; branch++) {
            gains[branch] = SimulationConfig.CHANNEL_AWGN.equals(channelModel)
                    ? new Complex(1.0, 0.0)
                    : rayleigh(random);
        }
        return gains;
    }

    private Complex createOfdmSubcarrierGain(int taps, Random random) {
        Complex aggregate = new Complex(0.0, 0.0);
        for (int tap = 0; tap < taps; tap++) {
            double weight = 1.0 / Math.sqrt(tap + 1.0);
            Complex tapGain = new Complex(random.nextGaussian() * INV_SQRT2 * weight, random.nextGaussian() * INV_SQRT2 * weight);
            aggregate = aggregate.add(tapGain);
        }
        return aggregate;
    }

    private Complex rayleigh(Random random) {
        return new Complex(random.nextGaussian() * INV_SQRT2, random.nextGaussian() * INV_SQRT2);
    }

    private WaveformProfile getWaveformProfile(SimulationConfig config) {
        if (SimulationConfig.WAVEFORM_OFDM128.equals(config.getWaveform())) {
            double cpPenalty = 128.0 / (128.0 + Math.max(0, config.getCyclicPrefix()));
            double eqFactor = SimulationConfig.EQUALIZER_ZF.equals(config.getEqualizerMode()) ? 0.95 : 0.85;
            return new WaveformProfile(0.93 * cpPenalty * eqFactor, 1, 4);
        }
        if (SimulationConfig.WAVEFORM_OFDM64.equals(config.getWaveform())) {
            double cpPenalty = 64.0 / (64.0 + Math.max(0, config.getCyclicPrefix()));
            double eqFactor = SimulationConfig.EQUALIZER_ZF.equals(config.getEqualizerMode()) ? 0.94 : 0.82;
            return new WaveformProfile(0.90 * cpPenalty * eqFactor, 1, 3);
        }
        return new WaveformProfile(1.0, 12, 1);
    }

    private static class WaveformProfile {
        private final double usefulFraction;
        private final int fadingSpan;
        private final int taps;

        private WaveformProfile(double usefulFraction, int fadingSpan, int taps) {
            this.usefulFraction = usefulFraction;
            this.fadingSpan = fadingSpan;
            this.taps = taps;
        }
    }

    public static class Transmission {
        private final Complex[][] received;
        private final Complex[][] gains;

        public Transmission(Complex[][] received, Complex[][] gains) {
            this.received = received;
            this.gains = gains;
        }

        public Complex[][] received() {
            return received;
        }

        public Complex[][] gains() {
            return gains;
        }
    }
}