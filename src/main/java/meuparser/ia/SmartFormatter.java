package meuparser.ia;

import meuparser.ia.ContentClassifier.ContentCategory;

/**
 * Formata conteãºdo de forma inteligente para melhorar a experiãªncia de usuã¡rios
 * com deficiãªncia visual Reorganiza, estrutura e prioriza conteãºdo com base em
 * sua relevã¢ncia e tipo
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
     * Formata inteligentemente o conteãºdo para facilitar a leitura em
     * dispositivos Braille - VERSãƒO ADAPTATIVA
     */
    public String format(String content, String url) {
        System.out.println("DEBUG: SmartFormatter adaptativo - Iniciando formataã§ã£o");

        if (content == null || content.trim().isEmpty()) {
            System.err.println("DEBUG: SmartFormatter - Conteãºdo ã© nulo ou vazio");
            return "";
        }

        System.out.println("DEBUG: SmartFormatter - Tamanho do conteãºdo original: " + content.length() + " caracteres");

        StringBuilder formattedContent = new StringBuilder();

        try {
            // 1. Classifica o conteãºdo USANDO URL tambã©m
            System.out.println("DEBUG: SmartFormatter - Classificando conteãºdo com URL");
            ContentCategory category;
            if (url != null) {
                category = classifier.classifyContent(content); // âœ… USA URL
            } else {
                category = classifier.classifyContent(content); // âœ… SEM URL
            }
            // 1.âš ï¸ CORREã‡ãƒO CRãTICA: Gera resumo PRIMEIRO, depois formata o resumo
            String categoryDescription = classifier.getCategoryDescription(category);
            System.out.println("DEBUG: SmartFormatter - Categoria identificada: " + category + " (" + categoryDescription + ")");

            // 2. Gera resumo adaptativo se o conteãºdo for muito longo
            String contentToFormat = content;
            if (content.length() > 500) {
                System.out.println("DEBUG: SmartFormatter - Gerando resumo adaptativo ANTES da formataã§ã£o");
                contentToFormat = summarizer.generateSummaryWithCategory(content, category);
                System.out.println("DEBUG: SmartFormatter - Resumo gerado: " + contentToFormat.length() + " caracteres");
            }

            // 3. Adiciona prefixo da categoria
            if (!categoryDescription.isEmpty()) {
                formattedContent.append("[TIPO DE PãGINA: ").append(categoryDescription.trim()).append("]\n\n");
            }

            // 4. Extrai e adiciona palavras-chave (do conteãºdo resumido)
            System.out.println("DEBUG: SmartFormatter - Extraindo palavras-chave");
            String keywordSection = keywordExtractor.generateKeywordSection(contentToFormat);
            formattedContent.append(keywordSection);
            System.out.println("DEBUG: SmartFormatter - Palavras-chave extraã­das: " + keywordSection.length() + " caracteres");

            // 5. Adiciona o conteãºdo resumido (nã£o o original!)
            System.out.println("DEBUG: SmartFormatter - Aplicando formataã§ã£o especã­fica para categoria: " + category);
            String formattedBody = formatByCategory(contentToFormat, category); // âœ… USA RESUMO
            formattedContent.append(formattedBody);
            System.out.println("DEBUG: SmartFormatter - Corpo formatado: " + formattedBody.length() + " caracteres");

            // 6. Separa seã§ãµes longas
            System.out.println("DEBUG: SmartFormatter - Separando seã§ãµes longas");
            String result = separateLongSections(formattedContent.toString());

            System.out.println("DEBUG: SmartFormatter adaptativo - Formataã§ã£o concluã­da");
            System.out.printf("DEBUG: Original: %d chars â†’ Resumo: %d chars â†’ Final formatado: %d chars\n",
                    content.length(), contentToFormat.length(), result.length());

            return result;

        } catch (Exception e) {
            System.err.println("DEBUG: Erro no SmartFormatter: " + e.getMessage());
            e.printStackTrace();
            return content; // Fallback para conteãºdo original em caso de erro
        }
    }

    /**
     * Formata o conteãºdo de acordo com sua categoria - CORRIGIDO
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
     * Formata conteãºdo de artigos, notã­cias e blogs
     */
    private String formatArticleContent(String content) {
        // Destaca tã­tulos e seã§ãµes
        content = content.replaceAll("==\\s+TãTULO PRINCIPAL:\\s+(.+)", "[TãTULO] $1");
        content = content.replaceAll("==\\s+SUBTãTULO:\\s+(.+)", "[SUBTãTULO] $1");
        content = content.replaceAll("==\\s+SEã‡ãƒO:\\s+(.+)", "[SEã‡ãƒO] $1");

        // Enfatiza citaã§ãµes
        content = content.replaceAll("\"([^\"]*)\"", "[CITAã‡ãƒO] \"$1\"");

        return content;
    }

    /**
     * Formata conteãºdo de formulã¡rios
     */
    private String formatFormContent(String content) {
        // Destaca campos de formulã¡rio
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
     * Formata conteãºdo de navegaã§ã£o
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
     * Formata conteãºdo de comã©rcio eletrã´nico
     */
    private String formatECommerceContent(String content) {
        // Destaca preã§os, produtos e botãµes de compra
        content = content.replaceAll("(?i)(R\\$\\s*[\\d.,]+|\\$\\s*[\\d.,]+|\\d+[.,]\\d{2}\\s*â‚¬)", "[PREã‡O] $1");
        content = content.replaceAll("(?i)\\b(comprar|compre agora|adicionar ao carrinho|checkout)\\b", "[COMPRA] $1");

        return content;
    }

    /**
     * Formata conteãºdo educacional
     */
    private String formatEducationalContent(String content) {
        // Destaca instruã§ãµes, exemplos e exercã­cios
        content = content.replaceAll("(?i)exemplo[s]?\\s*:", "[EXEMPLO]");
        content = content.replaceAll("(?i)exercã­cio[s]?\\s*:", "[EXERCãCIO]");
        content = content.replaceAll("(?i)importante\\s*:", "[IMPORTANTE]");

        return content;
    }

    /**
     * Formata conteãºdo genã©rico
     */
    private String formatGenericContent(String content) {
        // Aplicar formataã§ã£o genã©rica para melhorar legibilidade
        return content;
    }

    /**
     * Separa seã§ãµes longas com divisores para melhorar a navegaã§ã£o no
     * dispositivo Braille
     */
    private String separateLongSections(String content) {
        StringBuilder result = new StringBuilder();
        String[] paragraphs = content.split("\n\\s*\n");

        for (int i = 0; i < paragraphs.length; i++) {
            result.append(paragraphs[i]);

            // Adiciona separador entre parã¡grafos longos
            if (i < paragraphs.length - 1 && paragraphs[i].length() > 200) {
                result.append("\n\n------ â€¢ ------\n\n");
            } else if (i < paragraphs.length - 1) {
                result.append("\n\n");
            }
        }

        return result.toString();
    }
}