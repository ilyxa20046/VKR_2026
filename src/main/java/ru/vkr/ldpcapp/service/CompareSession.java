package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CompareSession {

    private static String leftTitle;
    private static SimulationConfig leftConfig;
    private static List<ResultPoint> leftResults = new ArrayList<>();

    private static String rightTitle;
    private static SimulationConfig rightConfig;
    private static List<ResultPoint> rightResults = new ArrayList<>();

    private static String stagedTitle;
    private static SimulationConfig stagedConfig;
    private static List<ResultPoint> stagedResults = new ArrayList<>();

    private CompareSession() {
    }

    public static void setComparison(String leftScenarioTitle,
                                     SimulationConfig leftScenarioConfig,
                                     List<ResultPoint> leftScenarioResults,
                                     String rightScenarioTitle,
                                     SimulationConfig rightScenarioConfig,
                                     List<ResultPoint> rightScenarioResults) {
        leftTitle = leftScenarioTitle;
        leftConfig = leftScenarioConfig;
        leftResults = leftScenarioResults == null ? new ArrayList<>() : new ArrayList<>(leftScenarioResults);
        rightTitle = rightScenarioTitle;
        rightConfig = rightScenarioConfig;
        rightResults = rightScenarioResults == null ? new ArrayList<>() : new ArrayList<>(rightScenarioResults);
    }

    public static boolean hasComparison() {
        return leftConfig != null
                && rightConfig != null
                && leftResults != null
                && rightResults != null
                && !leftResults.isEmpty()
                && !rightResults.isEmpty();
    }

    public static String getLeftTitle() {
        return leftTitle;
    }

    public static SimulationConfig getLeftConfig() {
        return leftConfig;
    }

    public static List<ResultPoint> getLeftResults() {
        return Collections.unmodifiableList(leftResults);
    }

    public static String getRightTitle() {
        return rightTitle;
    }

    public static SimulationConfig getRightConfig() {
        return rightConfig;
    }

    public static List<ResultPoint> getRightResults() {
        return Collections.unmodifiableList(rightResults);
    }

    public static void stageCurrentExperiment(String title,
                                              SimulationConfig config,
                                              List<ResultPoint> results) {
        stagedTitle = title;
        stagedConfig = config;
        stagedResults = results == null ? new ArrayList<>() : new ArrayList<>(results);
    }

    public static boolean hasStagedScenario() {
        return stagedConfig != null && stagedResults != null && !stagedResults.isEmpty();
    }

    public static String getStagedTitle() {
        return stagedTitle;
    }

    public static SimulationConfig getStagedConfig() {
        return stagedConfig;
    }

    public static List<ResultPoint> getStagedResults() {
        return Collections.unmodifiableList(stagedResults);
    }

    public static void pairWithCurrentExperiment(String currentTitle,
                                                 SimulationConfig currentConfig,
                                                 List<ResultPoint> currentResults) {
        if (!hasStagedScenario() || currentConfig == null || currentResults == null || currentResults.isEmpty()) {
            return;
        }
        setComparison(
                stagedTitle,
                stagedConfig,
                stagedResults,
                currentTitle,
                currentConfig,
                currentResults
        );
    }

    public static void clear() {
        leftTitle = null;
        leftConfig = null;
        leftResults = new ArrayList<>();
        rightTitle = null;
        rightConfig = null;
        rightResults = new ArrayList<>();
        stagedTitle = null;
        stagedConfig = null;
        stagedResults = new ArrayList<>();
    }
}
