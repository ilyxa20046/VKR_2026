package ru.vkr.ldpcapp.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ru.vkr.ldpcapp.model.ExperimentSummary;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.CompareSession;
import ru.vkr.ldpcapp.service.ExportService;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.util.Objects.nonNull;

public class CompareController {

    private final ExportService exportService = new ExportService();

    @FXML
    private Label compareStatusLabel;

    @FXML
    private Label leftScenarioLabel;

    @FXML
    private Label rightScenarioLabel;

    @FXML
    private Label leftWaveformChip;

    @FXML
    private Label leftSpatialChip;

    @FXML
    private Label rightWaveformChip;

    @FXML
    private Label rightSpatialChip;

    @FXML
    private TextArea leftSummaryLabel;

    @FXML
    private TextArea rightSummaryLabel;

    @FXML
    private Label winnerLabel;

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
    private LineChart<Number, Number> compareBerChart;

    @FXML
    private LineChart<Number, Number> compareBlerChart;

    @FXML
    private TableView<CompareRow> compareTable;

    @FXML
    private TableColumn<CompareRow, Double> snrColumn;

    @FXML
    private TableColumn<CompareRow, Double> leftBerColumn;

    @FXML
    private TableColumn<CompareRow, Double> rightBerColumn;

    @FXML
    private TableColumn<CompareRow, Double> leftBlerColumn;

    @FXML
    private TableColumn<CompareRow, Double> rightBlerColumn;

    @FXML
    private TableColumn<CompareRow, Double> leftThroughputColumn;

    @FXML
    private TableColumn<CompareRow, Double> rightThroughputColumn;

    @FXML
    private TableColumn<CompareRow, Double> leftSpectralColumn;

    @FXML
    private TableColumn<CompareRow, Double> rightSpectralColumn;

    @FXML
    private TextArea compareNarrativeArea;

    @FXML
    public void initialize() {
        configureTable();
        updateDefenseModeState(false);
        if (!CompareSession.hasComparison()) {
            compareStatusLabel.setText("Сравнение пока не подготовлено. На вкладке «Результаты» сохраните один запуск как сценарий A, затем выполните второй запуск и нажмите «Сравнить с текущим запуском».\n");
            compareNarrativeArea.setText("Здесь появится A/B-анализ двух сценариев после того, как вы подготовите пару запусков на вкладке «Результаты».\n\nРекомендуемый порядок работы:\n1. Выполните первый эксперимент.\n2. Нажмите «Подготовить как сценарий A».\n3. Выполните второй эксперимент.\n4. Нажмите «Сравнить с текущим запуском».\n5. Вернитесь на вкладку «Сравнение» для анализа BER/BLER, throughput, spectral efficiency и required SNR.");
            winnerLabel.setText("Пара сценариев ещё не подготовлена.");
            return;
        }

        SimulationConfig leftConfig = CompareSession.getLeftConfig();
        SimulationConfig rightConfig = CompareSession.getRightConfig();
        List<ResultPoint> leftResults = CompareSession.getLeftResults();
        List<ResultPoint> rightResults = CompareSession.getRightResults();

        leftScenarioLabel.setText(CompareSession.getLeftTitle());
        rightScenarioLabel.setText(CompareSession.getRightTitle());
        leftWaveformChip.setText(leftConfig.getWaveform());
        leftSpatialChip.setText(leftConfig.getSpatialMode());
        rightWaveformChip.setText(rightConfig.getWaveform());
        rightSpatialChip.setText(rightConfig.getSpatialMode());
        leftSummaryLabel.setText(leftConfig.toSummaryText());
        rightSummaryLabel.setText(rightConfig.toSummaryText());

        updateCharts(leftResults, rightResults);
        updateTable(leftResults, rightResults);
        updateNarrative(leftConfig, rightConfig, leftResults, rightResults);
        compareStatusLabel.setText("Сравнение сформировано. Ниже доступны итоговый вывод, графики и детальные секции анализа.");
    }

    private void configureTable() {
        snrColumn.setCellValueFactory(new PropertyValueFactory<>("snr"));
        leftBerColumn.setCellValueFactory(new PropertyValueFactory<>("leftBer"));
        rightBerColumn.setCellValueFactory(new PropertyValueFactory<>("rightBer"));
        leftBlerColumn.setCellValueFactory(new PropertyValueFactory<>("leftBler"));
        rightBlerColumn.setCellValueFactory(new PropertyValueFactory<>("rightBler"));
        leftThroughputColumn.setCellValueFactory(new PropertyValueFactory<>("leftThroughput"));
        rightThroughputColumn.setCellValueFactory(new PropertyValueFactory<>("rightThroughput"));
        leftSpectralColumn.setCellValueFactory(new PropertyValueFactory<>("leftSpectralEfficiency"));
        rightSpectralColumn.setCellValueFactory(new PropertyValueFactory<>("rightSpectralEfficiency"));

        leftThroughputColumn.setCellFactory(column -> new TextFieldTableCell<>(new javafx.util.converter.DoubleStringConverter()) {
            @Override
            public void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatThroughput(item));
            }
        });
        rightThroughputColumn.setCellFactory(column -> new TextFieldTableCell<>(new javafx.util.converter.DoubleStringConverter()) {
            @Override
            public void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatThroughput(item));
            }
        });
        leftSpectralColumn.setCellFactory(column -> new TextFieldTableCell<>(new javafx.util.converter.DoubleStringConverter()) {
            @Override
            public void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatSpectralEfficiency(item));
            }
        });
        rightSpectralColumn.setCellFactory(column -> new TextFieldTableCell<>(new javafx.util.converter.DoubleStringConverter()) {
            @Override
            public void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatSpectralEfficiency(item));
            }
        });
    }

    private void updateCharts(List<ResultPoint> leftResults, List<ResultPoint> rightResults) {
        compareBerChart.getData().clear();
        compareBlerChart.getData().clear();

        XYChart.Series<Number, Number> leftBerSeries = new XYChart.Series<>();
        leftBerSeries.setName(CompareSession.getLeftTitle() + " — BER LDPC");
        XYChart.Series<Number, Number> rightBerSeries = new XYChart.Series<>();
        rightBerSeries.setName(CompareSession.getRightTitle() + " — BER LDPC");

        XYChart.Series<Number, Number> leftBlerSeries = new XYChart.Series<>();
        leftBlerSeries.setName(CompareSession.getLeftTitle() + " — BLER LDPC");
        XYChart.Series<Number, Number> rightBlerSeries = new XYChart.Series<>();
        rightBlerSeries.setName(CompareSession.getRightTitle() + " — BLER LDPC");

        for (ResultPoint point : leftResults) {
            leftBerSeries.getData().add(new XYChart.Data<>(point.getSnr(), point.getBerLdpc()));
            leftBlerSeries.getData().add(new XYChart.Data<>(point.getSnr(), point.getBlerLdpc()));
        }
        for (ResultPoint point : rightResults) {
            rightBerSeries.getData().add(new XYChart.Data<>(point.getSnr(), point.getBerLdpc()));
            rightBlerSeries.getData().add(new XYChart.Data<>(point.getSnr(), point.getBlerLdpc()));
        }

        compareBerChart.getData().addAll(leftBerSeries, rightBerSeries);
        compareBlerChart.getData().addAll(leftBlerSeries, rightBlerSeries);
    }

    private void updateTable(List<ResultPoint> leftResults, List<ResultPoint> rightResults) {
        int rows = Math.min(leftResults.size(), rightResults.size());
        List<CompareRow> items = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            ResultPoint left = leftResults.get(i);
            ResultPoint right = rightResults.get(i);
            items.add(new CompareRow(
                    left.getSnr(),
                    left.getBerLdpc(),
                    right.getBerLdpc(),
                    left.getBlerLdpc(),
                    right.getBlerLdpc(),
                    left.getEffectiveThroughputMbps(),
                    right.getEffectiveThroughputMbps(),
                    left.getSpectralEfficiency(),
                    right.getSpectralEfficiency()
            ));
        }
        compareTable.setItems(FXCollections.observableArrayList(items));
    }

    private void updateNarrative(
            SimulationConfig leftConfig,
            SimulationConfig rightConfig,
            List<ResultPoint> leftResults,
            List<ResultPoint> rightResults
    ) {
        ExperimentSummary leftSummary = ExperimentSummary.from(leftResults);
        ExperimentSummary rightSummary = ExperimentSummary.from(rightResults);

        double leftScore = scenarioScore(leftSummary);
        double rightScore = scenarioScore(rightSummary);
        boolean comparable = Math.abs(leftScore - rightScore) >= 1e-9;
        boolean leftWinner = comparable && leftScore > rightScore;
        boolean rightWinner = comparable && rightScore > leftScore;

        String winner;
        if (!comparable) {
            winner = "Сценарии показывают сопоставимую эффективность в выбранной постановке.";
        } else if (leftWinner) {
            winner = "Более предпочтительным выглядит текущий эксперимент: он показывает лучшую суммарную эффективность по BER/BLER и энергетическому выигрышу.";
        } else {
            winner = "Более предпочтительным выглядит сценарий из истории: он показывает лучшую суммарную эффективность по BER/BLER и энергетическому выигрышу.";
        }
        winnerLabel.setText(winner);
        highlightWinner(leftWinner, rightWinner);
        defenseWinnerLabel.setText(!comparable ? "Сценарии сопоставимы" : leftWinner ? CompareSession.getLeftTitle() : CompareSession.getRightTitle());
        defenseThroughputLabel.setText(formatThroughput(Math.max(leftSummary.getPeakThroughputMbps(), rightSummary.getPeakThroughputMbps())));
        defenseSpectralLabel.setText(formatSpectralEfficiency(Math.max(leftSummary.getPeakSpectralEfficiency(), rightSummary.getPeakSpectralEfficiency())));
        Double bestRequiredSnr = minNullable(bestNullable(leftSummary.getRequiredSnrBerDb(), leftSummary.getRequiredSnrBlerDb()), bestNullable(rightSummary.getRequiredSnrBerDb(), rightSummary.getRequiredSnrBlerDb()));
        defenseSnrLabel.setText(formatRequiredSnr(bestRequiredSnr));

        String text = String.format(
                Locale.US,
                "Сравнение сценариев:%n%n" +
                        "1) Текущий эксперимент:%n" +
                        "• модуляция: %s%n" +
                        "• канал: %s%n" +
                        "• LDPC: %s%n" +
                        "• лучший BER gain: %s%n" +
                        "• лучший BLER gain: %s%n" +
                        "• BER gain @ 1e-3: %s%n" +
                        "• BLER gain @ 1e-1: %s%n" +
                        "• peak throughput: %s%n" +
                        "• peak spectral efficiency: %s%n" +
                        "• required SNR @ BER=1e-3: %s%n" +
                        "• required SNR @ BLER=1e-1: %s%n%n" +
                        "2) Сценарий из истории:%n" +
                        "• модуляция: %s%n" +
                        "• канал: %s%n" +
                        "• LDPC: %s%n" +
                        "• лучший BER gain: %s%n" +
                        "• лучший BLER gain: %s%n" +
                        "• BER gain @ 1e-3: %s%n" +
                        "• BLER gain @ 1e-1: %s%n" +
                        "• peak throughput: %s%n" +
                        "• peak spectral efficiency: %s%n" +
                        "• required SNR @ BER=1e-3: %s%n" +
                        "• required SNR @ BLER=1e-1: %s%n%n" +
                        "Итог:%n%s",
                leftConfig.getModulation(),
                leftConfig.getChannelModel(),
                SimulationConfig.getProfileName(leftConfig.getLdpcProfile()),
                formatGain(leftSummary.getBestBerGain()),
                formatGain(leftSummary.getBestBlerGain()),
                formatEnergy(leftSummary.getBerEnergyGainDb()),
                formatEnergy(leftSummary.getBlerEnergyGainDb()),
                formatThroughput(leftSummary.getPeakThroughputMbps()),
                formatSpectralEfficiency(leftSummary.getPeakSpectralEfficiency()),
                formatRequiredSnr(leftSummary.getRequiredSnrBerDb()),
                formatRequiredSnr(leftSummary.getRequiredSnrBlerDb()),
                rightConfig.getModulation(),
                rightConfig.getChannelModel(),
                SimulationConfig.getProfileName(rightConfig.getLdpcProfile()),
                formatGain(rightSummary.getBestBerGain()),
                formatGain(rightSummary.getBestBlerGain()),
                formatEnergy(rightSummary.getBerEnergyGainDb()),
                formatEnergy(rightSummary.getBlerEnergyGainDb()),
                formatThroughput(rightSummary.getPeakThroughputMbps()),
                formatSpectralEfficiency(rightSummary.getPeakSpectralEfficiency()),
                formatRequiredSnr(rightSummary.getRequiredSnrBerDb()),
                formatRequiredSnr(rightSummary.getRequiredSnrBlerDb()),
                winner
        );
        compareNarrativeArea.setText(text);
    }

    private double scenarioScore(ExperimentSummary summary) {
        double berGain = bounded(summary.getBestBerGain());
        double blerGain = bounded(summary.getBestBlerGain());
        double berEnergy = summary.getBerEnergyGainDb() == null ? 0.0 : summary.getBerEnergyGainDb();
        double blerEnergy = summary.getBlerEnergyGainDb() == null ? 0.0 : summary.getBlerEnergyGainDb();
        double throughput = summary.getPeakThroughputMbps() / 10.0;
        double spectral = summary.getPeakSpectralEfficiency() * 4.0;
        double requiredBerPenalty = summary.getRequiredSnrBerDb() == null ? 0.0 : -summary.getRequiredSnrBerDb();
        double requiredBlerPenalty = summary.getRequiredSnrBlerDb() == null ? 0.0 : -summary.getRequiredSnrBlerDb();
        return berGain + blerGain + 3.0 * berEnergy + 3.0 * blerEnergy + throughput + spectral + requiredBerPenalty + requiredBlerPenalty;
    }

    private double bounded(double value) {
        if (Double.isInfinite(value)) {
            return 1000.0;
        }
        return value;
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

    private String formatEnergy(Double value) {
        return value == null ? "н/д" : String.format(Locale.US, "%.2f дБ", value);
    }

    private String formatRequiredSnr(Double value) {
        return value == null ? "н/д" : String.format(Locale.US, "%.2f дБ", value);
    }

    private String formatThroughput(double value) {
        return String.format(Locale.US, "%.2f Mbps", value);
    }

    @FXML
    private void onToggleDefenseMode() {
        boolean enabled = defenseModeCheckBox != null && defenseModeCheckBox.isSelected();
        updateDefenseModeState(enabled);
        compareStatusLabel.setText(enabled
                ? "Режим защиты включён: акцент на победителе, throughput, spectral efficiency и required SNR."
                : "Сравнение двух сценариев сформировано. Ниже показаны BER/BLER-кривые и сравнительный вывод.");
    }

    @FXML
    private void onCopyComparisonNarrative() {
        ClipboardContent content = new ClipboardContent();
        content.putString(compareNarrativeArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
        compareStatusLabel.setText("Текст сравнения скопирован. Его можно использовать в отчёте или защите.");
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

    private Double bestNullable(Double a, Double b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }

    private Double minNullable(Double a, Double b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }

    private String formatSpectralEfficiency(double value) {
        return String.format(Locale.US, "%.3f bit/s/Hz", value);
    }

    private void highlightWinner(boolean leftWinner, boolean rightWinner) {
        toggleWinnerClass(leftScenarioLabel, leftWinner);
        toggleWinnerClass(leftWaveformChip, leftWinner);
        toggleWinnerClass(leftSpatialChip, leftWinner);
        toggleWinnerClass(rightScenarioLabel, rightWinner);
        toggleWinnerClass(rightWaveformChip, rightWinner);
        toggleWinnerClass(rightSpatialChip, rightWinner);
        toggleWinnerClass(winnerLabel, leftWinner || rightWinner);
    }

    private void toggleWinnerClass(Label label, boolean enabled) {
        if (label == null) {
            return;
        }
        if (enabled) {
            if (!label.getStyleClass().contains("scenario-winner-cell")) {
                label.getStyleClass().add("scenario-winner-cell");
            }
        } else {
            label.getStyleClass().remove("scenario-winner-cell");
        }
    }

    @FXML
    private void onSavePresentationSummary() {
        if (!CompareSession.hasComparison()) {
            compareStatusLabel.setText("Нет сравнения для сохранения. Сначала подготовьте два сценария.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранение краткой сводки Compare для защиты");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));
        fileChooser.setInitialFileName(exportService.buildSuggestedPresentationSummaryFileName("compare_defense_summary"));

        Window window = compareTable != null && compareTable.getScene() != null ? compareTable.getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            compareStatusLabel.setText("Сохранение презентационной Compare-сводки отменено.");
            return;
        }

        try {
            exportService.exportComparePresentationSummary(
                    file.toPath(),
                    CompareSession.getLeftConfig(),
                    CompareSession.getRightConfig(),
                    CompareSession.getLeftResults(),
                    CompareSession.getRightResults(),
                    CompareSession.getLeftTitle(),
                    CompareSession.getRightTitle()
            );
            compareStatusLabel.setText("Краткая Compare-сводка для защиты сохранена: " + file.getAbsolutePath());
        } catch (IOException exception) {
            compareStatusLabel.setText("Ошибка сохранения Compare-сводки: " + exception.getMessage());
        }
    }

    @FXML
    private void onSaveWordFriendlyReport() {
        if (!CompareSession.hasComparison()) {
            compareStatusLabel.setText("Нет сравнения для Word-friendly экспорта. Сначала подготовьте два сценария.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранение Word-friendly Compare-отчёта");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));
        fileChooser.setInitialFileName(exportService.buildSuggestedDocxFriendlyFileName("compare_word_friendly"));

        Window window = compareTable != null && compareTable.getScene() != null ? compareTable.getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            compareStatusLabel.setText("Сохранение Word-friendly Compare-отчёта отменено.");
            return;
        }

        try {
            String content = exportService.buildCompareDocxFriendlyText(
                    CompareSession.getLeftConfig(),
                    CompareSession.getRightConfig(),
                    CompareSession.getLeftResults(),
                    CompareSession.getRightResults(),
                    CompareSession.getLeftTitle(),
                    CompareSession.getRightTitle()
            );
            exportService.exportDocxFriendlyText(file.toPath(), content);
            compareStatusLabel.setText("Word-friendly Compare-отчёт сохранён: " + file.getAbsolutePath());
        } catch (IOException exception) {
            compareStatusLabel.setText("Ошибка сохранения Word-friendly Compare-отчёта: " + exception.getMessage());
        }
    }

    @FXML
    private void onSaveHtmlReport() {
        if (!CompareSession.hasComparison()) {
            compareStatusLabel.setText("Нет сравнения для HTML-экспорта. Сначала подготовьте два сценария.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранение HTML Compare-отчёта");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML files (*.html)", "*.html"));
        fileChooser.setInitialFileName(exportService.buildSuggestedHtmlFileName("compare_report"));

        Window window = compareTable != null && compareTable.getScene() != null ? compareTable.getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            compareStatusLabel.setText("Сохранение HTML Compare-отчёта отменено.");
            return;
        }

        try {
            String content = exportService.buildCompareHtmlText(
                    CompareSession.getLeftConfig(),
                    CompareSession.getRightConfig(),
                    CompareSession.getLeftResults(),
                    CompareSession.getRightResults(),
                    CompareSession.getLeftTitle(),
                    CompareSession.getRightTitle()
            );
            exportService.exportHtmlReport(file.toPath(), content);
            compareStatusLabel.setText("HTML Compare-отчёт сохранён: " + file.getAbsolutePath());
        } catch (IOException exception) {
            compareStatusLabel.setText("Ошибка сохранения HTML Compare-отчёта: " + exception.getMessage());
        }
    }

    public static class CompareRow {
        private final double snr;
        private final double leftBer;
        private final double rightBer;
        private final double leftBler;
        private final double rightBler;
        private final double leftThroughput;
        private final double rightThroughput;
        private final double leftSpectralEfficiency;
        private final double rightSpectralEfficiency;

        public CompareRow(
                double snr,
                double leftBer,
                double rightBer,
                double leftBler,
                double rightBler,
                double leftThroughput,
                double rightThroughput,
                double leftSpectralEfficiency,
                double rightSpectralEfficiency
        ) {
            this.snr = snr;
            this.leftBer = leftBer;
            this.rightBer = rightBer;
            this.leftBler = leftBler;
            this.rightBler = rightBler;
            this.leftThroughput = leftThroughput;
            this.rightThroughput = rightThroughput;
            this.leftSpectralEfficiency = leftSpectralEfficiency;
            this.rightSpectralEfficiency = rightSpectralEfficiency;
        }

        public double getSnr() {
            return snr;
        }

        public double getLeftBer() {
            return leftBer;
        }

        public double getRightBer() {
            return rightBer;
        }

        public double getLeftBler() {
            return leftBler;
        }

        public double getRightBler() {
            return rightBler;
        }

        public double getLeftThroughput() {
            return leftThroughput;
        }

        public double getRightThroughput() {
            return rightThroughput;
        }

        public double getLeftSpectralEfficiency() {
            return leftSpectralEfficiency;
        }

        public double getRightSpectralEfficiency() {
            return rightSpectralEfficiency;
        }
    }
}
