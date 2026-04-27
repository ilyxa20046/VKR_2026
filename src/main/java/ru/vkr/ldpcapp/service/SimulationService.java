package ru.vkr.ldpcapp.service;

import javafx.concurrent.Task;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.phy.channel.ChannelEngine;
import ru.vkr.ldpcapp.service.phy.crc.CrcEngine;
import ru.vkr.ldpcapp.service.phy.stats.StatsMath;
import ru.vkr.ldpcapp.service.phy.transport.BitTransport;
import ru.vkr.ldpcapp.service.phy.codec.CodecEngine;
import ru.vkr.ldpcapp.service.phy.metrics.PhyMetricsEngine;
import ru.vkr.ldpcapp.service.phy.runner.ExperimentRunner;
import ru.vkr.ldpcapp.service.config.SimulationConfigFactory;

import java.util.ArrayList;
import java.util.List;

public class SimulationService {
    private final CrcEngine crcEngine = new CrcEngine();
    private final BitTransport bitTransport = new BitTransport();
    private final StatsMath statsMath = new StatsMath();
    private final ChannelEngine channelEngine = new ChannelEngine();
    private final CodecEngine codecEngine = new CodecEngine();
    private final PhyMetricsEngine phyMetricsEngine = new PhyMetricsEngine();
    private final ExperimentRunner experimentRunner = new ExperimentRunner(
            crcEngine, bitTransport, statsMath, channelEngine, codecEngine, phyMetricsEngine
    );



    public Task<List<ResultPoint>> createTask(SimulationConfig config) {
        return new Task<>() {
            @Override
            protected List<ResultPoint> call() {
                List<Double> snrPoints = SimulationConfigFactory.buildSnrPoints(config);
                List<ResultPoint> results = new ArrayList<>();
                int totalPoints = Math.max(1, snrPoints.size());

                updateProgress(0, totalPoints);
                updateMessage("Подготовка OFDM/MIMO-like исследовательского эксперимента...");

                for (int i = 0; i < snrPoints.size(); i++) {
                    if (isCancelled()) {
                        updateMessage("Расчёт отменён");
                        break;
                    }

                    double snr = snrPoints.get(i);
                    updateMessage(String.format(
                            "Расчёт: %s / %s / %s / %s / %s, SNR = %.2f дБ (%d/%d)",
                            config.getModulation(),
                            config.getChannelModel(),
                            config.getWaveform(),
                            config.getSpatialMode(),
                            SimulationConfigFactory.getProfileDisplayName(config.getLdpcProfile(), config.getLiftingSize()),
                            snr,
                            i + 1,
                            totalPoints
                    ));
                    results.add(experimentRunner.simulatePoint(snr, config, i));
                    updateProgress(i + 1, totalPoints);
                }

                updateMessage("Исследовательское моделирование завершено");
                return results;
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
}
