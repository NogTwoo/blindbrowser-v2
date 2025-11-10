package meuparser.pipelines;

import java.util.List;
import java.text.DecimalFormat;

/**
 * Gerador automã¡tico de dados para o relatã³rio tã©cnico
 * Produz tabelas LaTeX e mã©tricas prontas para inserã§ã£o
 */
public class RelatorioDataGenerator {

    private static final String[] TEST_URLS = {
            "https://pt.wikipedia.org/wiki/Inteligãªncia_artificial",
            "https://pt.wikipedia.org/wiki/Acessibilidade_web",
            "https://g1.globo.com/tecnologia/",
            "https://educacao.uol.com.br/",
            "https://brasilescola.uol.com.br/informatica/",
            "https://brasilescola.uol.com.br/fisica/"
    };

    public static void main(String[] args) {
        RelatorioDataGenerator generator = new RelatorioDataGenerator();
        generator.generateFullReport();
    }

    public void generateFullReport() {
        System.out.println("ðŸŽ¯ GERADOR DE DADOS PARA RELATã“RIO Tã‰CNICO");
        System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");
        System.out.println();

        // 1. EXECUTAR BENCHMARK
        PerformanceProfiler profiler = new PerformanceProfiler();
        List<PerformanceProfiler.ProcessingMetrics> results = profiler.runBenchmark(TEST_URLS);

        // 2. GERAR TABELAS LATEX
        generateLatexTable(results);

        // 3. CALCULAR ESTATãSTICAS GERAIS
        generateStatistics(results);

        // 4. GERAR DADOS PARA EQUAã‡ã•ES
        generateEquationData(results);
    }

    private void generateLatexTable(List<PerformanceProfiler.ProcessingMetrics> results) {
        System.out.println("ðŸ“Š TABELA LATEX - SEã‡ãƒO 2.4.1");
        System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");

        // Cabeã§alho da tabela
        System.out.println("\\begin{table}[htbp]");
        System.out.println("\\centering");
        System.out.println("\\caption{Resultados experimentais de performance por componente}");
        System.out.println("\\label{tab:performance-experimental}");
        System.out.println("\\begin{tabular}{l c c c c c c}");
        System.out.println("\\toprule");
        System.out.println("\\textbf{Site} & \\textbf{$T_{\\text{parse}}$ (ms)} & \\textbf{$T_{\\text{class}}$ (ms)} & \\textbf{$T_{\\text{sum}}$ (ms)} & \\textbf{$T_{\\text{fmt}}$ (ms)} & \\textbf{$T_{\\text{e2e}}$ (ms)} & \\textbf{RRR} \\\\");
        System.out.println("\\midrule");

        // Dados das mã©tricas
        DecimalFormat df = new DecimalFormat("0.000");
        for (PerformanceProfiler.ProcessingMetrics m : results) {
            if (m.success) {
                System.out.printf("%s & %d & %d & %d & %d & %d & %s \\\\\n",
                        m.siteName, m.parseTime, m.classTime, m.sumTime,
                        m.formatTime, m.totalTime, df.format(m.noiseReductionRatio));
            }
        }

        System.out.println("\\bottomrule");
        System.out.println("\\end{tabular}");
        System.out.println("\\caption*{\\footnotesize Fonte: Experimento realizado pelo autor (2025).}");
        System.out.println("\\end{table}");
        System.out.println();
    }

    private void generateStatistics(List<PerformanceProfiler.ProcessingMetrics> results) {
        System.out.println("ðŸ“ˆ ESTATãSTICAS GERAIS");
        System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");

        List<PerformanceProfiler.ProcessingMetrics> successResults = results.stream()
                .filter(m -> m.success)
                .toList();

        if (successResults.isEmpty()) {
            System.out.println("âŒ Nenhum teste bem-sucedido!");
            return;
        }

        // Mã©dias
        double avgParse = successResults.stream().mapToLong(m -> m.parseTime).average().orElse(0);
        double avgClass = successResults.stream().mapToLong(m -> m.classTime).average().orElse(0);
        double avgSum = successResults.stream().mapToLong(m -> m.sumTime).average().orElse(0);
        double avgFormat = successResults.stream().mapToLong(m -> m.formatTime).average().orElse(0);
        double avgTotal = successResults.stream().mapToLong(m -> m.totalTime).average().orElse(0);
        double avgRRR = successResults.stream().mapToDouble(m -> m.noiseReductionRatio).average().orElse(0);

        System.out.printf("Mã‰DIAS EXPERIMENTAIS:\n");
        System.out.printf("- T_parse mã©dio: %.1f ms\n", avgParse);
        System.out.printf("- T_class mã©dio: %.1f ms\n", avgClass);
        System.out.printf("- T_sum mã©dio: %.1f ms\n", avgSum);
        System.out.printf("- T_fmt mã©dio: %.1f ms\n", avgFormat);
        System.out.printf("- T_e2e mã©dio: %.1f ms\n", avgTotal);
        System.out.printf("- RRR mã©dio: %.3f (%.1f%% de reduã§ã£o de ruã­do)\n", avgRRR, avgRRR * 100);
        System.out.println();

        // Para inserir no LaTeX
        System.out.println("ðŸ“ DADOS PARA INSERIR NO TEXTO:");
        System.out.printf("T_{{e2e}}^{{\\text{{experimental}}}} = %.0f + %.0f + %.0f + %.0f = %.0f\\text{{ ms}}\n",
                avgParse, avgClass, avgSum, avgFormat, avgTotal);
        System.out.println();
    }

    private void generateEquationData(List<PerformanceProfiler.ProcessingMetrics> results) {
        System.out.println("ðŸ”¢ DADOS PARA EQUAã‡ã•ES");
        System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");

        List<PerformanceProfiler.ProcessingMetrics> successResults = results.stream()
                .filter(m -> m.success).toList();

        if (successResults.isEmpty()) return;

        // Para Equaã§ã£o 2.2 (RRR)
        double avgOriginalTokens = successResults.stream().mapToInt(m -> m.originalTokens).average().orElse(0);
        double avgFinalTokens = successResults.stream().mapToInt(m -> m.finalTokens).average().orElse(0);
        double avgNoiseRemoved = successResults.stream().mapToInt(m -> m.noiseRemoved).average().orElse(0);

        System.out.printf("EQUAã‡ãƒO 2.2 (RRR) - Dados experimentais:\n");
        System.out.printf("- |R| mã©dio (tokens originais): %.0f\n", avgOriginalTokens);
        System.out.printf("- |R'| mã©dio (tokens finais): %.0f\n", avgFinalTokens);
        System.out.printf("- Ruã­do removido mã©dio: %.0f tokens\n", avgNoiseRemoved);
        System.out.printf("- RRR = 1 - (%.0f/%.0f) = %.3f\n", avgFinalTokens, avgOriginalTokens,
                1.0 - (avgFinalTokens/avgOriginalTokens));
        System.out.println();

        // Para Equaã§ã£o 2.3 (CR)
        double avgCR = successResults.stream().mapToDouble(m -> m.compressionRatio).average().orElse(0);
        System.out.printf("EQUAã‡ãƒO 2.3 (CR) - Taxa de compressã£o mã©dia: %.3f\n", avgCR);
        System.out.println();
    }
}