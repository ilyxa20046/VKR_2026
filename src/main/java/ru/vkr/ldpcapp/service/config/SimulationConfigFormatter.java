package ru.vkr.ldpcapp.service.config;

import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.Locale;

public class SimulationConfigFormatter {

    public String toSummaryText(SimulationConfig c) {
        return String.format(
                Locale.US,
                "Конфигурация эксперимента:%n" +
                        "• профиль LDPC: %s%n" +
                        "• семейство кода: %s%n" +
                        "• модуляция: %s%n" +
                        "• канал: %s%n" +
                        "• waveform: %s%n" +
                        "• spatial mode: %s%n" +
                        "• cyclic prefix: %d%n" +
                        "• equalizer: %s%n" +
                        "• информационный блок: %d бит%n" +
                        "• кодовое слово: %d информационных бит на одно слово%n" +
                        "• диапазон SNR: %.2f ... %.2f дБ с шагом %.2f дБ%n" +
                        "• число точек SNR: %d%n" +
                        "• блоков на точку: %d%n" +
                        "• общий объём моделирования: %d блоков%n" +
                        "• оценочное число информационных бит: %d%n" +
                        "• декодер: normalized min-sum, максимум %d итераций%n" +
                        "• коэффициент нормализации: %.2f%n" +
                        "• скорость кода: %.2f%n" +
                        "• описание профиля: %s%n" +
                        "• базовая скорость профиля: %.3f%n" +
                        "• эффективная скорость (с учетом rate matching): %.3f%n" +
                        "• MCS-like комбинация: %s%n",
                SimulationConfigFactory.getProfileDisplayName(c.getLdpcProfile(), c.getLiftingSize()),
                SimulationConfigFactory.getProfileFamily(c.getLdpcProfile()),
                c.getModulation(),
                c.getChannelModel(),
                c.getWaveform(),
                c.getSpatialMode(),
                c.getCyclicPrefix(),
                c.getEqualizerMode(),
                c.getInfoBlockLength(),
                SimulationConfigFactory.getCodeInfoLength(c),
                Math.min(c.getSnrStart(), c.getSnrEnd()),
                Math.max(c.getSnrStart(), c.getSnrEnd()),
                c.getSnrStep(),
                SimulationConfigFactory.getSnrPointCount(c),
                c.getBlocks(),
                SimulationConfigFactory.getExperimentBlockCount(c),
                SimulationConfigFactory.getEstimatedInformationBits(c),
                c.getMaxIterations(),
                c.getNormalization(),
                SimulationConfigFactory.getCodeRate(c),
                SimulationConfigFactory.getProfileDescription(c.getLdpcProfile()),
                SimulationConfigFactory.getProfileCodeRate(c.getLdpcProfile(), c.getNrBaseGraph()),
                SimulationConfigFactory.getEffectiveCodeRate(c),
                SimulationConfigFactory.getMcsLikeLabel(c)
        );
    }
}