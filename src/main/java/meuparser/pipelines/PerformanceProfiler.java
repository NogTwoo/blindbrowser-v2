package meuparser.pipelines;

import meuparser.*;
import meuparser.ia.*;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Profiler de performance para mediã§ã£o precisa dos tempos de processamento
 * Usado para gerar dados empã­ricos do relatã³rio tã©cnico
 * VERSãƒO ADAPTATIVA - Integrado com sistema de compressã£o inteligente por categoria
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
        public int originalTokens;  // Tokens do conteãºdo original
        public int summaryTokens;   // Tokens do RESUMO
        public int finalTokens;     // Tokens do conteãºdo formatado final
        public int noiseRemoved;    // Baseado no resumo
        public double noiseReductionRatio; // RRR baseado no resumo
        public double compressionRatio;    // CR baseado no resumo
        public double expectedCompressionRatio; // CR esperado pela categoria
        public double compressionEfficiency;   // Eficiãªncia da compressã£o
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
     * ADAPTATIVO: Usa compressã£o inteligente baseada na categoria do conteãºdo
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

            // 2. MEDIR T_class (Classificaã§ã£o)
            startTime = System.nanoTime();
            ContentClassifier classifier = new ContentClassifier();
            category = classifier.classifyContent(originalContent);
            metrics.category = category;
            endTime = System.nanoTime();
            metrics.classTime = (endTime - startTime) / 1_000_000;

            // 3. MEDIR T_sum (Sumarizaã§ã£o ADAPTATIVA)
            startTime = System.nanoTime();
            ContentSummarizer summarizer = new ContentSummarizer();
            summary = summarizer.generateSummaryWithCategory(originalContent, category);
            endTime = System.nanoTime();
            metrics.sumTime = (endTime - startTime) / 1_000_000;

            // 4. MEDIR T_fmt (Formataã§ã£o)
            startTime = System.nanoTime();
            SmartFormatter formatter = new SmartFormatter();
            finalContent = formatter.format(originalContent);
            endTime = System.nanoTime();
            metrics.formatTime = (endTime - startTime) / 1_000_000;

            // 5. CALCULAR Mã‰TRICAS ADAPTATIVAS
            metrics.originalTokens = countTokens(originalContent);
            metrics.summaryTokens = countTokens(summary);
            metrics.finalTokens = countTokens(finalContent);

            // CãLCULO CORRETO - Baseado no resumo:
            if (metrics.originalTokens > 0) {
                metrics.compressionRatio = (double) metrics.summaryTokens / metrics.originalTokens;
                metrics.noiseReductionRatio = Math.max(0.0, 1.0 - metrics.compressionRatio);
                metrics.noiseRemoved = metrics.originalTokens - metrics.summaryTokens;

                // Mã©tricas adaptativas
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
                System.out.printf("âš ï¸ ATENã‡ãƒO: Resumo maior que original! (%.3f) - %s\n",
                        metrics.compressionRatio, url);
            }

            // Anã¡lise de eficiãªncia adaptativa
            if (metrics.compressionEfficiency < 0.8 || metrics.compressionEfficiency > 1.2) {
                System.out.printf("âš ï¸ Eficiãªncia de compressã£o fora do esperado: %.2f para categoria %s\n",
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
     * Executa benchmark adaptativo em mãºltiplas URLs
     */
    public List<ProcessingMetrics> runBenchmark(String[] urls) {
        List<ProcessingMetrics> results = new ArrayList<>();

        System.out.println("ðŸš€ INICIANDO BENCHMARK ADAPTATIVO DE PERFORMANCE");
        System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");

        for (int i = 0; i < urls.length; i++) {
            System.out.printf("ðŸ“Š Testando [%d/%d]: %s\n", i+1, urls.length, urls[i]);

            ProcessingMetrics metrics = measureProcessing(urls[i]);
            results.add(metrics);

            if (metrics.success) {
                System.out.println("âœ… " + metrics.toString());

                // Debug detalhado das mã©tricas adaptativas
                System.out.printf("   ðŸ“Š Original: %d tokens â†’ Resumo: %d tokens â†’ Final: %d tokens\n",
                        metrics.originalTokens, metrics.summaryTokens, metrics.finalTokens);
                System.out.printf("   ðŸ“ˆ CR: %.3f (esperado: %.3f) | RRR: %.3f | Eficiãªncia: %.2f\n",
                        metrics.compressionRatio, metrics.expectedCompressionRatio,
                        metrics.noiseReductionRatio, metrics.compressionEfficiency);

                // Avaliaã§ã£o qualitativa
                String quality = evaluateCompressionQuality(metrics.compressionEfficiency);
                System.out.printf("   ðŸŽ¯ Qualidade da compressã£o: %s\n", quality);
            } else {
                System.out.println("âŒ " + metrics.errorMessage);
            }
            System.out.println();
        }

        // Relatã³rio consolidado
        generateAdaptiveSummary(results);

        return results;
    }

    /**
     * Gera relatã³rio consolidado do benchmark adaptativo
     */
    private void generateAdaptiveSummary(List<ProcessingMetrics> results) {
        List<ProcessingMetrics> successful = results.stream()
                .filter(m -> m.success)
                .collect(Collectors.toList());

        if (successful.isEmpty()) {
            System.out.println("âŒ Nenhum teste bem-sucedido para gerar relatã³rio");
            return;
        }

        System.out.println("ðŸ“Š RELATã“RIO CONSOLIDADO - BENCHMARK ADAPTATIVO");
        System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");

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

            System.out.printf("ðŸ“‚ %s (%d amostras):\n", category.name(), categoryResults.size());
            System.out.printf("   - Compressã£o mã©dia: %.3f (%.1f%% reduã§ã£o)\n", avgCompression, avgReduction * 100);
            System.out.printf("   - Eficiãªncia mã©dia: %.2f\n", avgEfficiency);
            System.out.println();
        }

        // Mã©tricas gerais
        double overallAvgReduction = successful.stream()
                .mapToDouble(m -> m.noiseReductionRatio)
                .average().orElse(0.0);

        double overallAvgEfficiency = successful.stream()
                .mapToDouble(m -> m.compressionEfficiency)
                .average().orElse(0.0);

        System.out.printf("ðŸŽ¯ Mã‰TRICAS GERAIS:\n");
        System.out.printf("   - Reduã§ã£o mã©dia geral: %.1f%%\n", overallAvgReduction * 100);
        System.out.printf("   - Eficiãªncia mã©dia geral: %.2f\n", overallAvgEfficiency);
        System.out.printf("   - Taxa de sucesso: %.1f%% (%d/%d)\n",
                (double) successful.size() / results.size() * 100, successful.size(), results.size());
    }

    /**
     * Avalia qualitativamente a eficiãªncia da compressã£o
     */
    private String evaluateCompressionQuality(double efficiency) {
        if (efficiency >= 0.9 && efficiency <= 1.1) {
            return "ðŸŽ¯ EXCELENTE (dentro do esperado)";
        } else if (efficiency >= 0.8 && efficiency <= 1.2) {
            return "âœ… BOM (prã³ximo ao esperado)";
        } else if (efficiency >= 0.7 && efficiency <= 1.3) {
            return "âš ï¸ ACEITãVEL (ligeiramente fora do esperado)";
        } else {
            return "âŒ PROBLEMãTICO (muito fora do esperado)";
        }
    }

    /**
     * Obtã©m ratio de compressã£o esperado para uma categoria
     */
    private double getExpectedCompressionRatio(ContentClassifier.ContentCategory category) {
        // Espelha os ratios do ContentSummarizer
        switch (category) {
            case NEWS: return 0.55;           // 45% reduã§ã£o
            case ARTICLE: return 0.65;        // 35% reduã§ã£o
            case EDUCATIONAL: return 0.70;    // 30% reduã§ã£o
            case E_COMMERCE: return 0.80;     // 20% reduã§ã£o
            case FORM: return 0.90;           // 10% reduã§ã£o
            case BLOG: return 0.60;           // 40% reduã§ã£o
            case NAVIGATION: return 0.50;     // 50% reduã§ã£o
            default: return 0.60;             // 40% reduã§ã£o
        }
    }

    /**
     * Mã©todo de debug adaptativo
     */
    public static void debugTokenCountAdaptive() {
        System.out.println("ðŸ” DEBUG: Sistema Adaptativo de Contagem de Tokens");
        System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");

        // Testa diferentes tipos de conteãºdo
        Map<ContentClassifier.ContentCategory, String> testContents = new HashMap<>();

        testContents.put(ContentClassifier.ContentCategory.NEWS,
                "BRASãLIA - O Governo Federal anunciou hoje novas medidas econã´micas que devem impactar " +
                        "diretamente a vida dos brasileiros. Segundo o Ministã©rio da Economia, as mudanã§as entram " +
                        "em vigor a partir do prã³ximo mãªs. A decisã£o foi tomada apã³s reuniã£o com especialistas " +
                        "que apontaram a necessidade de ajustes na polã­tica fiscal. O ministro declarou que " +
                        "as medidas sã£o essenciais para o controle da inflaã§ã£o. Os dados mostram crescimento " +
                        "de 3.2% no PIB do ãºltimo trimestre, resultado considerado positivo pelos analistas.");

        testContents.put(ContentClassifier.ContentCategory.E_COMMERCE,
                "Smartphone Premium XYZ - Especificaã§ãµes tã©cnicas: Tela OLED de 6.8 polegadas, " +
                        "processador octa-core de ãºltima geraã§ã£o, 256GB de armazenamento interno. Preã§o promocional " +
                        "por tempo limitado: R$ 2.499,00 em atã© 12x sem juros. Cã¢mera tripla de 108MP com " +
                        "estabilizaã§ã£o ã³ptica. Bateria de 5000mAh com carregamento rã¡pido. Produto com " +
                        "garantia de 2 anos e frete grã¡tis para todo o Brasil. Disponã­vel nas cores preto, " +
                        "azul e dourado. Avaliaã§ã£o dos clientes: 4.8 estrelas de 5.");

        ContentSummarizer summarizer = new ContentSummarizer();

        for (Map.Entry<ContentClassifier.ContentCategory, String> entry : testContents.entrySet()) {
            ContentClassifier.ContentCategory category = entry.getKey();
            String content = entry.getValue();

            String summary = summarizer.generateSummaryWithCategory(content, category);

            int originalTokens = countTokensStatic(content);
            int summaryTokens = countTokensStatic(summary);

            double compressionRatio = (double) summaryTokens / originalTokens;
            double reductionRatio = 1.0 - compressionRatio;

            System.out.printf("ðŸ“‚ CATEGORIA: %s\n", category.name());
            System.out.printf("   Original: %d tokens\n", originalTokens);
            System.out.printf("   Resumo: %d tokens\n", summaryTokens);
            System.out.printf("   Compressã£o: %.3f | Reduã§ã£o: %.1f%%\n", compressionRatio, reductionRatio * 100);
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
            if (domain.contains("mundoeducacao")) return "Mundo Educaã§ã£o";
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
 * Profiler de performance para mediã§ã£o precisa dos tempos de processamento
 * Usado para gerar dados empã­ricos do relatã³rio tã©cnico
 * VERSãƒO CORRIGIDA - Calcula mã©tricas baseadas no RESUMO, nã£o no formato final
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
 public int originalTokens;  // Tokens do conteãºdo original
 public int summaryTokens;   // Tokens do RESUMO (novo)
 public int finalTokens;     // Tokens do conteãºdo formatado final
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
  * CORRIGIDO: Calcula mã©tricas baseadas no resumo, nã£o no formato final
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

 // 2. MEDIR T_class (Classificaã§ã£o)
 startTime = System.nanoTime();
 ContentClassifier classifier = new ContentClassifier();
 ContentClassifier.ContentCategory category = classifier.classifyContent(originalContent);
 endTime = System.nanoTime();
 metrics.classTime = (endTime - startTime) / 1_000_000;

 // 3. MEDIR T_sum (Sumarizaã§ã£o)
 startTime = System.nanoTime();
 ContentSummarizer summarizer = new ContentSummarizer();
 summary = summarizer.generateSummary(originalContent); // âœ…Captura resumo
 endTime = System.nanoTime();
 metrics.sumTime = (endTime - startTime) / 1_000_000;

 // 4. MEDIR T_fmt (Formataã§ã£o)
 startTime = System.nanoTime();
 SmartFormatter formatter = new SmartFormatter();
 finalContent = formatter.format(originalContent);
 endTime = System.nanoTime();
 metrics.formatTime = (endTime - startTime) / 1_000_000;

 // 5. CALCULAR Mã‰TRICAS DERIVADAS - VERSãƒO CORRIGIDA
 // ANTES (problemã¡tico):
 // metrics.finalTokens = countTokens(finalContent); // âŒ Inclui formataã§ã£o
 // DEPOIS (correto):
 metrics.summaryTokens = countTokens(summary);       // âœ… Apenas o resumo
 metrics.finalTokens = countTokens(finalContent);    // Para referãªncia da formataã§ã£o

 // Calcular mã©tricas baseadas no RESUMO, nã£o no formato final:
 metrics.originalTokens = countTokens(originalContent);
 metrics.noiseRemoved = metrics.originalTokens - metrics.summaryTokens;
 metrics.noiseReductionRatio = Math.max(0.0, 1.0 - metrics.compressionRatio);

 // Proteã§ã£o contra divisã£o por zero e valores negativos
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
 System.out.printf("âš ï¸ ATENã‡ãƒO: Resumo maior que original! (%.3f) - %s\n",
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
 * Executa benchmark em mãºltiplas URLs
 */
/** public List<ProcessingMetrics> runBenchmark(String[] urls) {
 List<ProcessingMetrics> results = new ArrayList<>();

 System.out.println("ðŸš€ INICIANDO BENCHMARK DE PERFORMANCE");
 System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");

 for (int i = 0; i < urls.length; i++) {
 System.out.printf("ðŸ“Š Testando [%d/%d]: %s\n", i+1, urls.length, urls[i]);

 ProcessingMetrics metrics = measureProcessing(urls[i]);
 results.add(metrics);

 if (metrics.success) {
 System.out.println("âœ… " + metrics.toString());

 // Debug adicional das mã©tricas
 System.out.printf("   ðŸ“Š Original: %d tokens â†’ Resumo: %d tokens â†’ Final: %d tokens\n",
 metrics.originalTokens, metrics.summaryTokens, metrics.finalTokens);
 System.out.printf("   ðŸ“ˆ CR: %.3f | RRR: %.3f (%.1f%% reduã§ã£o)\n",
 metrics.compressionRatio, metrics.noiseReductionRatio,
 metrics.noiseReductionRatio * 100);
 } else {
 System.out.println("âŒ " + metrics.errorMessage);
 }
 System.out.println();
 }

 return results;
 }

 /**
 * Conta tokens de forma consistente, removendo espaã§os mãºltiplos
 */
/**  private int countTokens(String content) {
 if (content == null || content.trim().isEmpty()) return 0;

 // Normaliza espaã§os antes de contar
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
 if (domain.contains("mundoeducacao")) return "Mundo Educaã§ã£o";
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

