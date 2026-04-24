package ru.vkr.ldpcapp.model;

public class ResultPoint {

    private final double snr;
    private final double berUncoded;
    private final double berLdpc;
    private final double blerUncoded;
    private final double blerLdpc;
    private final double averageIterations;
    private final double successRatio;
    private final double effectiveThroughputMbps;
    private final double spectralEfficiency;

    private final double berLdpcCiLow;
    private final double berLdpcCiHigh;
    private final double blerLdpcCiLow;
    private final double blerLdpcCiHigh;
    private final int bitErrorsLdpc;
    private final int blockErrorsLdpc;
    private final int totalBits;
    private final int totalBlocks;
    private final double confidenceLevel;

    public ResultPoint(
            double snr,
            double berUncoded,
            double berLdpc,
            double blerUncoded,
            double blerLdpc,
            double averageIterations,
            double successRatio,
            double effectiveThroughputMbps,
            double spectralEfficiency
    ) {
        this(
                snr, berUncoded, berLdpc, blerUncoded, blerLdpc,
                averageIterations, successRatio, effectiveThroughputMbps, spectralEfficiency,
                0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0, 0.95
        );
    }

    public ResultPoint(
            double snr,
            double berUncoded,
            double berLdpc,
            double blerUncoded,
            double blerLdpc,
            double averageIterations,
            double successRatio,
            double effectiveThroughputMbps,
            double spectralEfficiency,
            double berLdpcCiLow,
            double berLdpcCiHigh,
            double blerLdpcCiLow,
            double blerLdpcCiHigh,
            int bitErrorsLdpc,
            int blockErrorsLdpc,
            int totalBits,
            int totalBlocks,
            double confidenceLevel
    ) {
        this.snr = snr;
        this.berUncoded = berUncoded;
        this.berLdpc = berLdpc;
        this.blerUncoded = blerUncoded;
        this.blerLdpc = blerLdpc;
        this.averageIterations = averageIterations;
        this.successRatio = successRatio;
        this.effectiveThroughputMbps = effectiveThroughputMbps;
        this.spectralEfficiency = spectralEfficiency;
        this.berLdpcCiLow = berLdpcCiLow;
        this.berLdpcCiHigh = berLdpcCiHigh;
        this.blerLdpcCiLow = blerLdpcCiLow;
        this.blerLdpcCiHigh = blerLdpcCiHigh;
        this.bitErrorsLdpc = bitErrorsLdpc;
        this.blockErrorsLdpc = blockErrorsLdpc;
        this.totalBits = totalBits;
        this.totalBlocks = totalBlocks;
        this.confidenceLevel = confidenceLevel;
    }

    public double getSnr() { return snr; }
    public double getBerUncoded() { return berUncoded; }
    public double getBerLdpc() { return berLdpc; }
    public double getBerCoded() { return berLdpc; }
    public double getBlerUncoded() { return blerUncoded; }
    public double getBlerLdpc() { return blerLdpc; }
    public double getBlerCoded() { return blerLdpc; }
    public double getAverageIterations() { return averageIterations; }
    public double getSuccessRatio() { return successRatio; }
    public double getSuccessfulCodewords() { return successRatio; }
    public double getEffectiveThroughputMbps() { return effectiveThroughputMbps; }
    public double getSpectralEfficiency() { return spectralEfficiency; }

    public double getBerLdpcCiLow() { return berLdpcCiLow; }
    public double getBerLdpcCiHigh() { return berLdpcCiHigh; }
    public double getBlerLdpcCiLow() { return blerLdpcCiLow; }
    public double getBlerLdpcCiHigh() { return blerLdpcCiHigh; }
    public int getBitErrorsLdpc() { return bitErrorsLdpc; }
    public int getBlockErrorsLdpc() { return blockErrorsLdpc; }
    public int getTotalBits() { return totalBits; }
    public int getTotalBlocks() { return totalBlocks; }
    public double getConfidenceLevel() { return confidenceLevel; }
}