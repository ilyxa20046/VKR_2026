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
        this.snr = snr;
        this.berUncoded = berUncoded;
        this.berLdpc = berLdpc;
        this.blerUncoded = blerUncoded;
        this.blerLdpc = blerLdpc;
        this.averageIterations = averageIterations;
        this.successRatio = successRatio;
        this.effectiveThroughputMbps = effectiveThroughputMbps;
        this.spectralEfficiency = spectralEfficiency;
    }

    public double getSnr() {
        return snr;
    }

    public double getBerUncoded() {
        return berUncoded;
    }

    public double getBerLdpc() {
        return berLdpc;
    }

    public double getBerCoded() {
        return berLdpc;
    }

    public double getBlerUncoded() {
        return blerUncoded;
    }

    public double getBlerLdpc() {
        return blerLdpc;
    }

    public double getBlerCoded() {
        return blerLdpc;
    }

    public double getAverageIterations() {
        return averageIterations;
    }

    public double getSuccessRatio() {
        return successRatio;
    }

    public double getSuccessfulCodewords() {
        return successRatio;
    }

    public double getEffectiveThroughputMbps() {
        return effectiveThroughputMbps;
    }

    public double getSpectralEfficiency() {
        return spectralEfficiency;
    }
}
