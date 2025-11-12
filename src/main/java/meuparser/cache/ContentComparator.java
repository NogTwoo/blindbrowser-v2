package meuparser.cache;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;

/**
 * Ferramenta para comparar conteúdo do cache com conteúdo extraído diretamente
 */

public class ContentComparator {

    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Compara dois conteúdos e gera um relatório de comparação
     * @param url URL que foi processada
     * @param freshContent Conteúdo extraído diretamente do site
     * @param cachedContent Conteúdo obtido do cache
     * @return Relatório de comparação
     */
    public static ComparisonResult compare(String url, String freshContent, String cachedContent) {
        if (freshContent == null) freshContent = "";
        if (cachedContent == null) cachedContent = "";

        ComparisonResult result = new ComparisonResult();
        result.url = url;
        result.timestamp = new Date();
        result.freshContentLength = freshContent.length();
        result.cachedContentLength = cachedContent.length();
        result.identical = freshContent.equals(cachedContent);

        if (!result.identical) {
            result.differences = calculateDifferences(freshContent, cachedContent);
        }

        return result;
    }

    /**
     * Calcula diferenças básicas entre dois textos
     */
    private static String calculateDifferences(String text1, String text2) {
        StringBuilder diff = new StringBuilder();

        // Comparação de tamanho
        int sizeDiff = text1.length() - text2.length();
        diff.append("Diferença de tamanho: ").append(sizeDiff).append(" caracteres\n");

        // Comparação de linhas
        String[] lines1 = text1.split("\n");
        String[] lines2 = text2.split("\n");

        diff.append("Linhas no conteúdo original: ").append(lines1.length).append("\n");
        diff.append("Linhas no conteúdo em cache: ").append(lines2.length).append("\n");

        // Verificar se começam igual
        int maxLines = Math.min(lines1.length, lines2.length);
        int equalLinesFromStart = 0;

        for (int i = 0; i < maxLines; i++) {
            if (lines1[i].equals(lines2[i])) {
                equalLinesFromStart++;
            } else {
                break;
            }
        }

        diff.append("Linhas iguais do início: ").append(equalLinesFromStart).append("/").append(maxLines).append("\n");

        // Calcular similaridade percentual simples
        double similarity = calculateSimilarity(text1, text2);
        diff.append("Similaridade aproximada: ").append(String.format("%.1f%%", similarity * 100));

        return diff.toString();
    }

    /**
     * Calcula similaridade simples baseada em caracteres comuns
     */
    private static double calculateSimilarity(String text1, String text2) {
        if (text1.isEmpty() && text2.isEmpty()) return 1.0;
        if (text1.isEmpty() || text2.isEmpty()) return 0.0;

        int maxLength = Math.max(text1.length(), text2.length());
        int commonChars = 0;
        int minLength = Math.min(text1.length(), text2.length());

        for (int i = 0; i < minLength; i++) {
            if (text1.charAt(i) == text2.charAt(i)) {
                commonChars++;
            }
        }

        return (double) commonChars / maxLength;
    }

    /**
     * Gera um log de comparação formatado
     */
    public static String generateComparisonLog(ComparisonResult result) {
        StringBuilder log = new StringBuilder();

        log.append("\n").append("=".repeat(80)).append("\n");
        log.append("COMPARAÇÃO CACHE vs SITE DIRETO\n");
        log.append("=".repeat(80)).append("\n");
        log.append("URL: ").append(result.url).append("\n");
        log.append("Timestamp: ").append(DATE_FORMAT.format(result.timestamp)).append("\n");
        log.append("Conteúdo Idêntico: ").append(result.identical ? "SIM" : "NÃO").append("\n");
        log.append("Tamanho Conteúdo Direto: ").append(result.freshContentLength).append(" caracteres\n");
        log.append("Tamanho Conteúdo Cache: ").append(result.cachedContentLength).append(" caracteres\n");

        if (!result.identical && result.differences != null) {
            log.append("\nDETALHES DAS DIFERENÇAS:\n");
            log.append("-".repeat(40)).append("\n");
            log.append(result.differences).append("\n");
        }

        log.append("=".repeat(80)).append("\n");

        return log.toString();
    }

    /**
     * Classe para armazenar resultado da comparação
     */
    public static class ComparisonResult {
        public String url;
        public Date timestamp;
        public boolean identical;
        public int freshContentLength;
        public int cachedContentLength;
        public String differences;

        public boolean hasSignificantDifferences() {
            if (identical) return false;

            // Considera significativo se a diferença de tamanho for maior que 5%
            int sizeDiff = Math.abs(freshContentLength - cachedContentLength);
            double percentDiff = (double) sizeDiff / Math.max(freshContentLength, cachedContentLength);

            return percentDiff > 0.05; // 5%
        }
    }
}