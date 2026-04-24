package ru.vkr.ldpcapp.service;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;

import java.util.Locale;

public class ChartInteractionService {

    public void installPointTooltips(LineChart<Number, Number> chart, String xLabel, String yLabel, boolean logScaleMetric) {
        Platform.runLater(() -> {
            if (chart == null || chart.getData() == null) {
                return;
            }

            for (XYChart.Series<Number, Number> series : chart.getData()) {
                for (XYChart.Data<Number, Number> point : series.getData()) {
                    Node node = point.getNode();
                    if (node == null) {
                        continue;
                    }

                    double xVal = point.getXValue().doubleValue();
                    double yVal = point.getYValue().doubleValue();

                    String xText = String.format(Locale.US, "%.2f", xVal);
                    String yText;
                    if (logScaleMetric) {
                        double metric = Math.pow(10.0, yVal);
                        yText = String.format(Locale.US, "%.3e (log10=%.2f)", metric, yVal);
                    } else {
                        yText = String.format(Locale.US, "%.4f", yVal);
                    }

                    Tooltip tooltip = new Tooltip(
                            series.getName()
                                    + "\n" + xLabel + ": " + xText
                                    + "\n" + yLabel + ": " + yText
                    );
                    Tooltip.install(node, tooltip);

                    node.setOnMouseEntered(e ->
                            node.setStyle("-fx-background-color: white, #0ea5e9; -fx-background-insets: 0, 2; -fx-background-radius: 6;"));
                    node.setOnMouseExited(e -> node.setStyle(""));
                }
            }
        });
    }

    public void enableZoomAndReset(LineChart<Number, Number> chart) {
        if (chart == null) {
            return;
        }

        NumberAxis xAxis = chart.getXAxis() instanceof NumberAxis ? (NumberAxis) chart.getXAxis() : null;
        NumberAxis yAxis = chart.getYAxis() instanceof NumberAxis ? (NumberAxis) chart.getYAxis() : null;
        if (xAxis == null || yAxis == null) {
            return;
        }

        Platform.runLater(() -> {
            Node plotArea = chart.lookup(".chart-plot-background");
            if (plotArea == null) {
                return;
            }

            final boolean initialXAuto = xAxis.isAutoRanging();
            final boolean initialYAuto = yAxis.isAutoRanging();

            plotArea.setOnScroll(event -> {
                if (xAxis.isAutoRanging() || yAxis.isAutoRanging()) {
                    xAxis.setAutoRanging(false);
                    yAxis.setAutoRanging(false);
                }

                double factor = event.getDeltaY() > 0 ? 0.90 : 1.10;
                zoomAxis(xAxis, factor);
                zoomAxis(yAxis, factor);
                event.consume();
            });

            plotArea.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    xAxis.setAutoRanging(initialXAuto);
                    yAxis.setAutoRanging(initialYAuto);
                }
            });
        });
    }

    private void zoomAxis(NumberAxis axis, double factor) {
        double min = axis.getLowerBound();
        double max = axis.getUpperBound();
        double center = (min + max) / 2.0;
        double halfRange = (max - min) / 2.0 * factor;

        // Чтобы ось не схлопывалась.
        halfRange = Math.max(halfRange, 1e-9);

        axis.setLowerBound(center - halfRange);
        axis.setUpperBound(center + halfRange);
    }
}