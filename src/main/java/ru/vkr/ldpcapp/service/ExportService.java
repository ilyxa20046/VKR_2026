package ru.vkr.ldpcapp.service;

import javafx.scene.chart.LineChart;
import ru.vkr.ldpcapp.model.BatchScenarioResult;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.config.SimulationConfigFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class ExportService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final CsvWriter csvWriter;
    private final ChartExportService chartExportService;
    private final TextReportWriter textReportWriter;
    private final ChapterThreeMaterialsService chapterThreeMaterialsService;
    private final BatchReportService batchReportService;
    private final PresentationSummaryService presentationSummaryService;
    private final RichDocumentService richDocumentService;
    private final SimulationConfigFormatter configFormatter = new SimulationConfigFormatter();

    public ExportService() {
        this(
                new CsvWriter(),
                new ChartExportService(),
                new TextReportWriter(),
                new ChapterThreeMaterialsService(),
                new BatchReportService(),
                new PresentationSummaryService(),
                new RichDocumentService()
        );
    }

    public ExportService(
            CsvWriter csvWriter,
            ChartExportService chartExportService,
            TextReportWriter textReportWriter,
            ChapterThreeMaterialsService chapterThreeMaterialsService,
            BatchReportService batchReportService,
            PresentationSummaryService presentationSummaryService,
            RichDocumentService richDocumentService
    ) {
        this.csvWriter = csvWriter;
        this.chartExportService = chartExportService;
        this.textReportWriter = textReportWriter;
        this.chapterThreeMaterialsService = chapterThreeMaterialsService;
        this.batchReportService = batchReportService;
        this.presentationSummaryService = presentationSummaryService;
        this.richDocumentService = richDocumentService;
    }

    public Path exportResultsCsv(Path targetPath, SimulationConfig config, List<ResultPoint> points) throws IOException {
        String content = csvWriter.buildResultsCsv(config, points);
        csvWriter.write(targetPath, content);
        return targetPath;
    }

    public Path exportChartPng(Path targetPath, LineChart<Number, Number> chart) throws IOException {
        return chartExportService.exportLineChartPng(chart, targetPath);
    }

    public Path exportTextReport(Path targetPath, String content) throws IOException {
        textReportWriter.write(targetPath, content);
        return targetPath;
    }

    public Path exportExperimentBundle(
            Path parentDirectory,
            SimulationConfig config,
            List<ResultPoint> points,
            String reportContent,
            LineChart<Number, Number> berChart,
            LineChart<Number, Number> blerChart
    ) throws IOException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        Path bundleDirectory = Files.createDirectories(parentDirectory.resolve("ldpc_bundle_" + timestamp));

        Path csvPath = bundleDirectory.resolve("results.csv");
        Path berChartPath = bundleDirectory.resolve("ber_chart.png");
        Path blerChartPath = bundleDirectory.resolve("bler_chart.png");
        Path reportPath = bundleDirectory.resolve("report.txt");
        exportResultsCsv(csvPath, config, points);
        exportChartPng(berChartPath, berChart);
        exportChartPng(blerChartPath, blerChart);
        exportTextReport(reportPath, reportContent);
        exportManifest(bundleDirectory, config, points);

        return bundleDirectory;
    }

    public String buildCsvText(SimulationConfig config, List<ResultPoint> points) {
        return csvWriter.buildResultsCsv(config, points);
    }

    public String buildChapterThreeMaterialsText(SimulationConfig config, List<ResultPoint> points) {
        return chapterThreeMaterialsService.buildChapterThreeMaterials(config, points);
    }

    public Path exportChapterThreeMaterials(Path targetPath, SimulationConfig config, List<ResultPoint> points) throws IOException {
        String content = buildChapterThreeMaterialsText(config, points);
        textReportWriter.write(targetPath, content);
        return targetPath;
    }

    public String buildSuggestedCsvFileName() {
        return "ldpc_results_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".csv";
    }

    public String buildSuggestedChartFileName(String chartName) {
        String suffix = chartName == null || chartName.isBlank() ? "chart" : chartName.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
        return "ldpc_" + suffix + "_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".png";
    }

    public String buildSuggestedReportFileName() {
        return "ldpc_report_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".txt";
    }

    public String buildSuggestedChapterThreeFileName() {
        return "chapter3_materials_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".txt";
    }

    public String buildPresentationSummaryText(SimulationConfig config, List<ResultPoint> points) {
        return presentationSummaryService.buildExperimentSummary(config, points);
    }

    public String buildComparePresentationSummaryText(
            SimulationConfig leftConfig,
            SimulationConfig rightConfig,
            List<ResultPoint> leftResults,
            List<ResultPoint> rightResults,
            String leftTitle,
            String rightTitle
    ) {
        return presentationSummaryService.buildCompareSummary(leftConfig, rightConfig, leftResults, rightResults, leftTitle, rightTitle);
    }

    public String buildBatchPresentationSummaryText(SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) {
        return presentationSummaryService.buildBatchSummary(baseConfig, scenarios);
    }

    public Path exportPresentationSummary(Path targetPath, SimulationConfig config, List<ResultPoint> points) throws IOException {
        textReportWriter.write(targetPath, buildPresentationSummaryText(config, points));
        return targetPath;
    }

    public Path exportComparePresentationSummary(
            Path targetPath,
            SimulationConfig leftConfig,
            SimulationConfig rightConfig,
            List<ResultPoint> leftResults,
            List<ResultPoint> rightResults,
            String leftTitle,
            String rightTitle
    ) throws IOException {
        textReportWriter.write(targetPath, buildComparePresentationSummaryText(leftConfig, rightConfig, leftResults, rightResults, leftTitle, rightTitle));
        return targetPath;
    }

    public Path exportBatchPresentationSummary(Path targetPath, SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) throws IOException {
        textReportWriter.write(targetPath, buildBatchPresentationSummaryText(baseConfig, scenarios));
        return targetPath;
    }

    public String buildSuggestedPresentationSummaryFileName(String scope) {
        String safeScope = scope == null || scope.isBlank() ? "presentation_summary" : scope.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
        return safeScope + "_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".txt";
    }

    public String buildExperimentDocxFriendlyText(SimulationConfig config, List<ResultPoint> points) {
        return richDocumentService.buildExperimentDocxText(config, points);
    }

    public String buildCompareDocxFriendlyText(
            SimulationConfig leftConfig,
            SimulationConfig rightConfig,
            List<ResultPoint> leftResults,
            List<ResultPoint> rightResults,
            String leftTitle,
            String rightTitle
    ) {
        return richDocumentService.buildCompareDocxText(leftConfig, rightConfig, leftResults, rightResults, leftTitle, rightTitle);
    }

    public String buildBatchDocxFriendlyText(SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) {
        return richDocumentService.buildBatchDocxText(baseConfig, scenarios);
    }

    public String buildExperimentHtmlText(SimulationConfig config, List<ResultPoint> points) {
        return richDocumentService.buildExperimentHtml(config, points);
    }

    public String buildCompareHtmlText(
            SimulationConfig leftConfig,
            SimulationConfig rightConfig,
            List<ResultPoint> leftResults,
            List<ResultPoint> rightResults,
            String leftTitle,
            String rightTitle
    ) {
        return richDocumentService.buildCompareHtml(leftConfig, rightConfig, leftResults, rightResults, leftTitle, rightTitle);
    }

    public String buildBatchHtmlText(SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) {
        return richDocumentService.buildBatchHtml(baseConfig, scenarios);
    }

    public Path exportDocxFriendlyText(Path targetPath, String content) throws IOException {
        textReportWriter.write(targetPath, content);
        return targetPath;
    }

    public Path exportHtmlReport(Path targetPath, String content) throws IOException {
        textReportWriter.write(targetPath, content);
        return targetPath;
    }

    public String buildSuggestedDocxFriendlyFileName(String scope) {
        String safeScope = scope == null || scope.isBlank() ? "word_friendly_report" : scope.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
        return safeScope + "_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".txt";
    }

    public String buildSuggestedHtmlFileName(String scope) {
        String safeScope = scope == null || scope.isBlank() ? "html_report" : scope.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
        return safeScope + "_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".html";
    }

    public String buildBatchReportText(SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) {
        return batchReportService.buildBatchReport(baseConfig, scenarios);
    }

    public String buildBatchNarrativeText(SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) {
        return batchReportService.buildBatchNarrative(baseConfig, scenarios);
    }

    public String buildBatchCsvText(List<BatchScenarioResult> scenarios) {
        return batchReportService.buildBatchCsv(scenarios);
    }

    public Path exportBatchReport(Path targetPath, SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) throws IOException {
        String content = buildBatchReportText(baseConfig, scenarios);
        textReportWriter.write(targetPath, content);
        return targetPath;
    }

    public Path exportBatchCsv(Path targetPath, List<BatchScenarioResult> scenarios) throws IOException {
        String content = buildBatchCsvText(scenarios);
        textReportWriter.write(targetPath, content);
        return targetPath;
    }

    public Path exportBatchBundle(Path parentDirectory, SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) throws IOException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        Path bundleDirectory = Files.createDirectories(parentDirectory.resolve("batch_bundle_" + timestamp));
        Path reportPath = bundleDirectory.resolve("batch_report.txt");
        Path csvPath = bundleDirectory.resolve("batch_summary.csv");
        Path narrativePath = bundleDirectory.resolve("batch_narrative.txt");
        Path manifestPath = bundleDirectory.resolve("manifest.txt");

        exportBatchReport(reportPath, baseConfig, scenarios);
        exportBatchCsv(csvPath, scenarios);
        exportTextReport(narrativePath, buildBatchNarrativeText(baseConfig, scenarios));
        exportTextReport(manifestPath, buildBatchManifest(baseConfig, scenarios, bundleDirectory));

        return bundleDirectory;
    }

    public String buildSuggestedBatchReportFileName() {
        return "batch_report_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".txt";
    }

    public String buildSuggestedBatchCsvFileName() {
        return "batch_summary_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".csv";
    }

    public String buildSuggestedBatchBundleDirectoryName() {
        return "batch_bundle_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }

    public String buildSuggestedBundleDirectoryName() {
        return "ldpc_bundle_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }

    private String buildBundleManifest(SimulationConfig config, List<ResultPoint> points, Path bundleDirectory) {
        return String.join(System.lineSeparator(),
                "LDPC Experiment Bundle",
                "",
                "Directory: " + bundleDirectory.toAbsolutePath(),
                "Generated: " + LocalDateTime.now(),
                "",
                "Included files:",
                "- results.csv",
                "- ber_chart.png",
                "- bler_chart.png",
                "- report.txt",
                "- manifest.txt",
                "",
                "Experiment parameters:",
                configFormatter.toSummaryText(config),
                "",
                "Points exported: " + (points == null ? 0 : points.size())
        );
    }

    private String buildBatchManifest(SimulationConfig baseConfig, List<BatchScenarioResult> scenarios, Path bundleDirectory) {
        return String.join(System.lineSeparator(),
                "LDPC Batch Experiment Bundle",
                "",
                "Directory: " + bundleDirectory.toAbsolutePath(),
                "Generated: " + LocalDateTime.now(),
                "",
                "Included files:",
                "- batch_report.txt",
                "- batch_summary.csv",
                "- batch_narrative.txt",
                "- manifest.txt",
                "",
                "Base configuration:",
                baseConfig == null ? "not provided" : configFormatter.toSummaryText(baseConfig),
                "",
                "Scenarios exported: " + (scenarios == null ? 0 : scenarios.size())
        );
    }

    private String buildManifestText(SimulationConfig config, List<ResultPoint> points) {
        StringBuilder sb = new StringBuilder();
        sb.append("LDPC Research Studio Manifest").append(System.lineSeparator());
        sb.append("timestamp=").append(java.time.OffsetDateTime.now()).append(System.lineSeparator());
        sb.append("appName=").append(AppMetadata.PRODUCT_NAME).append(System.lineSeparator());
        sb.append("appVersion=").append(AppMetadata.VERSION).append(System.lineSeparator());

        if (config != null) {
            sb.append("seed=").append(config.getSeed()).append(System.lineSeparator());
            sb.append("ldpcProfile=").append(config.getLdpcProfile()).append(System.lineSeparator());
            sb.append("modulation=").append(config.getModulation()).append(System.lineSeparator());
            sb.append("channel=").append(config.getChannelModel()).append(System.lineSeparator());
            sb.append("waveform=").append(config.getWaveform()).append(System.lineSeparator());
            sb.append("spatialMode=").append(config.getSpatialMode()).append(System.lineSeparator());
            sb.append("equalizer=").append(config.getEqualizerMode()).append(System.lineSeparator());

            sb.append("snrStart=").append(config.getSnrStart()).append(System.lineSeparator());
            sb.append("snrEnd=").append(config.getSnrEnd()).append(System.lineSeparator());
            sb.append("snrStep=").append(config.getSnrStep()).append(System.lineSeparator());

            sb.append("infoBlockLength=").append(config.getInfoBlockLength()).append(System.lineSeparator());
            sb.append("blocks=").append(config.getBlocks()).append(System.lineSeparator());
            sb.append("maxIterations=").append(config.getMaxIterations()).append(System.lineSeparator());
            sb.append("normalization=").append(config.getNormalization()).append(System.lineSeparator());

            sb.append("adaptiveStopEnabled=").append(config.isAdaptiveStopEnabled()).append(System.lineSeparator());
            sb.append("minErrorEventsPerSnr=").append(config.getMinErrorEventsPerSnr()).append(System.lineSeparator());
            sb.append("maxBlocksPerSnr=").append(config.getMaxBlocksPerSnr()).append(System.lineSeparator());
            sb.append("confidenceLevel=").append(config.getConfidenceLevel()).append(System.lineSeparator());
        }

        int snrPointCount = points == null ? 0 : points.size();
        sb.append("snrPointCount=").append(snrPointCount).append(System.lineSeparator());

        if (points != null && !points.isEmpty()) {
            ResultPoint last = points.get(points.size() - 1);
            sb.append("lastPointConfidenceLevel=").append(last.getConfidenceLevel()).append(System.lineSeparator());
            sb.append("lastPointTotalBits=").append(last.getTotalBits()).append(System.lineSeparator());
            sb.append("lastPointTotalBlocks=").append(last.getTotalBlocks()).append(System.lineSeparator());
            sb.append("lastPointBitErrorsLdpc=").append(last.getBitErrorsLdpc()).append(System.lineSeparator());
            sb.append("lastPointBlockErrorsLdpc=").append(last.getBlockErrorsLdpc()).append(System.lineSeparator());
        }

        return sb.toString();
    }
    private Path exportManifest(Path targetDir, SimulationConfig config, List<ResultPoint> points) throws IOException {
        Path out = targetDir.resolve("manifest.txt");
        Files.writeString(out, buildManifestText(config, points), StandardCharsets.UTF_8);
        return out;
    }
}
