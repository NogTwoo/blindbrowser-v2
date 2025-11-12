package meuparser.pipelines;

import meuparser.*;
import meuparser.ia.*;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Profiler de performance para medição precisa dos tempos de processamento
 * Usado para gerar dados empíricos do relatório técnico
 * VERSÃO ADAPTATIVA - Integrado com sistema de compressão inteligente por categoria
 */
public class PerformanceProfiler {

    public static class ProcessingMetrics {
        public String url;
        public String siteName;
        public long parseTime;      // T_parse
        public long classTime;      // T_class
        public long sumTime;        // T_sum
        public long formatTime;     // T_fmt
        public long totalTime;      // T_e2e
        public int originalTokens;  // Tokens do conteúdo original
        public int summaryTokens;   // Tokens do RESUMO
        public int finalTokens;     // Tokens do conteúdo formatado final
        public int noiseRemoved;    // Baseado no resumo
        public double noiseReductionRatio; // RRR baseado no resumo
        public double compressionRatio;    // CR baseado no resumo
        public double expectedCompressionRatio; // CR esperado pela categoria
        public double compressionEfficiency;   // Eficiência da compressão
        public ContentClassifier.ContentCategory category; // Categoria identificada
        public boolean success;
        public String errorMessage;

        @Override
        public String toString() {
            return String.format("Site: %s [%s] | Parse: %dms | Class: %dms | Sum: %dms | Format: %dms | Total: %dms | RRR: %.3f (%.1f%% vs %.1f%% esperado)",
                    siteName, category != null ? category.name() : "UNKNOWN",
                    parseTime, classTime, sumTime, formatTime, totalTime,
                    noiseReductionRatio, (1.0 - compressionRatio) * 100,
                    (1.0 - expectedCompressionRatio) * 100);
        }
    }

    /**
     * Mede performance completa de processamento de uma URL
     * ADAPTATIVO: Usa compressão inteligente baseada na categoria do conteúdo
     */
    public ProcessingMetrics measureProcessing(String url) {
        ProcessingMetrics metrics = new ProcessingMetrics();
        metrics.url = url;
        metrics.siteName = extractSiteName(url);

        long startTime, endTime;
        String originalContent = null;
        String summary = null;
        String finalContent = null;
        ContentClassifier.ContentCategory category = null;

        try {
            // 1. MEDIR T_parse (Parsing HTML)
            startTime = System.nanoTime();
            MeuParser parser = new JsoupParser();
            parser.ExtraiTexto(url);
            endTime = System.nanoTime();

            if (parser.getErro()) {
                metrics.success = false;
                metrics.errorMessage = "Erro no parsing: " + url;
                return metrics;
            }

            metrics.parseTime = (endTime - startTime) / 1_000_000;
            originalContent = parser.getTexto();
            metrics.originalTokens = countTokens(originalContent);

            // 2. MEDIR T_class (Classificação)
            startTime = System.nanoTime();
            ContentClassifier classifier = new ContentClassifier();
            category = classifier.classifyContent(originalContent);
            metrics.category = category;
            endTime = System.nanoTime();
            metrics.classTime = (endTime - startTime) / 1_000_000;

            // 3. MEDIR T_sum (Sumarização ADAPTATIVA)
            startTime = System.nanoTime();
            ContentSummarizer summarizer = new ContentSummarizer();
            summary = summarizer.generateSummaryWithCategory(originalContent, category);
            endTime = System.nanoTime();
            metrics.sumTime = (endTime - startTime) / 1_000_000;

            // 4. MEDIR T_fmt (Formatação)
            startTime = System.nanoTime();
            SmartFormatter formatter = new SmartFormatter();
            finalContent = formatter.format(originalContent);
            endTime = System.nanoTime();
            metrics.formatTime = (endTime - startTime) / 1_000_000;

            // 5. CALCULAR MÉTRICAS ADAPTATIVAS
            metrics.originalTokens = countTokens(originalContent);
            metrics.summaryTokens = countTokens(summary);
            metrics.finalTokens = countTokens(finalContent);

            // CÁLCULO CORRETO - Baseado no resumo:
            if (metrics.originalTokens > 0) {
                metrics.compressionRatio = (double) metrics.summaryTokens / metrics.originalTokens;
                metrics.noiseReductionRatio = Math.max(0.0, 1.0 - metrics.compressionRatio);
                metrics.noiseRemoved = metrics.originalTokens - metrics.summaryTokens;

                // Métricas adaptativas
                metrics.expectedCompressionRatio = getExpectedCompressionRatio(category);
                metrics.compressionEfficiency = metrics.compressionRatio / metrics.expectedCompressionRatio;
            } else {
                metrics.compressionRatio = 0.0;
                metrics.noiseReductionRatio = 0.0;
                metrics.noiseRemoved = 0;
                metrics.expectedCompressionRatio = 0.0;
                metrics.compressionEfficiency = 0.0;
            }

            // Verifica se hã¡ crescimento anã´malo
            if (metrics.compressionRatio > 1.0) {
                System.out.printf("⚠ ATENÇÃO: Resumo maior que original! (%.3f) - %s\n",
                        metrics.compressionRatio, url);
            }

            // Anã¡lise de eficiência adaptativa
            if (metrics.compressionEfficiency < 0.8 || metrics.compressionEfficiency > 1.2) {
                System.out.printf("⚠ Eficiência de compressão fora do esperado: %.2f para categoria %s\n",
                        metrics.compressionEfficiency, category.name());
            }

            metrics.totalTime = metrics.parseTime + metrics.classTime + metrics.sumTime + metrics.formatTime;
            metrics.success = true;

            // Cleanup
            summarizer.cleanup();

        } catch (Exception e) {
            metrics.success = false;
            metrics.errorMessage = "Erro durante profiling: " + e.getMessage();
            e.printStackTrace();
        }

        return metrics;
    }

    /**
     * Executa benchmark adaptativo em múltiplas URLs
     */
    public List<ProcessingMetrics> runBenchmark(String[] urls) {
        List<ProcessingMetrics> results = new ArrayList<>();

        System.out.println("🚀INICIANDO BENCHMARK ADAPTATIVO DE PERFORMANCE");
        System.out.println("===============================================");

        for (int i = 0; i < urls.length; i++) {
            System.out.printf("📊 Testando [%d/%d]: %s\n", i+1, urls.length, urls[i]);

            ProcessingMetrics metrics = measureProcessing(urls[i]);
            results.add(metrics);

            if (metrics.success) {
                System.out.println("✅ " + metrics.toString());

                // Debug detalhado das métricas adaptativas
                System.out.printf("   📊 Original: %d tokens → Resumo: %d tokens → Final: %d tokens\n",
                        metrics.originalTokens, metrics.summaryTokens, metrics.finalTokens);
                System.out.printf("   📈 CR: %.3f (esperado: %.3f) | RRR: %.3f | Eficiência: %.2f\n",
                        metrics.compressionRatio, metrics.expectedCompressionRatio,
                        metrics.noiseReductionRatio, metrics.compressionEfficiency);

                // Avaliação qualitativa
                String quality = evaluateCompressionQuality(metrics.compressionEfficiency);
                System.out.printf("   🎯 Qualidade da compressão: %s\n", quality);
            } else {
                System.out.println("❌ " + metrics.errorMessage);
            }
            System.out.println();
        }

        // Relatório consolidado
        generateAdaptiveSummary(results);

        return results;
    }

    /**
     * Gera relatório consolidado do benchmark adaptativo
     */
    private void generateAdaptiveSummary(List<ProcessingMetrics> results) {
        List<ProcessingMetrics> successful = results.stream()
                .filter(m -> m.success)
                .collect(Collectors.toList());

        if (successful.isEmpty()) {
            System.out.println("❌ Nenhum teste bem-sucedido para gerar relatório");
            return;
        }

        System.out.println("📊 RELATÓRIO CONSOLIDADO - BENCHMARK ADAPTATIVO");
        System.out.println("===============================================");

        // Agrupa por categoria
        Map<ContentClassifier.ContentCategory, List<ProcessingMetrics>> byCategory =
                successful.stream().collect(
                        Collectors.groupingBy(m -> m.category != null ? m.category : ContentClassifier.ContentCategory.UNKNOWN)
                );

        for (Map.Entry<ContentClassifier.ContentCategory, List<ProcessingMetrics>> entry : byCategory.entrySet()) {
            ContentClassifier.ContentCategory category = entry.getKey();
            List<ProcessingMetrics> categoryResults = entry.getValue();

            double avgCompression = categoryResults.stream()
                    .mapToDouble(m -> m.compressionRatio)
                    .average().orElse(0.0);

            double avgEfficiency = categoryResults.stream()
                    .mapToDouble(m -> m.compressionEfficiency)
                    .average().orElse(0.0);

            double avgReduction = categoryResults.stream()
                    .mapToDouble(m -> m.noiseReductionRatio)
                    .average().orElse(0.0);

            System.out.printf("📁 %s (%d amostras):\n", category.name(), categoryResults.size());
            System.out.printf("   - Compressão média: %.3f (%.1f%% redução)\n", avgCompression, avgReduction * 100);
            System.out.printf("   - Eficiência média: %.2f\n", avgEfficiency);
            System.out.println();
        }

        // Métricas gerais
        double overallAvgReduction = successful.stream()
                .mapToDouble(m -> m.noiseReductionRatio)
                .average().orElse(0.0);

        double overallAvgEfficiency = successful.stream()
                .mapToDouble(m -> m.compressionEfficiency)
                .average().orElse(0.0);

        System.out.printf("🎯 MÉTRICAS GERAIS:\n");
        System.out.printf("   - Redução média geral: %.1f%%\n", overallAvgReduction * 100);
        System.out.printf("   - Eficiência média geral: %.2f\n", overallAvgEfficiency);
        System.out.printf("   - Taxa de sucesso: %.1f%% (%d/%d)\n",
                (double) successful.size() / results.size() * 100, successful.size(), results.size());
    }

    /**
     * Avalia qualitativamente a eficiência da compressão
     */
    private String evaluateCompressionQuality(double efficiency) {
        if (efficiency >= 0.9 && efficiency <= 1.1) {
            return "🎯 EXCELENTE (dentro do esperado)";
        } else if (efficiency >= 0.8 && efficiency <= 1.2) {
            return "✅ BOM (próximo ao esperado)";
        } else if (efficiency >= 0.7 && efficiency <= 1.3) {
            return "⚠ ACEITÁVEL (ligeiramente fora do esperado)";
        } else {
            return "❌ PROBLEMÁTICO (muito fora do esperado)";
        }
    }

    /**
     * Obtém ratio de compressão esperado para uma categoria
     */
    private double getExpectedCompressionRatio(ContentClassifier.ContentCategory category) {
        // Espelha os ratios do ContentSummarizer
        switch (category) {
            case NEWS: return 0.55;           // 45% redução
            case ARTICLE: return 0.65;        // 35% redução
            case EDUCATIONAL: return 0.70;    // 30% redução
            case E_COMMERCE: return 0.80;     // 20% redução
            case FORM: return 0.90;           // 10% redução
            case BLOG: return 0.60;           // 40% redução
            case NAVIGATION: return 0.50;     // 50% redução
            default: return 0.60;             // 40% redução
        }
    }

    /**
     * Método de debug adaptativo
     */
    public static void debugTokenCountAdaptive() {
        System.out.println("⚙ DEBUG: Sistema Adaptativo de Contagem de Tokens");
        System.out.println("================================================");

        // Testa diferentes tipos de conteúdo
        Map<ContentClassifier.ContentCategory, String> testContents = new HashMap<>();

        testContents.put(ContentClassifier.ContentCategory.NEWS,
                "BRASÍLIA - O Governo Federal anunciou hoje novas medidas econã´micas que devem impactar " +
                        "diretamente a vida dos brasileiros. Segundo o Ministério da Economia, as mudanças entram " +
                        "em vigor a partir do próximo mês. A decisão foi tomada após reunião com especialistas " +
                        "que apontaram a necessidade de ajustes na política fiscal. O ministro declarou que " +
                        "as medidas são essenciais para o controle da inflação. Os dados mostram crescimento " +
                        "de 3.2% no PIB do último trimestre, resultado considerado positivo pelos analistas.");

        testContents.put(ContentClassifier.ContentCategory.E_COMMERCE,
                "Smartphone Premium XYZ - Especificaçãµes técnicas: Tela OLED de 6.8 polegadas, " +
                        "processador octa-core de última geração, 256GB de armazenamento interno. Preço promocional " +
                        "por tempo limitado: R$ 2.499,00 em até 12x sem juros. Cã¢mera tripla de 108MP com " +
                        "estabilização óptica. Bateria de 5000mAh com carregamento rã¡pido. Produto com " +
                        "garantia de 2 anos e frete grã¡tis para todo o Brasil. Disponível nas cores preto, " +
                        "azul e dourado. Avaliação dos clientes: 4.8 estrelas de 5.");

        ContentSummarizer summarizer = new ContentSummarizer();

        for (Map.Entry<ContentClassifier.ContentCategory, String> entry : testContents.entrySet()) {
            ContentClassifier.ContentCategory category = entry.getKey();
            String content = entry.getValue();

            String summary = summarizer.generateSummaryWithCategory(content, category);

            int originalTokens = countTokensStatic(content);
            int summaryTokens = countTokensStatic(summary);

            double compressionRatio = (double) summaryTokens / originalTokens;
            double reductionRatio = 1.0 - compressionRatio;

            System.out.printf("📁 CATEGORIA: %s\n", category.name());
            System.out.printf("   Original: %d tokens\n", originalTokens);
            System.out.printf("   Resumo: %d tokens\n", summaryTokens);
            System.out.printf("   Compressão: %.3f | Redução: %.1f%%\n", compressionRatio, reductionRatio * 100);
            System.out.println();
        }

        summarizer.cleanup();
    }

    /**
     * Conta tokens de forma consistente
     */
    private int countTokens(String content) {
        return countTokensStatic(content);
    }

    private static int countTokensStatic(String content) {
        if (content == null || content.trim().isEmpty()) return 0;
        String normalizedContent = content.trim().replaceAll("\\s+", " ");
        return normalizedContent.split("\\s+").length;
    }

    private String extractSiteName(String url) {
        try {
            String domain = url.replaceAll("https?://", "").split("/")[0];
            if (domain.contains("wikipedia")) return "Wikipedia";
            if (domain.contains("g1")) return "G1";
            if (domain.contains("uol")) return "UOL";
            if (domain.contains("brasilescola")) return "Brasil Escola";
            if (domain.contains("mundoeducacao")) return "Mundo Educação";
            if (domain.contains("canaltech")) return "Canaltech";
            if (domain.contains("olhardigital")) return "Olhar Digital";
            if (domain.contains("gov.br")) return "Governo";
            if (domain.contains("planalto")) return "Planalto";
            if (domain.contains("usp.br")) return "USP";
            if (domain.contains("unicamp")) return "Unicamp";
            if (domain.contains("pucsp")) return "PUC-SP";
            return domain;
        } catch (Exception e) {
            return "Desconhecido";
        }
    }
}



/** ANTIGO(COM ALGUNS CALCULOS ERRADOS E SEM ADAPTAVIDADE)package meuparser.pipelines;

 import meuparser.*;
 import meuparser.ia.*;
 import java.util.List;
 import java.util.ArrayList;

 /**
 * Profiler de performance para medição precisa dos tempos de processamento
 * Usado para gerar dados empíricos do relatório técnico
 * VERSÃO CORRIGIDA - Calcula métricas baseadas no RESUMO, não no formato final
 */
/**public class PerformanceProfiler {

 public static class ProcessingMetrics {
 public String url;
 public String siteName;
 public long parseTime;      // T_parse
 public long classTime;      // T_class
 public long sumTime;        // T_sum
 public long formatTime;     // T_fmt
 public long totalTime;      // T_e2e
 public int originalTokens;  // Tokens do conteúdo original
 public int summaryTokens;   // Tokens do RESUMO (novo)
 public int finalTokens;     // Tokens do conteúdo formatado final
 public int noiseRemoved;    // Baseado no resumo
 public double noiseReductionRatio; // RRR baseado no resumo
 public double compressionRatio;    // CR baseado no resumo
 public boolean success;
 public String errorMessage;

 @Override
 public String toString() {
 return String.format("Site: %s | Parse: %dms | Class: %dms | Sum: %dms | Format: %dms | Total: %dms | RRR: %.3f",
 siteName, parseTime, classTime, sumTime, formatTime, totalTime, noiseReductionRatio);
 }
 }

 /**
  * Mede performance completa de processamento de uma URL
  * CORRIGIDO: Calcula métricas baseadas no resumo, não no formato final
 */
/** public ProcessingMetrics measureProcessing(String url) {
 ProcessingMetrics metrics = new ProcessingMetrics();
 metrics.url = url;
 metrics.siteName = extractSiteName(url);

 long startTime, endTime;
 String originalContent = null;
 String summary = null;
 String finalContent = null;

 try {
 // 1. MEDIR T_parse (Parsing HTML)
 startTime = System.nanoTime();
 MeuParser parser = new JsoupParser();
 parser.ExtraiTexto(url);
 endTime = System.nanoTime();

 if (parser.getErro()) {
 metrics.success = false;
 metrics.errorMessage = "Erro no parsing: " + url;
 return metrics;
 }

 metrics.parseTime = (endTime - startTime) / 1_000_000; // Converter para ms
 originalContent = parser.getTexto();
 metrics.originalTokens = countTokens(originalContent);

 // 2. MEDIR T_class (Classificação)
 startTime = System.nanoTime();
 ContentClassifier classifier = new ContentClassifier();
 ContentClassifier.ContentCategory category = classifier.classifyContent(originalContent);
 endTime = System.nanoTime();
 metrics.classTime = (endTime - startTime) / 1_000_000;

 // 3. MEDIR T_sum (Sumarização)
 startTime = System.nanoTime();
 ContentSummarizer summarizer = new ContentSummarizer();
 summary = summarizer.generateSummary(originalContent); // ✅Captura resumo
 endTime = System.nanoTime();
 metrics.sumTime = (endTime - startTime) / 1_000_000;

 // 4. MEDIR T_fmt (Formatação)
 startTime = System.nanoTime();
 SmartFormatter formatter = new SmartFormatter();
 finalContent = formatter.format(originalContent);
 endTime = System.nanoTime();
 metrics.formatTime = (endTime - startTime) / 1_000_000;

 // 5. CALCULAR MÉTRICAS DERIVADAS - VERSÃO CORRIGIDA
 // ANTES (problemã¡tico):
 // metrics.finalTokens = countTokens(finalContent); // ❌ Inclui formatação
 // DEPOIS (correto):
 metrics.summaryTokens = countTokens(summary);       // ✅ Apenas o resumo
 metrics.finalTokens = countTokens(finalContent);    // Para referência da formatação

 // Calcular métricas baseadas no RESUMO, não no formato final:
 metrics.originalTokens = countTokens(originalContent);
 metrics.noiseRemoved = metrics.originalTokens - metrics.summaryTokens;
 metrics.noiseReductionRatio = Math.max(0.0, 1.0 - metrics.compressionRatio);

 // Proteção contra divisão por zero e valores negativos
 if (metrics.originalTokens > 0) {
 metrics.compressionRatio = (double) metrics.summaryTokens / metrics.originalTokens;
 metrics.noiseReductionRatio = Math.max(0.0, 1.0 - metrics.compressionRatio);
 metrics.noiseRemoved = metrics.originalTokens - metrics.summaryTokens;
 }

 return metrics;
 } catch (Exception e) {
 throw new RuntimeException(e);
 }

 /**         // Verifica se hã¡ crescimento anã´malo (para debug)
 if (metrics.compressionRatio > 1.0) {
 System.out.printf("âš ï¸ ATENÇÃO: Resumo maior que original! (%.3f) - %s\n",
 metrics.compressionRatio, url);
 }

 metrics.totalTime = metrics.parseTime + metrics.classTime + metrics.sumTime + metrics.formatTime;
 metrics.success = true;

 // Cleanup
 summarizer.cleanup();

 } catch (Exception e) {
 metrics.success = false;
 metrics.errorMessage = "Erro durante profiling: " + e.getMessage();
 e.printStackTrace();
 }

 return metrics;
 }

 /**
 * Executa benchmark em múltiplas URLs
 */
/** public List<ProcessingMetrics> runBenchmark(String[] urls) {
 List<ProcessingMetrics> results = new ArrayList<>();

 System.out.println("🚀INICIANDO BENCHMARK DE PERFORMANCE");
 System.out.println("=======================================");

 for (int i = 0; i < urls.length; i++) {
 System.out.printf("📊 Testando [%d/%d]: %s\n", i+1, urls.length, urls[i]);

 ProcessingMetrics metrics = measureProcessing(urls[i]);
 results.add(metrics);

 if (metrics.success) {
 System.out.println("✅ " + metrics.toString());

 // Debug adicional das métricas
 System.out.printf("   📊 Original: %d tokens → Resumo: %d tokens → Final: %d tokens\n",
 metrics.originalTokens, metrics.summaryTokens, metrics.finalTokens);
 System.out.printf("   📈 CR: %.3f | RRR: %.3f (%.1f%% redução)\n",
 metrics.compressionRatio, metrics.noiseReductionRatio,
 metrics.noiseReductionRatio * 100);
 } else {
 System.out.println("❌ " + metrics.errorMessage);
 }
 System.out.println();
 }

 return results;
 }

 /**
 * Conta tokens de forma consistente, removendo espaços múltiplos
 */
/**  private int countTokens(String content) {
 if (content == null || content.trim().isEmpty()) return 0;

 // Normaliza espaços antes de contar
 String normalizedContent = content.trim().replaceAll("\\s+", " ");
 return normalizedContent.split("\\s+").length;
 }

 private String extractSiteName(String url) {
 try {
 String domain = url.replaceAll("https?://", "").split("/")[0];
 if (domain.contains("wikipedia")) return "Wikipedia";
 if (domain.contains("g1")) return "G1";
 if (domain.contains("uol")) return "UOL";
 if (domain.contains("brasilescola")) return "Brasil Escola";
 if (domain.contains("mundoeducacao")) return "Mundo Educação";
 if (domain.contains("canaltech")) return "Canaltech";
 if (domain.contains("olhardigital")) return "Olhar Digital";
 if (domain.contains("gov.br")) return "Governo";
 if (domain.contains("planalto")) return "Planalto";
 if (domain.contains("usp.br")) return "USP";
 if (domain.contains("unicamp")) return "Unicamp";
 if (domain.contains("pucsp")) return "PUC-SP";
 return domain;
 } catch (Exception e) {
 return "Desconhecido";
 }
 }
 }
 */

