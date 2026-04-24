package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExperimentSession {

    private static SimulationConfig lastConfig;
    private static List<ResultPoint> lastResults = new ArrayList<>();

    private ExperimentSession() {
    }

    public static void save(SimulationConfig config, List<ResultPoint> results) {
        lastConfig = config;
        lastResults = new ArrayList<>(results);
    }

    public static SimulationConfig getLastConfig() {
        return lastConfig;
    }

    public static List<ResultPoint> getLastResults() {
        return Collections.unmodifiableList(lastResults);
    }

    public static boolean hasResults() {
        return lastResults != null && !lastResults.isEmpty();
    }

    public static void clear() {
        lastConfig = null;
        lastResults = new ArrayList<>();
    }
}
