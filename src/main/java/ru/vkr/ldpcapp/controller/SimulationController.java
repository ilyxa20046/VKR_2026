package ru.vkr.ldpcapp.controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.ConfigFileService;
import ru.vkr.ldpcapp.service.ExperimentSession;
import ru.vkr.ldpcapp.service.ParameterHelpService;
import ru.vkr.ldpcapp.service.SimulationService;
import ru.vkr.ldpcapp.service.config.SimulationConfigFactory;
import ru.vkr.ldpcapp.service.config.SimulationConfigFormatter;
import ru.vkr.ldpcapp.service.config.SimulationConfigValidator;
import ru.vkr.ldpcapp.service.config.SimulationConfigProfiles;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javafx.scene.control.ListCell;
import javafx.util.StringConverter;

public class SimulationController {

    private final SimulationService simulationService = new SimulationService();
    private final ParameterHelpService parameterHelpService = new ParameterHelpService();
    private final ConfigFileService configFileService = new ConfigFileService();
    private final SimulationConfigValidator configValidator = new SimulationConfigValidator();
    private final SimulationConfigFormatter configFormatter = new SimulationConfigFormatter();
    private final SimulationConfigProfiles configProfiles = new SimulationConfigProfiles();

    @FXML
    private CheckBox adaptiveStopCheckBox;
    @FXML
    private ComboBox<String> snrDomainComboBox;
    @FXML private CheckBox harqEnabledCheckBox;
    @FXML private Spinner<Integer> harqMaxRetxSpinner;
    @FXML
    private VBox advancedNrChainBox;
    @FXML
    private ComboBox<String> nrBaseGraphComboBox;
    @FXML
    private ComboBox<Integer> liftingSizeComboBox;
    @FXML
    private CheckBox crcEnabledCheckBox;
    @FXML
    private ComboBox<Integer> crcBitsComboBox;
    @FXML
    private CheckBox segmentationEnabledCheckBox;
    @FXML
    private CheckBox rateMatchingEnabledCheckBox;
    @FXML
    private ComboBox<String> blerCriterionComboBox;
    @FXML
    private Spinner<Integer> targetCodewordLengthSpinner;
    @FXML
    private Spinner<Integer> minErrorEventsSpinner;
    @FXML
    private Spinner<Integer> maxBlocksPerSnrSpinner;
    @FXML
    private Spinner<Double> confidenceLevelSpinner;
    @FXML
    private VBox advancedStatisticsBox;
    @FXML
    private ComboBox<String> ldpcProfileComboBox;
    @FXML
    private ScrollPane simulationScrollPane;
    @FXML
    private ComboBox<String> modulationComboBox;
    @FXML
    private ComboBox<String> channelComboBox;
    @FXML
    private ComboBox<String> waveformComboBox;
    @FXML
    private ComboBox<String> spatialModeComboBox;
    @FXML
    private Spinner<Integer> cyclicPrefixSpinner;
    @FXML
    private ComboBox<String> equalizerComboBox;
    @FXML
    private Spinner<Integer> infoBlockSpinner;
    @FXML
    private Spinner<Double> snrStartSpinner;
    @FXML
    private Spinner<Double> snrEndSpinner;
    @FXML
    private Spinner<Double> snrStepSpinner;
    @FXML
    private Spinner<Integer> blocksSpinner;
    @FXML
    private Spinner<Integer> maxIterationsSpinner;
    @FXML
    private Spinner<Double> normalizationSpinner;
    @FXML
    private Spinner<Integer> seedSpinner;
    @FXML
    private ProgressBar simulationProgressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private Label validationLabel;
    @FXML
    private TextArea configPreviewArea;
    @FXML
    private Label configFileStatusLabel;
    @FXML
    private Label modeStatusChip;
    @FXML
    private Button runButton;
    @FXML
    private CheckBox advancedModeCheckBox;
    @FXML
    private VBox advancedChannelSection;
    @FXML
    private VBox advancedIterationsBox;
    @FXML
    private VBox advancedNormalizationBox;
    @FXML
    private VBox advancedSeedBox;
    @FXML private ComboBox<String> decoderTypeComboBox;

    @FXML
    public void initialize() {
        setupFastScroll();
        ldpcProfileComboBox.setItems(FXCollections.observableArrayList(SimulationConfigFactory.supportedLdpcProfiles()));
        setupLdpcProfileComboPresentation();
        modulationComboBox.setItems(FXCollections.observableArrayList(SimulationConfigFactory.supportedModulations()));
        channelComboBox.setItems(FXCollections.observableArrayList(SimulationConfigFactory.supportedChannels()));
        waveformComboBox.setItems(FXCollections.observableArrayList(SimulationConfigFactory.supportedWaveforms()));
        spatialModeComboBox.setItems(FXCollections.observableArrayList(SimulationConfigFactory.supportedSpatialModes()));
        equalizerComboBox.setItems(FXCollections.observableArrayList(SimulationConfigFactory.supportedEqualizerModes()));

        cyclicPrefixSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 32, 8, 1));
        infoBlockSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(12, 4800, 240, 12));
        snrStartSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-5.0, 25.0, 0.0, 0.5));
        snrEndSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-5.0, 25.0, 8.0, 0.5));
        snrStepSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5, 5.0, 1.0, 0.5));
        blocksSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(20, 5000, 80, 10));
        maxIterationsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 100, 12, 1));
        normalizationSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.50, 1.00, 0.85, 0.05));
        seedSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 2025, 1));

        if (minErrorEventsSpinner != null) {
            minErrorEventsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                    1, 100000, SimulationConfig.DEFAULT_MIN_ERROR_EVENTS_PER_SNR, 5
            ));
        }
        if (maxBlocksPerSnrSpinner != null) {
            maxBlocksPerSnrSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                    20, 200000, SimulationConfig.DEFAULT_MAX_BLOCKS_PER_SNR, 50
            ));
        }
        if (confidenceLevelSpinner != null) {
            confidenceLevelSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(
                    0.80, 0.999, SimulationConfig.DEFAULT_CONFIDENCE_LEVEL, 0.01
            ));
        }
        if (nrBaseGraphComboBox != null) {
            nrBaseGraphComboBox.setItems(FXCollections.observableArrayList(
                    SimulationConfig.NR_BG_AUTO,
                    SimulationConfig.NR_BG1,
                    SimulationConfig.NR_BG2
            ));
            nrBaseGraphComboBox.setValue(SimulationConfig.NR_BG_AUTO);
        }

        if (liftingSizeComboBox != null) {
            liftingSizeComboBox.setItems(FXCollections.observableArrayList(8, 16, 32));
            liftingSizeComboBox.setValue(8);
        }

        if (crcBitsComboBox != null) {
            crcBitsComboBox.setItems(FXCollections.observableArrayList(
                    SimulationConfig.CRC_NONE,
                    SimulationConfig.CRC_16
            ));
            crcBitsComboBox.setValue(SimulationConfig.CRC_NONE);
        }
        if (blerCriterionComboBox != null) {
            blerCriterionComboBox.setItems(FXCollections.observableArrayList(SimulationConfigFactory.supportedBlerCriteria()));
            blerCriterionComboBox.setValue(SimulationConfig.BLER_BY_BIT_MISMATCH);
        }

        if (targetCodewordLengthSpinner != null) {
            targetCodewordLengthSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50000, 0, 8)
            );
            targetCodewordLengthSpinner.setEditable(true);
        }
        if (snrDomainComboBox != null) {
            snrDomainComboBox.setItems(FXCollections.observableArrayList(SimulationConfigFactory.supportedSnrDomains()));
            snrDomainComboBox.setValue(SimulationConfig.SNR_DOMAIN_EB_N0);
        }
        if (decoderTypeComboBox != null) {
            decoderTypeComboBox.setItems(FXCollections.observableArrayList(
                    SimulationConfigFactory.supportedDecoderTypes()
            ));
            decoderTypeComboBox.setValue(SimulationConfig.DECODER_NMS);
        }
        if (harqMaxRetxSpinner != null) {
            harqMaxRetxSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 8, SimulationConfig.DEFAULT_HARQ_MAX_RETX, 1)
            );
            harqMaxRetxSpinner.setEditable(true);
        }
        if (harqEnabledCheckBox != null) {
            harqEnabledCheckBox.setSelected(SimulationConfig.DEFAULT_HARQ_ENABLED);
        }

        configureEditableSpinners();
        setupNrChainUiRules();
        attachPreviewListeners();
        attachScientificListeners();
        if (advancedModeCheckBox != null) {
            advancedModeCheckBox.setSelected(true);
        }
        updateAdvancedModeVisibility();
        updateModeStatusChip();
        applyConfig(configProfiles.recommendedProfile());
        simulationProgressBar.setProgress(0.0);
        progressLabel.setText("Расчёт не запущен");
        validationLabel.setText("Параметры готовы к запуску");
        configFileStatusLabel.setText("Используйте готовые исследовательские профили для быстрого старта, а при необходимости сохраняйте и повторно открывайте собственные конфигурации эксперимента.");
        installTooltips();
        updatePreview();
    }

    @FXML
    private void onApplyRecommendedProfile() {
        applyNamedResearchProfile("Опорный 5G-like сценарий");
    }

    @FXML
    private void onApplyResearchReferenceProfile() {
        applyNamedResearchProfile("Опорный 5G-like сценарий");
    }

    @FXML
    private void onApplyAwgnBaselineProfile() {
        applyNamedResearchProfile("AWGN baseline / SISO");
    }

    @FXML
    private void onApplyRayleighOfdmProfile() {
        applyNamedResearchProfile("Rayleigh + OFDM baseline");
    }
    @FXML
    private void onApplyDefense256QamProfile() {
        applyNamedDefenseProfile("Защита · 256-QAM stress");
    }

    @FXML
    private void onApplyDiversityRobustProfile() {
        applyNamedResearchProfile("Diversity-устойчивый профиль");
    }

    @FXML
    private void onApplyHighRate16QamProfile() {
        applyNamedResearchProfile("Высокоскоростной 16-QAM");
    }

    @FXML
    private void onApplyEducationalCalibrationProfile() {
        applyNamedResearchProfile("Учебная калибровка");
    }

    @FXML
    private void onApplyDeepFadeOfdmProfile() {
        applyNamedResearchProfile("Глубокий fading + OFDM");
    }

    @FXML
    private void onApplyDefenseAwgnProfile() {
        applyNamedDefenseProfile("Защита · Базовый AWGN сценарий");
    }

    @FXML
    private void onApplyDefenseRayleighProfile() {
        applyNamedDefenseProfile("Защита · Rayleigh vs OFDM");
    }

    @FXML
    private void onApplyDefenseDiversityProfile() {
        applyNamedDefenseProfile("Защита · Diversity gain");
    }

    @FXML
    private void onApplyDefenseHighRateProfile() {
        applyNamedDefenseProfile("Защита · 16-QAM trade-off");
    }

    @FXML
    private void onApplyDefenseOfdmProfile() {
        applyNamedDefenseProfile("Защита · OFDM showcase");
    }

    @FXML
    private void onApplyDefenseLdpcVsPolarLdpcProfile() {
        applyNamedDefenseProfile("Защита · LDPC vs Polar · LDPC ref");
    }

    @FXML
    private void onApplyDefenseLdpcVsPolarPolarProfile() {
        applyNamedDefenseProfile("Защита · LDPC vs Polar · Polar ref");
    }

    @FXML
    private void onToggleAdvancedMode() {
        updateAdvancedModeVisibility();
        updateModeStatusChip();
        updatePreview();
        validationLabel.setText(advancedModeCheckBox != null && advancedModeCheckBox.isSelected()
                ? "Расширенный режим активен: доступны OFDM/MIMO и decoder-тонкие настройки"
                : "Базовый режим активен: скрыты продвинутые параметры, оставлены основные поля эксперимента");
    }

    @FXML
    private void onResetForm() {
        applyConfig(configProfiles.recommendedProfile());
        simulationProgressBar.setProgress(0.0);
        progressLabel.setText("Форма сброшена");
        validationLabel.setText("Используются значения по умолчанию");
        updatePreview();
    }

    @FXML
    private void onOpenHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Справка по параметрам моделирования");
        alert.setHeaderText("Описание параметров, метрик и интерпретации результатов");
        TextArea helpArea = new TextArea(parameterHelpService.buildHelpDialogText());
        helpArea.setEditable(false);
        helpArea.setWrapText(true);
        helpArea.setPrefColumnCount(70);
        helpArea.setPrefRowCount(28);
        helpArea.getStyleClass().add("mono-text-area");
        alert.getDialogPane().setContent(helpArea);
        alert.getDialogPane().getStylesheets().add(SimulationController.class
                .getResource("/ru/vkr/ldpcapp/styles/app.css")
                .toExternalForm());
        alert.getDialogPane().getStyleClass().add("about-dialog");
        alert.showAndWait();
        validationLabel.setText("Открыта расширенная справка по параметрам и метрикам эксперимента");
    }

    @FXML
    private void onSaveConfig() {
        try {
            SimulationConfig config = readConfig();
            configValidator.validate(config);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Сохранить конфигурацию эксперимента");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Properties (*.properties)", "*.properties"));
            fileChooser.setInitialFileName(configFileService.buildSuggestedFileName());
            File file = fileChooser.showSaveDialog(getWindow());
            if (file == null) {
                configFileStatusLabel.setText("Сохранение конфигурации отменено пользователем.");
                return;
            }

            configFileService.save(file.toPath(), config);
            configFileStatusLabel.setText("Конфигурация сохранена: " + file.getAbsolutePath());
        } catch (IllegalArgumentException exception) {
            configFileStatusLabel.setText(exception.getMessage());
            showError("Ошибка параметров эксперимента", exception.getMessage());
        } catch (IOException exception) {
            configFileStatusLabel.setText("Не удалось сохранить конфигурацию.");
            showError("Ошибка сохранения конфигурации", exception.getMessage());
        }
    }

    private void setupLdpcProfileComboPresentation() {
        if (ldpcProfileComboBox == null) {
            return;
        }

        ldpcProfileComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String profileId) {
                return profileId == null ? "" : SimulationConfigFactory.getProfileUiName(profileId);
            }

            @Override
            public String fromString(String string) {
                if (string == null || string.isBlank()) {
                    return null;
                }
                for (String id : ldpcProfileComboBox.getItems()) {
                    if (SimulationConfigFactory.getProfileUiName(id).equals(string)) {
                        return id;
                    }
                }
                return ldpcProfileComboBox.getValue();
            }
        });

        ldpcProfileComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : SimulationConfigFactory.getProfileUiName(item));
            }
        });

        ldpcProfileComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : SimulationConfigFactory.getProfileUiName(item));
            }
        });
    }

    @FXML
    private void onLoadConfig() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Загрузить конфигурацию эксперимента");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Properties (*.properties)", "*.properties"));
            File file = fileChooser.showOpenDialog(getWindow());
            if (file == null) {
                configFileStatusLabel.setText("Загрузка конфигурации отменена пользователем.");
                return;
            }

            SimulationConfig config = configFileService.load(file.toPath());
            applyConfig(config);
            updatePreview();
            configFileStatusLabel.setText("Конфигурация загружена: " + file.getAbsolutePath());
            validationLabel.setText("Параметры загружены из файла и готовы к запуску");
        } catch (IllegalArgumentException exception) {
            configFileStatusLabel.setText(exception.getMessage());
            showError("Ошибка чтения конфигурации", exception.getMessage());
        } catch (IOException exception) {
            configFileStatusLabel.setText("Не удалось загрузить конфигурацию.");
            showError("Ошибка загрузки конфигурации", exception.getMessage());
        }
    }

    @FXML
    private void onRunSimulation() {
        try {
            SimulationConfig config = readConfig();
            configValidator.validate(config);
            updatePreview();

            Task<List<ResultPoint>> task = simulationService.createTask(config);
            simulationProgressBar.progressProperty().unbind();
            simulationProgressBar.progressProperty().bind(task.progressProperty());
            progressLabel.textProperty().unbind();
            progressLabel.textProperty().bind(task.messageProperty());
            runButton.setDisable(true);
            validationLabel.setText("Идёт исследовательский расчёт BER/BLER по всем точкам SNR...");

            task.setOnSucceeded(event -> {
                simulationProgressBar.progressProperty().unbind();
                progressLabel.textProperty().unbind();
                List<ResultPoint> results = task.getValue();
                ExperimentSession.save(config, results);
                simulationProgressBar.setProgress(1.0);
                progressLabel.setText("Моделирование завершено: получено " + results.size() + " точек SNR");
                validationLabel.setText("Результаты сохранены. Откройте экран результатов для анализа BER/BLER, throughput, spectral efficiency и OFDM/MIMO-like выигрышей.");
                runButton.setDisable(false);
                updatePreview();
            });

            task.setOnFailed(event -> {
                simulationProgressBar.progressProperty().unbind();
                progressLabel.textProperty().unbind();
                simulationProgressBar.setProgress(0.0);
                progressLabel.setText("Во время расчёта произошла ошибка");
                validationLabel.setText(task.getException() == null ? "Неизвестная ошибка" : task.getException().getMessage());
                runButton.setDisable(false);
                showError(
                        "Ошибка моделирования",
                        task.getException() == null ? "Неизвестная ошибка при расчёте." : task.getException().getMessage()
                );
            });

            task.setOnCancelled(event -> {
                simulationProgressBar.progressProperty().unbind();
                progressLabel.textProperty().unbind();
                simulationProgressBar.setProgress(0.0);
                progressLabel.setText("Расчёт отменён");
                validationLabel.setText("Вычислительный процесс был остановлен");
                runButton.setDisable(false);
            });

            Thread worker = new Thread(task, "ldpc-research-simulation-task");
            worker.setDaemon(true);
            worker.start();
        } catch (IllegalArgumentException exception) {
            simulationProgressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            simulationProgressBar.setProgress(0.0);
            progressLabel.setText("Запуск невозможен");
            validationLabel.setText(exception.getMessage());
            runButton.setDisable(false);
            showError("Ошибка параметров эксперимента", exception.getMessage());
        }
    }

    public SimulationConfig readConfig() {
        commitEditorValues();
        String profile = ldpcProfileComboBox.getValue();
        String waveform = waveformComboBox.getValue();

        int z = liftingSizeComboBox == null || liftingSizeComboBox.getValue() == null
                ? 8
                : liftingSizeComboBox.getValue();

        String nrBg = nrBaseGraphComboBox == null || nrBaseGraphComboBox.getValue() == null
                ? SimulationConfig.NR_BG_AUTO
                : nrBaseGraphComboBox.getValue();

        int normalizedInfoLength = SimulationConfigFactory.normalizeInfoBlockLength(
                infoBlockSpinner.getValue(),
                profile,
                z,
                nrBg
        );

        SimulationConfig config = new SimulationConfig(
                normalizedInfoLength,
                snrStartSpinner.getValue(),
                snrEndSpinner.getValue(),
                snrStepSpinner.getValue(),
                blocksSpinner.getValue(),
                maxIterationsSpinner.getValue(),
                normalizationSpinner.getValue(),
                seedSpinner.getValue(),
                modulationComboBox.getValue(),
                channelComboBox.getValue(),
                profile,
                waveform,
                spatialModeComboBox.getValue(),
                SimulationConfigFactory.normalizeCyclicPrefix(cyclicPrefixSpinner.getValue(), waveform),
                equalizerComboBox.getValue()
        );

        config.setAdaptiveStopEnabled(adaptiveStopCheckBox == null || adaptiveStopCheckBox.isSelected());
        config.setMinErrorEventsPerSnr(minErrorEventsSpinner == null
                ? SimulationConfig.DEFAULT_MIN_ERROR_EVENTS_PER_SNR
                : minErrorEventsSpinner.getValue());
        config.setMaxBlocksPerSnr(maxBlocksPerSnrSpinner == null
                ? SimulationConfig.DEFAULT_MAX_BLOCKS_PER_SNR
                : maxBlocksPerSnrSpinner.getValue());
        config.setConfidenceLevel(confidenceLevelSpinner == null
                ? SimulationConfig.DEFAULT_CONFIDENCE_LEVEL
                : confidenceLevelSpinner.getValue());
        config.setNrBaseGraph(SimulationConfig.NR_BG_AUTO);
        config.setLiftingSize(8);

        config.setCrcEnabled(true);
        config.setCrcBits(SimulationConfig.CRC_16);
        config.setSegmentationEnabled(true);
        config.setRateMatchingEnabled(true);
        config.setTargetCodewordLength(192); // пример
        config.setBlerCriterion(SimulationConfig.BLER_BY_BIT_MISMATCH);

        if (nrBaseGraphComboBox != null) {
            config.setNrBaseGraph(nrBaseGraphComboBox.getValue());
        }
        if (liftingSizeComboBox != null) {
            config.setLiftingSize(liftingSizeComboBox.getValue());
        }
        if (crcEnabledCheckBox != null) {
            config.setCrcEnabled(crcEnabledCheckBox.isSelected());
        }
        if (crcBitsComboBox != null) {
            config.setCrcBits(crcBitsComboBox.getValue());
        }
        if (segmentationEnabledCheckBox != null) {
            config.setSegmentationEnabled(segmentationEnabledCheckBox.isSelected());
        }
        if (rateMatchingEnabledCheckBox != null) {
            config.setRateMatchingEnabled(rateMatchingEnabledCheckBox.isSelected());
        }
        if (targetCodewordLengthSpinner != null) {
            config.setTargetCodewordLength(targetCodewordLengthSpinner.getValue());
        }
        if (blerCriterionComboBox != null) {
            config.setBlerCriterion(blerCriterionComboBox.getValue());
        }
        if (!config.isCrcEnabled()) {
            config.setCrcBits(SimulationConfig.CRC_NONE);
        } else if (config.getCrcBits() == SimulationConfig.CRC_NONE) {
            config.setCrcBits(SimulationConfig.CRC_16);
        }
        if (!config.isCrcEnabled() && SimulationConfig.BLER_BY_CRC_FAIL.equals(config.getBlerCriterion())) {
            config.setBlerCriterion(SimulationConfig.BLER_BY_BIT_MISMATCH);
        }
        if (decoderTypeComboBox != null) {
            config.setDecoderType(decoderTypeComboBox.getValue());
        }
        if (harqEnabledCheckBox != null) {
            config.setHarqEnabled(harqEnabledCheckBox.isSelected());
        }
        if (harqMaxRetxSpinner != null) {
            config.setHarqMaxRetx(harqMaxRetxSpinner.getValue());
        }
        if (nrBaseGraphComboBox != null) {
            config.setNrBaseGraph(nrBaseGraphComboBox.getValue());
        }

        if (!config.isRateMatchingEnabled()) {
            config.setTargetCodewordLength(0);
        } else if (config.getTargetCodewordLength() > 0 && config.getTargetCodewordLength() < config.getInfoBlockLength()) {
            config.setTargetCodewordLength(config.getInfoBlockLength());
        }
        if (snrDomainComboBox != null && snrDomainComboBox.getValue() != null) {
            config.setSnrDomain(snrDomainComboBox.getValue());
        } else {
            config.setSnrDomain(SimulationConfig.SNR_DOMAIN_EB_N0);
        }

        return config;
    }

    public void openFromExternalConfig(SimulationConfig config, boolean autoRun) {
        if (config == null) {
            return;
        }
        applyConfig(config);
        updatePreview();
        validationLabel.setText(autoRun
                ? "Сценарий загружен из истории. Автоматически запускается моделирование..."
                : "Сценарий загружен из истории и готов к повторному анализу.");
        progressLabel.setText(autoRun ? "Подготовка автоматического запуска..." : "Сценарий из истории применён к форме");
        if (autoRun) {
            onRunSimulation();
        }
        if (harqEnabledCheckBox != null) {
            harqEnabledCheckBox.setSelected(config.isHarqEnabled());
        }
        if (harqMaxRetxSpinner != null && harqMaxRetxSpinner.getValueFactory() != null) {
            harqMaxRetxSpinner.getValueFactory().setValue(config.getHarqMaxRetx());
        }
    }

    private void setupFastScroll() {
        if (simulationScrollPane == null) {
            return;
        }

        simulationScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            // Не мешаем zoom-механикам (если где-то используешь Ctrl+wheel)
            if (event.isControlDown()) {
                return;
            }

            double deltaY = event.getDeltaY();
            if (Math.abs(deltaY) < 0.001) {
                return;
            }

            // Коэффициент скорости прокрутки: подбери 1.6..3.0
            double speedFactor = 2.2;

            double contentHeight = simulationScrollPane.getContent() == null
                    ? 1.0
                    : simulationScrollPane.getContent().getBoundsInLocal().getHeight();
            double viewportHeight = simulationScrollPane.getViewportBounds().getHeight();
            double scrollableHeight = Math.max(1.0, contentHeight - viewportHeight);

            double dv = -(deltaY * speedFactor) / scrollableHeight;
            double newV = clamp(simulationScrollPane.getVvalue() + dv, 0.0, 1.0);

            simulationScrollPane.setVvalue(newV);
            event.consume();
        });
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void attachPreviewListeners() {
        cyclicPrefixSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        infoBlockSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        snrStartSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        snrEndSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        snrStepSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        blocksSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        maxIterationsSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        normalizationSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        seedSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());

        if (adaptiveStopCheckBox != null) {
            adaptiveStopCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        }
        if (minErrorEventsSpinner != null) {
            minErrorEventsSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        }
        if (maxBlocksPerSnrSpinner != null) {
            maxBlocksPerSnrSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        }
        if (confidenceLevelSpinner != null) {
            confidenceLevelSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        }
        if (harqEnabledCheckBox != null) {
            harqEnabledCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        }
        if (harqMaxRetxSpinner != null) {
            harqMaxRetxSpinner.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        }
    }

    private void attachScientificListeners() {
        ldpcProfileComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            adjustInfoBlockForProfile(newValue);
            updatePreview();
        });
        if (liftingSizeComboBox != null) {
            liftingSizeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
                adjustInfoBlockForProfile(ldpcProfileComboBox.getValue());
                updatePreview();
            });
        }
        if (snrDomainComboBox != null) {
            snrDomainComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        }
        if (decoderTypeComboBox != null) {
            decoderTypeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        }
        if (nrBaseGraphComboBox != null) {
            nrBaseGraphComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
                adjustInfoBlockForProfile(ldpcProfileComboBox.getValue());
                updatePreview();
            });
        }
        modulationComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        channelComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        waveformComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            adjustCyclicPrefixForWaveform(newValue);
            updatePreview();
        });
        equalizerComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        spatialModeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
    }

    private void configureEditableSpinners() {
        cyclicPrefixSpinner.setEditable(true);
        infoBlockSpinner.setEditable(true);
        snrStartSpinner.setEditable(true);
        snrEndSpinner.setEditable(true);
        snrStepSpinner.setEditable(true);
        blocksSpinner.setEditable(true);
        maxIterationsSpinner.setEditable(true);
        normalizationSpinner.setEditable(true);
        seedSpinner.setEditable(true);

        if (minErrorEventsSpinner != null) {
            minErrorEventsSpinner.setEditable(true);
        }
        if (maxBlocksPerSnrSpinner != null) {
            maxBlocksPerSnrSpinner.setEditable(true);
        }
        if (confidenceLevelSpinner != null) {
            confidenceLevelSpinner.setEditable(true);
        }
        if (harqMaxRetxSpinner != null) {
            harqMaxRetxSpinner.setEditable(true);
        }
    }

    private void applyConfig(SimulationConfig config) {
        ldpcProfileComboBox.setValue(config.getLdpcProfile());
        modulationComboBox.setValue(config.getModulation());
        channelComboBox.setValue(config.getChannelModel());
        waveformComboBox.setValue(config.getWaveform());
        spatialModeComboBox.setValue(config.getSpatialMode());
        cyclicPrefixSpinner.getValueFactory().setValue(config.getCyclicPrefix());
        equalizerComboBox.setValue(config.getEqualizerMode());
        infoBlockSpinner.getValueFactory().setValue(config.getInfoBlockLength());
        snrStartSpinner.getValueFactory().setValue(config.getSnrStart());
        snrEndSpinner.getValueFactory().setValue(config.getSnrEnd());
        snrStepSpinner.getValueFactory().setValue(config.getSnrStep());
        blocksSpinner.getValueFactory().setValue(config.getBlocks());
        maxIterationsSpinner.getValueFactory().setValue(config.getMaxIterations());
        normalizationSpinner.getValueFactory().setValue(config.getNormalization());
        seedSpinner.getValueFactory().setValue(config.getSeed());
        adjustInfoBlockForProfile(config.getLdpcProfile());

        if (adaptiveStopCheckBox != null) {
            adaptiveStopCheckBox.setSelected(config.isAdaptiveStopEnabled());
        }
        if (minErrorEventsSpinner != null && minErrorEventsSpinner.getValueFactory() != null) {
            minErrorEventsSpinner.getValueFactory().setValue(config.getMinErrorEventsPerSnr());
        }
        if (maxBlocksPerSnrSpinner != null && maxBlocksPerSnrSpinner.getValueFactory() != null) {
            maxBlocksPerSnrSpinner.getValueFactory().setValue(config.getMaxBlocksPerSnr());
        }
        if (confidenceLevelSpinner != null && confidenceLevelSpinner.getValueFactory() != null) {
            confidenceLevelSpinner.getValueFactory().setValue(config.getConfidenceLevel());
        }
        if (nrBaseGraphComboBox != null) {
            nrBaseGraphComboBox.setValue(config.getNrBaseGraph());
        }
        if (liftingSizeComboBox != null) {
            liftingSizeComboBox.setValue(config.getLiftingSize());
        }
        if (crcEnabledCheckBox != null) {
            crcEnabledCheckBox.setSelected(config.isCrcEnabled());
        }
        if (crcBitsComboBox != null) {
            crcBitsComboBox.setValue(config.getCrcBits());
        }
        if (segmentationEnabledCheckBox != null) {
            segmentationEnabledCheckBox.setSelected(config.isSegmentationEnabled());
        }
        if (rateMatchingEnabledCheckBox != null) {
            rateMatchingEnabledCheckBox.setSelected(config.isRateMatchingEnabled());
        }
        if (targetCodewordLengthSpinner != null && targetCodewordLengthSpinner.getValueFactory() != null) {
            targetCodewordLengthSpinner.getValueFactory().setValue(config.getTargetCodewordLength());
        }
        if (blerCriterionComboBox != null) {
            blerCriterionComboBox.setValue(config.getBlerCriterion());
        }
        if (snrDomainComboBox != null) {
            snrDomainComboBox.setValue(config.getSnrDomain());
        }
        if (decoderTypeComboBox != null) {
            decoderTypeComboBox.setValue(config.getDecoderType());
        }
        if (snrDomainComboBox != null) {
            snrDomainComboBox.setValue(
                    config.getSnrDomain() == null ? SimulationConfig.SNR_DOMAIN_EB_N0 : config.getSnrDomain()
            );
        }
        if (decoderTypeComboBox != null) {
            decoderTypeComboBox.setValue(
                    config.getDecoderType() == null ? SimulationConfig.DECODER_NMS : config.getDecoderType()
            );
        }
    }

    private void updatePreview() {
        try {
            SimulationConfig config = readConfig();
            configValidator.validate(config);
            configPreviewArea.setText(configFormatter.toSummaryText(config));
            validationLabel.setText("Параметры корректны");
            runButton.setDisable(false);
        } catch (Exception exception) {
            configPreviewArea.setText("Предпросмотр недоступен: " + exception.getMessage());
            validationLabel.setText("Обнаружена ошибка во входных параметрах");
            runButton.setDisable(true);
        }
    }

    private void adjustInfoBlockForProfile(String profile) {
        if (profile == null) {
            return;
        }

        int current = infoBlockSpinner.getValue();
        int z = (liftingSizeComboBox == null || liftingSizeComboBox.getValue() == null)
                ? 8
                : liftingSizeComboBox.getValue();

        String nrBg = (nrBaseGraphComboBox == null || nrBaseGraphComboBox.getValue() == null)
                ? SimulationConfig.NR_BG_AUTO
                : nrBaseGraphComboBox.getValue();

        int normalized = SimulationConfigFactory.normalizeInfoBlockLength(
                current,
                profile,
                z,
                nrBg
        );

        infoBlockSpinner.getValueFactory().setValue(normalized);
    }

    private void applyNamedResearchProfile(String profileName) {
        SimulationConfig config = configProfiles.byResearchProfileName(profileName);
        applyConfig(config);
        validationLabel.setText("Применён профиль: " + profileName);
        updatePreview();
    }

    private void applyNamedDefenseProfile(String profileName) {
        SimulationConfig config = configProfiles.byDefenseProfileName(profileName);
        applyConfig(config);
        if (advancedModeCheckBox != null) {
            advancedModeCheckBox.setSelected(true);
        }
        updateAdvancedModeVisibility();
        updateModeStatusChip();
        validationLabel.setText("Применён сценарий для защиты: " + profileName);
        progressLabel.setText("Сценарий для демонстрации подготовлен");
        updatePreview();
    }

    private void setupNrChainUiRules() {
        if (crcEnabledCheckBox != null && crcBitsComboBox != null) {
            crcEnabledCheckBox.selectedProperty().addListener((obs, oldValue, enabled) -> {
                if (enabled) {
                    if (crcBitsComboBox.getValue() == null || crcBitsComboBox.getValue() == SimulationConfig.CRC_NONE) {
                        crcBitsComboBox.setValue(SimulationConfig.CRC_16);
                    }
                } else {
                    crcBitsComboBox.setValue(SimulationConfig.CRC_NONE);
                    if (blerCriterionComboBox != null) {
                        blerCriterionComboBox.setValue(SimulationConfig.BLER_BY_BIT_MISMATCH);
                    }
                }
                updatePreview();
            });

            crcBitsComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == null) {
                    return;
                }
                if (newValue == SimulationConfig.CRC_NONE && crcEnabledCheckBox.isSelected()) {
                    crcEnabledCheckBox.setSelected(false);
                } else if (newValue > SimulationConfig.CRC_NONE && !crcEnabledCheckBox.isSelected()) {
                    crcEnabledCheckBox.setSelected(true);
                }
                updatePreview();
            });
        }

        if (rateMatchingEnabledCheckBox != null && targetCodewordLengthSpinner != null) {
            rateMatchingEnabledCheckBox.selectedProperty().addListener((obs, oldValue, enabled) -> {
                Integer currentE = targetCodewordLengthSpinner.getValue();
                int infoLen = infoBlockSpinner != null ? infoBlockSpinner.getValue() : 0;

                if (enabled) {
                    if (currentE == null || currentE <= 0) {
                        int suggested = Math.max(infoLen, 192);
                        targetCodewordLengthSpinner.getValueFactory().setValue(suggested);
                    }
                } else {
                    targetCodewordLengthSpinner.getValueFactory().setValue(0);
                }
                updatePreview();
            });

            targetCodewordLengthSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == null) {
                    return;
                }
                int infoLen = infoBlockSpinner != null ? infoBlockSpinner.getValue() : 0;

                if (rateMatchingEnabledCheckBox.isSelected() && newValue > 0 && newValue < infoLen) {
                    targetCodewordLengthSpinner.getValueFactory().setValue(infoLen);
                    return;
                }

                if (newValue > 0 && !rateMatchingEnabledCheckBox.isSelected()) {
                    rateMatchingEnabledCheckBox.setSelected(true);
                } else if (newValue == 0 && rateMatchingEnabledCheckBox.isSelected()) {
                    rateMatchingEnabledCheckBox.setSelected(false);
                }

                updatePreview();
            });
        }

        if (blerCriterionComboBox != null) {
            blerCriterionComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == null) {
                    return;
                }
                if (SimulationConfig.BLER_BY_CRC_FAIL.equals(newValue)
                        && crcEnabledCheckBox != null
                        && !crcEnabledCheckBox.isSelected()) {
                    crcEnabledCheckBox.setSelected(true);
                    if (crcBitsComboBox != null) {
                        crcBitsComboBox.setValue(SimulationConfig.CRC_16);
                    }
                }
                updatePreview();
            });
        }

        if (infoBlockSpinner != null && targetCodewordLengthSpinner != null && rateMatchingEnabledCheckBox != null) {
            infoBlockSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == null) {
                    return;
                }
                if (rateMatchingEnabledCheckBox.isSelected()) {
                    int e = targetCodewordLengthSpinner.getValue() == null ? 0 : targetCodewordLengthSpinner.getValue();
                    if (e > 0 && e < newValue) {
                        targetCodewordLengthSpinner.getValueFactory().setValue(newValue);
                    }
                }
                updatePreview();
            });
        }
    }

    private void updateAdvancedModeVisibility() {
        boolean advanced = advancedModeCheckBox != null && advancedModeCheckBox.isSelected();
        setManagedVisible(advancedChannelSection, advanced);
        setManagedVisible(advancedIterationsBox, advanced);
        setManagedVisible(advancedNormalizationBox, advanced);
        setManagedVisible(advancedSeedBox, advanced);
        setManagedVisible(advancedStatisticsBox, advanced);
        setManagedVisible(advancedNrChainBox, advanced);
    }

    private void setManagedVisible(VBox node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void updateModeStatusChip() {
        if (modeStatusChip == null) {
            return;
        }
        boolean advanced = advancedModeCheckBox != null && advancedModeCheckBox.isSelected();
        modeStatusChip.setText(advanced ? "Расширенный режим" : "Базовый режим");
        modeStatusChip.getStyleClass().removeAll("simulation-mode-chip-basic", "simulation-mode-chip-advanced");
        modeStatusChip.getStyleClass().add(advanced ? "simulation-mode-chip-advanced" : "simulation-mode-chip-basic");
    }

    private void installTooltips() {
        ldpcProfileComboBox.setTooltip(new Tooltip(parameterHelpService.getTooltip("ldpcProfile")));
        modulationComboBox.setTooltip(new Tooltip(parameterHelpService.getTooltip("modulation")));
        channelComboBox.setTooltip(new Tooltip(parameterHelpService.getTooltip("channel")));
        waveformComboBox.setTooltip(new Tooltip(parameterHelpService.getTooltip("waveform")));
        spatialModeComboBox.setTooltip(new Tooltip(parameterHelpService.getTooltip("spatial")));
        infoBlockSpinner.setTooltip(new Tooltip(parameterHelpService.getTooltip("infoBlock")));
        blocksSpinner.setTooltip(new Tooltip(parameterHelpService.getTooltip("blocks")));
        maxIterationsSpinner.setTooltip(new Tooltip(parameterHelpService.getTooltip("iterations")));
        snrStartSpinner.setTooltip(new Tooltip(parameterHelpService.getTooltip("snrStart")));
        snrEndSpinner.setTooltip(new Tooltip(parameterHelpService.getTooltip("snrEnd")));
        snrStepSpinner.setTooltip(new Tooltip(parameterHelpService.getTooltip("snrStep")));
        normalizationSpinner.setTooltip(new Tooltip(parameterHelpService.getTooltip("normalization")));
        seedSpinner.setTooltip(new Tooltip(parameterHelpService.getTooltip("seed")));
        runButton.setTooltip(new Tooltip(parameterHelpService.getTooltip("run")));
        if (advancedModeCheckBox != null) {
            advancedModeCheckBox.setTooltip(new Tooltip(parameterHelpService.getTooltip("advancedMode")));
        }
        if (blerCriterionComboBox != null) {
            blerCriterionComboBox.setTooltip(new Tooltip(parameterHelpService.getTooltip("blerCriterion")));
        }
        if (snrDomainComboBox != null) {
            snrDomainComboBox.setTooltip(new Tooltip(parameterHelpService.getTooltip("snrDomain")));
        }
        if (decoderTypeComboBox != null) {
            decoderTypeComboBox.setTooltip(new Tooltip(parameterHelpService.getTooltip("decoderType")));
        }
        configPreviewArea.setTooltip(new Tooltip("Текстовое резюме текущего сценария: канал, модуляция, LDPC-профиль, диапазон SNR, OFDM/MIMO-like параметры и параметры декодера."));
    }

    private void commitEditorValues() {
        cyclicPrefixSpinner.increment(0);
        infoBlockSpinner.increment(0);
        snrStartSpinner.increment(0);
        snrEndSpinner.increment(0);
        snrStepSpinner.increment(0);
        blocksSpinner.increment(0);
        maxIterationsSpinner.increment(0);
        normalizationSpinner.increment(0);
        seedSpinner.increment(0);

        if (minErrorEventsSpinner != null) {
            minErrorEventsSpinner.increment(0);
        }
        if (maxBlocksPerSnrSpinner != null) {
            maxBlocksPerSnrSpinner.increment(0);
        }
        if (confidenceLevelSpinner != null) {
            confidenceLevelSpinner.increment(0);
        }
    }

    private void adjustCyclicPrefixForWaveform(String waveform) {
        if (waveform == null) {
            return;
        }
        int current = cyclicPrefixSpinner.getValue();
        int normalized = SimulationConfigFactory.normalizeCyclicPrefix(current, waveform);
        cyclicPrefixSpinner.getValueFactory().setValue(normalized);
        if (SimulationConfig.WAVEFORM_SC.equals(waveform)) {
            equalizerComboBox.setValue(SimulationConfig.EQUALIZER_NONE);
        } else if (equalizerComboBox.getValue() == null || SimulationConfig.EQUALIZER_NONE.equals(equalizerComboBox.getValue())) {
            equalizerComboBox.setValue(SimulationConfig.EQUALIZER_ZF);
        }
    }

    private Window getWindow() {
        return runButton == null || runButton.getScene() == null ? null : runButton.getScene().getWindow();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
