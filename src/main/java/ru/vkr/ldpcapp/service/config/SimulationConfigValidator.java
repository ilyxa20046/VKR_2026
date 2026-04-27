package ru.vkr.ldpcapp.service.config;

import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.List;

public class SimulationConfigValidator {

    public void validate(SimulationConfig c) {
        if (!SimulationConfigFactory.supportedModulations().contains(c.getModulation())) {
            throw new IllegalArgumentException("Выбрана неподдерживаемая модуляция.");
        }
        if (!SimulationConfigFactory.supportedChannels().contains(c.getChannelModel())) {
            throw new IllegalArgumentException("Выбран неподдерживаемый тип канала.");
        }
        if (!SimulationConfigFactory.supportedLdpcProfiles().contains(c.getLdpcProfile())) {
            throw new IllegalArgumentException("Выбран неподдерживаемый профиль LDPC.");
        }
        if (!SimulationConfigFactory.supportedWaveforms().contains(c.getWaveform())) {
            throw new IllegalArgumentException("Выбран неподдерживаемый тип waveform/OFDM-представления.");
        }
        if (!SimulationConfigFactory.supportedSpatialModes().contains(c.getSpatialMode())) {
            throw new IllegalArgumentException("Выбран неподдерживаемый пространственный режим.");
        }
        if (!SimulationConfigFactory.supportedEqualizerModes().contains(c.getEqualizerMode())) {
            throw new IllegalArgumentException("Выбран неподдерживаемый режим эквализации.");
        }

        if (!SimulationConfigFactory.supportedSnrDomains().contains(c.getSnrDomain())) {
            throw new IllegalArgumentException("Неподдерживаемый домен SNR: " + c.getSnrDomain());
        }

        if (!SimulationConfigFactory.supportedDecoderTypes().contains(c.getDecoderType())) {
            throw new IllegalArgumentException("Неподдерживаемый тип декодера: " + c.getDecoderType());
        }

        int normalizedCp = SimulationConfigFactory.normalizeCyclicPrefix(c.getCyclicPrefix(), c.getWaveform());
        if (c.getCyclicPrefix() != normalizedCp) {
            throw new IllegalArgumentException("Длина cyclic prefix не соответствует выбранному waveform/OFDM-режиму.");
        }

        if (!List.of(SimulationConfig.NR_BG_AUTO, SimulationConfig.NR_BG1, SimulationConfig.NR_BG2).contains(c.getNrBaseGraph())) {
            throw new IllegalArgumentException("Неверный выбор base graph: " + c.getNrBaseGraph());
        }
        if (c.getLiftingSize() != 8 && c.getLiftingSize() != 16 && c.getLiftingSize() != 32) {
            throw new IllegalArgumentException("Lifting size должен быть 8, 16 или 32.");
        }

        // Ключевая правка: кратность зависит от BG1/BG2
        int kStep = SimulationConfigFactory.getProfileInfoWordLength(
                c.getLdpcProfile(),
                c.getLiftingSize(),
                c.getNrBaseGraph()
        );
        if (c.getInfoBlockLength() < kStep || c.getInfoBlockLength() % kStep != 0) {
            throw new IllegalArgumentException("Длина информационного блока должна быть кратна " + kStep + ".");
        }

        if (c.getSnrStep() <= 0.0) {
            throw new IllegalArgumentException("Шаг SNR должен быть положительным.");
        }
        if (c.getBlocks() < 20) {
            throw new IllegalArgumentException("Количество блоков должно быть не меньше 20.");
        }
        if (c.getMaxIterations() < 2) {
            throw new IllegalArgumentException("Число итераций декодера должно быть не меньше 2.");
        }
        if (c.getNormalization() < 0.5 || c.getNormalization() > 1.0) {
            throw new IllegalArgumentException("Коэффициент normalizing factor должен находиться в диапазоне 0.50..1.00.");
        }
        if (c.getSeed() < 1) {
            throw new IllegalArgumentException("Seed генератора должен быть положительным.");
        }

        if (c.getMinErrorEventsPerSnr() < 1) {
            throw new IllegalArgumentException("min error events per SNR должен быть >= 1.");
        }
        if (c.getMaxBlocksPerSnr() < c.getBlocks()) {
            throw new IllegalArgumentException("max blocks per SNR должен быть >= blocks.");
        }
        if (c.getConfidenceLevel() <= 0.0 || c.getConfidenceLevel() >= 1.0) {
            throw new IllegalArgumentException("Уровень доверия должен быть в интервале (0;1).");
        }

        if (c.isCrcEnabled() && c.getCrcBits() != SimulationConfig.CRC_16) {
            throw new IllegalArgumentException("Сейчас поддерживается только CRC-16.");
        }
        if (!c.isCrcEnabled() && c.getCrcBits() != SimulationConfig.CRC_NONE) {
            throw new IllegalArgumentException("Если CRC выключен, crcBits должен быть 0.");
        }

        if (c.getTargetCodewordLength() < 0) {
            throw new IllegalArgumentException("targetCodewordLength не может быть отрицательным.");
        }
        if (c.isRateMatchingEnabled() && c.getTargetCodewordLength() == 0) {
            throw new IllegalArgumentException("Для rate matching задайте targetCodewordLength > 0.");
        }

        if (!SimulationConfigFactory.supportedBlerCriteria().contains(c.getBlerCriterion())) {
            throw new IllegalArgumentException("Неподдерживаемый критерий BLER: " + c.getBlerCriterion());
        }
        if (SimulationConfig.BLER_BY_CRC_FAIL.equals(c.getBlerCriterion()) && !c.isCrcEnabled()) {
            throw new IllegalArgumentException("Критерий BLER по CRC доступен только при включенном CRC.");
        }

        if (!c.isSegmentationEnabled() && (c.getInfoBlockLength() + c.getCrcBits()) > SimulationConfigFactory.getCodeInfoLength(c)) {
            throw new IllegalArgumentException("При выключенной segmentation TB должен помещаться в одно кодовое слово.");
        }
    }
}