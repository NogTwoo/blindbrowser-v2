package meuparser.ia.DualModeManager;

import meuparser.JsoupParser;
import meuparser.ia.ContentClassifier;
import meuparser.ia.ContentSummarizer;

import java.util.*;
import java.util.regex.Pattern;
import javax.swing.*;

/**
 * Gerenciador de Conteúdo Dual Mode - Resumido/Completo
 * Permite ao usuã¡rio alternar entre visã£o resumida e completa
 */
public class DualModeContentManager {

    // Conteúdos armazenados
    private String currentUrl;
    private String essentialContent;
    private String completeContent;
    private String originalContent;   // Conteúdo bruto original

    // Estado atual
    private ContentMode currentMode = ContentMode.ESSENTIAL;
    private Map<String, String> searchHistory = new HashMap<>();

    // Componentes do sistema
    private ContentSummarizer summarizer;
    private ContentClassifier classifier;
    private JsoupParser parser;

    // Estatísticas
    private long essentialLoadTime;
    private long completeLoadTime;

    public enum ContentMode {
        ESSENTIAL("Modo Resumido", "Leitura otimizada (~10 min)", "ðŸ“„"),
        COMPLETE("Modo Completo", "Conteúdo integral", "ðŸ“–");

        private final String name;
        private final String description;
        private final String icon;

        ContentMode(String name, String description, String icon) {
            this.name = name;
            this.description = description;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return icon + " " + name + " - " + description;
        }
    }

    public DualModeContentManager() {
        this.summarizer = new ContentSummarizer();
        this.classifier = new ContentClassifier();
        this.parser = new JsoupParser();
    }

    /**
     * Carrega e processa conteúdo de uma URL
     */
    public void loadContent(String url) throws Exception {
        this.currentUrl = url;

        System.out.println("ðŸ”„ Carregando conteúdo de: " + url);

        // 1. Extrai conteúdo com Jsoup
        long startTime = System.currentTimeMillis();
        parser.ExtraiTexto(url);

        if (parser.getErro()) {
            throw new Exception("Erro ao extrair conteúdo: " + url);
        }

        this.originalContent = parser.getTexto();
        completeLoadTime = System.currentTimeMillis() - startTime;

        // 2. Classifica o conteúdo
        ContentClassifier.ContentCategory category = classifier.classifyContent(originalContent);

        // 3. Gera versã£o resumida
        startTime = System.currentTimeMillis();
        this.essentialContent = summarizer.generateSummaryWithCategory(
                originalContent, category);
        essentialLoadTime = System.currentTimeMillis() - startTime;

        // 4. Limpa versã£o completa (preservando estrutura)
        this.completeContent = cleanButPreserve(originalContent);

        // 5. Inicia no modo ESSENTIAL
        this.currentMode = ContentMode.ESSENTIAL;

        // 6. Log de estatísticas
        logLoadingStats();
    }

    /**
     * Alterna entre modos e retorna o conteúdo
     */
    public ContentSwitchResult toggleMode() {
        ContentSwitchResult result = new ContentSwitchResult();

        if (currentMode == ContentMode.ESSENTIAL) {
            currentMode = ContentMode.COMPLETE;
            result.content = completeContent;
            result.message = "MODO COMPLETO ATIVADO - Conteúdo integral disponível";
            result.readingTime = completeContent.length() / 50; // 50 chars/min
        } else {
            currentMode = ContentMode.ESSENTIAL;
            result.content = essentialContent;
            result.message = "MODO RESUMIDO ATIVADO - Leitura otimizada";
            result.readingTime = essentialContent.length() / 50;
        }

        result.mode = currentMode;
        result.characterCount = result.content.length();

        return result;
    }

    /**
     * Busca termo no conteúdo completo com contexto
     */
    public SearchResult searchInComplete(String keyword) {
        if (completeContent == null || keyword == null || keyword.trim().isEmpty()) {
            return new SearchResult(false, "Busca inválida", null);
        }

        String keywordLower = keyword.toLowerCase();
        String contentLower = completeContent.toLowerCase();

        // Encontra todas as ocorrências
        List<SearchMatch> matches = new ArrayList<>();
        int index = 0;

        while ((index = contentLower.indexOf(keywordLower, index)) != -1) {
            // Extrai contexto (150 chars antes e depois)
            int contextStart = Math.max(0, index - 150);
            int contextEnd = Math.min(completeContent.length(), index + keyword.length() + 150);

            String context = completeContent.substring(contextStart, contextEnd);

            // Marca o termo encontrado
            String markedContext = context.replaceAll(
                    "(?i)" + Pattern.quote(keyword),
                    ">>> " + keyword.toUpperCase() + " <<<"
            );

            matches.add(new SearchMatch(index, markedContext, contextStart));
            index += keyword.length();
        }

        // Salva no histã³rico
        searchHistory.put(keyword, String.valueOf(matches.size()));

        if (matches.isEmpty()) {
            return new SearchResult(false,
                    "Termo '" + keyword + "' não encontrado", null);
        } else {
            String message = String.format(
                    "Encontradas %d ocorrências de '%s'", matches.size(), keyword);
            return new SearchResult(true, message, matches);
        }
    }

    /**
     * Navega para seção específica no conteúdo
     */
    public String navigateToSection(String sectionName) {
        if (completeContent == null) return "Conteúdo não carregado";

        // Procura por marcadores de seção
        String[] sectionMarkers = {
                "== " + sectionName,
                "## " + sectionName,
                sectionName.toUpperCase() + ":",
                "[" + sectionName + "]"
        };

        for (String marker : sectionMarkers) {
            int index = completeContent.indexOf(marker);
            if (index != -1) {
                // Retorna a seção (até prã³ximo marcador ou 1000 chars)
                int endIndex = Math.min(index + 1000, completeContent.length());

                // Procura prã³ximo marcador de seção
                String nextSection = completeContent.substring(index + marker.length());
                int nextMarker = nextSection.indexOf("==");
                if (nextMarker != -1) {
                    endIndex = Math.min(index + nextMarker, endIndex);
                }

                return completeContent.substring(index, endIndex);
            }
        }

        return "Seção '" + sectionName + "' nã£o encontrada";
    }

    /**
     * Obtém estatísticas do conteúdo
     */
    public ContentStats getContentStats() {
        ContentStats stats = new ContentStats();

        stats.essentialChars = essentialContent != null ? essentialContent.length() : 0;
        stats.completeChars = completeContent != null ? completeContent.length() : 0;
        stats.originalChars = originalContent != null ? originalContent.length() : 0;

        stats.essentialReadingTime = stats.essentialChars / 50; // minutos
        stats.completeReadingTime = stats.completeChars / 50;

        stats.reductionPercentage = stats.originalChars > 0 ?
                ((1.0 - (double)stats.essentialChars / stats.originalChars) * 100) : 0;

        stats.currentMode = currentMode;
        stats.url = currentUrl;

        // Conta sentenças
        stats.essentialSentences = countSentences(essentialContent);
        stats.completeSentences = countSentences(completeContent);

        return stats;
    }

    /**
     * Exporta conteúdo atual para arquivo
     */
    public void exportCurrentContent(String filename) throws Exception {
        String content = (currentMode == ContentMode.ESSENTIAL) ?
                essentialContent : completeContent;

        try (java.io.PrintWriter writer = new java.io.PrintWriter(filename)) {
            writer.println("=".repeat(60));
            writer.println("BLINDBROWSER - CONTEÚDO EXPORTADO");
            writer.println("=".repeat(60));
            writer.println("URL: " + currentUrl);
            writer.println("Modo: " + currentMode.toString());
            writer.println("Data: " + new Date());
            writer.println("=".repeat(60));
            writer.println();
            writer.println(content);
        }

        System.out.println("Aguarde… Conteúdo exportado para: " + filename);
    }

    // Métodos auxiliares

    private String cleanButPreserve(String content) {
        if (content == null) return "";

        return content
                .replaceAll("\\[MENU.*?\\]", "")
                .replaceAll("\\[NAVEGAÇÃO.*?\\]", "")
                .replaceAll("\\[PUBLICIDADE.*?\\]", "")
                .replaceAll("\\[RODAPÉ.*?\\]", "")
                .replaceAll("\\s{3,}", "\n\n") // Remove espaços excessivos
                .trim();
    }

    private void logLoadingStats() {
        System.out.printf("Aguarde… Conteúdo carregado:\n");
        System.out.printf("   - Original: %d chars\n", originalContent.length());
        System.out.printf("   - Resumido: %d chars (%.1f%% redução)\n",
                essentialContent.length(),
                (1.0 - (double)essentialContent.length()/originalContent.length()) * 100);
        System.out.printf("   - Tempo processamento: %dms (completo) + %dms (resumo)\n",
                completeLoadTime, essentialLoadTime);
        System.out.println("   - Pressione F2 para alternar entre modos");
    }

    private int countSentences(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.split("[.!?]+").length;
    }

    // Getters

    public ContentMode getCurrentMode() {
        return currentMode;
    }

    public String getCurrentContent() {
        return currentMode == ContentMode.ESSENTIAL ? essentialContent : completeContent;
    }

    public String getEssentialContent() {
        return essentialContent;
    }

    public String getCompleteContent() {
        return completeContent;
    }

    public Map<String, String> getSearchHistory() {
        return new HashMap<>(searchHistory);
    }

    // Classes auxiliares

    public static class ContentSwitchResult {
        public String content;
        public ContentMode mode;
        public String message;
        public int characterCount;
        public int readingTime; // minutos
    }

    public static class SearchResult {
        public boolean found;
        public String message;
        public List<SearchMatch> matches;

        public SearchResult(boolean found, String message, List<SearchMatch> matches) {
            this.found = found;
            this.message = message;
            this.matches = matches;
        }
    }

    public static class SearchMatch {
        public int position;
        public String context;
        public int contextStart;

        public SearchMatch(int position, String context, int contextStart) {
            this.position = position;
            this.context = context;
            this.contextStart = contextStart;
        }
    }

    public static class ContentStats {
        public int essentialChars;
        public int completeChars;
        public int originalChars;
        public int essentialReadingTime;
        public int completeReadingTime;
        public double reductionPercentage;
        public ContentMode currentMode;
        public String url;
        public int essentialSentences;
        public int completeSentences;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\uD83D\uDCCA ESTATÍSTICAS DO CONTEÚDO\n");
            sb.append("â”€".repeat(40)).append("\n");
            sb.append(String.format("URL: %s\n", url));
            sb.append(String.format("Modo atual: %s\n", currentMode.name));
            sb.append("\nRESUMIDO:\n");
            sb.append(String.format("  %d caracteres\n", essentialChars));
            sb.append(String.format("  %d sentenças\n", essentialSentences));
            sb.append(String.format("  ~%d min leitura Braille\n", essentialReadingTime));
            sb.append("\nCOMPLETO:\n");
            sb.append(String.format("   %d caracteres\n", completeChars));
            sb.append(String.format("   %d sentenças\n", completeSentences));
            sb.append(String.format("   ~%d min leitura Braille\n", completeReadingTime));
            sb.append(String.format("\nRedução: %.1f%%\n", reductionPercentage));

            return sb.toString();
        }
    }
}