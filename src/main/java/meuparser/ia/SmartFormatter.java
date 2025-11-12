package meuparser.ia;

import meuparser.ia.ContentClassifier.ContentCategory;

/**
 * Formata conteúdo de forma inteligente para melhorar a experiência de usuários
 * com deficiência visual Reorganiza, estrutura e prioriza conteúdo com base em
 * sua relevância e tipo
 */
public class SmartFormatter {

    private final ContentClassifier classifier;
    private final ContentSummarizer summarizer;
    private final keywordExtractor keywordExtractor;

    public SmartFormatter() {
        this.classifier = new ContentClassifier();
        this.summarizer = new ContentSummarizer();
        this.keywordExtractor = new keywordExtractor();
    }

    public String format(String content) {
        return format(content, null);
    }


    /**
     * Formata inteligentemente o conteúdo para facilitar a leitura em
     * dispositivos Braille - VERSÃO ADAPTATIVA
     */
    public String format(String content, String url) {
        System.out.println("DEBUG: SmartFormatter adaptativo - Iniciando formatação");

        if (content == null || content.trim().isEmpty()) {
            System.err.println("DEBUG: SmartFormatter - Conteúdo é nulo ou vazio");
            return "";
        }

        System.out.println("DEBUG: SmartFormatter - Tamanho do conteúdo original: " + content.length() + " caracteres");

        StringBuilder formattedContent = new StringBuilder();

        try {
            // 1. Classifica o conteúdo USANDO URL também
            System.out.println("DEBUG: SmartFormatter - Classificando conteúdo com URL");
            ContentCategory category;
            if (url != null) {
                category = classifier.classifyContent(content); // âœ… USA URL
            } else {
                category = classifier.classifyContent(content); // âœ… SEM URL
            }
            // 1.âš ï¸ CORREÇÃO CRÍTICA: Gera resumo PRIMEIRO, depois formata o resumo
            String categoryDescription = classifier.getCategoryDescription(category);
            System.out.println("DEBUG: SmartFormatter - Categoria identificada: " + category + " (" + categoryDescription + ")");

            // 2. Gera resumo adaptativo se o conteúdo for muito longo
            String contentToFormat = content;
            if (content.length() > 500) {
                System.out.println("DEBUG: SmartFormatter - Gerando resumo adaptativo ANTES da formatação");
                contentToFormat = summarizer.generateSummaryWithCategory(content, category);
                System.out.println("DEBUG: SmartFormatter - Resumo gerado: " + contentToFormat.length() + " caracteres");
            }

            // 3. Adiciona prefixo da categoria
            if (!categoryDescription.isEmpty()) {
                formattedContent.append("[TIPO DE PÁGINA: ").append(categoryDescription.trim()).append("]\n\n");
            }

            // 4. Extrai e adiciona palavras-chave (do conteúdo resumido)
            System.out.println("DEBUG: SmartFormatter - Extraindo palavras-chave");
            String keywordSection = keywordExtractor.generateKeywordSection(contentToFormat);
            formattedContent.append(keywordSection);
            System.out.println("DEBUG: SmartFormatter - Palavras-chave extraídas: " + keywordSection.length() + " caracteres");

            // 5. Adiciona o conteúdo resumido (não o original!)
            System.out.println("DEBUG: SmartFormatter - Aplicando formatação específica para categoria: " + category);
            String formattedBody = formatByCategory(contentToFormat, category); // âœ… USA RESUMO
            formattedContent.append(formattedBody);
            System.out.println("DEBUG: SmartFormatter - Corpo formatado: " + formattedBody.length() + " caracteres");

            // 6. Separa seções longas
            System.out.println("DEBUG: SmartFormatter - Separando seções longas");
            String result = separateLongSections(formattedContent.toString());

            System.out.println("DEBUG: SmartFormatter adaptativo - Formatação concluída");
            System.out.printf("DEBUG: Original: %d chars → Resumo: %d chars → Final formatado: %d chars\n",
                    content.length(), contentToFormat.length(), result.length());

            return result;

        } catch (Exception e) {
            System.err.println("DEBUG: Erro no SmartFormatter: " + e.getMessage());
            e.printStackTrace();
            return content; // Fallback para conteúdo original em caso de erro
        }
    }

    /**
     * Formata o conteúdo de acordo com sua categoria - CORRIGIDO
     */
    private String formatByCategory(String content, ContentCategory category) {
        switch (category) {
            case ARTICLE:
            case NEWS:
            case BLOG:
                return formatArticleContent(content);
            case FORM:
                return formatFormContent(content);
            case NAVIGATION:
                return formatNavigationContent(content);
            case E_COMMERCE:
                return formatECommerceContent(content);
            case EDUCATIONAL:
                return formatEducationalContent(content);
            case UNKNOWN:
            default:
                return formatGenericContent(content);
        }
    }

    /**
     * Formata conteúdo de artigos, notícias e blogs
     */
    private String formatArticleContent(String content) {
        // Destaca títulos e seções
        content = content.replaceAll("==\\s+TÍTULO PRINCIPAL:\\s+(.+)", "[TÍTULO] $1");
        content = content.replaceAll("==\\s+SUBTÍTULO:\\s+(.+)", "[SUBTÍTULO] $1");
        content = content.replaceAll("==\\s+SEÇÃO:\\s+(.+)", "[SEÇÃO] $1");

        // Enfatiza citações
        content = content.replaceAll("\"([^\"]*)\"", "[CITAÇÃO] \"$1\"");

        return content;
    }

    /**
     * Formata conteúdo de formulários
     */
    private String formatFormContent(String content) {
        // Destaca campos de formulário
        content = content.replaceAll("\\[CAMPO DE ENTRADA([^\\]]*)\\]", "[CAMPO] $1");

        // Numera os campos de entrada
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        int fieldCount = 0;

        for (String line : lines) {
            if (line.contains("[CAMPO]")) {
                fieldCount++;
                line = line.replaceAll("\\[CAMPO\\]", "[CAMPO " + fieldCount + "]");
            }
            result.append(line).append("\n");
        }

        return result.toString();
    }

    /**
     * Formata conteúdo de navegação
     */
    private String formatNavigationContent(String content) {
        // Destaca e numera links
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        int linkCount = 0;

        for (String line : lines) {
            if (line.contains("[LINK:")) {
                linkCount++;
                line = line.replaceAll("\\[LINK:", "[LINK " + linkCount + ":");
            }
            result.append(line).append("\n");
        }

        return result.toString();
    }

    /**
     * Formata conteúdo de comércio eletrônico
     */
    private String formatECommerceContent(String content) {
        // Destaca preços, produtos e botões de compra
        content = content.replaceAll("(?i)(R\\$\\s*[\\d.,]+|\\$\\s*[\\d.,]+|\\d+[.,]\\d{2}\\s*â‚¬)", "[PREÇO] $1");
        content = content.replaceAll("(?i)\\b(comprar|compre agora|adicionar ao carrinho|checkout)\\b", "[COMPRA] $1");

        return content;
    }

    /**
     * Formata conteúdo educacional
     */
    private String formatEducationalContent(String content) {
        // Destaca instruções, exemplos e exercícios
        content = content.replaceAll("(?i)exemplo[s]?\\s*:", "[EXEMPLO]");
        content = content.replaceAll("(?i)exercício[s]?\\s*:", "[EXERCÍCIO]");
        content = content.replaceAll("(?i)importante\\s*:", "[IMPORTANTE]");

        return content;
    }

    /**
     * Formata conteúdo genérico
     */
    private String formatGenericContent(String content) {
        // Aplicar formatação genérica para melhorar legibilidade
        return content;
    }

    /**
     * Separa seções longas com divisores para melhorar a navegação no
     * dispositivo Braille
     */
    private String separateLongSections(String content) {
        StringBuilder result = new StringBuilder();
        String[] paragraphs = content.split("\n\\s*\n");

        for (int i = 0; i < paragraphs.length; i++) {
            result.append(paragraphs[i]);

            // Adiciona separador entre parágrafos longos
            if (i < paragraphs.length - 1 && paragraphs[i].length() > 200) {
                result.append("\n\n------ • ------\n\n");
            } else if (i < paragraphs.length - 1) {
                result.append("\n\n");
            }
        }

        return result.toString();
    }
}