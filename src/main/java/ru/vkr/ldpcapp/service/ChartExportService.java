package ru.vkr.ldpcapp.service;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ChartExportService {

    public Path exportLineChartPng(LineChart<Number, Number> chart, Path targetPath) throws IOException {
        Objects.requireNonNull(chart, "chart must not be null");
        Objects.requireNonNull(targetPath, "targetPath must not be null");

        chart.applyCss();
        chart.layout();

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);

        WritableImage image = chart.snapshot(parameters, null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

        Path absolutePath = targetPath.toAbsolutePath();
        Path parent = absolutePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        boolean written = ImageIO.write(bufferedImage, "png", absolutePath.toFile());
        if (!written) {
            throw new IOException("Не удалось сохранить изображение графика в формате PNG");
        }

        return absolutePath;
    }
}
