package meuparser.pipelines;

import meuparser.ia.AIStats;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Coletor centralizado de mã©tricas do sistema BlindBrowser
 * Agrega dados de performance, precisã£o e qualidade de extraã§ã£o
 */
public class MetricsCollector {

    public static class SystemMetrics {
        // Mã©tricas de Performance
        public double avgParseTime;
        public double avgClassificationTime;
        public double avgSummarizationTime;
        public double avgFormattingTime;
        public double avgEndToEndTime;

        // Mã©tricas de Qualidade
        public double avgNoiseReduction;
        public double avgCompressionRatio;
        public double avgPrecision;
        public double avgRecall;
        public double avgF1Score;

        // Mã©tricas de Conteãºdo
        public double avgOriginalTokens;
        public double avgFinalTokens;
        public double avgKeywordCoverage;

        // Estatã­sticas Gerais
        public int totalTests;
        public int successfulTests;
        public double successRate;
        public List<String> errorMessages;

        public SystemMetrics() {
            this.errorMessages = new ArrayList<>();
        }
    }

    /**
     * Agrega mã©tricas de mãºltiplos testes de performance
     */
    public SystemMetrics aggregateMetrics(List<PerformanceProfiler.ProcessingMetrics> performanceData) {
        SystemMetrics metrics = new SystemMetrics();

        List<PerformanceProfiler.ProcessingMetrics> successfulTests = performanceData.stream()
                .filter(m -> m.success)
                .collect(Collectors.toList());

        metrics.totalTests = performanceData.size();
        metrics.successfulTests = successfulTests.size();
        metrics.successRate = (double) metrics.successfulTests / metrics.totalTests;

        // Coleta mensagens de erro
        metrics.errorMessages = performanceData.stream()
                .filter(m -> !m.success)
                .map(m -> m.errorMessage)
                .collect(Collectors.toList());

        if (successfulTests.isEmpty()) {
            return metrics; // Retorna mã©tricas vazias se nã£o hã¡ testes bem-sucedidos
        }

        // Calcula mã©dias de performance
        metrics.avgParseTime = successfulTests.stream()
                .mapToLong(m -> m.parseTime)
                .average()
                .orElse(0.0);

        metrics.avgClassificationTime = successfulTests.stream()
                .mapToLong(m -> m.classTime)
                .average()
                .orElse(0.0);

        metrics.avgSummarizationTime = successfulTests.stream()
                .mapToLong(m -> m.sumTime)
                .average()
                .orElse(0.0);

        metrics.avgFormattingTime = successfulTests.stream()
                .mapToLong(m -> m.formatTime)
                .average()
                .orElse(0.0);

        metrics.avgEndToEndTime = successfulTests.stream()
                .mapToLong(m -> m.totalTime)
                .average()
                .orElse(0.0);

        // Calcula mã©tricas de qualidade
        metrics.avgNoiseReduction = successfulTests.stream()
                .mapToDouble(m -> m.noiseReductionRatio)
                .average()
                .orElse(0.0);

        metrics.avgCompressionRatio = successfulTests.stream()
                .mapToDouble(m -> m.compressionRatio)
                .average()
                .orElse(0.0);

        // Mã©tricas de conteãºdo
        metrics.avgOriginalTokens = successfulTests.stream()
                .mapToInt(m -> m.originalTokens)
                .average()
                .orElse(0.0);

        metrics.avgFinalTokens = successfulTests.stream()
                .mapToInt(m -> m.finalTokens)
                .average()
                .orElse(0.0);

        return metrics;
    }

    /**
     * Calcula mã©tricas de precisã£o e recall baseadas em ground truth
     */
    public PrecisionRecallMetrics calculatePrecisionRecall(List<GroundTruthSample> groundTruth,
                                                           List<ExtractionResult> results) {
        PrecisionRecallMetrics metrics = new PrecisionRecallMetrics();

        if (groundTruth.size() != results.size()) {
            throw new IllegalArgumentException("Tamanhos de ground truth e resultados devem ser iguais");
        }

        double totalPrecision = 0.0;
        double totalRecall = 0.0;

        for (int i = 0; i < groundTruth.size(); i++) {
            GroundTruthSample truth = groundTruth.get(i);
            ExtractionResult result = results.get(i);

            Set<String> trueContent = tokenizeContent(truth.mainContent);
            Set<String> extractedContent = tokenizeContent(result.extractedContent);

            // Calcula TP, FP, FN
            Set<String> truePositives = new HashSet<>(extractedContent);
            truePositives.retainAll(trueContent);

            Set<String> falsePositives = new HashSet<>(extractedContent);
            falsePositives.removeAll(trueContent);

            Set<String> falseNegatives = new HashSet<>(trueContent);
            falseNegatives.removeAll(extractedContent);

            // Precisã£o e Recall para esta amostra
            double precision = truePositives.isEmpty() ? 0.0 :
                    (double) truePositives.size() / (truePositives.size() + falsePositives.size());
            double recall = truePositives.isEmpty() ? 0.0 :
                    (double) truePositives.size() / (truePositives.size() + falseNegatives.size());

            totalPrecision += precision;
            totalRecall += recall;
        }

        metrics.avgPrecision = totalPrecision / groundTruth.size();
        metrics.avgRecall = totalRecall / groundTruth.size();
        metrics.avgF1Score = 2 * (metrics.avgPrecision * metrics.avgRecall) /
                (metrics.avgPrecision + metrics.avgRecall);

        return metrics;
    }

    /**
     * Gera relatã³rio completo das mã©tricas
     */
    public void generateMetricsReport(SystemMetrics metrics) {
        System.out.println("\nðŸ“Š RELATã“RIO COMPLETO DE Mã‰TRICAS");
        System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");

        // Seã§ã£o de Performance
        System.out.println("\nðŸš€ Mã‰TRICAS DE PERFORMANCE:");
        System.out.printf("- Parse mã©dio: %.1f ms\n", metrics.avgParseTime);
        System.out.printf("- Classificaã§ã£o mã©dia: %.1f ms\n", metrics.avgClassificationTime);
        System.out.printf("- Sumarizaã§ã£o mã©dia: %.1f ms\n", metrics.avgSummarizationTime);
        System.out.printf("- Formataã§ã£o mã©dia: %.1f ms\n", metrics.avgFormattingTime);
        System.out.printf("- Tempo total mã©dio: %.1f ms\n", metrics.avgEndToEndTime);

        // Seã§ã£o de Qualidade
        System.out.println("\nâœ¨ Mã‰TRICAS DE QUALIDADE:");
        System.out.printf("- Reduã§ã£o de ruã­do: %.3f (%.1f%%)\n",
                metrics.avgNoiseReduction, metrics.avgNoiseReduction * 100);
        System.out.printf("- Taxa de compressã£o: %.3f\n", metrics.avgCompressionRatio);
        System.out.printf("- Precisã£o mã©dia: %.3f\n", metrics.avgPrecision);
        System.out.printf("- Recall mã©dio: %.3f\n", metrics.avgRecall);
        System.out.printf("- F1-Score mã©dio: %.3f\n", metrics.avgF1Score);

        // Seã§ã£o de Conteãºdo
        System.out.println("\nðŸ“ Mã‰TRICAS DE CONTEãšDO:");
        System.out.printf("- Tokens originais mã©dios: %.0f\n", metrics.avgOriginalTokens);
        System.out.printf("- Tokens finais mã©dios: %.0f\n", metrics.avgFinalTokens);
        System.out.printf("- Reduã§ã£o mã©dia de tokens: %.0f\n",
                metrics.avgOriginalTokens - metrics.avgFinalTokens);

        // Seã§ã£o de Confiabilidade
        System.out.println("\nðŸŽ¯ CONFIABILIDADE DO SISTEMA:");
        System.out.printf("- Total de testes: %d\n", metrics.totalTests);
        System.out.printf("- Testes bem-sucedidos: %d\n", metrics.successfulTests);
        System.out.printf("- Taxa de sucesso: %.1f%%\n", metrics.successRate * 100);

        if (!metrics.errorMessages.isEmpty()) {
            System.out.println("\nâŒ ERROS IDENTIFICADOS:");
            metrics.errorMessages.forEach(error -> System.out.println("- " + error));
        }
    }

    // Classes auxiliares
    public static class PrecisionRecallMetrics {
        public double avgPrecision;
        public double avgRecall;
        public double avgF1Score;
    }

    public static class GroundTruthSample {
        public String url;
        public String mainContent;
        public Set<String> irrelevantContent;

        public GroundTruthSample(String url, String mainContent, Set<String> irrelevantContent) {
            this.url = url;
            this.mainContent = mainContent;
            this.irrelevantContent = irrelevantContent;
        }
    }

    public static class ExtractionResult {
        public String url;
        public String extractedContent;

        public ExtractionResult(String url, String extractedContent) {
            this.url = url;
            this.extractedContent = extractedContent;
        }
    }

    private Set<String> tokenizeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(content.toLowerCase().split("\\W+"))
                .filter(token -> token.length() > 2) // Remove tokens muito pequenos
                .collect(Collectors.toSet());
    }
}