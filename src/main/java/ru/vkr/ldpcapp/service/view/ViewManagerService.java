package ru.vkr.ldpcapp.service.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import ru.vkr.ldpcapp.MainApplication;

import java.io.IOException;
import java.net.URL;

public class ViewManagerService {

    private final StackPane contentPane;

    public ViewManagerService(StackPane contentPane) {
        this.contentPane = contentPane;
    }

    public void loadView(String resourcePath) throws IOException {
        URL resource = MainApplication.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }
        FXMLLoader loader = new FXMLLoader(resource);
        System.out.println("Loading FXML: " + resource);
        Parent view = loader.load();
        contentPane.getChildren().setAll(view);
    }
}