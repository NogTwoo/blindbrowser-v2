package meuparser.ia.nlp;

import java.util.List;

/**
 * Interface comum para todos os sumarizadores de NLP
 * Permite trocar implementações sem afetar o código cliente
 */
public interface INLPSummarizer {

    /**
     * Gera resumo extrativo do conteúdo
     */
    String summarize(String content, int maxSentences);

    /**
     * Gera resumo com configuração padrão
     */
    default String summarize(String content) {
        return summarize(content, 3);
    }

    /**
     * Extrai sentenças mais importantes ordenadas por relevância
     */
    List<String> extractKeysentences(String content, int count);

    /**
     * Calcula score de relevância para uma sentença
     */
    double calculateSentenceRelevance(String sentence, String fullContext);

    /**
     * Obtém informações sobre o provedor NLP
     */
    NLPProviderInfo getProviderInfo();

    /**
     * Verifica se o provedor está pronto para uso
     */
    boolean isReady();

    /**
     * Inicializa recursos necessários (modelos, etc.)
     */
    void initialize() throws Exception;

    /**
     * Libera recursos
     */
    void cleanup();

    /**
     * Informações sobre o provedor
     */
    class NLPProviderInfo {
        public String name;
        public String version;
        public String description;
        public boolean supportsGPU;
        public boolean requiresInternet;
        public long estimatedMemoryUsage;

        public NLPProviderInfo(String name, String version, String description,
                               boolean supportsGPU, boolean requiresInternet, long memoryUsage) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.supportsGPU = supportsGPU;
            this.requiresInternet = requiresInternet;
            this.estimatedMemoryUsage = memoryUsage;
        }

        @Override
        public String toString() {
            return String.format("%s v%s (%s) [GPU: %s, Internet: %s, RAM: ~%dMB]",
                    name, version, description, supportsGPU ? "Sim" : "Não",
                    requiresInternet ? "Sim" : "Não", estimatedMemoryUsage / 1_000_000);
        }
    }
}