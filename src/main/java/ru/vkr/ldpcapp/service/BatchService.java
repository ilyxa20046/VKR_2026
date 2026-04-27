package ru.vkr.ldpcapp.service;

import javafx.concurrent.Task;
import ru.vkr.ldpcapp.model.BatchScenarioResult;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.config.SimulationConfigFactory;
import ru.vkr.ldpcapp.service.config.SimulationConfigValidator;

import java.util.ArrayList;
import java.util.List;

public class BatchService {

    private final SimulationService simulationService = new SimulationService();
    private final SimulationConfigValidator configValidator = new SimulationConfigValidator();

    public Task<List<BatchScenarioResult>> createTask(
            SimulationConfig baseConfig,
            List<String> modulations,
            List<String> channels,
            List<String> profiles
    ) {
        return new Task<>() {
            @Override
            protected List<BatchScenarioResult> call() {
                List<ScenarioDefinition> scenarios = buildScenarioDefinitions(baseConfig, modulations, channels, profiles);
                List<BatchScenarioResult> results = new ArrayList<>();
                int total = scenarios.size();

                updateProgress(0, Math.max(1, total));
                updateMessage("Подготовка batch-эксперимента...");

                for (int index = 0; index < scenarios.size(); index++) {
                    if (isCancelled()) {
                        updateMessage("Batch-расчёт отменён");
                        break;
                    }

                    ScenarioDefinition definition = scenarios.get(index);
                    updateMessage("Batch " + (index + 1) + "/" + total + ": " + definition.label());
                    List<ResultPoint> points = simulationService.runBlocking(definition.config());
                    results.add(new BatchScenarioResult(definition.label(), definition.config(), points));
                    updateProgress(index + 1, total);
                }

                updateMessage("Batch-расчёт завершён");
                return results;
            }
        };
    }

    private List<ScenarioDefinition> buildScenarioDefinitions(
            SimulationConfig baseConfig,
            List<String> modulations,
            List<String> channels,
            List<String> profiles
    ) {
        validateSelections(baseConfig, modulations, channels, profiles);

        List<ScenarioDefinition> scenarios = new ArrayList<>();
        int offset = 0;

        for (String profile : profiles) {
            for (String channel : channels) {
                for (String modulation : modulations) {
                    SimulationConfig config = copyConfig(baseConfig);
                    config.setLdpcProfile(profile);
                    config.setInfoBlockLength(SimulationConfigFactory.normalizeInfoBlockLength(
                            config.getInfoBlockLength(),
                            profile,
                            config.getLiftingSize()
                    ));
                    config.setChannelModel(channel);
                    config.setModulation(modulation);
                    config.setSeed(baseConfig.getSeed() + offset * 97);
                    configValidator.validate(config);

                    String label = modulation
                            + " / " + channel
                            + " / " + SimulationConfigFactory.getProfileDisplayName(profile, config.getLiftingSize())
                            + " / " + config.getWaveform()
                            + " / " + config.getSpatialMode();
                    scenarios.add(new ScenarioDefinition(label, config));
                    offset++;
                }
            }
        }

        return scenarios;
    }

    private void validateSelections(
            SimulationConfig baseConfig,
            List<String> modulations,
            List<String> channels,
            List<String> profiles
    ) {
        if (baseConfig == null) {
            throw new IllegalArgumentException("Не задана базовая конфигурация batch-эксперимента.");
        }
        if (modulations == null || modulations.isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы одну модуляцию для batch-сравнения.");
        }
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы один тип канала для batch-сравнения.");
        }
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы один профиль LDPC для batch-сравнения.");
        }
    }

    private SimulationConfig copyConfig(SimulationConfig source) {
        SimulationConfig copy = new SimulationConfig(
                source.getInfoBlockLength(),
                source.getSnrStart(),
                source.getSnrEnd(),
                source.getSnrStep(),
                source.getBlocks(),
                source.getMaxIterations(),
                source.getNormalization(),
                source.getSeed(),
                source.getModulation(),
                source.getChannelModel(),
                source.getLdpcProfile(),
                source.getWaveform(),
                source.getSpatialMode(),
                source.getCyclicPrefix(),
                source.getEqualizerMode(),
                source.isAdaptiveStopEnabled(),
                source.getMinErrorEventsPerSnr(),
                source.getMaxBlocksPerSnr(),
                source.getConfidenceLevel()
        );

        copy.setNrBaseGraph(source.getNrBaseGraph());
        copy.setLiftingSize(source.getLiftingSize());
        copy.setCrcEnabled(source.isCrcEnabled());
        copy.setCrcBits(source.getCrcBits());
        copy.setSegmentationEnabled(source.isSegmentationEnabled());
        copy.setRateMatchingEnabled(source.isRateMatchingEnabled());
        copy.setTargetCodewordLength(source.getTargetCodewordLength());
        copy.setBlerCriterion(source.getBlerCriterion());
        return copy;
    }

    private record ScenarioDefinition(String label, SimulationConfig config) {
    }
}
