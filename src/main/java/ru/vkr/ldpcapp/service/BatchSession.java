package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.BatchScenarioResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BatchSession {

    private static List<BatchScenarioResult> lastBatchResults = new ArrayList<>();
    private static ru.vkr.ldpcapp.model.SimulationConfig lastBaseConfig;

    private BatchSession() {
    }

    public static void save(ru.vkr.ldpcapp.model.SimulationConfig baseConfig, List<BatchScenarioResult> results) {
        lastBaseConfig = baseConfig;
        lastBatchResults = results == null ? new ArrayList<>() : new ArrayList<>(results);
    }

    public static void save(List<BatchScenarioResult> results) {
        save(lastBaseConfig, results);
    }

    public static List<BatchScenarioResult> getLastBatchResults() {
        return Collections.unmodifiableList(lastBatchResults);
    }

    public static ru.vkr.ldpcapp.model.SimulationConfig getLastBaseConfig() {
        return lastBaseConfig;
    }

    public static boolean hasResults() {
        return lastBatchResults != null && !lastBatchResults.isEmpty();
    }

    public static void clear() {
        lastBaseConfig = null;
        lastBatchResults = new ArrayList<>();
    }
}
