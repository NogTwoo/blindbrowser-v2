package meuparser.pipelines;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Exportador de dados de teste para mãºltiplos formatos
 * Suporta CSV, JSON, LaTeX e relatã³rios HTML
 */
public class TestDataExporter {

    private final DecimalFormat df = new DecimalFormat("0.000");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    /**
     * Exporta dados para CSV
     */
    public void exportToCSV(List<PerformanceProfiler.ProcessingMetrics> data, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Cabeã§alho CSV
            writer.println("Site,URL,ParseTime_ms,ClassTime_ms,SumTime_ms,FormatTime_ms,TotalTime_ms," +
                    "OriginalTokens,FinalTokens,NoiseRemoved,NoiseReductionRatio,CompressionRatio,Success,Error");

            // Dados
            for (PerformanceProfiler.ProcessingMetrics m : data) {
                writer.printf("%s,\"%s\",%d,%d,%d,%d,%d,%d,%d,%d,%s,%s,%s,\"%s\"\n",
                        m.siteName, m.url, m.parseTime, m.classTime, m.sumTime, m.formatTime, m.totalTime,
                        m.originalTokens, m.finalTokens, m.noiseRemoved, df.format(m.noiseReductionRatio),
                        df.format(m.compressionRatio), m.success, m.errorMessage != null ? m.errorMessage : "");
            }

            System.out.println("âœ… Dados exportados para CSV: " + filename);
        } catch (IOException e) {
            System.err.println("âŒ Erro ao exportar CSV: " + e.getMessage());
        }
    }

    /**
     * Exporta dados para JSON
     */
    public void exportToJSON(List<PerformanceProfiler.ProcessingMetrics> data,
                             MetricsCollector.SystemMetrics aggregatedMetrics, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("{");
            writer.println("  \"timestamp\": \"" + new Date() + "\",");
            writer.println("  \"blindbrowser_version\": \"1.0.0\",");
            writer.println("  \"test_summary\": {");
            writer.printf("    \"total_tests\": %d,\n", aggregatedMetrics.totalTests);
            writer.printf("    \"successful_tests\": %d,\n", aggregatedMetrics.successfulTests);
            writer.printf("    \"success_rate\": %s,\n", df.format(aggregatedMetrics.successRate));
            writer.printf("    \"avg_e2e_time_ms\": %s,\n", df.format(aggregatedMetrics.avgEndToEndTime));
            writer.printf("    \"avg_noise_reduction\": %s\n", df.format(aggregatedMetrics.avgNoiseReduction));
            writer.println("  },");
            writer.println("  \"detailed_results\": [");

            for (int i = 0; i < data.size(); i++) {
                PerformanceProfiler.ProcessingMetrics m = data.get(i);
                writer.println("    {");
                writer.printf("      \"site_name\": \"%s\",\n", m.siteName);
                writer.printf("      \"url\": \"%s\",\n", m.url);
                writer.printf("      \"parse_time_ms\": %d,\n", m.parseTime);
                writer.printf("      \"class_time_ms\": %d,\n", m.classTime);
                writer.printf("      \"sum_time_ms\": %d,\n", m.sumTime);
                writer.printf("      \"format_time_ms\": %d,\n", m.formatTime);
                writer.printf("      \"total_time_ms\": %d,\n", m.totalTime);
                writer.printf("      \"original_tokens\": %d,\n", m.originalTokens);
                writer.printf("      \"final_tokens\": %d,\n", m.finalTokens);
                writer.printf("      \"noise_reduction_ratio\": %s,\n", df.format(m.noiseReductionRatio));
                writer.printf("      \"compression_ratio\": %s,\n", df.format(m.compressionRatio));
                writer.printf("      \"success\": %s", m.success);
                if (m.errorMessage != null) {
                    writer.printf(",\n      \"error\": \"%s\"", m.errorMessage);
                }
                writer.println();
                writer.print("    }");
                if (i < data.size() - 1) writer.println(",");
            }

            writer.println();
            writer.println("  ]");
            writer.println("}");

            System.out.println("âœ… Dados exportados para JSON: " + filename);
        } catch (IOException e) {
            System.err.println("âŒ Erro ao exportar JSON: " + e.getMessage());
        }
    }

    /**
     * Exporta tabela LaTeX completa para o relatã³rio
     */
    public void exportLatexTable(List<PerformanceProfiler.ProcessingMetrics> data, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("% Tabela gerada automaticamente pelo TestDataExporter");
            writer.println("% Data: " + new Date());
            writer.println();

            writer.println("\\begin{table}[htbp]");
            writer.println("\\centering");
            writer.println("\\caption{Resultados experimentais detalhados do sistema BlindBrowser}");
            writer.println("\\label{tab:resultados-experimentais-detalhados}");
            writer.println("\\begin{tabular}{l c c c c c c c}");
            writer.println("\\toprule");
            writer.println("\\textbf{Site} & \\textbf{$T_{\\mathrm{parse}}$} & \\textbf{$T_{\\mathrm{class}}$} & \\textbf{$T_{\\mathrm{sum}}$} & \\textbf{$T_{\\mathrm{fmt}}$} & \\textbf{$T_{\\mathrm{e2e}}$} & \\textbf{RRR} & \\textbf{CR} \\\\");
            writer.println("& \\textbf{(ms)} & \\textbf{(ms)} & \\textbf{(ms)} & \\textbf{(ms)} & \\textbf{(ms)} & & \\\\");
            writer.println("\\midrule");

            // Dados principais
            for (PerformanceProfiler.ProcessingMetrics m : data) {
                if (m.success) {
                    writer.printf("%s & %d & %d & %d & %d & %d & %s & %s \\\\\n",
                            m.siteName, m.parseTime, m.classTime, m.sumTime,
                            m.formatTime, m.totalTime, df.format(m.noiseReductionRatio),
                            df.format(m.compressionRatio));
                }
            }

            writer.println("\\midrule");

            // Linha de mã©dias
            List<PerformanceProfiler.ProcessingMetrics> successData = data.stream()
                    .filter(m -> m.success).toList();

            if (!successData.isEmpty()) {
                double avgParse = successData.stream().mapToLong(m -> m.parseTime).average().orElse(0);
                double avgClass = successData.stream().mapToLong(m -> m.classTime).average().orElse(0);
                double avgSum = successData.stream().mapToLong(m -> m.sumTime).average().orElse(0);
                double avgFormat = successData.stream().mapToLong(m -> m.formatTime).average().orElse(0);
                double avgTotal = successData.stream().mapToLong(m -> m.totalTime).average().orElse(0);
                double avgRRR = successData.stream().mapToDouble(m -> m.noiseReductionRatio).average().orElse(0);
                double avgCR = successData.stream().mapToDouble(m -> m.compressionRatio).average().orElse(0);

                writer.printf("\\textbf{Mã‰DIA} & \\textbf{%.0f} & \\textbf{%.0f} & \\textbf{%.0f} & \\textbf{%.0f} & \\textbf{%.0f} & \\textbf{%s} & \\textbf{%s} \\\\\n",
                        avgParse, avgClass, avgSum, avgFormat, avgTotal, df.format(avgRRR), df.format(avgCR));
            }

            writer.println("\\bottomrule");
            writer.println("\\end{tabular}");
            writer.println("\\caption*{\\footnotesize Fonte: Experimentos realizados pelo autor (2025). \\\\");
            writer.println("RRR = Reduã§ã£o Relativa de Ruã­do; CR = Taxa de Compressã£o.}");
            writer.println("\\end{table}");

            System.out.println("âœ… Tabela LaTeX exportada: " + filename);
        } catch (IOException e) {
            System.err.println("âŒ Erro ao exportar LaTeX: " + e.getMessage());
        }
    }

    /**
     * Gera relatã³rio HTML completo
     */
    public void generateHTMLReport(List<PerformanceProfiler.ProcessingMetrics> data,
                                   MetricsCollector.SystemMetrics aggregatedMetrics, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html>");
            writer.println("<head>");
            writer.println("    <meta charset='UTF-8'>");
            writer.println("    <title>BlindBrowser - Relatã³rio de Performance</title>");
            writer.println("    <style>");
            writer.println("        body { font-family: Arial, sans-serif; margin: 40px; }");
            writer.println("        table { border-collapse: collapse; width: 100%; }");
            writer.println("        th, td { border: 1px solid #ddd; padding: 8px; text-align: center; }");
            writer.println("        th { background-color: #f2f2f2; }");
            writer.println("        .summary { background-color: #e8f4f8; padding: 20px; margin: 20px 0; }");
            writer.println("        .success { color: green; font-weight: bold; }");
            writer.println("        .error { color: red; font-weight: bold; }");
            writer.println("    </style>");
            writer.println("</head>");
            writer.println("<body>");

            writer.println("<h1>ðŸŽ¯ BlindBrowser - Relatã³rio de Performance</h1>");
            writer.println("<p><strong>Data do teste:</strong> " + new Date() + "</p>");

            // Resumo executivo
            writer.println("<div class='summary'>");
            writer.println("<h2>ðŸ“Š Resumo Executivo</h2>");
            writer.printf("<p><strong>Total de testes:</strong> %d</p>\n", aggregatedMetrics.totalTests);
            writer.printf("<p><strong>Testes bem-sucedidos:</strong> %d</p>\n", aggregatedMetrics.successfulTests);
            writer.printf("<p><strong>Taxa de sucesso:</strong> %.1f%%</p>\n", aggregatedMetrics.successRate * 100);
            writer.printf("<p><strong>Tempo mã©dio end-to-end:</strong> %.1f ms</p>\n", aggregatedMetrics.avgEndToEndTime);
            writer.printf("<p><strong>Reduã§ã£o mã©dia de ruã­do:</strong> %.1f%%</p>\n", aggregatedMetrics.avgNoiseReduction * 100);
            writer.println("</div>");

            // Tabela detalhada
            writer.println("<h2>ðŸ“ˆ Resultados Detalhados</h2>");
            writer.println("<table>");
            writer.println("<thead>");
            writer.println("<tr><th>Site</th><th>Parse (ms)</th><th>Class (ms)</th><th>Sum (ms)</th><th>Format (ms)</th><th>Total (ms)</th><th>RRR</th><th>Status</th></tr>");
            writer.println("</thead>");
            writer.println("<tbody>");

            for (PerformanceProfiler.ProcessingMetrics m : data) {
                String statusClass = m.success ? "success" : "error";
                String statusText = m.success ? "âœ… OK" : "âŒ ERRO";

                writer.printf("<tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%s</td><td class='%s'>%s</td></tr>\n",
                        m.siteName, m.parseTime, m.classTime, m.sumTime, m.formatTime, m.totalTime,
                        df.format(m.noiseReductionRatio), statusClass, statusText);
            }

            writer.println("</tbody>");
            writer.println("</table>");

            writer.println("</body>");
            writer.println("</html>");

            System.out.println("âœ… Relatã³rio HTML gerado: " + filename);
        } catch (IOException e) {
            System.err.println("âŒ Erro ao gerar HTML: " + e.getMessage());
        }
    }

    /**
     * Exporta todos os formatos de uma vez
     */
    public void exportAll(List<PerformanceProfiler.ProcessingMetrics> data,
                          MetricsCollector.SystemMetrics aggregatedMetrics) {
        String timestamp = dateFormat.format(new Date());
        String baseName = "blindbrowser_results_" + timestamp;

        exportToCSV(data, baseName + ".csv");
        exportToJSON(data, aggregatedMetrics, baseName + ".json");
        exportLatexTable(data, baseName + ".tex");
        generateHTMLReport(data, aggregatedMetrics, baseName + ".html");

        System.out.println("\nðŸŽ‰ TODOS OS FORMATOS EXPORTADOS:");
        System.out.println("- CSV: " + baseName + ".csv");
        System.out.println("- JSON: " + baseName + ".json");
        System.out.println("- LaTeX: " + baseName + ".tex");
        System.out.println("- HTML: " + baseName + ".html");
    }
}