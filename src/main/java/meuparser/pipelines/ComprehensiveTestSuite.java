package meuparser.pipelines;

import java.util.*;

/**
 * Suite completa de testes para validaã§ã£o abrangente do sistema BlindBrowser
 * Inclui testes funcionais, de stress, precisã£o e usabilidade
 */
public class ComprehensiveTestSuite {

    // URLs organizadas por categoria para testes especã­ficos
    private static final Map<String, String[]> TEST_URLS_BY_CATEGORY = new HashMap<String, String[]>() {{
        put("EDUCACIONAL", new String[]{
                "https://pt.wikipedia.org/wiki/Inteligãªncia_artificial",
                "https://pt.wikipedia.org/wiki/Acessibilidade_web",
                "https://brasilescola.uol.com.br/informatica/",
                "https://brasilescola.uol.com.br/fisica/",
                "https://mundoeducacao.uol.com.br/informatica/"
        });

        put("NOTICIAS", new String[]{
                "https://g1.globo.com/tecnologia/",
                "https://tecnologia.uol.com.br/",
                "https://canaltech.com.br/",
                "https://olhardigital.com.br/"
        });

        put("GOVERNO", new String[]{
                "https://www.gov.br/pt-br",
                "https://www.planalto.gov.br/",
                "https://www.brasil.gov.br/"
        });

        put("INSTITUCIONAL", new String[]{
                "https://www.usp.br/",
                "https://www.unicamp.br/",
                "https://www.pucsp.br/"
        });
    }};

    public static void main(String[] args) {
        ComprehensiveTestSuite suite = new ComprehensiveTestSuite();
        suite.runComprehensiveTests();
    }

    /**
     * Executa suite completa de testes
     */
    public void runComprehensiveTests() {
        System.out.println("ðŸŽ¯ SUITE COMPLETA DE TESTES BLINDBROWSER");
        System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");
        System.out.println();

        // 1. Teste de Performance por Categoria
        runPerformanceTestsByCategory();

        // 2. Teste de Stress (mãºltiplas execuã§ãµes)
        runStressTest();

        // 3. Teste de Confiabilidade
        runReliabilityTest();

        // 4. Geraã§ã£o de Relatã³rio Final
        generateFinalReport();
    }

    /**
     * Testa performance por categoria de site
     */
    private void runPerformanceTestsByCategory() {
        System.out.println("ðŸ“Š 1. TESTE DE PERFORMANCE POR CATEGORIA");
        System.out.println("â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€");

        PerformanceProfiler profiler = new PerformanceProfiler();
        EnhancedMetricsCollector collector = new EnhancedMetricsCollector();
        Map<String, List<PerformanceProfiler.ProcessingMetrics>> resultsByCategory = new HashMap<>();

        for (Map.Entry<String, String[]> entry : TEST_URLS_BY_CATEGORY.entrySet()) {
            String category = entry.getKey();
            String[] urls = entry.getValue();

            System.out.printf("\nðŸ” Testando categoria: %s (%d URLs)\n", category, urls.length);

            List<PerformanceProfiler.ProcessingMetrics> categoryResults = profiler.runBenchmark(urls);
            resultsByCategory.put(category, categoryResults);

            // Anã¡lise por categoria
            MetricsCollector.SystemMetrics categoryMetrics = collector.aggregateMetrics(categoryResults);
            System.out.printf("âœ… %s - Sucesso: %.1f%% | Tempo mã©dio: %.1f ms | RRR mã©dio: %.3f\n",
                    category, categoryMetrics.successRate * 100,
                    categoryMetrics.avgEndToEndTime, categoryMetrics.avgNoiseReduction);
        }

        System.out.println("\n" + "â•".repeat(50));
    }

    /**
     * Teste de stress com mãºltiplas execuã§ãµes
     */
    private void runStressTest() {
        System.out.println("\nðŸš€ 2. TESTE DE STRESS (MãšLTIPLAS EXECUã‡ã•ES)");
        System.out.println("â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€");

        String[] stressUrls = {
                "https://pt.wikipedia.org/wiki/Java_(linguagem_de_programaã§ã£o)",
                "https://brasilescola.uol.com.br/informatica/"
        };

        int iterations = 5;
        PerformanceProfiler profiler = new PerformanceProfiler();
        List<PerformanceProfiler.ProcessingMetrics> allStressResults = new ArrayList<>();

        for (int i = 1; i <= iterations; i++) {
            System.out.printf("\nðŸ”„ Iteraã§ã£o %d/%d\n", i, iterations);

            List<PerformanceProfiler.ProcessingMetrics> iterationResults = profiler.runBenchmark(stressUrls);
            allStressResults.addAll(iterationResults);

            // Pausa breve entre iteraã§ãµes
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Anã¡lise de consistãªncia
        analyzeConsistency(allStressResults, iterations);
        System.out.println("\n" + "â•".repeat(50));
    }

    /**
     * Teste de confiabilidade com URLs problemã¡ticas
     */
    private void runReliabilityTest() {
        System.out.println("\nðŸ›¡ï¸ 3. TESTE DE CONFIABILIDADE");
        System.out.println("â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€");

        String[] challengingUrls = {
                "https://httpbin.org/delay/3",        // URL com delay
                "https://httpstat.us/404",            // URL 404
                "https://httpstat.us/500",            // URL 500
                "https://example.com/nonexistent",   // URL inexistente
                "https://pt.wikipedia.org/wiki/Test", // URL vã¡lida para controle
        };

        System.out.printf("ðŸ§ª Testando %d URLs desafiadoras...\n", challengingUrls.length);

        PerformanceProfiler profiler = new PerformanceProfiler();
        List<PerformanceProfiler.ProcessingMetrics> reliabilityResults = profiler.runBenchmark(challengingUrls);

        // Anã¡lise de robustez
        long successCount = reliabilityResults.stream().mapToLong(m -> m.success ? 1 : 0).sum();
        double robustnessRate = (double) successCount / reliabilityResults.size();

        System.out.printf("ðŸŽ¯ Taxa de robustez: %.1f%% (%d/%d sucessos)\n",
                robustnessRate * 100, (int)successCount, reliabilityResults.size());

        // Lista erros encontrados
        System.out.println("\nðŸ“ Anã¡lise de falhas:");
        reliabilityResults.stream()
                .filter(m -> !m.success)
                .forEach(m -> System.out.printf("âŒ %s: %s\n", m.url, m.errorMessage));

        System.out.println("\n" + "â•".repeat(50));
    }

    /**
     * Gera relatã³rio final consolidado
     */
    private void generateFinalReport() {
        System.out.println("\nðŸ“‹ 4. GERAã‡ãƒO DE RELATã“RIO FINAL");
        System.out.println("â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€");

        // Executa um teste final completo
        List<String> allUrls = new ArrayList<>();
        TEST_URLS_BY_CATEGORY.values().forEach(urls -> allUrls.addAll(Arrays.asList(urls)));

        PerformanceProfiler profiler = new PerformanceProfiler();
        List<PerformanceProfiler.ProcessingMetrics> finalResults =
                profiler.runBenchmark(allUrls.toArray(new String[0]));

        // Agrega mã©tricas finais
        EnhancedMetricsCollector collector = new EnhancedMetricsCollector();
        MetricsCollector.SystemMetrics finalMetrics = collector.aggregateMetrics(finalResults);


        // Exporta para todos os formatos
        TestDataExporter exporter = new TestDataExporter();
        exporter.exportAll(finalResults, finalMetrics);

        // Relatã³rio final no console
        collector.generateEnhancedReport(finalMetrics, finalResults);

        System.out.println("\nðŸŽ‰ TESTE COMPLETO FINALIZADO!");
        System.out.println("â•".repeat(50));
        System.out.println("ðŸ“Š RESUMO EXECUTIVO:");
        System.out.printf("- Total de URLs testadas: %d\n", finalResults.size());
        System.out.printf("- Taxa de sucesso geral: %.1f%%\n", finalMetrics.successRate * 100);
        System.out.printf("- Tempo mã©dio de processamento: %.1f ms\n", finalMetrics.avgEndToEndTime);
        System.out.printf("- Reduã§ã£o mã©dia de ruã­do: %.1f%%\n", finalMetrics.avgNoiseReduction * 100);
        System.out.printf("- Melhoria estimada para usuã¡rios: %.1f%% menos tempo de leitura\n",
                (1.0 - finalMetrics.avgCompressionRatio) * 100);
    }

    /**
     * Analisa consistãªncia dos resultados em mãºltiplas execuã§ãµes
     */
    private void analyzeConsistency(List<PerformanceProfiler.ProcessingMetrics> results, int iterations) {
        System.out.println("\nðŸ“ˆ ANãLISE DE CONSISTãŠNCIA:");

        // Agrupa por URL
        Map<String, List<PerformanceProfiler.ProcessingMetrics>> resultsByUrl = new HashMap<>();
        results.forEach(m ->
                resultsByUrl.computeIfAbsent(m.url, k -> new ArrayList<>()).add(m)
        );

        for (Map.Entry<String, List<PerformanceProfiler.ProcessingMetrics>> entry : resultsByUrl.entrySet()) {
            List<PerformanceProfiler.ProcessingMetrics> urlResults = entry.getValue();
            String siteName = urlResults.get(0).siteName;

            // Calcula variabilidade
            double[] times = urlResults.stream()
                    .filter(m -> m.success)
                    .mapToDouble(m -> m.totalTime)
                    .toArray();

            if (times.length > 1) {
                double avgTime = Arrays.stream(times).average().orElse(0);
                double variance = Arrays.stream(times)
                        .map(t -> Math.pow(t - avgTime, 2))
                        .average().orElse(0);
                double stdDev = Math.sqrt(variance);
                double cv = stdDev / avgTime; // Coeficiente de variaã§ã£o

                System.out.printf("  %s: Mã©dia=%.1fms, StdDev=%.1fms, CV=%.2f%%\n",
                        siteName, avgTime, stdDev, cv * 100);
            }
        }
    }
}