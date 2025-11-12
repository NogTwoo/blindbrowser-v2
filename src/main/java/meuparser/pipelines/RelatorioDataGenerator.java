package meuparser.pipelines;

import java.util.List;
import java.text.DecimalFormat;

/**
 * Gerador automã¡tico de dados para o relatã³rio técnico
 * Produz tabelas LaTeX e métricas prontas para inserção
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
        System.out.println("🎯 GERADOR DE DADOS PARA RELATã“RIO TÉCNICO");
        System.out.println("============================================");
        System.out.println();

        // 1. EXECUTAR BENCHMARK
        PerformanceProfiler profiler = new PerformanceProfiler();
        List<PerformanceProfiler.ProcessingMetrics> results = profiler.runBenchmark(TEST_URLS);

        // 2. GERAR TABELAS LATEX
        generateLatexTable(results);

        // 3. CALCULAR ESTATÍSTICAS GERAIS
        generateStatistics(results);

        // 4. GERAR DADOS PARA EQUAÇÕES
        generateEquationData(results);
    }

    private void generateLatexTable(List<PerformanceProfiler.ProcessingMetrics> results) {
        System.out.println("📊 TABELA LATEX - SEÇÃO 2.4.1");
        System.out.println("=======================================");

        // Cabeçalho da tabela
        System.out.println("\\begin{table}[htbp]");
        System.out.println("\\centering");
        System.out.println("\\caption{Resultados experimentais de performance por componente}");
        System.out.println("\\label{tab:performance-experimental}");
        System.out.println("\\begin{tabular}{l c c c c c c}");
        System.out.println("\\toprule");
        System.out.println("\\textbf{Site} & \\textbf{$T_{\\text{parse}}$ (ms)} & \\textbf{$T_{\\text{class}}$ (ms)} & \\textbf{$T_{\\text{sum}}$ (ms)} & \\textbf{$T_{\\text{fmt}}$ (ms)} & \\textbf{$T_{\\text{e2e}}$ (ms)} & \\textbf{RRR} \\\\");
        System.out.println("\\midrule");

        // Dados das métricas
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
        System.out.println("📈 ESTATÍSTICAS GERAIS");
        System.out.println("=====================");

        List<PerformanceProfiler.ProcessingMetrics> successResults = results.stream()
                .filter(m -> m.success)
                .toList();

        if (successResults.isEmpty()) {
            System.out.println("❌ Nenhum teste bem-sucedido!");
            return;
        }

        // Médias
        double avgParse = successResults.stream().mapToLong(m -> m.parseTime).average().orElse(0);
        double avgClass = successResults.stream().mapToLong(m -> m.classTime).average().orElse(0);
        double avgSum = successResults.stream().mapToLong(m -> m.sumTime).average().orElse(0);
        double avgFormat = successResults.stream().mapToLong(m -> m.formatTime).average().orElse(0);
        double avgTotal = successResults.stream().mapToLong(m -> m.totalTime).average().orElse(0);
        double avgRRR = successResults.stream().mapToDouble(m -> m.noiseReductionRatio).average().orElse(0);

        System.out.printf("MÉDIAS EXPERIMENTAIS:\n");
        System.out.printf("- T_parse médio: %.1f ms\n", avgParse);
        System.out.printf("- T_class médio: %.1f ms\n", avgClass);
        System.out.printf("- T_sum médio: %.1f ms\n", avgSum);
        System.out.printf("- T_fmt médio: %.1f ms\n", avgFormat);
        System.out.printf("- T_e2e médio: %.1f ms\n", avgTotal);
        System.out.printf("- RRR médio: %.3f (%.1f%% de redução de ruído)\n", avgRRR, avgRRR * 100);
        System.out.println();

        // Para inserir no LaTeX
        System.out.println("📖 DADOS PARA INSERIR NO TEXTO:");
        System.out.printf("T_{{e2e}}^{{\\text{{experimental}}}} = %.0f + %.0f + %.0f + %.0f = %.0f\\text{{ ms}}\n",
                avgParse, avgClass, avgSum, avgFormat, avgTotal);
        System.out.println();
    }

    private void generateEquationData(List<PerformanceProfiler.ProcessingMetrics> results) {
        System.out.println("ðŸ”¢ DADOS PARA EQUAÇÕES");
        System.out.println("=====================");

        List<PerformanceProfiler.ProcessingMetrics> successResults = results.stream()
                .filter(m -> m.success).toList();

        if (successResults.isEmpty()) return;

        // Para Equação 2.2 (RRR)
        double avgOriginalTokens = successResults.stream().mapToInt(m -> m.originalTokens).average().orElse(0);
        double avgFinalTokens = successResults.stream().mapToInt(m -> m.finalTokens).average().orElse(0);
        double avgNoiseRemoved = successResults.stream().mapToInt(m -> m.noiseRemoved).average().orElse(0);

        System.out.printf("EQUAÇÃO 2.2 (RRR) - Dados experimentais:\n");
        System.out.printf("- |R| médio (tokens originais): %.0f\n", avgOriginalTokens);
        System.out.printf("- |R'| médio (tokens finais): %.0f\n", avgFinalTokens);
        System.out.printf("- Ruído removido médio: %.0f tokens\n", avgNoiseRemoved);
        System.out.printf("- RRR = 1 - (%.0f/%.0f) = %.3f\n", avgFinalTokens, avgOriginalTokens,
                1.0 - (avgFinalTokens/avgOriginalTokens));
        System.out.println();

        // Para Equação 2.3 (CR)
        double avgCR = successResults.stream().mapToDouble(m -> m.compressionRatio).average().orElse(0);
        System.out.printf("EQUAÇÃO 2.3 (CR) - Taxa de compressão média: %.3f\n", avgCR);
        System.out.println();
    }
}