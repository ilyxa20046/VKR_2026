package ru.vkr.ldpcapp.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ru.vkr.ldpcapp.model.ExperimentSummary;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.BerTheoryService;
import ru.vkr.ldpcapp.service.ChapterThreeMaterialsService;
import ru.vkr.ldpcapp.service.ChartInteractionService;
import ru.vkr.ldpcapp.service.CompareSession;
import ru.vkr.ldpcapp.service.ExperimentSession;
import ru.vkr.ldpcapp.service.ExportService;
import ru.vkr.ldpcapp.service.ReportService;
import ru.vkr.ldpcapp.service.config.SimulationConfigFactory;
import ru.vkr.ldpcapp.service.config.SimulationConfigProfiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javafx.util.StringConverter;

public class ResultsController {

    private final ExportService exportService = new ExportService();
    private final ReportService reportService = new ReportService();
    private final SimulationConfigProfiles configProfiles = new SimulationConfigProfiles();
    private final ChapterThreeMaterialsService chapterThreeMaterialsService = new ChapterThreeMaterialsService();
    private final ChartInteractionService chartInteractionService = new ChartInteractionService();
    private final BerTheoryService berTheoryService = new BerTheoryService();

    private SimulationConfig currentConfig;
    private List<ResultPoint> currentPoints = List.of();
    private String currentDetailedReport = "";
    private String currentChapterThreeMaterials = "";

    @FXML private Label bestBerGainLabel;
    @FXML private Label bestBlerGainLabel;
    @FXML private Label peakThroughputLabel;
    @FXML private Label peakSpectralEfficiencyLabel;
    @FXML private Label requiredSnrBerLabel;
    @FXML private Label requiredSnrBlerLabel;
    @FXML private Label averageIterationsLabel;
    @FXML private Label successRatioLabel;
    @FXML private Label averageThroughputLabel;
    @FXML private Label averageSpectralEfficiencyLabel;
    @FXML private Label blerCriterionLabel;
    @FXML private Label nrChainStatusLabel;
    @FXML private Label exportStatusLabel;

    @FXML private Label resultsModulationChip;
    @FXML private Label resultsChannelChip;
    @FXML private Label resultsWaveformChip;
    @FXML private Label resultsSpatialChip;
    @FXML private Label resultsLdpcChip;
    @FXML private Label resultsScenarioModeChip;

    @FXML private CheckBox defenseModeCheckBox;
    @FXML private Label compareScenarioStatusChip;
    @FXML private HBox defenseSummaryBox;
    @FXML private Label defenseBerLabel;
    @FXML private Label defenseBlerLabel;
    @FXML private Label defenseThroughputLabel;
    @FXML private Label defenseSnrLabel;

    @FXML private LineChart<Number, Number> berChart;
    @FXML private LineChart<Number, Number> blerChart;
    @FXML private LineChart<Number, Number> throughputChart;
    @FXML private LineChart<Number, Number> spectralChart;

    @FXML private TableView<ResultPoint> resultsTable;
    @FXML private TableColumn<ResultPoint, Number> snrColumn;
    @FXML private TableColumn<ResultPoint, Number> berUncodedColumn;
    @FXML private TableColumn<ResultPoint, Number> berLdpcColumn;
    @FXML private TableColumn<ResultPoint, Number> blerUncodedColumn;
    @FXML private TableColumn<ResultPoint, Number> blerLdpcColumn;
    @FXML private TableColumn<ResultPoint, Number> iterationsColumn;
    @FXML private TableColumn<ResultPoint, Number> successColumn;
    @FXML private TableColumn<ResultPoint, Number> throughputColumn;
    @FXML private TableColumn<ResultPoint, Number> spectralEfficiencyColumn;

    @FXML private TextArea narrativeArea;

    @FXML
    public void initialize() {
        berChart.getStyleClass().add("chart-mpl");
        blerChart.getStyleClass().add("chart-mpl");
        throughputChart.getStyleClass().add("chart-mpl");
        spectralChart.getStyleClass().add("chart-mpl");
        configureTable();
        updateDefenseModeState(false);
        updateCompareScenarioStatus();

        if (ExperimentSession.hasResults()) {
            currentConfig = ExperimentSession.getLastConfig();
            setResults(ExperimentSession.getLastResults());
            exportStatusLabel.setText("Если в gain-метриках указано «ошибки не наблюдались», это означает отсутствие ошибок в текущей выборке эксперимента.");
        } else {
            currentConfig = configProfiles.recommendedProfile();
            currentConfig.setWaveform(SimulationConfig.WAVEFORM_OFDM64);
            currentConfig.setSpatialMode(SimulationConfig.SPATIAL_SISO);
            currentConfig.setCyclicPrefix(SimulationConfigFactory.normalizeCyclicPrefix(8, currentConfig.getWaveform()));
            currentConfig.setEqualizerMode(SimulationConfig.EQUALIZER_ZF);
            loadDemoData();
            exportStatusLabel.setText("На данный момент открыты демонстрационные результаты.");
        }
    }

    public void setResults(List<ResultPoint> points) {
        currentPoints = points == null ? List.of() : List.copyOf(points);
        resultsTable.setItems(FXCollections.observableArrayList(currentPoints));
        updateCharts(currentPoints);
        updateSummary(currentPoints);
        updateNarrative(currentPoints);
    }

    private void configureTable() {
        snrColumn.setCellValueFactory(new PropertyValueFactory<>("snr"));
        berUncodedColumn.setCellValueFactory(new PropertyValueFactory<>("berUncoded"));
        berLdpcColumn.setCellValueFactory(new PropertyValueFactory<>("berLdpc"));
        blerUncodedColumn.setCellValueFactory(new PropertyValueFactory<>("blerUncoded"));
        blerLdpcColumn.setCellValueFactory(new PropertyValueFactory<>("blerLdpc"));
        iterationsColumn.setCellValueFactory(new PropertyValueFactory<>("averageIterations"));
        successColumn.setCellValueFactory(new PropertyValueFactory<>("successRatio"));
        throughputColumn.setCellValueFactory(new PropertyValueFactory<>("effectiveThroughputMbps"));
        spectralEfficiencyColumn.setCellValueFactory(new PropertyValueFactory<>("spectralEfficiency"));
    }

    private void updateCharts(List<ResultPoint> points) {
        berChart.getData().clear();
        blerChart.getData().clear();
        if (throughputChart != null) throughputChart.getData().clear();
        if (spectralChart != null) spectralChart.getData().clear();

        if (points == null || points.isEmpty()) {
            return;
        }

        XYChart.Series<Number, Number> berUncodedSeries = new XYChart.Series<>();
        berUncodedSeries.setName("BER без кодирования");

        XYChart.Series<Number, Number> berLdpcSeries = new XYChart.Series<>();
        berLdpcSeries.setName("BER LDPC");

        XYChart.Series<Number, Number> blerUncodedSeries = new XYChart.Series<>();
        blerUncodedSeries.setName("BLER без кодирования");

        XYChart.Series<Number, Number> blerLdpcSeries = new XYChart.Series<>();
        blerLdpcSeries.setName("BLER LDPC");

        XYChart.Series<Number, Number> throughputSeries = new XYChart.Series<>();
        throughputSeries.setName("Полезная скорость LDPC");

        XYChart.Series<Number, Number> spectralSeries = new XYChart.Series<>();
        spectralSeries.setName("Спектральная эффективность LDPC");

        XYChart.Series<Number, Number> berTheorySeries = new XYChart.Series<>();
        berTheorySeries.setName("BER теория (AWGN, без кодирования)");

        XYChart.Series<Number, Number> berBpskTheorySeries = new XYChart.Series<>();
        berBpskTheorySeries.setName("BER теория BPSK (AWGN, Q-функция)");

        XYChart.Series<Number, Number> shannonBoundSeries = new XYChart.Series<>();
        shannonBoundSeries.setName("Граница Шеннона (R)");

        boolean isAwgn = currentConfig != null
                && SimulationConfig.CHANNEL_AWGN.equals(currentConfig.getChannelModel());

        double minSnr = Double.POSITIVE_INFINITY;
        double maxSnr = Double.NEGATIVE_INFINITY;
        double maxThroughput = 0.0;
        double maxSpectral = 0.0;

        for (ResultPoint point : points) {
            double snr = point.getSnr();
            minSnr = Math.min(minSnr, snr);
            maxSnr = Math.max(maxSnr, snr);

            berUncodedSeries.getData().add(new XYChart.Data<>(snr, toLog10(point.getBerUncoded())));
            berLdpcSeries.getData().add(new XYChart.Data<>(snr, toLog10(point.getBerLdpc())));
            blerUncodedSeries.getData().add(new XYChart.Data<>(snr, toLog10(point.getBlerUncoded())));
            blerLdpcSeries.getData().add(new XYChart.Data<>(snr, toLog10(point.getBlerLdpc())));
            throughputSeries.getData().add(new XYChart.Data<>(snr, point.getEffectiveThroughputMbps()));
            spectralSeries.getData().add(new XYChart.Data<>(snr, point.getSpectralEfficiency()));

            maxThroughput = Math.max(maxThroughput, point.getEffectiveThroughputMbps());
            maxSpectral = Math.max(maxSpectral, point.getSpectralEfficiency());

            if (isAwgn && currentConfig != null) {
                double berTheory = berTheoryService.theoreticalBerAwgnUncoded(
                        currentConfig.getModulation(),
                        currentConfig.getSnrDomain(),
                        snr
                );
                berTheorySeries.getData().add(new XYChart.Data<>(snr, toLog10(berTheory)));
            }
        }

        if (isAwgn && currentConfig != null) {
            // BPSK Q-curve (reference) over x-range
            double step = 0.25;
            for (double snr = minSnr; snr <= maxSnr + 1e-9; snr += step) {
                double ebN0DbForBpsk = snr; // for BPSK reference
                double berBpsk = berTheoryService.theoreticalBerBpskAwgnFromEbN0(ebN0DbForBpsk);
                berBpskTheorySeries.getData().add(new XYChart.Data<>(snr, toLog10(berBpsk)));
            }

            // Shannon bound for current code rate as vertical line
            double codeRate = SimulationConfigFactory.getCodeRate(currentConfig);
            double shEbN0Db = berTheoryService.shannonLimitEbN0Db(codeRate);
            double shX = berTheoryService.convertEbN0ToSnrDomain(
                    shEbN0Db,
                    currentConfig.getSnrDomain(),
                    currentConfig.getModulation(),
                    codeRate
            );

            shannonBoundSeries.getData().add(new XYChart.Data<>(shX, -6.0));
            shannonBoundSeries.getData().add(new XYChart.Data<>(shX, 0.0));
        }

        if (isAwgn) {
            berChart.getData().setAll(
                    berUncodedSeries,
                    berLdpcSeries,
                    berTheorySeries,
                    berBpskTheorySeries,
                    shannonBoundSeries
            );
        } else {
            berChart.getData().setAll(berUncodedSeries, berLdpcSeries);
        }

        blerChart.getData().setAll(blerUncodedSeries, blerLdpcSeries);
        if (throughputChart != null) throughputChart.getData().setAll(throughputSeries);
        if (spectralChart != null) spectralChart.getData().setAll(spectralSeries);

        configureBerBlerAxes(minSnr, maxSnr);
        configureMetricAxes(minSnr, maxSnr, maxThroughput, maxSpectral);

        chartInteractionService.installPointTooltips(berChart, "SNR (dB)", "BER", true);
        chartInteractionService.installPointTooltips(blerChart, "SNR (dB)", "BLER", true);
        if (throughputChart != null) {
            chartInteractionService.installPointTooltips(throughputChart, "SNR (dB)", "Throughput (Mbps)", false);
        }
        if (spectralChart != null) {
            chartInteractionService.installPointTooltips(spectralChart, "SNR (dB)", "Spectral (bit/s/Hz)", false);
        }

        chartInteractionService.enableZoomAndReset(berChart);
        chartInteractionService.enableZoomAndReset(blerChart);
        if (throughputChart != null) chartInteractionService.enableZoomAndReset(throughputChart);
        if (spectralChart != null) chartInteractionService.enableZoomAndReset(spectralChart);
    }

    private void configureBerBlerAxes(double minSnr, double maxSnr) {
        NumberAxis berX = (NumberAxis) berChart.getXAxis();
        NumberAxis berY = (NumberAxis) berChart.getYAxis();
        NumberAxis blerX = (NumberAxis) blerChart.getXAxis();
        NumberAxis blerY = (NumberAxis) blerChart.getYAxis();

        double xMin = Math.floor(minSnr) - 0.5;
        double xMax = Math.ceil(maxSnr) + 0.5;

        berX.setAutoRanging(false);
        berX.setLowerBound(xMin);
        berX.setUpperBound(xMax);
        berX.setTickUnit(1.0);

        blerX.setAutoRanging(false);
        blerX.setLowerBound(xMin);
        blerX.setUpperBound(xMax);
        blerX.setTickUnit(1.0);

        berY.setAutoRanging(false);
        berY.setLowerBound(-6.0);
        berY.setUpperBound(0.0);
        berY.setTickUnit(1.0);

        blerY.setAutoRanging(false);
        blerY.setLowerBound(-6.0);
        blerY.setUpperBound(0.0);
        blerY.setTickUnit(1.0);

        berX.setLabel("SNR (дБ)");
        blerX.setLabel("SNR (дБ)");
        berY.setLabel("log10(BER)");
        blerY.setLabel("log10(BLER)");


        javafx.util.StringConverter<Number> logFormatter = new javafx.util.StringConverter<>() {
            @Override
            public String toString(Number value) {
                return "10^" + (int) Math.round(value.doubleValue());
            }
            @Override
            public Number fromString(String string) {
                return 0;
            }
        };

        berY.setTickLabelFormatter(logFormatter);
        blerY.setTickLabelFormatter(logFormatter);
    }

    private void configureMetricAxes(double minSnr, double maxSnr, double maxThroughput, double maxSpectral) {
        if (throughputChart != null) {
            NumberAxis x = (NumberAxis) throughputChart.getXAxis();
            NumberAxis y = (NumberAxis) throughputChart.getYAxis();
            x.setAutoRanging(false);
            x.setLowerBound(Math.floor(minSnr) - 0.5);
            x.setUpperBound(Math.ceil(maxSnr) + 0.5);
            x.setTickUnit(1.0);

            y.setAutoRanging(false);
            y.setLowerBound(0.0);
            y.setUpperBound(Math.max(1.0, maxThroughput * 1.15));
            y.setTickUnit(Math.max(1.0, y.getUpperBound() / 8.0));
            x.setLabel("SNR, дБ");
            y.setLabel("Скорость передачи, Мбит/с");
        }

        if (spectralChart != null) {
            NumberAxis x = (NumberAxis) spectralChart.getXAxis();
            NumberAxis y = (NumberAxis) spectralChart.getYAxis();
            x.setAutoRanging(false);
            x.setLowerBound(Math.floor(minSnr) - 0.5);
            x.setUpperBound(Math.ceil(maxSnr) + 0.5);
            x.setTickUnit(1.0);

            y.setAutoRanging(false);
            y.setLowerBound(0.0);
            y.setUpperBound(Math.max(0.2, maxSpectral * 1.15));
            y.setTickUnit(Math.max(0.1, y.getUpperBound() / 8.0));
            x.setLabel("SNR, дБ");
            y.setLabel("бит/с/Гц");
        }
    }

    private void updateSummary(List<ResultPoint> points) {
        ExperimentSummary summary = ExperimentSummary.from(points);

        bestBerGainLabel.setText(formatGain(summary.getBestBerGain()));
        bestBlerGainLabel.setText(formatGain(summary.getBestBlerGain()));
        peakThroughputLabel.setText(formatThroughput(summary.getPeakThroughputMbps()));
        peakSpectralEfficiencyLabel.setText(formatEfficiency(summary.getPeakSpectralEfficiency()));
        requiredSnrBerLabel.setText(formatDb(summary.getRequiredSnrBerDb()));
        requiredSnrBlerLabel.setText(formatDb(summary.getRequiredSnrBlerDb()));
        averageIterationsLabel.setText(formatFixed(summary.getAverageIterations()));
        successRatioLabel.setText(formatFixed(summary.getAverageSuccessRatio() * 100.0) + "%");
        averageThroughputLabel.setText(formatThroughput(summary.getAverageThroughputMbps()));
        averageSpectralEfficiencyLabel.setText(formatEfficiency(summary.getAverageSpectralEfficiency()));

        if (currentConfig != null) {
            resultsModulationChip.setText(currentConfig.getModulation());
            resultsChannelChip.setText(currentConfig.getChannelModel());
            resultsWaveformChip.setText(currentConfig.getWaveform());
            resultsSpatialChip.setText(currentConfig.getSpatialMode());
            resultsLdpcChip.setText(SimulationConfigFactory.getProfileDisplayName(currentConfig.getLdpcProfile(), currentConfig.getLiftingSize()));
            resultsScenarioModeChip.setText("Канал: " + currentConfig.getChannelModel() + " / Эквалайзер: " + currentConfig.getEqualizerMode());

            if (blerCriterionLabel != null) {
                blerCriterionLabel.setText(SimulationConfigFactory.getBlerCriterionUiName(currentConfig.getBlerCriterion()));
            }
            if (nrChainStatusLabel != null) {
                nrChainStatusLabel.setText(
                        "CRC " + (currentConfig.isCrcEnabled() ? "вкл" : "выкл") +
                                " / Сегментация " + (currentConfig.isSegmentationEnabled() ? "вкл" : "выкл") +
                                " / Согл. скорости " + (currentConfig.isRateMatchingEnabled() ? "вкл" : "выкл")
                );
            }
        }

        defenseBerLabel.setText(formatGain(summary.getBestBerGain()));
        defenseBlerLabel.setText(formatGain(summary.getBestBlerGain()));
        defenseThroughputLabel.setText(formatThroughput(summary.getPeakThroughputMbps()));
        defenseSnrLabel.setText(formatDb(summary.getRequiredSnrBerDb()));
    }

    private void updateNarrative(List<ResultPoint> points) {
        if (points == null || points.isEmpty()) {
            currentDetailedReport = "";
            currentChapterThreeMaterials = "";
            narrativeArea.setText("Результаты отсутствуют.");
            return;
        }

        String narrative = reportService.buildShortNarrative(currentConfig, points);
        currentDetailedReport = reportService.buildDetailedReport(currentConfig, points);
        currentChapterThreeMaterials = chapterThreeMaterialsService.buildChapterThreeMaterials(currentConfig, points);

        narrativeArea.setText(
                narrative
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + buildConfidenceAppendix(points)
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + buildReliabilityWarning(points)
        );
    }

    @FXML
    private void onToggleDefenseMode() {
        boolean enabled = defenseModeCheckBox != null && defenseModeCheckBox.isSelected();
        updateDefenseModeState(enabled);
        exportStatusLabel.setText(enabled
                ? "Режим демонстрации включён: акцент на крупных KPI и демонстрационных итогах."
                : "Режим демонстрации выключен: доступен полный аналитический режим.");
    }

    @FXML
    private void onStageForCompare() {
        if (currentConfig == null || currentPoints == null || currentPoints.isEmpty()) {
            exportStatusLabel.setText("Нет результатов для подготовки сценария A. Сначала выполните моделирование.");
            return;
        }
        CompareSession.stageCurrentExperiment("Сценарий A", currentConfig, currentPoints);
        updateCompareScenarioStatus();
        exportStatusLabel.setText("Сценарий A выбран. Выполните второй запуск и нажмите «Сравнить A/B».");
    }

    @FXML
    private void onCompareWithStaged() {
        if (currentConfig == null || currentPoints == null || currentPoints.isEmpty()) {
            exportStatusLabel.setText("Нет текущих результатов для сравнения. Сначала выполните моделирование.");
            return;
        }
        if (!CompareSession.hasStagedScenario()) {
            exportStatusLabel.setText("Сначала сохраните один из запусков как сценарий A.");
            return;
        }
        CompareSession.pairWithCurrentExperiment("Сценарий B", currentConfig, currentPoints);
        updateCompareScenarioStatus();
        exportStatusLabel.setText("Пара A/B готова. Откройте вкладку «Сравнение».");
    }

    @FXML
    private void onExportCsv() {
        if (currentPoints == null || currentPoints.isEmpty()) {
            exportStatusLabel.setText("Нет результатов для экспорта. Сначала выполните моделирование.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранение CSV-результатов");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        fileChooser.setInitialFileName(exportService.buildSuggestedCsvFileName());

        Window window = resultsTable.getScene() != null ? resultsTable.getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            exportStatusLabel.setText("Экспорт CSV отменён пользователем.");
            return;
        }

        try {
            Path savedPath = exportService.exportResultsCsv(file.toPath(), currentConfig, currentPoints);
            exportStatusLabel.setText("CSV успешно сохранён: " + savedPath.toAbsolutePath());
        } catch (IOException exception) {
            exportStatusLabel.setText("Ошибка сохранения CSV: " + exception.getMessage());
        }
    }

    @FXML
    private void onExportBerChart() {
        exportChart(berChart, "ber_chart", "Сохранение графика BER");
    }

    @FXML
    private void onExportBlerChart() {
        exportChart(blerChart, "bler_chart", "Сохранение графика BLER");
    }

    @FXML
    private void onExportExperimentBundle() {
        if (currentPoints == null || currentPoints.isEmpty()) {
            exportStatusLabel.setText("Нет результатов для пакетного экспорта. Сначала выполните моделирование.");
            return;
        }
        if (currentDetailedReport == null || currentDetailedReport.isBlank()) {
            exportStatusLabel.setText("Полный отчёт недоступен. Сначала выполните моделирование.");
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите каталог для экспорта полного комплекта");
        Window window = resultsTable.getScene() != null ? resultsTable.getScene().getWindow() : null;
        File selectedDirectory = directoryChooser.showDialog(window);
        if (selectedDirectory == null) {
            exportStatusLabel.setText("Пакетный экспорт отменён пользователем.");
            return;
        }

        try {
            Path bundleDirectory = exportService.exportExperimentBundle(
                    selectedDirectory.toPath(),
                    currentConfig,
                    currentPoints,
                    currentDetailedReport,
                    berChart,
                    blerChart
            );
            exportStatusLabel.setText("Комплект материалов успешно сохранён: " + bundleDirectory.toAbsolutePath());
        } catch (IOException exception) {
            exportStatusLabel.setText("Ошибка пакетного экспорта: " + exception.getMessage());
        }
    }

    @FXML
    private void onCopyNarrative() {
        String text = narrativeArea.getText();
        if (text == null || text.isBlank()) {
            exportStatusLabel.setText("Текстовый вывод пуст. Копирование не выполнено.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        exportStatusLabel.setText("Краткий аналитический вывод скопирован в буфер обмена.");
    }

    @FXML
    private void onCopyDetailedReport() {
        if (currentDetailedReport == null || currentDetailedReport.isBlank()) {
            exportStatusLabel.setText("Полный отчёт пока недоступен. Сначала выполните моделирование.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(currentDetailedReport);
        Clipboard.getSystemClipboard().setContent(content);
        exportStatusLabel.setText("Развёрнутый отчёт скопирован в буфер обмена.");
    }

    @FXML
    private void onSaveDetailedReport() {
        if (currentDetailedReport == null || currentDetailedReport.isBlank()) {
            exportStatusLabel.setText("Нет отчёта для сохранения. Сначала выполните моделирование.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранение текстового отчёта");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));
        fileChooser.setInitialFileName(exportService.buildSuggestedReportFileName());

        Window window = resultsTable.getScene() != null ? resultsTable.getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            exportStatusLabel.setText("Сохранение отчёта отменено пользователем.");
            return;
        }

        try {
            Path savedPath = exportService.exportTextReport(file.toPath(), currentDetailedReport);
            exportStatusLabel.setText("TXT-отчёт успешно сохранён: " + savedPath.toAbsolutePath());
        } catch (IOException exception) {
            exportStatusLabel.setText("Ошибка сохранения TXT-отчёта: " + exception.getMessage());
        }
    }

    @FXML
    private void onCopyChapterThreeMaterials() {
        if (currentChapterThreeMaterials == null || currentChapterThreeMaterials.isBlank()) {
            exportStatusLabel.setText("Материалы для главы 3 недоступны. Сначала выполните моделирование.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(currentChapterThreeMaterials);
        Clipboard.getSystemClipboard().setContent(content);
        exportStatusLabel.setText("Материалы для главы 3 скопированы в буфер обмена.");
    }

    @FXML
    private void onSaveChapterThreeMaterials() {
        if (currentChapterThreeMaterials == null || currentChapterThreeMaterials.isBlank()) {
            exportStatusLabel.setText("Материалы для главы 3 отсутствуют. Сначала выполните моделирование.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранение материалов для главы 3");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));
        fileChooser.setInitialFileName(exportService.buildSuggestedChapterThreeFileName());

        Window window = resultsTable.getScene() != null ? resultsTable.getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            exportStatusLabel.setText("Сохранение материалов главы 3 отменено пользователем.");
            return;
        }

        try {
            Path savedPath = exportService.exportChapterThreeMaterials(file.toPath(), currentConfig, currentPoints);
            exportStatusLabel.setText("Материалы для главы 3 сохранены: " + savedPath.toAbsolutePath());
        } catch (IOException exception) {
            exportStatusLabel.setText("Ошибка сохранения материалов главы 3: " + exception.getMessage());
        }
    }

    @FXML
    private void onSavePresentationSummary() {
        if (currentPoints == null || currentPoints.isEmpty()) {
            exportStatusLabel.setText("Нет результатов для краткой презентационной сводки. Сначала выполните моделирование.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранение краткой сводки для защиты");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));
        fileChooser.setInitialFileName(exportService.buildSuggestedPresentationSummaryFileName("defense_summary"));

        Window window = resultsTable.getScene() != null ? resultsTable.getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            exportStatusLabel.setText("Сохранение краткой сводки отменено пользователем.");
            return;
        }

        try {
            Path savedPath = exportService.exportPresentationSummary(file.toPath(), currentConfig, currentPoints);
            exportStatusLabel.setText("Краткая сводка для защиты сохранена: " + savedPath.toAbsolutePath());
        } catch (IOException exception) {
            exportStatusLabel.setText("Ошибка сохранения краткой сводки: " + exception.getMessage());
        }
    }

    @FXML
    private void onSaveWordFriendlyReport() {
        if (currentPoints == null || currentPoints.isEmpty()) {
            exportStatusLabel.setText("Нет результатов для Word-friendly экспорта. Сначала выполните моделирование.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранение Word-friendly отчёта");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));
        fileChooser.setInitialFileName(exportService.buildSuggestedDocxFriendlyFileName("single_word_friendly"));

        Window window = resultsTable.getScene() != null ? resultsTable.getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            exportStatusLabel.setText("Сохранение Word-friendly отчёта отменено пользователем.");
            return;
        }

        try {
            String content = exportService.buildExperimentDocxFriendlyText(currentConfig, currentPoints);
            Path savedPath = exportService.exportDocxFriendlyText(file.toPath(), content);
            exportStatusLabel.setText("Word-friendly отчёт сохранён: " + savedPath.toAbsolutePath());
        } catch (IOException exception) {
            exportStatusLabel.setText("Ошибка сохранения Word-friendly отчёта: " + exception.getMessage());
        }
    }

    @FXML
    private void onSaveHtmlReport() {
        if (currentPoints == null || currentPoints.isEmpty()) {
            exportStatusLabel.setText("Нет результатов для HTML-экспорта. Сначала выполните моделирование.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранение HTML-отчёта");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML files (*.html)", "*.html"));
        fileChooser.setInitialFileName(exportService.buildSuggestedHtmlFileName("single_report"));

        Window window = resultsTable.getScene() != null ? resultsTable.getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            exportStatusLabel.setText("Сохранение HTML-отчёта отменено пользователем.");
            return;
        }

        try {
            String content = exportService.buildExperimentHtmlText(currentConfig, currentPoints);
            Path savedPath = exportService.exportHtmlReport(file.toPath(), content);
            exportStatusLabel.setText("HTML-отчёт сохранён: " + savedPath.toAbsolutePath());
        } catch (IOException exception) {
            exportStatusLabel.setText("Ошибка сохранения HTML-отчёта: " + exception.getMessage());
        }
    }

    private void exportChart(LineChart<Number, Number> chart, String chartKey, String dialogTitle) {
        if (chart == null) {
            exportStatusLabel.setText("График недоступен для экспорта.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(dialogTitle);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png"));
        fileChooser.setInitialFileName(exportService.buildSuggestedChartFileName(chartKey));

        Window window = resultsTable.getScene() != null ? resultsTable.getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            exportStatusLabel.setText("Экспорт графика отменён пользователем.");
            return;
        }

        try {
            Path savedPath = exportService.exportChartPng(file.toPath(), chart);
            exportStatusLabel.setText("PNG успешно сохранён: " + savedPath.toAbsolutePath());
        } catch (IOException exception) {
            exportStatusLabel.setText("Ошибка сохранения PNG: " + exception.getMessage());
        }
    }

    private void loadDemoData() {
        setResults(List.of(
                new ResultPoint(0.0, 0.1620, 0.0810, 0.9900, 0.9200, 11.90, 0.23, 0.80, 0.04),
                new ResultPoint(1.0, 0.1210, 0.0490, 0.9600, 0.7600, 11.10, 0.38, 2.40, 0.12),
                new ResultPoint(2.0, 0.0810, 0.0220, 0.8600, 0.4800, 9.80, 0.57, 5.20, 0.26),
                new ResultPoint(3.0, 0.0510, 0.0090, 0.6900, 0.2400, 8.10, 0.73, 7.60, 0.38),
                new ResultPoint(4.0, 0.0280, 0.0032, 0.4300, 0.1010, 6.60, 0.85, 8.99, 0.45),
                new ResultPoint(5.0, 0.0130, 0.0011, 0.2200, 0.0360, 5.10, 0.92, 9.64, 0.48),
                new ResultPoint(6.0, 0.0054, 0.00032, 0.0820, 0.0100, 4.10, 0.97, 9.90, 0.50),
                new ResultPoint(7.0, 0.0018, 0.00008, 0.0260, 0.0022, 3.30, 0.99, 9.98, 0.50),
                new ResultPoint(8.0, 0.00055, 0.00001, 0.0075, 0.0005, 2.80, 0.995, 10.00, 0.50)
        ));
    }

    private void updateDefenseModeState(boolean enabled) {
        if (defenseSummaryBox != null) {
            defenseSummaryBox.setVisible(enabled);
            defenseSummaryBox.setManaged(enabled);
        }
        if (defenseModeCheckBox != null) {
            defenseModeCheckBox.setSelected(enabled);
        }
    }

    private void applyMplStyle(LineChart<Number, Number> chart) {
        if (chart != null && !chart.getStyleClass().contains("chart-mpl")) {
            chart.getStyleClass().add("chart-mpl");
        }
    }

    private void updateCompareScenarioStatus() {
        if (compareScenarioStatusChip == null) return;

        compareScenarioStatusChip.getStyleClass().removeAll("compare-stage-pill-ready", "compare-stage-pill-complete");
        if (CompareSession.hasComparison()) {
            compareScenarioStatusChip.setText("Сравнение A/B готово");
            compareScenarioStatusChip.getStyleClass().add("compare-stage-pill-complete");
        } else if (CompareSession.hasStagedScenario()) {
            compareScenarioStatusChip.setText("Сценарий A сохранён");
            compareScenarioStatusChip.getStyleClass().add("compare-stage-pill-ready");
        } else {
            compareScenarioStatusChip.setText("Сценарий A не выбран");
        }
    }

    private String formatGain(double value) {
        if (Double.isInfinite(value)) return "ошибки не наблюдались";
        if (value >= 100.0) return String.format("%.0fx", value);
        if (value >= 10.0) return String.format("%.1fx", value);
        return String.format("%.2fx", value);
    }

    private String formatThroughput(double value) {
        return String.format("%.2f Мбит/с", value);
    }

    private String formatEfficiency(double value) {
        return String.format("%.2f бит/с/Гц", value);
    }

    private String formatDb(Double value) {
        return value == null ? "н/д" : String.format("%.2f дБ", value);
    }

    private String formatFixed(double value) {
        return String.format("%.2f", value);
    }

    private String buildConfidenceAppendix(List<ResultPoint> points) {
        if (points == null || points.isEmpty()) {
            return "Доверительные интервалы недоступны: нет данных.";
        }

        ResultPoint last = points.get(points.size() - 1);

        double avgRetx = points.stream().mapToDouble(ResultPoint::getAverageRetx).average().orElse(0.0);
        double harqSucc = points.stream().mapToDouble(ResultPoint::getHarqSuccessRatio).average().orElse(0.0);

        return String.format(
                "Статистическая достоверность:%n" +
                        "• уровень доверия: %.2f%n" +
                        "• BER LDPC, доверительный интервал (последняя точка): [%s; %s]%n" +
                        "• BLER LDPC, доверительный интервал (последняя точка): [%s; %s]%n" +
                        "• объём выборки последней точки: %d бит, %d блоков%n" +
                        "• HARQ, среднее число дополнительных передач на кодовое слово: %.2f%n" +
                        "• HARQ, доля успешного декодирования: %.2f%%",
                last.getConfidenceLevel(),
                formatSci(last.getBerLdpcCiLow()),
                formatSci(last.getBerLdpcCiHigh()),
                formatSci(last.getBlerLdpcCiLow()),
                formatSci(last.getBlerLdpcCiHigh()),
                last.getTotalBits(),
                last.getTotalBlocks(),
                avgRetx,
                harqSucc * 100.0
        );
    }

    private String formatSci(double value) {
        return String.format("%.3e", value);
    }

    private double toLog10(double value) {
        return Math.log10(Math.max(1e-8, value));
    }

    private String buildReliabilityWarning(List<ResultPoint> points) {
        if (points == null || points.isEmpty()) {
            return "";
        }

        long weak = points.stream()
                .filter(p -> p.getBitErrorsLdpc() < 20 || p.getBlockErrorsLdpc() < 20)
                .count();

        if (weak == 0) {
            return "Статистическая надежность: достаточная по всем точкам SNR.";
        }
        return "Внимание: в " + weak + " точк(ах) SNR мало событий ошибок (<20), оценка может быть нестабильной.";
    }
}