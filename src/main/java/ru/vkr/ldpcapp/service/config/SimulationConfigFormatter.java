package ru.vkr.ldpcapp.service.config;

import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.Locale;

public class SimulationConfigFormatter {

    public String toSummaryText(SimulationConfig c) {
        return String.format(
                Locale.US,
                "Конфигурация эксперимента:%n" +
                        "• профиль кодирования: %s%n" +
                        "• семейство кода: %s%n" +
                        "• модуляция: %s%n" +
                        "• тип канала: %s%n" +
                        "• форма сигнала: %s%n" +
                        "• пространственный режим: %s%n" +
                        "• циклический префикс: %d%n" +
                        "• режим эквализации: %s%n" +
                        "• информационный блок: %d бит%n" +
                        "• кодовое слово: %d информационных бит на одно слово%n" +
                        "• диапазон SNR: %.2f ... %.2f дБ, шаг %.2f дБ%n" +
                        "• число точек SNR: %d%n" +
                        "• блоков на точку SNR: %d%n" +
                        "• общий объём моделирования: %d блоков%n" +
                        "• оценочное число информационных бит: %d%n" +
                        "• тип декодера: %s, максимум %d итераций%n" +
                        "• коэффициент нормализации: %.2f%n" +
                        "• скорость кода: %.3f%n" +
                        "• описание профиля: %s%n",
                SimulationConfigFactory.getProfileDisplayName(c.getLdpcProfile(), c.getLiftingSize(), c.getNrBaseGraph()),
                SimulationConfigFactory.getProfileFamily(c.getLdpcProfile()),
                c.getModulation(),
                SimulationConfigFactory.getChannelUiName(c.getChannelModel()),
                SimulationConfigFactory.getWaveformUiName(c.getWaveform()),
                SimulationConfigFactory.getSpatialModeUiName(c.getSpatialMode()),
                c.getCyclicPrefix(),
                SimulationConfigFactory.getEqualizerUiName(c.getEqualizerMode()),
                c.getInfoBlockLength(),
                SimulationConfigFactory.getCodeInfoLength(c),
                Math.min(c.getSnrStart(), c.getSnrEnd()),
                Math.max(c.getSnrStart(), c.getSnrEnd()),
                c.getSnrStep(),
                SimulationConfigFactory.getSnrPointCount(c),
                c.getBlocks(),
                SimulationConfigFactory.getExperimentBlockCount(c),
                SimulationConfigFactory.getEstimatedInformationBits(c),
                SimulationConfigFactory.getDecoderTypeUiName(c.getDecoderType()),
                c.getMaxIterations(),
                c.getNormalization(),
                SimulationConfigFactory.getCodeRate(c),
                SimulationConfigFactory.getProfileDescription(c.getLdpcProfile())
        );
    }
}