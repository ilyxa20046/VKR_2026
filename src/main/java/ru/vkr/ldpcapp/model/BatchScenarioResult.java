package ru.vkr.ldpcapp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BatchScenarioResult {

    private final String scenarioLabel;
    private final SimulationConfig config;
    private final List<ResultPoint> points;
    private final ExperimentSummary summary;

    public BatchScenarioResult(String scenarioLabel, SimulationConfig config, List<ResultPoint> points) {
        this.scenarioLabel = scenarioLabel;
        this.config = copyConfig(config);
        this.points = new ArrayList<>(points);
        this.summary = ExperimentSummary.from(this.points);
    }

    public String getScenarioLabel() {
        return scenarioLabel;
    }

    public SimulationConfig getConfig() {
        return copyConfig(config);
    }

    public List<ResultPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public ExperimentSummary getSummary() {
        return summary;
    }

    public String getModulation() {
        return config.getModulation();
    }

    public String getChannel() {
        return config.getChannelModel();
    }

    public String getLdpcProfileName() {
        return SimulationConfig.getProfileName(config.getLdpcProfile());
    }

    private SimulationConfig copyConfig(SimulationConfig source) {
        if (source == null) {
            return null;
        }
        return new SimulationConfig(
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
    }
}
