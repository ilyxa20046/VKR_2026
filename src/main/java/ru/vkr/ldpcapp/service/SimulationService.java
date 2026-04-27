package ru.vkr.ldpcapp.service;

import javafx.concurrent.Task;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.config.SimulationConfigFactory;
import ru.vkr.ldpcapp.service.phy.channel.ChannelEngine;
import ru.vkr.ldpcapp.service.phy.codec.CodecEngine;
import ru.vkr.ldpcapp.service.phy.crc.CrcEngine;
import ru.vkr.ldpcapp.service.phy.metrics.PhyMetricsEngine;
import ru.vkr.ldpcapp.service.phy.runner.ExperimentRunner;
import ru.vkr.ldpcapp.service.phy.stats.StatsMath;
import ru.vkr.ldpcapp.service.phy.transport.BitTransport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

public class SimulationService {

    private final CrcEngine crcEngine = new CrcEngine();
    private final BitTransport bitTransport = new BitTransport();
    private final StatsMath statsMath = new StatsMath();
    private final ChannelEngine channelEngine = new ChannelEngine();
    private final CodecEngine codecEngine = new CodecEngine();
    private final PhyMetricsEngine phyMetricsEngine = new PhyMetricsEngine();

    private final ExperimentRunner experimentRunner = new ExperimentRunner(
            crcEngine,
            bitTransport,
            statsMath,
            channelEngine,
            codecEngine,
            phyMetricsEngine
    );

    public Task<List<ResultPoint>> createTask(SimulationConfig config) {
        return new Task<>() {
            @Override
            protected List<ResultPoint> call() {
                List<Double> snrPoints = SimulationConfigFactory.buildSnrPoints(config);
                int totalPoints = Math.max(1, snrPoints.size());

                updateProgress(0, totalPoints);
                updateMessage("Подготовка эксперимента...");

                if (snrPoints.isEmpty()) {
                    return List.of();
                }

                int workers = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
                workers = Math.min(workers, snrPoints.size());

                ExecutorService pool = Executors.newFixedThreadPool(workers, r -> {
                    Thread t = new Thread(r, "ldpc-snr-worker");
                    t.setDaemon(true);
                    return t;
                });

                CompletionService<IndexedPointResult> completion = new ExecutorCompletionService<>(pool);
                List<Future<IndexedPointResult>> futures = new ArrayList<>(snrPoints.size());

                try {
                    for (int i = 0; i < snrPoints.size(); i++) {
                        final int index = i;
                        final double snrDb = snrPoints.get(i);

                        Future<IndexedPointResult> f = completion.submit(() -> {
                            // Локальные движки на задачу, без shared state между потоками
                            ExperimentRunner localRunner = new ExperimentRunner(
                                    new CrcEngine(),
                                    new BitTransport(),
                                    new StatsMath(),
                                    new ChannelEngine(),
                                    new CodecEngine(),
                                    new PhyMetricsEngine()
                            );
                            ResultPoint point = localRunner.simulatePoint(snrDb, config, index);
                            return new IndexedPointResult(index, point);
                        });

                        futures.add(f);
                    }

                    List<IndexedPointResult> done = new ArrayList<>(snrPoints.size());
                    int finished = 0;

                    while (finished < snrPoints.size()) {
                        if (isCancelled()) {
                            for (Future<IndexedPointResult> f : futures) {
                                f.cancel(true);
                            }
                            updateMessage("Расчёт отменён");
                            return List.of();
                        }

                        Future<IndexedPointResult> future = completion.poll();
                        if (future == null) {
                            updateMessage("Идёт расчёт... завершено точек: " + finished + "/" + totalPoints);
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException("Расчёт прерван", e);
                            }
                            continue;
                        }
                        if (future == null) {
                            updateMessage("Идёт расчёт... завершено точек: " + finished + "/" + totalPoints);
                            continue;
                        }

                        IndexedPointResult result = future.get();
                        done.add(result);
                        finished++;

                        updateProgress(finished, totalPoints);
                        updateMessage("Рассчитано точек SNR: " + finished + "/" + totalPoints);
                    }

                    done.sort(Comparator.comparingInt(IndexedPointResult::index));
                    List<ResultPoint> ordered = new ArrayList<>(done.size());
                    for (IndexedPointResult r : done) {
                        ordered.add(r.point());
                    }

                    updateMessage("Расчёт завершён: " + ordered.size() + " точек");
                    return ordered;

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Расчёт прерван", e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("Ошибка при расчёте точки SNR", e.getCause());
                } finally {
                    pool.shutdownNow();
                }
            }
        };
    }

    public List<ResultPoint> runBlocking(SimulationConfig config) {
        List<ResultPoint> results = new ArrayList<>();
        List<Double> snrPoints = SimulationConfigFactory.buildSnrPoints(config);
        for (int i = 0; i < snrPoints.size(); i++) {
            results.add(experimentRunner.simulatePoint(snrPoints.get(i), config, i));
        }
        return results;
    }

    private record IndexedPointResult(int index, ResultPoint point) {}
}