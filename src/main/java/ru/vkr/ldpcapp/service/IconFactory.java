package ru.vkr.ldpcapp.service;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.util.Objects;

public final class IconFactory {
    private IconFactory() {
    }

    public static ImageView icon(String name, double size) {
        String path = "/ru/vkr/ldpcapp/icons/" + name + ".svg";
        InputStream stream = IconFactory.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalArgumentException("Icon not found: " + path);
        }

        Image image = new Image(stream);
        ImageView view = new ImageView(image);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.getStyleClass().add("svg-icon");
        return view;
    }
}
