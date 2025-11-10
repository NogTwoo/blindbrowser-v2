package meuparser.ia;

import java.util.*;

/**
 * Seletor de Sentenã§as ESSENCIAIS para Braille
 * Foco: QUALIDADE sobre QUANTIDADE
 * Meta: Mã¡xima informaã§ã£o em mã­nimo espaã§o
 */
public class EssentialSentenceSelector {

    /**
     * Seleciona as sentenã§as MAIS ESSENCIAIS
     * Objetivo: Transmitir a ESSãŠNCIA em pouquã­ssimas palavras
     */
    public String selectEssentialContent(
            String content,
            ContentClassifier.ContentCategory category,
            int maxCharacters) {

        // 1. Identifica a INFORMAã‡ãƒO PRINCIPAL (nã£o todas as palavras-chave)
        String mainTopic = extractMainTopic(content);

        // 2. Extrai fatos essenciais por categoria
        List<String> essentialFacts = extractEssentialFacts(content, category);

        // 3. Monta resumo ULTRA-CONCISO
        StringBuilder essential = new StringBuilder();

        // Adiciona tã³pico principal
        essential.append(mainTopic).append(". ");

        // Adiciona fatos essenciais atã© o limite
        for (String fact : essentialFacts) {
            if (essential.length() + fact.length() > maxCharacters) break;
            essential.append(fact).append(". ");
        }

        return essential.toString().trim();
    }

    /**
     * Extrai O ãšNICO tã³pico mais importante
     */
    private String extractMainTopic(String content) {
        // Pega a primeira sentenã§a significativa (geralmente o lead)
        String[] sentences = content.split("(?<=[.!?])\\s+");

        for (String sentence : sentences) {
            // Ignora sentenã§as muito curtas ou com metadados
            if (sentence.length() > 50 &&
                    !sentence.contains("[") &&
                    !sentence.toLowerCase().contains("menu")) {

                // Limpa e retorna
                return sentence.replaceAll("\\s+", " ").trim();
            }
        }

        return sentences.length > 0 ? sentences[0] : "";
    }

    /**
     * Extrai fatos ESSENCIAIS baseado na categoria
     */
    private List<String> extractEssentialFacts(String content,
                                               ContentClassifier.ContentCategory category) {
        List<String> facts = new ArrayList<>();

        switch (category) {
            case NEWS:
                // Para notã­cias: QUEM, O QUãŠ, QUANDO
                facts.add(extractWho(content));
                facts.add(extractWhat(content));
                facts.add(extractWhen(content));
                break;

            case EDUCATIONAL:
                // Para educacional: DEFINIã‡ãƒO, EXEMPLO
                facts.add(extractDefinition(content));
                facts.add(extractExample(content));
                break;

            case E_COMMERCE:
                // Para e-commerce: PRODUTO, PREã‡O
                facts.add(extractProduct(content));
                facts.add(extractPrice(content));
                break;

            default:
                // Genã©rico: primeiras 2 sentenã§as importantes
                facts.addAll(extractTopSentences(content, 2));
        }

        // Remove fatos vazios
        facts.removeIf(String::isEmpty);
        return facts;
    }

    // Mã©todos auxiliares de extraã§ã£o
    private String extractWho(String content) {
        // Busca por padrãµes de agentes (pessoas, organizaã§ãµes)
        if (content.contains("Governo")) return "Governo Federal";
        if (content.contains("Ministã©rio")) return "Ministã©rio";
        if (content.contains("Presidente")) return "Presidente";
        // etc...
        return "";
    }

    private String extractWhat(String content) {
        // Busca por aã§ãµes/verbos principais
        String[] verbs = {"anunciou", "declarou", "decidiu", "aprovou", "lanã§ou"};
        for (String verb : verbs) {
            int idx = content.indexOf(verb);
            if (idx > 0) {
                // Extrai a sentenã§a contendo o verbo
                int start = content.lastIndexOf('.', idx) + 1;
                int end = content.indexOf('.', idx);
                if (end > start) {
                    return content.substring(start, end).trim();
                }
            }
        }
        return "";
    }

    private String extractWhen(String content) {
        // Busca por marcadores temporais
        String[] timeMarkers = {"hoje", "ontem", "amanhã£", "segunda", "terã§a"};
        for (String marker : timeMarkers) {
            if (content.toLowerCase().contains(marker)) {
                return marker;
            }
        }
        return "";
    }

    private String extractDefinition(String content) {
        // Busca por padrãµes de definiã§ã£o
        String[] patterns = {" ã© ", " sã£o ", "consiste em", "define-se como"};
        for (String pattern : patterns) {
            int idx = content.indexOf(pattern);
            if (idx > 0) {
                int start = Math.max(0, idx - 50);
                int end = Math.min(content.length(), idx + 100);
                return content.substring(start, end).trim();
            }
        }
        return "";
    }

    private String extractExample(String content) {
        // Busca por exemplos
        if (content.contains("por exemplo")) {
            int idx = content.indexOf("por exemplo");
            int end = content.indexOf('.', idx);
            if (end > idx) {
                return content.substring(idx, end);
            }
        }
        return "";
    }

    private String extractProduct(String content) {
        // Extrai nome do produto (geralmente no inã­cio)
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.length() > 10 && line.length() < 100) {
                return line.trim();
            }
        }
        return "";
    }

    private String extractPrice(String content) {
        // Busca por valores monetã¡rios
        if (content.contains("R$")) {
            int idx = content.indexOf("R$");
            int end = Math.min(content.length(), idx + 20);
            return content.substring(idx, end).trim();
        }
        return "";
    }

    private List<String> extractTopSentences(String content, int count) {
        return Arrays.stream(content.split("(?<=[.!?])\\s+"))
                .filter(s -> s.length() > 30 && s.length() < 150)
                .limit(count)
                .collect(ArrayList::new,
                        (list, s) -> list.add(s.trim()),
                        ArrayList::addAll);
    }
}