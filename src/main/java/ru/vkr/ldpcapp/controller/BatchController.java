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
import javafx.scene.chart.NumberAxis;
import javafx.util.StringConverter;
import java.util.LinkedHashMap;
import java.util.Map;

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
//    @FXML private CheckBox polarProfileCheckBox;
    @FXML private CheckBox nrBg1CheckBox;
    @FXML private CheckBox nrBg2CheckBox;

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
        currentBaseConfig = configProfiles.recommendedProfile();
        batchBerChart.getStyleClass().add("chart-mpl");
        batchBlerChart.getStyleClass().add("chart-mpl");
        batchProgressBar.setProgress(0.0);
        batchStatusLabel.setText("Выберите набор сценариев и запустите пакетный анализ для прямого сравнения нескольких режимов передачи.");
        batchNarrativeArea.setText("Пакетный анализ позволяет сравнить несколько комбинаций модуляции, канала и LDPC-профиля в рамках одного исследовательского запуска.");
        if (BatchSession.getLastBaseConfig() != null) {
            currentBaseConfig = copyConfig(BatchSession.getLastBaseConfig());
        }
        syncSelectionsWithBaseConfig();
        updateBaseConfigPreview();

    }

    @FXML
    private void onLoadCurrentExperiment() {
        onApplyModelingConfigToBatch();
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

    private double displayBler(ResultPoint p, boolean coded) {
        return coded ? p.getBlerLdpc() : p.getBlerUncoded();
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

    @FXML
    private void onApplyModelingConfigToBatch() {
        SimulationConfig fromSession = ExperimentSession.getLastConfig();
        if (fromSession == null) {
            batchStatusLabel.setText("Нет конфигурации из вкладки «Моделирование». Сначала запустите моделирование хотя бы один раз.");
            return;
        }

        currentBaseConfig = copyConfig(fromSession);
        syncSelectionsWithBaseConfig();
        updateBaseConfigPreview();
        batchStatusLabel.setText("Параметры из «Моделирование» применены к пакетному анализу.");
    }

    private void syncSelectionsWithBaseConfig() {
        if (currentBaseConfig == null) {
            return;
        }

        // 1) Сброс
        bpskCheckBox.setSelected(false);
        qpskCheckBox.setSelected(false);
        qam16CheckBox.setSelected(false);
        qam64CheckBox.setSelected(false);
        qam256CheckBox.setSelected(false);

        awgnCheckBox.setSelected(false);
        rayleighCheckBox.setSelected(false);

        educationalProfileCheckBox.setSelected(false);
        qcProfileCheckBox.setSelected(false);
        nrProfileCheckBox.setSelected(false);
        turboProfileCheckBox.setSelected(false);

        if (nrBg1CheckBox != null) nrBg1CheckBox.setSelected(false);
        if (nrBg2CheckBox != null) nrBg2CheckBox.setSelected(false);

        rateR13CheckBox.setSelected(false);
        rateR12CheckBox.setSelected(false);
        rateR23CheckBox.setSelected(false);
        rateR56CheckBox.setSelected(false);

        // 2) Модуляция
        switch (currentBaseConfig.getModulation()) {
            case SimulationConfig.MOD_BPSK -> bpskCheckBox.setSelected(true);
            case SimulationConfig.MOD_QPSK -> qpskCheckBox.setSelected(true);
            case SimulationConfig.MOD_16QAM -> qam16CheckBox.setSelected(true);
            case SimulationConfig.MOD_64QAM -> qam64CheckBox.setSelected(true);
            case SimulationConfig.MOD_256QAM -> qam256CheckBox.setSelected(true);
            default -> qpskCheckBox.setSelected(true);
        }

        // 3) Канал
        if (SimulationConfig.CHANNEL_AWGN.equals(currentBaseConfig.getChannelModel())) {
            awgnCheckBox.setSelected(true);
        } else if (SimulationConfig.CHANNEL_RAYLEIGH.equals(currentBaseConfig.getChannelModel())) {
            rayleighCheckBox.setSelected(true);
        } else {
            awgnCheckBox.setSelected(true);
        }

        // 4) Профиль
        String profile = currentBaseConfig.getLdpcProfile();
        if (SimulationConfig.PROFILE_EDU.equals(profile)) {
            educationalProfileCheckBox.setSelected(true);
        } else if (SimulationConfig.PROFILE_QC.equals(profile)) {
            qcProfileCheckBox.setSelected(true);
        } else if (SimulationConfig.PROFILE_TURBO_LTE.equals(profile)) {
            turboProfileCheckBox.setSelected(true);
        } else if (SimulationConfig.PROFILE_5GNR_BG1.equals(profile) || SimulationConfig.PROFILE_5GNR_BG2.equals(profile)) {
            nrProfileCheckBox.setSelected(true);
            if (nrBg1CheckBox != null && SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) {
                nrBg1CheckBox.setSelected(true);
            }
            if (nrBg2CheckBox != null && SimulationConfig.PROFILE_5GNR_BG2.equals(profile)) {
                nrBg2CheckBox.setSelected(true);
            }
        } else {
            nrProfileCheckBox.setSelected(true);
            if (nrBg1CheckBox != null) nrBg1CheckBox.setSelected(true);
        }

        updateNrBgVisibility();

        // 5) Скорость кода: ближайший preset к effective rate
        double rEff = SimulationConfigFactory.getEffectiveCodeRate(currentBaseConfig);
        double d13 = Math.abs(rEff - 1.0 / 3.0);
        double d12 = Math.abs(rEff - 1.0 / 2.0);
        double d23 = Math.abs(rEff - 2.0 / 3.0);
        double d56 = Math.abs(rEff - 5.0 / 6.0);

        double min = Math.min(Math.min(d13, d12), Math.min(d23, d56));
        if (min == d13) rateR13CheckBox.setSelected(true);
        else if (min == d12) rateR12CheckBox.setSelected(true);
        else if (min == d23) rateR23CheckBox.setSelected(true);
        else rateR56CheckBox.setSelected(true);
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
//        polarProfileCheckBox.setSelected(false);

        rateR13CheckBox.setSelected(false);
        rateR12CheckBox.setSelected(true);
        rateR23CheckBox.setSelected(false);
        rateR56CheckBox.setSelected(false);

        if (nrBg1CheckBox != null) nrBg1CheckBox.setSelected(true);
        if (nrBg2CheckBox != null) nrBg2CheckBox.setSelected(true);
        updateNrBgVisibility();
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

        String text = configFormatter.toBaseParamsText(currentBaseConfig) + String.format(
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

        if (scenarios == null || scenarios.isEmpty()) {
            return;
        }

        Map<String, XYChart.Series<Number, Number>> uncodedBerByChannel = new LinkedHashMap<>();
        Map<String, XYChart.Series<Number, Number>> uncodedBlerByChannel = new LinkedHashMap<>();

        double minSnr = Double.POSITIVE_INFINITY;
        double maxSnr = Double.NEGATIVE_INFINITY;

        // 1) Отдельные uncoded-кривые по (модуляция + канал)
        for (BatchScenarioResult scenario : scenarios) {
            String uncodedKey = SimulationConfigFactory.getModulationUiName(scenario.getModulation())
                    + " · "
                    + SimulationConfigFactory.getChannelUiName(scenario.getChannel())
                    + " · без кодирования";

            XYChart.Series<Number, Number> uncodedBerSeries = uncodedBerByChannel.computeIfAbsent(
                    uncodedKey,
                    key -> {
                        XYChart.Series<Number, Number> s = new XYChart.Series<>();
                        s.setName(key + " (BER)");
                        return s;
                    }
            );

            XYChart.Series<Number, Number> uncodedBlerSeries = uncodedBlerByChannel.computeIfAbsent(
                    uncodedKey,
                    key -> {
                        XYChart.Series<Number, Number> s = new XYChart.Series<>();
                        s.setName(key + " (BLER)");
                        return s;
                    }
            );

            for (ResultPoint point : scenario.getPoints()) {
                minSnr = Math.min(minSnr, point.getSnr());
                maxSnr = Math.max(maxSnr, point.getSnr());

                double berUncodedPlot = upperBoundIfZero(point.getBerUncoded(), point.getTotalBits());
                double blerUncodedPlot = upperBoundIfZero(displayBler(point, false), point.getTotalBlocks());

                uncodedBerSeries.getData().add(new XYChart.Data<>(point.getSnr(), toLog10Safe(berUncodedPlot)));
                uncodedBlerSeries.getData().add(new XYChart.Data<>(point.getSnr(), toLog10Safe(blerUncodedPlot)));
            }
        }

        // 2) Добавляем uncoded сначала
        for (XYChart.Series<Number, Number> s : uncodedBerByChannel.values()) {
            batchBerChart.getData().add(s);
            styleUncodedSeries(s);
        }
        for (XYChart.Series<Number, Number> s : uncodedBlerByChannel.values()) {
            batchBlerChart.getData().add(s);
            styleUncodedSeries(s);
        }

        // 3) Coded-кривые
        for (BatchScenarioResult scenario : scenarios) {
            XYChart.Series<Number, Number> berSeries = new XYChart.Series<>();
            berSeries.setName(scenario.getScenarioLabel());

            XYChart.Series<Number, Number> blerSeries = new XYChart.Series<>();
            blerSeries.setName(scenario.getScenarioLabel());

            for (ResultPoint point : scenario.getPoints()) {
                double berCodedPlot = upperBoundIfZero(point.getBerLdpc(), point.getTotalBits());
                double blerCodedPlot = upperBoundIfZero(displayBler(point, true), point.getTotalBlocks());

                berSeries.getData().add(new XYChart.Data<>(point.getSnr(), toLog10Safe(berCodedPlot)));
                blerSeries.getData().add(new XYChart.Data<>(point.getSnr(), toLog10Safe(blerCodedPlot)));
            }

            batchBerChart.getData().add(berSeries);
            batchBlerChart.getData().add(blerSeries);
        }

        configureBatchBerLogAxis(minSnr, maxSnr);
        configureBatchBlerLogAxis(minSnr, maxSnr);
    }

    private void updateNarrative(List<BatchScenarioResult> scenarios) {
        batchNarrativeArea.setText(batchReportService.buildBatchNarrative(currentBaseConfig, scenarios));
    }

    private List<XYChart.Data<Number, Number>> monotonicNonIncreasingLogSeries(List<XYChart.Data<Number, Number>> raw) {
        List<XYChart.Data<Number, Number>> out = new ArrayList<>(raw.size());
        double best = Double.POSITIVE_INFINITY; // в лог-шкале меньше = лучше (ниже)
        for (XYChart.Data<Number, Number> p : raw) {
            double y = p.getYValue().doubleValue();
            if (y < best) {
                best = y;
            }
            out.add(new XYChart.Data<>(p.getXValue(), best));
        }
        return out;
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

    private void configureLogAxes(LineChart<Number, Number> chart) {
        NumberAxis x = (NumberAxis) chart.getXAxis();
        NumberAxis y = (NumberAxis) chart.getYAxis();

        x.setLabel("Eb/N0 (dB)");
        y.setLabel("FER");

        y.setAutoRanging(false);
        y.setLowerBound(-6.0);
        y.setUpperBound(0.0);
        y.setTickUnit(1.0);

        y.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return "10^" + (int) Math.round(value.doubleValue());
            }
            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private void configureBatchBerLogAxis(double minSnr, double maxSnr) {
        NumberAxis x = (NumberAxis) batchBerChart.getXAxis();
        NumberAxis y = (NumberAxis) batchBerChart.getYAxis();

        x.setAutoRanging(false);
        x.setLowerBound(Math.floor(minSnr) - 0.5);
        x.setUpperBound(Math.ceil(maxSnr) + 0.5);
        x.setTickUnit(1.0);
        x.setLabel("SNR, дБ");

        y.setAutoRanging(false);
        y.setLowerBound(-6.0);
        y.setUpperBound(0.0);
        y.setTickUnit(1.0);
        y.setLabel("BER");

        y.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return "10^" + (int) Math.round(value.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private void configureBatchBlerLogAxis(double minSnr, double maxSnr) {
        NumberAxis x = (NumberAxis) batchBlerChart.getXAxis();
        NumberAxis y = (NumberAxis) batchBlerChart.getYAxis();

        x.setAutoRanging(false);
        x.setLowerBound(Math.floor(minSnr) - 0.5);
        x.setUpperBound(Math.ceil(maxSnr) + 0.5);
        x.setTickUnit(1.0);
        x.setLabel("SNR, дБ");

        y.setAutoRanging(false);
        y.setLowerBound(-6.0);
        y.setUpperBound(0.0);
        y.setTickUnit(1.0);
        y.setLabel("BLER");

        y.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return "10^" + (int) Math.round(value.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private double toLog10(double value) {
        return Math.log10(Math.max(1e-12, value));
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

    private static final double ALPHA_95 = 0.05;

    private double upperBoundIfZero(double p, int nTrials) {
        if (p > 0.0) return p;
        if (nTrials <= 0) return Double.NaN;
        return -Math.log(ALPHA_95) / nTrials; // ~2.996 / N
    }

    private double toLog10OrNaN(double p) {
        if (p <= 0.0 || !Double.isFinite(p)) {
            return Double.NaN; // разрыв линии
        }
        return Math.log10(Math.max(1e-12, p));
    }

    private double toLog10Safe(double p) {
        return Math.log10(Math.max(1e-12, p));
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
        if (qcProfileCheckBox.isSelected())          values.add(SimulationConfig.PROFILE_QC);

        if (nrProfileCheckBox.isSelected()) {
            boolean bg1 = nrBg1CheckBox == null || nrBg1CheckBox.isSelected();
            boolean bg2 = nrBg2CheckBox != null && nrBg2CheckBox.isSelected();
            if (bg1) values.add(SimulationConfig.PROFILE_5GNR_BG1);
            if (bg2) values.add(SimulationConfig.PROFILE_5GNR_BG2);
            // Если ни один не выбран — добавляем BG1 по умолчанию
            if (!bg1 && !bg2) values.add(SimulationConfig.PROFILE_5GNR_BG1);
        }

        if (turboProfileCheckBox.isSelected()) values.add(SimulationConfig.PROFILE_TURBO_LTE);
//        if (polarProfileCheckBox.isSelected())  values.add(SimulationConfig.PROFILE_POLAR);
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

    private void styleUncodedSeries(XYChart.Series<Number, Number> series) {
        if (series.getNode() != null) {
            series.getNode().getStyleClass().add("uncoded-series");
        }
        series.getData().forEach(d -> {
            if (d.getNode() != null) {
                d.getNode().setVisible(false); // убираем маркеры точек
            }
        });
    }

    private void updateNrBgVisibility() {
        boolean nrSelected = nrProfileCheckBox != null && nrProfileCheckBox.isSelected();
        if (nrBg1CheckBox != null) {
            nrBg1CheckBox.setVisible(nrSelected);
            nrBg1CheckBox.setManaged(nrSelected);
        }
        if (nrBg2CheckBox != null) {
            nrBg2CheckBox.setVisible(nrSelected);
            nrBg2CheckBox.setManaged(nrSelected);
        }
    }

    @FXML
    private void onNrProfileChanged() {
        updateNrBgVisibility();
        onSelectionChanged();
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
                source.getEqualizerMode()
        );

        // ВАЖНО: чтобы batch не откатывался к дефолтам
        copy.setAdaptiveStopEnabled(source.isAdaptiveStopEnabled());
        copy.setMinErrorEventsPerSnr(source.getMinErrorEventsPerSnr());
        copy.setMaxBlocksPerSnr(source.getMaxBlocksPerSnr());
        copy.setConfidenceLevel(source.getConfidenceLevel());

        // Доп. параметры цепочки NR
        copy.setSnrDomain(source.getSnrDomain());
        copy.setDecoderType(source.getDecoderType());
        copy.setNrBaseGraph(source.getNrBaseGraph());
        copy.setLiftingSize(source.getLiftingSize());
        copy.setCrcEnabled(source.isCrcEnabled());
        copy.setCrcBits(source.getCrcBits());
        copy.setSegmentationEnabled(source.isSegmentationEnabled());
        copy.setRateMatchingEnabled(source.isRateMatchingEnabled());
        copy.setTargetCodewordLength(source.getTargetCodewordLength());
        copy.setBlerCriterion(source.getBlerCriterion());
        copy.setHarqEnabled(source.isHarqEnabled());
        copy.setHarqMaxRetx(source.getHarqMaxRetx());

        return copy;
    }

    private String join(List<String> values) {
        return values.isEmpty() ? "не выбрано" : String.join(", ", values);
    }

    private String formatGain(double ratio) {
        if (!Double.isFinite(ratio) || ratio <= 0.0) {
            return "н/д";
        }
        double db = 10.0 * Math.log10(ratio);
        return String.format(Locale.US, "%+.2f дБ", db);
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
