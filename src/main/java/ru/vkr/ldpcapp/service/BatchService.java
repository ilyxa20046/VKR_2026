package ru.vkr.ldpcapp.service;

import javafx.concurrent.Task;
import ru.vkr.ldpcapp.model.BatchScenarioResult;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.config.SimulationConfigFactory;
import ru.vkr.ldpcapp.service.config.SimulationConfigValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BatchService {

    private final SimulationService simulationService = new SimulationService();
    private final SimulationConfigValidator configValidator = new SimulationConfigValidator();

    // Backward-compatible signature
    public Task<List<BatchScenarioResult>> createTask(
            SimulationConfig baseConfig,
            List<String> modulations,
            List<String> channels,
            List<String> profiles
    ) {
        return createTask(baseConfig, modulations, channels, profiles, List.of());
    }

    public Task<List<BatchScenarioResult>> createTask(
            SimulationConfig baseConfig,
            List<String> modulations,
            List<String> channels,
            List<String> profiles,
            List<Double> rates
    ) {
        return new Task<>() {
            @Override
            protected List<BatchScenarioResult> call() {
                validateInput(baseConfig, modulations, channels, profiles);

                List<Double> effectiveRates = (rates == null || rates.isEmpty())
                        ? List.of((Double) null)
                        : List.copyOf(rates);

                int total = modulations.size() * channels.size() * profiles.size() * effectiveRates.size();
                int done = 0;

                updateProgress(0, Math.max(1, total));
                updateMessage("Подготовка пакетного анализа...");

                List<BatchScenarioResult> out = new ArrayList<>();
                int seedOffset = 0;

                for (String modulation : modulations) {
                    for (String channel : channels) {
                        for (String profile : profiles) {
                            for (Double rate : effectiveRates) {
                                if (isCancelled()) {
                                    updateMessage("Пакетный анализ отменён");
                                    return out;
                                }

                                SimulationConfig cfg = copyBase(baseConfig);
                                cfg.setModulation(modulation);
                                cfg.setChannelModel(channel);
                                cfg.setLdpcProfile(profile);

                                // Нормализуем длину блока под выбранный профиль/BG/Z
                                cfg.setInfoBlockLength(SimulationConfigFactory.normalizeInfoBlockLength(
                                        cfg.getInfoBlockLength(),
                                        cfg.getLdpcProfile(),
                                        cfg.getLiftingSize(),
                                        cfg.getNrBaseGraph()
                                ));

                                // Детерминированно меняем seed для разных сценариев
                                cfg.setSeed(baseConfig.getSeed() + seedOffset * 97);
                                seedOffset++;

                                applyRatePreset(cfg, rate);

                                configValidator.validate(cfg);

                                String rateLabel = rate == null
                                        ? "R=base"
                                        : "R=" + SimulationConfigFactory.formatRate(rate);

                                String scenarioLabel =
                                        SimulationConfigFactory.getModulationUiName(modulation) + " · " +
                                                SimulationConfigFactory.getChannelUiName(channel) + " · " +
                                                SimulationConfigFactory.getProfileUiName(profile) + " · " +
                                                rateLabel;

                                List<ResultPoint> points = simulationService.runBlocking(cfg);
                                BatchScenarioResult scenario = buildScenarioResult(scenarioLabel, cfg, points);
                                out.add(scenario);

                                done++;
                                updateProgress(done, total);
                                updateMessage(String.format(
                                        Locale.US,
                                        "Рассчитано сценариев: %d / %d",
                                        done,
                                        total
                                ));
                            }
                        }
                    }
                }

                updateMessage("Пакетный анализ завершён: " + out.size() + " сценариев");
                return out;
            }
        };
    }

    private void applyRatePreset(SimulationConfig cfg, Double rate) {
        if (rate == null) {
            return;
        }
        cfg.setRateMatchingEnabled(true);
        int e = SimulationConfigFactory.computeTargetCodewordLengthForRate(cfg, rate);
        cfg.setTargetCodewordLength(e);
    }

    private BatchScenarioResult buildScenarioResult(
            String scenarioLabel,
            SimulationConfig config,
            List<ResultPoint> points
    ) {
        return new BatchScenarioResult(scenarioLabel, config, points);
    }

    private void validateInput(
            SimulationConfig baseConfig,
            List<String> modulations,
            List<String> channels,
            List<String> profiles
    ) {
        if (baseConfig == null) {
            throw new IllegalArgumentException("Не задана базовая конфигурация пакетного анализа.");
        }
        if (modulations == null || modulations.isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы одну модуляцию.");
        }
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы один тип канала.");
        }
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы один профиль кодирования.");
        }
    }

    private SimulationConfig copyBase(SimulationConfig source) {
        SimulationConfig c = new SimulationConfig(
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
                source.getEqualizerMode()
        );

        c.setSnrDomain(source.getSnrDomain());
        c.setDecoderType(source.getDecoderType());

        c.setNrBaseGraph(source.getNrBaseGraph());
        c.setLiftingSize(source.getLiftingSize());

        c.setCrcEnabled(source.isCrcEnabled());
        c.setCrcBits(source.getCrcBits());

        c.setSegmentationEnabled(source.isSegmentationEnabled());
        c.setRateMatchingEnabled(source.isRateMatchingEnabled());
        c.setTargetCodewordLength(source.getTargetCodewordLength());

        c.setBlerCriterion(source.getBlerCriterion());

        c.setAdaptiveStopEnabled(source.isAdaptiveStopEnabled());
        c.setMinErrorEventsPerSnr(source.getMinErrorEventsPerSnr());
        c.setMaxBlocksPerSnr(source.getMaxBlocksPerSnr());
        c.setConfidenceLevel(source.getConfidenceLevel());

        c.setHarqEnabled(source.isHarqEnabled());
        c.setHarqMaxRetx(source.getHarqMaxRetx());

        return c;
    }
}