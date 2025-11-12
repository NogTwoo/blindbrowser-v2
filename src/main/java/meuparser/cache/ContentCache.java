package meuparser.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Adicione estes imports no topo do ContentCache.java
import meuparser.cache.ContentComparator.ComparisonResult;

/**
 * Sistema de cache para conteúdo web extraído
 * Thread-safe e com limpeza automática de itens expirados
 */
public class ContentCache {
    private final Map<String, CachedContent> cache = new ConcurrentHashMap<>();
    private final int maxSize;
    private final long defaultTtlMillis;

    // Adicione estes campos na classe ContentCache
    private boolean enableComparison = false;
    private final List<ComparisonResult> comparisonHistory = new ArrayList<>();

    /**
     * Construtor com valores padrão
     */
    public ContentCache() {
        this.maxSize = 100;
        this.defaultTtlMillis = 30 * 60 * 1000; // 30 minutos
    }

    /**
     * Construtor com configurações customizadas
     * @param maxSize Tamanho máximo do cache
     * @param defaultTtlMillis TTL padrão em milissegundos
     */
    public ContentCache(int maxSize, long defaultTtlMillis) {
        this.maxSize = maxSize;
        this.defaultTtlMillis = defaultTtlMillis;
    }

    /**
     * Obtém conteúdo do cache se existir e não estiver expirado
     * @param url A URL do conteúdo
     * @return O conteúdo se disponível, vazio caso contrário
     */
    public Optional<String> get(String url) {
        if (url == null || url.trim().isEmpty()) {
            return Optional.empty();
        }

        // Normalizar URL para cache (remover fragmentos, etc.)
        String normalizedUrl = normalizeUrl(url);

        CachedContent cached = cache.get(normalizedUrl);
        if (cached != null) {
            if (!cached.isExpired()) {
                System.out.println("Cache HIT para URL: " + url);
                return Optional.of(cached.getContent());
            } else {
                // Remove item expirado
                cache.remove(normalizedUrl);
                System.out.println("Cache EXPIRED para URL: " + url);
            }
        }

        System.out.println("Cache MISS para URL: " + url);
        return Optional.empty();
    }

    /**
     * Armazena conteúdo no cache
     * @param url A URL do conteúdo
     * @param content O conteúdo a ser armazenado
     */
    public void put(String url, String content) {
        if (url == null || url.trim().isEmpty() || content == null) {
            return;
        }

        String normalizedUrl = normalizeUrl(url);

        // Verificar se precisa fazer limpeza
        if (cache.size() >= maxSize) {
            evictOldest();
        }

        // Remover itens expirados antes de adicionar novo
        cleanExpiredEntries();

        // Adicionar novo item
        CachedContent cachedContent = new CachedContent(content, System.currentTimeMillis(), defaultTtlMillis);
        cache.put(normalizedUrl, cachedContent);

        System.out.println("Cache STORE para URL: " + url + " (tamanho atual: " + cache.size() + ")");
    }

    /**
     * Remove o item mais antigo do cache
     */
    private void evictOldest() {
        if (cache.isEmpty()) {
            return;
        }

        // Encontrar o item mais antigo
        String oldestKey = null;
        long oldestTimestamp = Long.MAX_VALUE;

        for (Map.Entry<String, CachedContent> entry : cache.entrySet()) {
            if (entry.getValue().getTimestamp() < oldestTimestamp) {
                oldestTimestamp = entry.getValue().getTimestamp();
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
            System.out.println("Cache EVICT: removido item mais antigo");
        }
    }

    /**
     * Remove todos os itens expirados do cache
     */
    public void cleanExpiredEntries() {
        List<String> expiredKeys = new ArrayList<>();

        for (Map.Entry<String, CachedContent> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredKeys.add(entry.getKey());
            }
        }

        for (String key : expiredKeys) {
            cache.remove(key);
        }

        if (!expiredKeys.isEmpty()) {
            System.out.println("Cache CLEANUP: removidos " + expiredKeys.size() + " itens expirados");
        }
    }

    /**
     * Normaliza URL para uso como chave do cache
     * @param url URL original
     * @return URL normalizada
     */
    private String normalizeUrl(String url) {
        // Remover fragmentos (#)
        String normalized = url.split("#")[0];

        // Remover trailing slash se houver
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        // Converter para minúsculas para comparação case-insensitive
        return normalized.toLowerCase();
    }

    /**
     * Limpa todo o cache
     */
    public void clear() {
        cache.clear();
        System.out.println("Cache CLEAR: todos os itens removidos");
    }

    /**
     * Verifica se uma URL está no cache
     * @param url A URL a verificar
     * @return true se estiver em cache e não expirado
     */
    public boolean contains(String url) {
        return get(url).isPresent();
    }

    /**
     * Obtém estatísticas do cache
     * @return Objeto com estatísticas do cache
     */
    public CacheStats getStats() {
        cleanExpiredEntries(); // Limpar antes de calcular estatísticas

        return new CacheStats(
                cache.size(),
                maxSize,
                calculateTotalContentSize(),
                countExpiredEntries()
        );
    }

    /**
     * Calcula o tamanho total do conteúdo em cache
     */
    private long calculateTotalContentSize() {
        return cache.values().stream()
                .mapToLong(content -> content.getContent().length())
                .sum();
    }

    /**
     * Conta quantos itens estão expirados
     */
    private int countExpiredEntries() {
        return (int) cache.values().stream()
                .mapToInt(content -> content.isExpired() ? 1 : 0)
                .sum();
    }

    // Adicione estes métodos ã  classe ContentCache

    /**
     * Habilita/desabilita comparação entre cache e conteúdo fresco
     * @param enable true para habilitar comparação
     */
    public void setComparisonEnabled(boolean enable) {
        this.enableComparison = enable;
        System.out.println("Comparação de cache " + (enable ? "HABILITADA" : "DESABILITADA"));
    }

    /**
     * Obtém conteúdo com opção de comparar com conteúdo fresco
     * @param url A URL do conteúdo
     * @param freshContentProvider Função que fornece conteúdo fresco se necessário para comparação
     * @return O conteúdo se disponível, vazio caso contrário
     */
    public Optional<String> getWithComparison(String url, java.util.function.Supplier<String> freshContentProvider) {
        Optional<String> cachedResult = get(url);

        if (cachedResult.isPresent() && enableComparison && freshContentProvider != null) {
            // Executar comparação em background para não atrasar a resposta
            new Thread(() -> {
                try {
                    System.out.println("Iniciando comparação cache vs site para: " + url);
                    String freshContent = freshContentProvider.get();

                    ComparisonResult comparison = ContentComparator.compare(url, freshContent, cachedResult.get());
                    comparisonHistory.add(comparison);

                    // Manter apenas as últimas 10 comparações
                    if (comparisonHistory.size() > 10) {
                        comparisonHistory.remove(0);
                    }

                    String comparisonLog = ContentComparator.generateComparisonLog(comparison);
                    System.out.println(comparisonLog);

                    // Se há diferenças significativas, alertar
                    if (comparison.hasSignificantDifferences()) {
                        System.out.println("⚠ ALERTA: Diferenças significativas detectadas entre cache e conteúdo atual!");
                    }

                } catch (Exception e) {
                    System.err.println("Erro durante comparação: " + e.getMessage());
                }
            }, "ContentComparison-" + System.currentTimeMillis()).start();
        }

        return cachedResult;
    }

    /**
     * Obtém histórico de comparações realizadas
     * @return Lista com resultados das últimas comparações
     */
    public List<ComparisonResult> getComparisonHistory() {
        return new ArrayList<>(comparisonHistory);
    }

    /**
     * Gera relatório resumido das comparações
     * @return Relatório das comparações realizadas
     */
    public String getComparisonReport() {
        if (comparisonHistory.isEmpty()) {
            return "Nenhuma comparação realizada ainda.";
        }

        StringBuilder report = new StringBuilder();
        report.append("RELATÓRIO DE COMPARAÇÕES DE CACHE\n");
        report.append("=".repeat(50)).append("\n");
        report.append("Total de comparações: ").append(comparisonHistory.size()).append("\n");

        long identicalCount = comparisonHistory.stream()
                .mapToLong(result -> result.identical ? 1 : 0)
                .sum();

        long significantDifferencesCount = comparisonHistory.stream()
                .mapToLong(result -> result.hasSignificantDifferences() ? 1 : 0)
                .sum();

        report.append("Conteúdo idêntico: ").append(identicalCount).append("/").append(comparisonHistory.size()).append("\n");
        report.append("Diferenças significativas: ").append(significantDifferencesCount).append("/").append(comparisonHistory.size()).append("\n");

        double accuracy = (double) identicalCount / comparisonHistory.size() * 100;
        report.append("Precisão do cache: ").append(String.format("%.1f%%", accuracy)).append("\n");

        report.append("\núltimas comparações:\n");
        report.append("-".repeat(30)).append("\n");

        for (int i = comparisonHistory.size() - 1; i >= Math.max(0, comparisonHistory.size() - 5); i--) {
            ComparisonResult result = comparisonHistory.get(i);
            report.append(String.format("%s - %s - %s\n",
                    ContentComparator.DATE_FORMAT.format(result.timestamp),
                    result.identical ? "IGUAL" : "DIFERENTE",
                    result.url.length() > 50 ? result.url.substring(0, 47) + "..." : result.url));
        }

        return report.toString();
    }

    /**
     * Classe para estatísticas do cache
     */
    public static class CacheStats {
        private final int currentSize;
        private final int maxSize;
        private final long totalContentSize;
        private final int expiredEntries;

        public CacheStats(int currentSize, int maxSize, long totalContentSize, int expiredEntries) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.totalContentSize = totalContentSize;
            this.expiredEntries = expiredEntries;
        }

        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
        public long getTotalContentSize() { return totalContentSize; }
        public int getExpiredEntries() { return expiredEntries; }
        public double getUsagePercentage() { return (double) currentSize / maxSize * 100; }

        @Override
        public String toString() {
            return String.format("Cache Stats: %d/%d itens (%.1f%%), %d bytes, %d expirados",
                    currentSize, maxSize, getUsagePercentage(), totalContentSize, expiredEntries);
        }
    }
}