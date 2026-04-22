package ru.vkr.ldpcapp.service;

import ru.vkr.ldpcapp.model.BatchScenarioResult;
import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;

import java.util.List;

public class RichDocumentService {

    private final ReportService reportService;
    private final BatchReportService batchReportService;
    private final PresentationSummaryService presentationSummaryService;

    public RichDocumentService() {
        this(new ReportService(), new BatchReportService(), new PresentationSummaryService());
    }

    public RichDocumentService(
            ReportService reportService,
            BatchReportService batchReportService,
            PresentationSummaryService presentationSummaryService
    ) {
        this.reportService = reportService;
        this.batchReportService = batchReportService;
        this.presentationSummaryService = presentationSummaryService;
    }

    public String buildExperimentDocxText(SimulationConfig config, List<ResultPoint> points) {
        String detailed = reportService.buildDetailedReport(config, points);
        String summary = presentationSummaryService.buildExperimentSummary(config, points);

        return String.join(System.lineSeparator(),
                "LDPC RESEARCH STUDIO",
                "Документ для Word / DOCX-friendly вставки",
                "",
                "1. Краткая сводка для защиты",
                summary,
                "",
                "2. Развёрнутый отчёт эксперимента",
                detailed,
                "",
                "3. Рекомендация по вставке",
                "Ниже приведён текст в plain-text friendly структуре: его удобно переносить в Word, где можно применить стили заголовков, таблиц и нумерованных списков."
        );
    }

    public String buildCompareDocxText(
            SimulationConfig leftConfig,
            SimulationConfig rightConfig,
            List<ResultPoint> leftResults,
            List<ResultPoint> rightResults,
            String leftTitle,
            String rightTitle
    ) {
        String summary = presentationSummaryService.buildCompareSummary(
                leftConfig,
                rightConfig,
                leftResults,
                rightResults,
                leftTitle,
                rightTitle
        );

        return String.join(System.lineSeparator(),
                "LDPC RESEARCH STUDIO",
                "Документ для Word / DOCX-friendly вставки",
                "",
                "1. Краткая сравнительная сводка",
                summary,
                "",
                "2. Описание сценария A",
                leftConfig == null ? "не указано" : leftConfig.toSummaryText(),
                "",
                "3. Описание сценария B",
                rightConfig == null ? "не указано" : rightConfig.toSummaryText(),
                "",
                "4. Итог для текста диплома",
                "Этот фрагмент можно напрямую использовать в разделе сравнительного анализа главы 3, дополнив его рисунками BER(SNR), BLER(SNR) и таблицей required SNR / throughput / spectral efficiency."
        );
    }

    public String buildBatchDocxText(SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) {
        String summary = presentationSummaryService.buildBatchSummary(baseConfig, scenarios);
        String detailed = batchReportService.buildBatchReport(baseConfig, scenarios);

        return String.join(System.lineSeparator(),
                "LDPC RESEARCH STUDIO",
                "Документ для Word / DOCX-friendly вставки",
                "",
                "1. Краткая batch-сводка",
                summary,
                "",
                "2. Развёрнутый batch-отчёт",
                detailed,
                "",
                "3. Рекомендация по оформлению",
                "Этот текст подходит для переноса в Word перед вставкой в раздел сравнительного анализа, приложения и материалы к защите."
        );
    }

    public String buildExperimentHtml(SimulationConfig config, List<ResultPoint> points) {
        return wrapHtml(
                "Single experiment report",
                toHtml(reportService.buildDetailedReport(config, points)),
                toHtml(presentationSummaryService.buildExperimentSummary(config, points))
        );
    }

    public String buildCompareHtml(
            SimulationConfig leftConfig,
            SimulationConfig rightConfig,
            List<ResultPoint> leftResults,
            List<ResultPoint> rightResults,
            String leftTitle,
            String rightTitle
    ) {
        String body = presentationSummaryService.buildCompareSummary(
                leftConfig,
                rightConfig,
                leftResults,
                rightResults,
                leftTitle,
                rightTitle
        );
        return wrapHtml("Compare report", toHtml(body));
    }

    public String buildBatchHtml(SimulationConfig baseConfig, List<BatchScenarioResult> scenarios) {
        return wrapHtml(
                "Batch report",
                toHtml(presentationSummaryService.buildBatchSummary(baseConfig, scenarios)),
                toHtml(batchReportService.buildBatchReport(baseConfig, scenarios))
        );
    }

    private String wrapHtml(String title, String... sections) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!doctype html><html lang=\"ru\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("<title>").append(escapeHtml(title)).append("</title>")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;margin:40px;line-height:1.6;color:#111827;background:#f8fafc;}")
                .append("h1{font-size:28px;margin-bottom:16px;}pre{white-space:pre-wrap;background:#ffffff;border:1px solid #dbe4f0;border-radius:16px;padding:20px;}")
                .append(".card{margin-bottom:20px;} ")
                .append("</style></head><body>")
                .append("<h1>").append(escapeHtml(title)).append("</h1>");

        for (String section : sections) {
            builder.append("<div class=\"card\"><pre>")
                    .append(section)
                    .append("</pre></div>");
        }

        builder.append("</body></html>");
        return builder.toString();
    }

    private String toHtml(String text) {
        return escapeHtml(text).replace("\n", "<br>");
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
