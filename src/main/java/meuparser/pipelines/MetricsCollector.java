package meuparser.pipelines;

import meuparser.ia.AIStats;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Coletor centralizado de métricas do sistema BlindBrowser
 * Agrega dados de performance, precisão e qualidade de extração
 */
public class MetricsCollector {

    public static class SystemMetrics {
        // Métricas de Performance
        public double avgParseTime;
        public double avgClassificationTime;
        public double avgSummarizationTime;
        public double avgFormattingTime;
        public double avgEndToEndTime;

        // Métricas de Qualidade
        public double avgNoiseReduction;
        public double avgCompressionRatio;
        public double avgPrecision;
        public double avgRecall;
        public double avgF1Score;

        // Métricas de Conteãºdo
        public double avgOriginalTokens;
        public double avgFinalTokens;
        public double avgKeywordCoverage;

        // Estatísticas Gerais
        public int totalTests;
        public int successfulTests;
        public double successRate;
        public List<String> errorMessages;

        public SystemMetrics() {
            this.errorMessages = new ArrayList<>();
        }
    }

    /**
     * Agrega métricas de mãºltiplos testes de performance
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
            return metrics; // Retorna métricas vazias se não hã¡ testes bem-sucedidos
        }

        // Calcula médias de performance
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

        // Calcula métricas de qualidade
        metrics.avgNoiseReduction = successfulTests.stream()
                .mapToDouble(m -> m.noiseReductionRatio)
                .average()
                .orElse(0.0);

        metrics.avgCompressionRatio = successfulTests.stream()
                .mapToDouble(m -> m.compressionRatio)
                .average()
                .orElse(0.0);

        // Métricas de conteãºdo
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
     * Calcula métricas de precisão e recall baseadas em ground truth
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

            // Precisão e Recall para esta amostra
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
     * Gera relatã³rio completo das métricas
     */
    public void generateMetricsReport(SystemMetrics metrics) {
        System.out.println("\nðŸ“Š RELATã“RIO COMPLETO DE MÉTRICAS");
        System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");

        // Seção de Performance
        System.out.println("\nðŸš€ MÉTRICAS DE PERFORMANCE:");
        System.out.printf("- Parse médio: %.1f ms\n", metrics.avgParseTime);
        System.out.printf("- Classificação média: %.1f ms\n", metrics.avgClassificationTime);
        System.out.printf("- Sumarização média: %.1f ms\n", metrics.avgSummarizationTime);
        System.out.printf("- Formatação média: %.1f ms\n", metrics.avgFormattingTime);
        System.out.printf("- Tempo total médio: %.1f ms\n", metrics.avgEndToEndTime);

        // Seção de Qualidade
        System.out.println("\nâœ¨ MÉTRICAS DE QUALIDADE:");
        System.out.printf("- Redução de ruído: %.3f (%.1f%%)\n",
                metrics.avgNoiseReduction, metrics.avgNoiseReduction * 100);
        System.out.printf("- Taxa de compressão: %.3f\n", metrics.avgCompressionRatio);
        System.out.printf("- Precisão média: %.3f\n", metrics.avgPrecision);
        System.out.printf("- Recall médio: %.3f\n", metrics.avgRecall);
        System.out.printf("- F1-Score médio: %.3f\n", metrics.avgF1Score);

        // Seção de Conteãºdo
        System.out.println("\nðŸ“ MÉTRICAS DE CONTEÚDO:");
        System.out.printf("- Tokens originais médios: %.0f\n", metrics.avgOriginalTokens);
        System.out.printf("- Tokens finais médios: %.0f\n", metrics.avgFinalTokens);
        System.out.printf("- Redução média de tokens: %.0f\n",
                metrics.avgOriginalTokens - metrics.avgFinalTokens);

        // Seção de Confiabilidade
        System.out.println("\n🎯 CONFIABILIDADE DO SISTEMA:");
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