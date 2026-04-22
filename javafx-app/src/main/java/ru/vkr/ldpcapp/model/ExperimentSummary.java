package ru.vkr.ldpcapp.model;

import java.util.Comparator;
import java.util.List;

public class ExperimentSummary {

    private final double bestBerGain;
    private final double bestBlerGain;
    private final double minCodedBer;
    private final double minCodedBler;
    private final double averageIterations;
    private final double averageSuccessRatio;
    private final Double firstStableBlerSnr;
    private final Double berEnergyGainDb;
    private final Double blerEnergyGainDb;
    private final double averageThroughputMbps;
    private final double peakThroughputMbps;
    private final double averageSpectralEfficiency;
    private final double peakSpectralEfficiency;
    private final Double requiredSnrBerDb;
    private final Double requiredSnrBlerDb;

    public ExperimentSummary(
            double bestBerGain,
            double bestBlerGain,
            double minCodedBer,
            double minCodedBler,
            double averageIterations,
            double averageSuccessRatio,
            Double firstStableBlerSnr,
            Double berEnergyGainDb,
            Double blerEnergyGainDb,
            double averageThroughputMbps,
            double peakThroughputMbps,
            double averageSpectralEfficiency,
            double peakSpectralEfficiency,
            Double requiredSnrBerDb,
            Double requiredSnrBlerDb
    ) {
        this.bestBerGain = bestBerGain;
        this.bestBlerGain = bestBlerGain;
        this.minCodedBer = minCodedBer;
        this.minCodedBler = minCodedBler;
        this.averageIterations = averageIterations;
        this.averageSuccessRatio = averageSuccessRatio;
        this.firstStableBlerSnr = firstStableBlerSnr;
        this.berEnergyGainDb = berEnergyGainDb;
        this.blerEnergyGainDb = blerEnergyGainDb;
        this.averageThroughputMbps = averageThroughputMbps;
        this.peakThroughputMbps = peakThroughputMbps;
        this.averageSpectralEfficiency = averageSpectralEfficiency;
        this.peakSpectralEfficiency = peakSpectralEfficiency;
        this.requiredSnrBerDb = requiredSnrBerDb;
        this.requiredSnrBlerDb = requiredSnrBlerDb;
    }

    public static ExperimentSummary from(List<ResultPoint> points) {
        if (points == null || points.isEmpty()) {
            return new ExperimentSummary(1.0, 1.0, 0.0, 0.0, 0.0, 0.0, null, null, null, 0.0, 0.0, 0.0, 0.0, null, null);
        }

        double bestBerGain = points.stream()
                .mapToDouble(point -> gain(point.getBerUncoded(), point.getBerLdpc()))
                .max()
                .orElse(1.0);

        double bestBlerGain = points.stream()
                .mapToDouble(point -> gain(point.getBlerUncoded(), point.getBlerLdpc()))
                .max()
                .orElse(1.0);

        double minCodedBer = points.stream()
                .map(ResultPoint::getBerLdpc)
                .min(Comparator.naturalOrder())
                .orElse(0.0);

        double minCodedBler = points.stream()
                .map(ResultPoint::getBlerLdpc)
                .min(Comparator.naturalOrder())
                .orElse(0.0);

        double averageIterations = points.stream()
                .mapToDouble(ResultPoint::getAverageIterations)
                .average()
                .orElse(0.0);

        double averageSuccessRatio = points.stream()
                .mapToDouble(ResultPoint::getSuccessRatio)
                .average()
                .orElse(0.0);

        Double firstStableBlerSnr = points.stream()
                .filter(point -> point.getBlerLdpc() <= 0.1)
                .map(ResultPoint::getSnr)
                .findFirst()
                .orElse(null);

        double averageThroughputMbps = points.stream()
                .mapToDouble(ResultPoint::getEffectiveThroughputMbps)
                .average()
                .orElse(0.0);

        double peakThroughputMbps = points.stream()
                .mapToDouble(ResultPoint::getEffectiveThroughputMbps)
                .max()
                .orElse(0.0);

        double averageSpectralEfficiency = points.stream()
                .mapToDouble(ResultPoint::getSpectralEfficiency)
                .average()
                .orElse(0.0);

        double peakSpectralEfficiency = points.stream()
                .mapToDouble(ResultPoint::getSpectralEfficiency)
                .max()
                .orElse(0.0);

        Double requiredSnrBerDb = estimateThreshold(points, true, true, 1e-3);
        Double requiredSnrBlerDb = estimateThreshold(points, false, true, 1e-1);

        return new ExperimentSummary(
                bestBerGain,
                bestBlerGain,
                minCodedBer,
                minCodedBler,
                averageIterations,
                averageSuccessRatio,
                firstStableBlerSnr,
                estimateEnergyGain(points, true, 1e-3),
                estimateEnergyGain(points, false, 1e-1),
                averageThroughputMbps,
                peakThroughputMbps,
                averageSpectralEfficiency,
                peakSpectralEfficiency,
                requiredSnrBerDb,
                requiredSnrBlerDb
        );
    }

    public double getBestBerGain() {
        return bestBerGain;
    }

    public double getBestBlerGain() {
        return bestBlerGain;
    }

    public double getMinCodedBer() {
        return minCodedBer;
    }

    public double getMinCodedBler() {
        return minCodedBler;
    }

    public double getAverageIterations() {
        return averageIterations;
    }

    public double getAverageSuccessRatio() {
        return averageSuccessRatio;
    }

    public Double getFirstStableBlerSnr() {
        return firstStableBlerSnr;
    }

    public Double getBerEnergyGainDb() {
        return berEnergyGainDb;
    }

    public Double getBlerEnergyGainDb() {
        return blerEnergyGainDb;
    }

    public double getAverageThroughputMbps() {
        return averageThroughputMbps;
    }

    public double getPeakThroughputMbps() {
        return peakThroughputMbps;
    }

    public double getAverageSpectralEfficiency() {
        return averageSpectralEfficiency;
    }

    public double getPeakSpectralEfficiency() {
        return peakSpectralEfficiency;
    }

    public Double getRequiredSnrBerDb() {
        return requiredSnrBerDb;
    }

    public Double getRequiredSnrBlerDb() {
        return requiredSnrBlerDb;
    }

    private static Double estimateEnergyGain(List<ResultPoint> points, boolean berMetric, double target) {
        Double uncodedThreshold = estimateThreshold(points, berMetric, false, target);
        Double codedThreshold = estimateThreshold(points, berMetric, true, target);
        if (uncodedThreshold == null || codedThreshold == null) {
            return null;
        }
        return uncodedThreshold - codedThreshold;
    }

    private static Double estimateThreshold(List<ResultPoint> points, boolean berMetric, boolean coded, double target) {
        if (points.isEmpty()) {
            return null;
        }

        double firstValue = metric(points.get(0), berMetric, coded);
        if (firstValue <= target) {
            return null;
        }

        boolean everReached = points.stream().mapToDouble(point -> metric(point, berMetric, coded)).anyMatch(value -> value <= target);
        if (!everReached) {
            return null;
        }

        for (int i = 0; i < points.size() - 1; i++) {
            ResultPoint currentPoint = points.get(i);
            ResultPoint nextPoint = points.get(i + 1);
            double current = metric(currentPoint, berMetric, coded);
            double next = metric(nextPoint, berMetric, coded);
            if ((current >= target && next <= target) || (current <= target && next >= target)) {
                double logCurrent = Math.log10(Math.max(current, 1e-12));
                double logNext = Math.log10(Math.max(next, 1e-12));
                double logTarget = Math.log10(Math.max(target, 1e-12));
                double ratio = Math.abs(logNext - logCurrent) < 1e-12 ? 0.0 : (logTarget - logCurrent) / (logNext - logCurrent);
                return currentPoint.getSnr() + ratio * (nextPoint.getSnr() - currentPoint.getSnr());
            }
        }

        return null;
    }

    private static double metric(ResultPoint point, boolean berMetric, boolean coded) {
        if (berMetric) {
            return coded ? point.getBerLdpc() : point.getBerUncoded();
        }
        return coded ? point.getBlerLdpc() : point.getBlerUncoded();
    }

    private static double gain(double base, double improved) {
        if (improved <= 0.0 && base > 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        if (improved <= 0.0) {
            return 1.0;
        }
        return base / improved;
    }
}
