package ru.vkr.ldpcapp.service;

import org.apache.commons.math3.special.Erf;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.config.SimulationConfigFactory;

public class BerTheoryService {

    public double theoreticalBerAwgnUncoded(String modulation, String snrDomain, double snrDb) {
        double ebN0Db = toEbN0Uncoded(snrDb, snrDomain, modulation);
        double ebN0 = Math.pow(10.0, ebN0Db / 10.0);

        return switch (modulation) {
            case SimulationConfig.MOD_BPSK, SimulationConfig.MOD_QPSK -> q(Math.sqrt(2.0 * ebN0));
            case SimulationConfig.MOD_16QAM -> berMQamGray(16, ebN0);
            case SimulationConfig.MOD_64QAM -> berMQamGray(64, ebN0);
            case SimulationConfig.MOD_256QAM -> berMQamGray(256, ebN0);
            default -> throw new IllegalArgumentException("Unsupported modulation: " + modulation);
        };
    }

    private double toEbN0Uncoded(double snrDb, String snrDomain, String modulation) {
        if (SimulationConfig.SNR_DOMAIN_EB_N0.equals(snrDomain)) {
            return snrDb;
        }
        int m = SimulationConfigFactory.bitsPerSymbol(modulation);
        return snrDb - 10.0 * Math.log10(Math.max(1e-12, m));
    }

    private double berMQamGray(int M, double ebN0) {
        double k = Math.log(M) / Math.log(2.0);
        double qArg = Math.sqrt((3.0 * k / (M - 1.0)) * ebN0);
        double value = (4.0 / k) * (1.0 - 1.0 / Math.sqrt(M)) * q(qArg);
        return Math.max(1e-12, Math.min(0.5, value));
    }
    public double theoreticalBerBpskAwgnFromEbN0(double ebN0Db) {
        double ebN0 = Math.pow(10.0, ebN0Db / 10.0);
        return q(Math.sqrt(2.0 * ebN0));
    }

    /**
     * Shannon limit in Eb/N0 for given spectral efficiency/code rate R (bit/s/Hz).
     * (Eb/N0)_min = (2^R - 1)/R
     */
    public double shannonLimitEbN0Db(double codeRate) {
        double r = Math.max(1e-9, codeRate);
        double ebN0Linear = (Math.pow(2.0, r) - 1.0) / r;
        return 10.0 * Math.log10(ebN0Linear);
    }

    /** Convert Eb/N0(dB) to displayed SNR domain used on X-axis */
    public double convertEbN0ToSnrDomain(
            double ebN0Db,
            String snrDomain,
            String modulation,
            double codeRate
    ) {
        if (SimulationConfig.SNR_DOMAIN_EB_N0.equals(snrDomain)) {
            return ebN0Db;
        }
        int m = SimulationConfigFactory.bitsPerSymbol(modulation);
        double r = Math.max(1e-12, codeRate);
        return ebN0Db + 10.0 * Math.log10(Math.max(1e-12, m * r));
    }

    private double q(double x) {
        return 0.5 * Erf.erfc(x / Math.sqrt(2.0));
    }
}