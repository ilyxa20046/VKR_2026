package ru.vkr.ldpcapp.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ru.vkr.ldpcapp.service.view.ViewManagerService;

import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.List;
import ru.vkr.ldpcapp.service.AppMetadata;
import ru.vkr.ldpcapp.service.IconFactory;
import ru.vkr.ldpcapp.service.ParameterHelpService;

import java.io.IOException;

public class MainController {

    private static final String DASHBOARD_VIEW = "/ru/vkr/ldpcapp/view/DashboardView.fxml";
    private final ParameterHelpService parameterHelpService = new ParameterHelpService();
    private static final String SIMULATION_VIEW = "/ru/vkr/ldpcapp/view/SimulationView.fxml";
    private static final String RESULTS_VIEW = "/ru/vkr/ldpcapp/view/ResultsView.fxml";
    private static final String COMPARE_VIEW = "/ru/vkr/ldpcapp/view/CompareView.fxml";
    private static final String BATCH_VIEW = "/ru/vkr/ldpcapp/view/BatchView.fxml";
    private ViewManagerService viewManagerService;
    private ResourceBundle messages;

    @FXML
    private StackPane contentPane;

    @FXML
    private Label statusLabel;


    @FXML
    private Button helpButton;

    @FXML
    private Button aboutButton;

    @FXML
    private Button dashboardNavButton;

    @FXML
    private Button simulationNavButton;

    @FXML
    private Button resultsNavButton;

    @FXML
    private Button compareNavButton;

    @FXML
    private Button batchNavButton;

    @FXML
    public void initialize() {
        this.viewManagerService = new ViewManagerService(contentPane);
        this.messages = ResourceBundle.getBundle("ru.vkr.ldpcapp.messages");
        installIcons();
        showDashboard();
    }

    @FXML
    private void onOpenDashboard() {
        showDashboard();
    }

    @FXML
    private void onOpenSimulation() {
        showView(SIMULATION_VIEW, messages.getString("status.simulation_open"), simulationNavButton);
    }

    @FXML
    private void onOpenResults() {
        showView(RESULTS_VIEW, "открыт экран результатов эксперимента", resultsNavButton);
    }

    @FXML
    private void onOpenCompare() {
        openComparisonView();
    }

    @FXML
    private void onOpenBatch() {
        showView(BATCH_VIEW, "открыт экран пакетного анализа сценариев", batchNavButton);
    }

    @FXML
    private void onOpenHelpCenter() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Справка по приложению");
        alert.setHeaderText("Help Center · параметры, метрики и интерпретация результатов");

        javafx.scene.control.TextArea helpArea = new javafx.scene.control.TextArea(parameterHelpService.buildHelpDialogText());
        helpArea.setEditable(false);
        helpArea.setWrapText(true);
        helpArea.setPrefColumnCount(72);
        helpArea.setPrefRowCount(30);
        helpArea.getStyleClass().add("mono-text-area");

        alert.getDialogPane().setContent(helpArea);
        alert.getDialogPane().getStylesheets().add(MainController.class
                .getResource("/ru/vkr/ldpcapp/styles/app.css")
                .toExternalForm());
        alert.getDialogPane().getStyleClass().add("about-dialog");
        alert.showAndWait();
        setStatus("открыт центр справки по параметрам, метрикам и интерпретации результатов");
    }

    @FXML
    private void onOpenAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, AppMetadata.buildAboutText(), ButtonType.OK);
        alert.setTitle("О программе");
        alert.setHeaderText(AppMetadata.PRODUCT_NAME + " · v" + AppMetadata.VERSION);
        alert.getDialogPane().getStylesheets().add(MainController.class
                .getResource("/ru/vkr/ldpcapp/styles/app.css")
                .toExternalForm());
        alert.getDialogPane().getStyleClass().add("about-dialog");
        alert.showAndWait();
        setStatus("открыто окно со сведениями о приложении");
    }

    private void showDashboard() {
        showView(DASHBOARD_VIEW, "открыт главный экран приложения", dashboardNavButton);
    }

    private void installIcons() {
        helpButton.setGraphic(IconFactory.icon("lab", 16));
        helpButton.setTooltip(new javafx.scene.control.Tooltip("Открыть центр справки по параметрам, метрикам и интерпретации результатов."));
        aboutButton.setGraphic(IconFactory.icon("lab", 16));

        dashboardNavButton.setGraphic(IconFactory.icon("dashboard", 18));
        simulationNavButton.setGraphic(IconFactory.icon("simulation", 18));
        resultsNavButton.setGraphic(IconFactory.icon("results", 18));
        compareNavButton.setGraphic(IconFactory.icon("compare", 18));
        batchNavButton.setGraphic(IconFactory.icon("batch", 18));
    }

    private void openComparisonView() {
        showView(COMPARE_VIEW, "открыт экран сравнения двух сценариев", compareNavButton);
    }

    private void showView(String resourcePath, String statusMessage, Button activeButton) {
        try {
            viewManagerService.loadView(resourcePath);
            setActiveNavButton(activeButton);
            setStatus(statusMessage);
        } catch (IOException exception) {
            showPlaceholder(
                    "Ошибка загрузки экрана",
                    "Не удалось загрузить представление " + resourcePath + ". Проверьте FXML-файл, контроллер и ресурсы приложения."
            );
            setStatus("не удалось загрузить выбранный экран приложения");
        }
    }

    private void showPlaceholder(String title, String description) {
        VBox card = new VBox(14);
        card.getStyleClass().add("placeholder-card");
        card.setMaxWidth(780);
        card.setPadding(new Insets(28));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("placeholder-title");
        titleLabel.setWrapText(true);

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("placeholder-text");
        descriptionLabel.setWrapText(true);

        Label nextStepLabel = new Label("Ближайшие шаги: добавить ResultsView.fxml, модели результатов, вычислительный сервис LDPC и экспорт данных.");
        nextStepLabel.getStyleClass().add("placeholder-note");
        nextStepLabel.setWrapText(true);

        card.getChildren().setAll(titleLabel, descriptionLabel, nextStepLabel);
        contentPane.getChildren().setAll(card);
    }

    private void setActiveNavButton(Button activeButton) {
        List<Button> navButtons = Arrays.asList(
                dashboardNavButton,
                simulationNavButton,
                resultsNavButton,
                compareNavButton,
                batchNavButton
        );

        for (Button button : navButtons) {
            button.getStyleClass().remove("nav-button-selected");
        }

        if (activeButton != null && !activeButton.getStyleClass().contains("nav-button-selected")) {
            activeButton.getStyleClass().add("nav-button-selected");
        }
    }

    private void setStatus(String message) {
        statusLabel.setText("Статус: " + message);
    }
}
