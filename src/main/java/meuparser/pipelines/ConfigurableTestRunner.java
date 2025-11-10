package meuparser.pipelines;

import java.util.*;

/**
 * Runner configurã¡vel para testes personalizados
 * Permite ajustar parã¢metros especã­ficos para diferentes cenã¡rios
 */
public class ConfigurableTestRunner {

    public static class TestConfiguration {
        public String[] testUrls;
        public int iterations = 1;
        public long delayBetweenTests = 0; // ms
        public boolean exportResults = true;
        public boolean generateLatex = true;
        public boolean generateHTML = true;
        public boolean verboseOutput = true;
        public String outputPrefix = "test_results";

        // Parã¢metros especã­ficos do BlindBrowser
        public int parseTimeout = 10000; // ms
        public boolean enableCache = true;
        public boolean measureMemoryUsage = false;
    }

    public static void main(String[] args) {
        // Configuraã§ã£o rã¡pida para relatã³rio
        ConfigurableTestRunner runner = new ConfigurableTestRunner();

        TestConfiguration quickConfig = new TestConfiguration();
        quickConfig.testUrls = new String[]{
                "https://pt.wikipedia.org/wiki/Inteligãªncia_artificial",
                "https://brasilescola.uol.com.br/informatica/",
                "https://g1.globo.com/tecnologia/"
        };
        quickConfig.iterations = 3;
        quickConfig.outputPrefix = "relatorio_final";

        runner.runConfigurableTest(quickConfig);
    }

    /**
     * Executa teste com configuraã§ã£o personalizada
     */
    public List<PerformanceProfiler.ProcessingMetrics> runConfigurableTest(TestConfiguration config) {
        System.out.println("âš™ï¸ TESTE CONFIGURãVEL BLINDBROWSER");
        System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");
        System.out.printf("URLs: %d | Iteraã§ãµes: %d | Delay: %dms\n",
                config.testUrls.length, config.iterations, config.delayBetweenTests);
        System.out.println("â•".repeat(40));

        List<PerformanceProfiler.ProcessingMetrics> allResults = new ArrayList<>();
        PerformanceProfiler profiler = new PerformanceProfiler();

        for (int iteration = 1; iteration <= config.iterations; iteration++) {
            if (config.verboseOutput) {
                System.out.printf("\nðŸ”„ ITERAã‡ãƒO %d/%d\n", iteration, config.iterations);
                System.out.println("-".repeat(30));
            }

            List<PerformanceProfiler.ProcessingMetrics> iterationResults =
                    profiler.runBenchmark(config.testUrls);
            allResults.addAll(iterationResults);

            // Delay entre iteraã§ãµes se configurado
            if (config.delayBetweenTests > 0 && iteration < config.iterations) {
                if (config.verboseOutput) {
                    System.out.printf("â±ï¸ Aguardando %dms...\n", config.delayBetweenTests);
                }
                try {
                    Thread.sleep(config.delayBetweenTests);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Processa resultados
        if (config.exportResults) {
            exportResults(allResults, config);
        }

        // Relatã³rio de consolidaã§ã£o
        generateConsolidatedReport(allResults, config);

        return allResults;
    }

    /**
     * Exporta resultados baseado na configuraã§ã£o
     */
    private void exportResults(List<PerformanceProfiler.ProcessingMetrics> results, TestConfiguration config) {
        System.out.println("\nðŸ“¤ EXPORTANDO RESULTADOS...");

        MetricsCollector collector = new MetricsCollector();
        MetricsCollector.SystemMetrics aggregated = collector.aggregateMetrics(results);

        TestDataExporter exporter = new TestDataExporter();

        if (config.generateLatex) {
            String latexFile = config.outputPrefix + "_tabela.tex";
            exporter.exportLatexTable(results, latexFile);
        }

        if (config.generateHTML) {
            String htmlFile = config.outputPrefix + "_relatorio.html";
            exporter.generateHTMLReport(results, aggregated, htmlFile);
        }

        // Sempre exporta CSV para dados brutos
        String csvFile = config.outputPrefix + "_dados.csv";
        exporter.exportToCSV(results, csvFile);

        System.out.println("âœ… Exportaã§ã£o concluã­da!");
    }

    /**
     * Gera relatã³rio consolidado personalizado
     */
    private void generateConsolidatedReport(List<PerformanceProfiler.ProcessingMetrics> results,
                                            TestConfiguration config) {
        System.out.println("\nðŸ“Š RELATã“RIO CONSOLIDADO");
        System.out.println("â•".repeat(40));

        List<PerformanceProfiler.ProcessingMetrics> successResults = results.stream()
                .filter(m -> m.success)
                .toList();

        if (successResults.isEmpty()) {
            System.out.println("âŒ Nenhum resultado bem-sucedido!");
            return;
        }

        // Estatã­sticas gerais
        double avgE2E = successResults.stream().mapToLong(m -> m.totalTime).average().orElse(0);
        double avgRRR = successResults.stream().mapToDouble(m -> m.noiseReductionRatio).average().orElse(0);
        double avgCR = successResults.stream().mapToDouble(m -> m.compressionRatio).average().orElse(0);
        double successRate = (double) successResults.size() / results.size();

        System.out.printf("ðŸŽ¯ Mã‰TRICAS PRINCIPAIS:\n");
        System.out.printf("- Taxa de sucesso: %.1f%% (%d/%d)\n",
                successRate * 100, successResults.size(), results.size());
        System.out.printf("- Tempo mã©dio end-to-end: %.1f ms\n", avgE2E);
        System.out.printf("- Reduã§ã£o mã©dia de ruã­do: %.1f%%\n", avgRRR * 100);
        System.out.printf("- Taxa de compressã£o: %.3f\n", avgCR);

        // Anã¡lise por site
        System.out.println("\nðŸ“ˆ ANãLISE POR SITE:");
        Map<String, List<PerformanceProfiler.ProcessingMetrics>> bySite = new HashMap<>();
        successResults.forEach(m -> bySite.computeIfAbsent(m.siteName, k -> new ArrayList<>()).add(m));

        bySite.forEach((site, siteResults) -> {
            double siteAvgTime = siteResults.stream().mapToLong(m -> m.totalTime).average().orElse(0);
            double siteAvgRRR = siteResults.stream().mapToDouble(m -> m.noiseReductionRatio).average().orElse(0);
            System.out.printf("  %s: %.1fms | RRR=%.3f | Testes=%d\n",
                    site, siteAvgTime, siteAvgRRR, siteResults.size());
        });

        // Dados para inserir no relatã³rio LaTeX
        System.out.println("\nðŸ“ DADOS PARA RELATã“RIO LATEX:");
        System.out.println("\\begin{itemize}");
        System.out.printf("\\item Tempo mã©dio de processamento: %.0f ms\n", avgE2E);
        System.out.printf("\\item Reduã§ã£o mã©dia de ruã­do: %.1f\\%%\n", avgRRR * 100);
        System.out.printf("\\item Taxa de compressã£o: %.3f\n", avgCR);
        System.out.printf("\\item Confiabilidade do sistema: %.1f\\%%\n", successRate * 100);
        System.out.println("\\end{itemize}");

        System.out.println("\nðŸŽ‰ Relatã³rio consolidado gerado com sucesso!");
    }

    /**
     * Configuraã§ãµes prã©-definidas para diferentes cenã¡rios
     */
    public static class PresetConfigurations {

        public static TestConfiguration quickTest() {
            TestConfiguration config = new TestConfiguration();
            config.testUrls = new String[]{
                    "https://pt.wikipedia.org/wiki/Java_(linguagem_de_programaã§ã£o)"
            };
            config.iterations = 1;
            config.outputPrefix = "quick_test";
            return config;
        }

        public static TestConfiguration relatorioFinal() {
            TestConfiguration config = new TestConfiguration();
            config.testUrls = new String[]{
                    "https://pt.wikipedia.org/wiki/Inteligãªncia_artificial",
                    "https://pt.wikipedia.org/wiki/Acessibilidade_web",
                    "https://brasilescola.uol.com.br/informatica/",
                    "https://g1.globo.com/tecnologia/",
                    "https://tecnologia.uol.com.br/"
            };
            config.iterations = 3;
            config.delayBetweenTests = 2000;
            config.outputPrefix = "relatorio_final_blindbrowser";
            config.generateLatex = true;
            config.generateHTML = true;
            return config;
        }

        public static TestConfiguration stressTest() {
            TestConfiguration config = new TestConfiguration();
            config.testUrls = new String[]{
                    "https://pt.wikipedia.org/wiki/Java_(linguagem_de_programaã§ã£o)",
                    "https://brasilescola.uol.com.br/informatica/"
            };
            config.iterations = 10;
            config.delayBetweenTests = 1000;
            config.outputPrefix = "stress_test";
            config.verboseOutput = false;
            return config;
        }
    }
}