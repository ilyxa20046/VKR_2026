package ru.vkr.ldpcapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.vkr.ldpcapp.service.AppMetadata;

import java.net.URL;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getResource("/ru/vkr/ldpcapp/view/MainLayout.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 820);
        URL stylesheet = getResource("/ru/vkr/ldpcapp/styles/app.css");
        scene.getStylesheets().add(stylesheet.toExternalForm());

        stage.setTitle(AppMetadata.PRODUCT_NAME + " · v" + AppMetadata.VERSION);
        stage.setMinWidth(1100);
        stage.setMinHeight(760);
        stage.setScene(scene);
        stage.show();
    }

    private URL getResource(String path) {
        URL resource = MainApplication.class.getResource(path);
        if (resource == null) {
            throw new IllegalStateException("Resource not found: " + path);
        }
        return resource;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
