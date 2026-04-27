package ru.vkr.ldpcapp.service.phy.stats;

public class StatsMath {

    public record Interval(double low, double high) {}

    public Interval wilsonInterval(int errors, int total, double confidenceLevel) {
        if (total <= 0) {
            return new Interval(0.0, 0.0);
        }
        double p = (double) errors / total;
        double z = zScore(confidenceLevel);
        double z2 = z * z;
        double denominator = 1.0 + z2 / total;
        double center = (p + z2 / (2.0 * total)) / denominator;
        double radius = (z / denominator) * Math.sqrt((p * (1.0 - p) + z2 / (4.0 * total)) / total);

        return new Interval(
                Math.max(0.0, center - radius),
                Math.min(1.0, center + radius)
        );
    }

    public double safeDivide(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    public double safeDivide(double numerator, int denominator) {
        return denominator == 0 ? 0.0 : numerator / denominator;
    }

    public double safeDivide(double numerator, double denominator) {
        return Math.abs(denominator) < 1e-12 ? 0.0 : numerator / denominator;
    }

    private double zScore(double confidenceLevel) {
        if (confidenceLevel >= 0.999) return 3.29;
        if (confidenceLevel >= 0.99) return 2.58;
        if (confidenceLevel >= 0.98) return 2.33;
        if (confidenceLevel >= 0.95) return 1.96;
        if (confidenceLevel >= 0.90) return 1.64;
        if (confidenceLevel >= 0.80) return 1.28;
        return 1.0;
    }
}