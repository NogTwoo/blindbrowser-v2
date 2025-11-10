package meuparser.ia;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extrai palavras-chave de conteúdo HTML para auxiliar usuários com deficiência visual
 * a entender rapidamente os tópicos principais de uma página
 */
public class keywordExtractor {

    private static final int DEFAULT_KEYWORD_COUNT = 5;
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "a", "à", "ao", "aos", "aquela", "aquelas", "aquele", "aqueles", "aquilo", "as", "às", "até", "com", 
            "como", "da", "das", "de", "dela", "delas", "dele", "deles", "depois", "do", "dos", "e", "é", "ela", 
            "elas", "ele", "eles", "em", "entre", "era", "eram", "éramos", "essa", "essas", "esse", "esses", 
            "esta", "estas", "este", "estes", "eu", "foi", "fomos", "for", "foram", "forem", "formos", "fosse", 
            "fossem", "fôssemos", "há", "isso", "isto", "já", "lhe", "lhes", "mais", "mas", "me", "mesmo", 
            "meu", "meus", "minha", "minhas", "muito", "na", "não", "nas", "nem", "no", "nos", "nós", "nossa", 
            "nossas", "nosso", "nossos", "num", "numa", "o", "os", "ou", "para", "pela", "pelas", "pelo", 
            "pelos", "por", "qual", "quando", "que", "quem", "são", "se", "seja", "sejam", "sejamos", "sem", 
            "será", "serão", "serei", "seremos", "seria", "seriam", "seríamos", "seu", "seus", "só", "somos", 
            "sou", "sua", "suas", "também", "te", "tem", "temos", "tenho", "teu", "teus", "tinha", "tinham", 
            "tínhamos", "tua", "tuas", "um", "uma", "você", "vocês", "vos"
    ));

    /**
     * Extrai palavras-chave do conteúdo fornecido
     * 
     * @param content O conteúdo textual da página
     * @return Lista de palavras-chave extraídas
     */
    public List<String> extractKeywords(String content) {
        return extractKeywords(content, DEFAULT_KEYWORD_COUNT);
    }
    
    /**
     * Extrai um número específico de palavras-chave do conteúdo
     * 
     * @param content O conteúdo textual da página
     * @param count Número de palavras-chave a extrair
     * @return Lista de palavras-chave extraídas
     */
    public List<String> extractKeywords(String content, int count) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // Remove marcações e caracteres especiais
        content = cleanContent(content);
        
        // Divide o conteúdo em palavras
        String[] words = content.toLowerCase().split("\\s+");
        
        // Conta frequência das palavras
        Map<String, Integer> wordFrequency = new HashMap<>();
        for (String word : words) {
            if (isValidKeyword(word)) {
                wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
            }
        }
        
        // Ordena por frequência e seleciona as principais
        return wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Limpa o conteúdo removendo marcações e caracteres indesejados
     */
    private String cleanContent(String content) {
        // Remove marcações especiais como [LINK: texto]
        content = content.replaceAll("\\[.*?\\]", "");
        
        // Remove caracteres especiais e números
        content = content.replaceAll("[^\\p{L}\\s]", " ");
        
        // Remove espaços em excesso
        content = content.replaceAll("\\s+", " ").trim();
        
        return content;
    }
    
    /**
     * Verifica se uma palavra é um candidato válido para palavra-chave
     */
    private boolean isValidKeyword(String word) {
        // Verifica comprimento mínimo
        if (word == null || word.length() < 3) {
            return false;
        }
        
        // Verifica se não é uma stopword
        if (STOPWORDS.contains(word)) {
            return false;
        }
        
        // Verifica se contém apenas letras
        return word.matches("^\\p{L}+$");
    }
    
    /**
     * Gera uma string formatada com palavras-chave para incluir no início do documento
     * 
     * @param content O conteúdo a analisar
     * @return String formatada com palavras-chave
     */
    public String generateKeywordSection(String content) {
        List<String> keywords = extractKeywords(content);
        if (keywords.isEmpty()) {
            return "";
        }
        
        return "\n[PALAVRAS-CHAVE: " + String.join(", ", keywords) + "]\n\n";
    }
}