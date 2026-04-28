package ru.vkr.ldpcapp.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ru.vkr.ldpcapp.model.BatchScenarioResult;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.BatchFileService;
import ru.vkr.ldpcapp.service.BatchReportService;
import ru.vkr.ldpcapp.service.BatchService;
import ru.vkr.ldpcapp.service.BatchSession;
import ru.vkr.ldpcapp.service.ExperimentSession;
import ru.vkr.ldpcapp.service.ExportService;
import ru.vkr.ldpcapp.service.config.SimulationConfigFormatter;
import ru.vkr.ldpcapp.service.config.SimulationConfigProfiles;
import ru.vkr.ldpcapp.service.config.SimulationConfigFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BatchController {

    private final BatchService batchService = new BatchService();
    private final BatchReportService batchReportService = new BatchReportService();
    private final ExportService exportService = new ExportService();
    private final BatchFileService batchFileService = new BatchFileService();
    private final SimulationConfigProfiles configProfiles = new SimulationConfigProfiles();
    private final SimulationConfigFormatter configFormatter = new SimulationConfigFormatter();
    private SimulationConfig currentBaseConfig;
    private List<BatchScenarioResult> currentBatchResults = new ArrayList<>();
    private BatchScenarioResult highlightedBestScenario;

    @FXML
    private CheckBox bpskCheckBox;

    @FXML
    private CheckBox qpskCheckBox;

    @FXML
    private CheckBox qam16CheckBox;

    @FXML
    private CheckBox awgnCheckBox;

    @FXML
    private CheckBox rayleighCheckBox;

    @FXML
    private CheckBox educationalProfileCheckBox;

    @FXML
    private CheckBox qcProfileCheckBox;
    @FXML private CheckBox qam64CheckBox;
    @FXML private CheckBox qam256CheckBox;

    @FXML private CheckBox nrProfileCheckBox;
    @FXML private CheckBox turboProfileCheckBox;
    @FXML private CheckBox polarProfileCheckBox;

    @FXML private CheckBox rateR13CheckBox;
    @FXML private CheckBox rateR12CheckBox;
    @FXML private CheckBox rateR23CheckBox;
    @FXML private CheckBox rateR56CheckBox;

    @FXML
    private TextArea batchBaseConfigArea;

    @FXML
    private Label batchWaveformChip;

    @FXML
    private Label batchSpatialChip;

    @FXML
    private TextArea batchNarrativeArea;

    @FXML
    private ProgressBar batchProgressBar;

    @FXML
    private Label batchStatusLabel;

    @FXML
    private Button batchRunButton;

    @FXML
    private CheckBox defenseModeCheckBox;

    @FXML
    private HBox defenseSummaryBox;

    @FXML
    private Label defenseWinnerLabel;

    @FXML
    private Label defenseThroughputLabel;

    @FXML
    private Label defenseSpectralLabel;

    @FXML
    private Label defenseSnrLabel;

    @FXML
    private TableView<BatchScenarioResult> batchTable;

    @FXML
    private TableColumn<BatchScenarioResult, String> scenarioColumn;

    @FXML
    private TableColumn<BatchScenarioResult, String> modulationColumn;

    @FXML
    private TableColumn<BatchScenarioResult, String> channelColumn;

    @FXML
    private TableColumn<BatchScenarioResult, String> profileColumn;

    @FXML
    private TableColumn<BatchScenarioResult, String> bestBerGainColumn;

    @FXML
    private TableColumn<BatchScenarioResult, String> bestBlerGainColumn;

    @FXML
    private TableColumn<BatchScenarioResult, String> berEnergyGainColumn;

    @FXML
    private TableColumn<BatchScenarioResult, String> blerEnergyGainColumn;

    @FXML
    private TableColumn<BatchScenarioResult, String> throughputColumn;

    @FXML
    private TableColumn<BatchScenarioResult, String> spectralEfficiencyColumn;

    @FXML
    private TableColumn<BatchScenarioResult, String> requiredSnrBerColumn;

    @FXML
    private TableColumn<BatchScenarioResult, String> requiredSnrBlerColumn;

    @FXML
    private LineChart<Number, Number> batchBerChart;

    @FXML
    private LineChart<Number, Number> batchBlerChart;

    @FXML
    public void initialize() {
        configureTable();
        updateDefenseModeState(false);
        restoreSuggestedSelection();
        loadBaseConfigFromSession();
        batchProgressBar.setProgress(0.0);
        batchStatusLabel.setText("Выберите набор сценариев и запустите пакетный анализ для прямого сравнения нескольких режимов передачи.");
        batchNarrativeArea.setText("Пакетный анализ позволяет сравнить несколько комбинаций модуляции, канала и LDPC-профиля в рамках одного исследовательского запуска.");
        if (BatchSession.getLastBaseConfig() != null) {
            currentBaseConfig = copyConfig(BatchSession.getLastBaseConfig());
            updateBaseConfigPreview();
        }
        if (BatchSession.hasResults()) {
            currentBatchResults = new ArrayList<>(BatchSession.getLastBatchResults());
            updateBatchResults(currentBatchResults);
            batchStatusLabel.setText("Загружены последние результаты пакетного анализа из текущего сеанса приложения.");
        }
    }

    @FXML
    private void onLoadCurrentExperiment() {
        loadBaseConfigFromSession();
        batchStatusLabel.setText("Базовая конфигурация загружена из последнего одиночного эксперимента.");
    }

    @FXML
    private void onUseRecommendedProfile() {
        currentBaseConfig = configProfiles.recommendedProfile();
        updateBaseConfigPreview();
        batchStatusLabel.setText("Для пакетного анализа применён рекомендуемый исследовательский профиль.");
    }

    @FXML
    private void onSelectionChanged() {
        updateBaseConfigPreview();
    }

    @FXML
    private void onRunBatch() {
        List<String> modulations = selectedModulations();
        List<String> channels = selectedChannels();
        List<String> profiles = selectedProfiles();
        List<Double> rates = selectedRates();

        try {
            Task<List<BatchScenarioResult>> task =
                    batchService.createTask(currentBaseConfig, modulations, channels, profiles, rates);
            batchRunButton.setDisable(true);
            batchProgressBar.progressProperty().unbind();
            batchProgressBar.progressProperty().bind(task.progressProperty());
            batchStatusLabel.textProperty().unbind();
            batchStatusLabel.textProperty().bind(task.messageProperty());

            task.setOnSucceeded(event -> {
                batchProgressBar.progressProperty().unbind();
                batchStatusLabel.textProperty().unbind();
                batchRunButton.setDisable(false);
                batchProgressBar.setProgress(1.0);
                List<BatchScenarioResult> scenarios = task.getValue();
                BatchSession.save(currentBaseConfig, scenarios);
                updateBatchResults(scenarios);
                batchStatusLabel.setText("Пакетный анализ завершён. Ниже доступны сводка, графики и таблица результатов. Число сценариев: " + scenarios.size());
            });

            task.setOnFailed(event -> {
                batchProgressBar.progressProperty().unbind();
                batchStatusLabel.textProperty().unbind();
                batchRunButton.setDisable(false);
                batchProgressBar.setProgress(0.0);
                Throwable exception = task.getException();
                batchStatusLabel.setText(exception == null ? "Пакетный анализ завершился ошибкой." : exception.getMessage());
            });

            Thread worker = new Thread(task, "ldpc-batch-task");
            worker.setDaemon(true);
            worker.start();
        } catch (IllegalArgumentException exception) {
            batchStatusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void onExportBatchReport() {
        if (!ensureBatchResults()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить пакетный отчёт");
        chooser.setInitialFileName(exportService.buildSuggestedBatchReportFileName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
        var file = chooser.showSaveDialog(getWindow());
        if (file == null) {
            return;
        }

        try {
            exportService.exportBatchReport(file.toPath(), currentBaseConfig, currentBatchResults);
            batchStatusLabel.setText("Пакетный отчёт сохранён: " + file.getName());
        } catch (Exception exception) {
            batchStatusLabel.setText("Ошибка сохранения отчёта пакетного анализа: " + exception.getMessage());
        }
    }

    @FXML
    private void onExportBatchCsv() {
        if (!ensureBatchResults()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить CSV");
        chooser.setInitialFileName(exportService.buildSuggestedBatchCsvFileName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        var file = chooser.showSaveDialog(getWindow());
        if (file == null) {
            return;
        }

        try {
            exportService.exportBatchCsv(file.toPath(), currentBatchResults);
            batchStatusLabel.setText("CSV сохранён: " + file.getName());
        } catch (Exception exception) {
            batchStatusLabel.setText("Ошибка сохранения CSV: " + exception.getMessage());
        }
    }

    @FXML
    private void onExportBatchBundle() {
        if (!ensureBatchResults()) {
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Выберите каталог для комплекта материалов");
        var directory = chooser.showDialog(getWindow());
        if (directory == null) {
            return;
        }

        try {
            var bundlePath = exportService.exportBatchBundle(directory.toPath(), currentBaseConfig, currentBatchResults);
            batchStatusLabel.setText("Комплект материалов сохранён: " + bundlePath.getFileName());
        } catch (Exception exception) {
            batchStatusLabel.setText("Ошибка сохранения комплекта материалов: " + exception.getMessage());
        }
    }

    @FXML
    private void onCopyBatchReport() {
        if (!ensureBatchResults()) {
            return;
        }

        String report = exportService.buildBatchReportText(currentBaseConfig, currentBatchResults);
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(report);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        batchStatusLabel.setText("Отчёт скопирован в буфер обмена.");
    }

    @FXML
    private void onSavePresentationSummary() {
        if (!ensureBatchResults()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить краткую сводку для защиты");
        chooser.setInitialFileName(exportService.buildSuggestedPresentationSummaryFileName("batch_defense_summary"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
        var file = chooser.showSaveDialog(getWindow());
        if (file == null) {
            return;
        }

        try {
            exportService.exportBatchPresentationSummary(file.toPath(), currentBaseConfig, currentBatchResults);
            batchStatusLabel.setText("Сводка для защиты сохранена: " + file.getName());
        } catch (Exception exception) {
            batchStatusLabel.setText("Ошибка сохранения сводки для защиты: " + exception.getMessage());
        }
    }

    @FXML
    private void onSaveWordFriendlyReport() {
        if (!ensureBatchResults()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить Word-версию отчёта");
        chooser.setInitialFileName(exportService.buildSuggestedDocxFriendlyFileName("batch_word_friendly"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
        var file = chooser.showSaveDialog(getWindow());
        if (file == null) {
            return;
        }

        try {
            String content = exportService.buildBatchDocxFriendlyText(currentBaseConfig, currentBatchResults);
            exportService.exportDocxFriendlyText(file.toPath(), content);
            batchStatusLabel.setText("Word-версия отчёта сохранена: " + file.getName());
        } catch (Exception exception) {
            batchStatusLabel.setText("Ошибка сохранения Word-версии отчёта: " + exception.getMessage());
        }
    }

    @FXML
    private void onSaveHtmlReport() {
        if (!ensureBatchResults()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить HTML-версию отчёта");
        chooser.setInitialFileName(exportService.buildSuggestedHtmlFileName("batch_report"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML files", "*.html"));
        var file = chooser.showSaveDialog(getWindow());
        if (file == null) {
            return;
        }

        try {
            String content = exportService.buildBatchHtmlText(currentBaseConfig, currentBatchResults);
            exportService.exportHtmlReport(file.toPath(), content);
            batchStatusLabel.setText("HTML-версия отчёта сохранена: " + file.getName());
        } catch (Exception exception) {
            batchStatusLabel.setText("Ошибка сохранения HTML-отчёта: " + exception.getMessage());
        }
    }

    @FXML
    private void onSaveBatchProject() {
        if (!ensureBatchResults()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить проект пакетного анализа");
        chooser.setInitialFileName(batchFileService.buildSuggestedBatchProjectFileName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Properties files", "*.properties"));
        var file = chooser.showSaveDialog(getWindow());
        if (file == null) {
            return;
        }

        try {
            batchFileService.saveBatchExperiment(file.toPath(), currentBaseConfig, currentBatchResults);
            batchStatusLabel.setText("Проект пакетного анализа сохранён: " + file.getName());
        } catch (Exception exception) {
            batchStatusLabel.setText("Ошибка сохранения проекта пакетного анализа: " + exception.getMessage());
        }
    }

    @FXML
    private void onLoadBatchProject() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Открыть проект пакетного анализа");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Properties files", "*.properties"));
        var file = chooser.showOpenDialog(getWindow());
        if (file == null) {
            return;
        }

        try {
            BatchFileService.BatchFileData data = batchFileService.loadBatchExperiment(file.toPath());
            currentBaseConfig = data.baseConfig() == null ? configProfiles.recommendedProfile() : copyConfig(data.baseConfig());
            updateBaseConfigPreview();
            currentBatchResults = new ArrayList<>(data.scenarios());
            BatchSession.save(currentBaseConfig, currentBatchResults);
            updateBatchResults(currentBatchResults);
            batchStatusLabel.setText("Проект анализа загружен: " + file.getName());
        } catch (Exception exception) {
            batchStatusLabel.setText("Ошибка загрузки проекта анализа: " + exception.getMessage());
        }
    }

    private void restoreSuggestedSelection() {
        bpskCheckBox.setSelected(false);
        qpskCheckBox.setSelected(true);
        qam16CheckBox.setSelected(true);
        qam64CheckBox.setSelected(false);
        qam256CheckBox.setSelected(false);

        awgnCheckBox.setSelected(true);
        rayleighCheckBox.setSelected(true);

        educationalProfileCheckBox.setSelected(false);
        qcProfileCheckBox.setSelected(true);
        nrProfileCheckBox.setSelected(true);
        turboProfileCheckBox.setSelected(true);
        polarProfileCheckBox.setSelected(false);

        rateR13CheckBox.setSelected(false);
        rateR12CheckBox.setSelected(true);
        rateR23CheckBox.setSelected(false);
        rateR56CheckBox.setSelected(false);
    }

    private void loadBaseConfigFromSession() {
        SimulationConfig fromSession = ExperimentSession.getLastConfig();
        currentBaseConfig = fromSession == null ? configProfiles.recommendedProfile() : copyConfig(fromSession);
        updateBaseConfigPreview();
    }

    private void updateBaseConfigPreview() {
        if (currentBaseConfig == null) {
            batchBaseConfigArea.setText("Базовая конфигурация ещё не загружена.");
            return;
        }

        batchWaveformChip.setText(SimulationConfigFactory.getWaveformUiName(currentBaseConfig.getWaveform()));
        batchSpatialChip.setText(SimulationConfigFactory.getSpatialModeUiName(currentBaseConfig.getSpatialMode()));

        List<String> modulationUi = selectedModulations().stream()
                .map(SimulationConfigFactory::getModulationUiName)
                .collect(Collectors.toList());

        List<String> channelUi = selectedChannels().stream()
                .map(SimulationConfigFactory::getChannelUiName)
                .collect(Collectors.toList());

        List<String> profileUi = selectedProfiles().stream()
                .map(SimulationConfigFactory::getProfileUiName)
                .collect(Collectors.toList());

        List<String> rateUi = selectedRates().stream()
                .map(r -> "R=" + SimulationConfigFactory.formatRate(r))
                .collect(Collectors.toList());

        int scenarioCount = selectedModulations().size()
                * selectedChannels().size()
                * selectedProfiles().size()
                * Math.max(1, selectedRates().size());

        String text = configFormatter.toSummaryText(currentBaseConfig) + String.format(
                Locale.US,
                "%n%nКомбинации для пакетного анализа:%n" +
                        "• модуляции: %s%n" +
                        "• каналы: %s%n" +
                        "• профили кодирования: %s%n" +
                        "• скорости кода: %s%n" +
                        "• ожидаемое число сценариев: %d",
                join(modulationUi),
                join(channelUi),
                join(profileUi),
                join(rateUi),
                scenarioCount
        );
        batchBaseConfigArea.setText(text);
    }

    private void configureTable() {
        scenarioColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getScenarioLabel()));
        scenarioColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                } else {
                    BatchScenarioResult row = getTableView().getItems().get(getIndex());
                    if (highlightedBestScenario != null && row == highlightedBestScenario) {
                        setText("★ " + item);
                    } else {
                        setText(item);
                    }
                }
            }
        });
        modulationColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(SimulationConfigFactory.getModulationUiName(cell.getValue().getModulation()))
        );
        channelColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(SimulationConfigFactory.getChannelUiName(cell.getValue().getChannel()))
        );
        profileColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(SimulationConfigFactory.getProfileUiName(cell.getValue().getConfig().getLdpcProfile()))
        );
        bestBerGainColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatGain(cell.getValue().getSummary().getBestBerGain())));
        bestBlerGainColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatGain(cell.getValue().getSummary().getBestBlerGain())));
        berEnergyGainColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDb(cell.getValue().getSummary().getBerEnergyGainDb())));
        blerEnergyGainColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDb(cell.getValue().getSummary().getBlerEnergyGainDb())));
        throughputColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatThroughput(cell.getValue().getSummary().getPeakThroughputMbps())));
        spectralEfficiencyColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatSpectralEfficiency(cell.getValue().getSummary().getPeakSpectralEfficiency())));
        requiredSnrBerColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDb(cell.getValue().getSummary().getRequiredSnrBerDb())));
        requiredSnrBlerColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDb(cell.getValue().getSummary().getRequiredSnrBlerDb())));

        batchTable.setRowFactory(table -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(BatchScenarioResult item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("batch-best-row", "batch-normal-row");
                if (empty || item == null) {
                    return;
                }
                if (highlightedBestScenario != null && item == highlightedBestScenario) {
                    getStyleClass().add("batch-best-row");
                } else {
                    getStyleClass().add("batch-normal-row");
                }
            }
        });
    }

    private void updateBatchResults(List<BatchScenarioResult> scenarios) {
        currentBatchResults = scenarios == null ? new ArrayList<>() : new ArrayList<>(scenarios);
        highlightedBestScenario = currentBatchResults.stream()
                .max(Comparator.comparingDouble(s -> scenarioScore(s.getSummary())))
                .orElse(null);
        batchTable.setItems(FXCollections.observableArrayList(currentBatchResults));
        updateCharts(currentBatchResults);
        updateNarrative(currentBatchResults);
        updateDefenseSummary(currentBatchResults);
        batchTable.refresh();
    }

    private void updateCharts(List<BatchScenarioResult> scenarios) {
        batchBerChart.getData().clear();
        batchBlerChart.getData().clear();

        for (BatchScenarioResult scenario : scenarios) {
            XYChart.Series<Number, Number> berSeries = new XYChart.Series<>();
            berSeries.setName(scenario.getScenarioLabel());
            XYChart.Series<Number, Number> blerSeries = new XYChart.Series<>();
            blerSeries.setName(scenario.getScenarioLabel());

            for (ResultPoint point : scenario.getPoints()) {
                berSeries.getData().add(new XYChart.Data<>(point.getSnr(), point.getBerLdpc()));
                blerSeries.getData().add(new XYChart.Data<>(point.getSnr(), point.getBlerLdpc()));
            }

            batchBerChart.getData().add(berSeries);
            batchBlerChart.getData().add(blerSeries);
        }
    }

    private void updateNarrative(List<BatchScenarioResult> scenarios) {
        batchNarrativeArea.setText(batchReportService.buildBatchNarrative(currentBaseConfig, scenarios));
    }

    private boolean ensureBatchResults() {
        if (currentBatchResults == null || currentBatchResults.isEmpty()) {
            batchStatusLabel.setText("Сначала выполните расчёт или загрузите последние результаты пакетного анализа.");
            return false;
        }
        return true;
    }

    private Window getWindow() {
        return batchStatusLabel == null || batchStatusLabel.getScene() == null ? null : batchStatusLabel.getScene().getWindow();
    }

    private List<String> selectedModulations() {
        List<String> values = new ArrayList<>();
        if (bpskCheckBox.isSelected()) values.add(SimulationConfig.MOD_BPSK);
        if (qpskCheckBox.isSelected()) values.add(SimulationConfig.MOD_QPSK);
        if (qam16CheckBox.isSelected()) values.add(SimulationConfig.MOD_16QAM);
        if (qam64CheckBox.isSelected()) values.add(SimulationConfig.MOD_64QAM);
        if (qam256CheckBox.isSelected()) values.add(SimulationConfig.MOD_256QAM);
        return values;
    }

    private List<String> selectedChannels() {
        List<String> values = new ArrayList<>();
        if (awgnCheckBox.isSelected()) values.add(SimulationConfig.CHANNEL_AWGN);
        if (rayleighCheckBox.isSelected()) values.add(SimulationConfig.CHANNEL_RAYLEIGH);
        return values;
    }

    private List<String> selectedProfiles() {
        List<String> values = new ArrayList<>();
        if (educationalProfileCheckBox.isSelected()) values.add(SimulationConfig.PROFILE_EDU);
        if (qcProfileCheckBox.isSelected()) values.add(SimulationConfig.PROFILE_QC);
        if (nrProfileCheckBox.isSelected()) values.add(SimulationConfig.PROFILE_5GNR_BG1);
        if (turboProfileCheckBox.isSelected()) values.add(SimulationConfig.PROFILE_TURBO_LTE);
        if (polarProfileCheckBox.isSelected()) values.add(SimulationConfig.PROFILE_POLAR);
        return values;
    }
    private List<Double> selectedRates() {
        List<Double> values = new ArrayList<>();
        if (rateR13CheckBox.isSelected()) values.add(1.0 / 3.0);
        if (rateR12CheckBox.isSelected()) values.add(1.0 / 2.0);
        if (rateR23CheckBox.isSelected()) values.add(2.0 / 3.0);
        if (rateR56CheckBox.isSelected()) values.add(5.0 / 6.0);
        return values;
    }

    private SimulationConfig copyConfig(SimulationConfig source) {
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

    private String join(List<String> values) {
        return values.isEmpty() ? "не выбрано" : String.join(", ", values);
    }

    private String formatGain(double value) {
        if (Double.isInfinite(value)) {
            return "ошибки не наблюдались";
        }
        if (value >= 100.0) {
            return String.format(Locale.US, "%.0fx", value);
        }
        if (value >= 10.0) {
            return String.format(Locale.US, "%.1fx", value);
        }
        return String.format(Locale.US, "%.2fx", value);
    }

    private String formatDb(Double value) {
        return value == null ? "н/д" : String.format(Locale.US, "%.2f дБ", value);
    }

    private String formatThroughput(double value) {
        return String.format(Locale.US, "%.2f Мбит/с", value);
    }

    private String formatSpectralEfficiency(double value) {
        return String.format(Locale.US, "%.2f бит/с/Гц", value);
    }

    @FXML
    private void onToggleDefenseMode() {
        boolean enabled = defenseModeCheckBox != null && defenseModeCheckBox.isSelected();
        updateDefenseModeState(enabled);
        batchStatusLabel.setText(enabled
                ? "Режим защиты включён: крупные KPI показывают лучший сценарий пакетного анализа."
                : "Выберите набор сценариев и запустите пакетный расчёт для сравнительного анализа.");
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

    private void updateDefenseSummary(List<BatchScenarioResult> scenarios) {
        if (scenarios == null || scenarios.isEmpty()) {
            defenseWinnerLabel.setText("—");
            defenseThroughputLabel.setText("—");
            defenseSpectralLabel.setText("—");
            defenseSnrLabel.setText("—");
            return;
        }

        BatchScenarioResult winner = highlightedBestScenario != null ? highlightedBestScenario : scenarios.get(0);

        double maxThroughput = scenarios.stream()
                .map(BatchScenarioResult::getSummary)
                .mapToDouble(summary -> summary.getPeakThroughputMbps())
                .max()
                .orElse(0.0);
        double maxSpectral = scenarios.stream()
                .map(BatchScenarioResult::getSummary)
                .mapToDouble(summary -> summary.getPeakSpectralEfficiency())
                .max()
                .orElse(0.0);
        Double bestRequiredSnr = scenarios.stream()
                .map(BatchScenarioResult::getSummary)
                .flatMap(summary -> java.util.stream.Stream.of(summary.getRequiredSnrBerDb(), summary.getRequiredSnrBlerDb()))
                .filter(java.util.Objects::nonNull)
                .min(Double::compareTo)
                .orElse(null);

        defenseWinnerLabel.setText(
                SimulationConfigFactory.getModulationUiName(winner.getModulation()) + " · " +
                        SimulationConfigFactory.getChannelUiName(winner.getChannel()) + " · " +
                        SimulationConfigFactory.getWaveformUiName(winner.getConfig().getWaveform())
        );
        defenseThroughputLabel.setText(formatThroughput(maxThroughput));
        defenseSpectralLabel.setText(formatSpectralEfficiency(maxSpectral));
        defenseSnrLabel.setText(formatDb(bestRequiredSnr));
    }

    private double scenarioScore(ru.vkr.ldpcapp.model.ExperimentSummary summary) {
        double berGain = safe(summary.getBestBerGain());
        double blerGain = safe(summary.getBestBlerGain());
        double berEnergy = summary.getBerEnergyGainDb() == null ? 0.0 : summary.getBerEnergyGainDb();
        double blerEnergy = summary.getBlerEnergyGainDb() == null ? 0.0 : summary.getBlerEnergyGainDb();
        double throughput = summary.getPeakThroughputMbps() / 10.0;
        double spectral = summary.getPeakSpectralEfficiency() * 4.0;
        double requiredBerPenalty = summary.getRequiredSnrBerDb() == null ? 0.0 : -summary.getRequiredSnrBerDb();
        double requiredBlerPenalty = summary.getRequiredSnrBlerDb() == null ? 0.0 : -summary.getRequiredSnrBlerDb();
        return berGain + blerGain + 3.0 * berEnergy + 3.0 * blerEnergy + throughput + spectral + requiredBerPenalty + requiredBlerPenalty;
    }

    private double safe(double value) {
        return Double.isInfinite(value) ? 1000.0 : value;
    }
}
