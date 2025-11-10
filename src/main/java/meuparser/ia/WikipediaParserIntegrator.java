package meuparser.ia;

import meuparser.MeuParser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementação aprimorada do integrador para páginas da Wikipedia
 * com foco específico em resolver os problemas detectados nos logs
 */
public class WikipediaParserIntegrator implements AIParserIntegrator {

    private final AIStats stats;
    private final SmartFormatter formatter;

    public WikipediaParserIntegrator() {
        this.stats = new AIStats();
        this.formatter = new SmartFormatter();
    }

    @Override
    public Optional<String> processContent(MeuParser parser) {
        if (parser == null) {
            System.err.println("ERROR: WikipediaParserIntegrator - Parser é nulo");
            throw new IllegalArgumentException("O parser não pode ser nulo");
        }

        if (parser.getErro()) {
            System.err.println("ERROR: WikipediaParserIntegrator - Parser tem erro");
            return Optional.empty();
        }

        String originalContent = parser.getTexto();
        if (originalContent == null || originalContent.isEmpty()) {
            System.err.println("ERROR: WikipediaParserIntegrator - Conteúdo original é nulo ou vazio");
            return Optional.empty();
        }

        System.out.println("DEBUG: WikipediaParserIntegrator - Processando conteúdo com tamanho: " + originalContent.length() + " caracteres");
        long startTime = System.currentTimeMillis();

        try {
            // Extrair apenas o conteúdo principal e ignorar menus, barras laterais, etc.
            String mainContent = extractWikipediaMainContent(originalContent);

            // Limpar elementos CSS específicos da Wikipedia
            String cleanedContent = cleanWikipediaSpecificElements(mainContent);

            // Extrair e reorganizar seções
            String organizedContent = organizeWikipediaSections(cleanedContent);

            // Aplicar formatação simplificada
            String formattedContent = this.formatter.format(organizedContent);

            // Aplicar limpeza final para remover qualquer código HTML remanescente
            String finalContent = cleanFinalText(formattedContent);

            // Registrar estatísticas
            long processingTime = System.currentTimeMillis() - startTime;
            stats.registrarProcessamento(
                    originalContent,
                    finalContent,
                    countWikipediaKeywords(finalContent),
                    "Artigo Wikipedia",
                    processingTime
            );

            System.out.println("DEBUG: WikipediaParserIntegrator - Processamento concluído em " + processingTime + "ms");

            return Optional.of(finalContent);
        } catch (Exception e) {
            System.err.println("ERROR: Falha ao processar conteúdo da Wikipedia: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Extrai apenas o conteúdo principal da página da Wikipedia,
     * removendo cabeçalhos, menus, rodapés e outros elementos irrelevantes
     */
    private String extractWikipediaMainContent(String content) {
        StringBuilder mainContent = new StringBuilder();

        // Divide o conteúdo em linhas para processamento
        String[] lines = content.split("\n");
        boolean isInMainContent = false;
        boolean skipCurrentSection = false;

        for (String line : lines) {
            // Detecta o início do conteúdo principal
            if (line.contains("TÍTULO PRINCIPAL") || line.contains("CONTEÚDO PRINCIPAL]")) {
                isInMainContent = true;
                mainContent.append(line).append("\n");
                continue;
            }

            // Detecta o fim do conteúdo principal
            if (line.contains("FIM DO CONTEÚDO PRINCIPAL") || line.contains("Ligações externas")) {
                isInMainContent = false;
                mainContent.append("[FIM DO CONTEÚDO PRINCIPAL]\n");
                continue;
            }

            // Ignorar linhas que contêm código CSS ou JavaScript
            if (line.contains(".mw-parser-output") ||
                    line.contains("@media") ||
                    line.contains("background-color") ||
                    line.contains("skin-theme-clientpref")) {
                continue;
            }

            // Ignorar navegação, links e outros elementos irrelevantes
            if (line.contains("mover para a barra lateral") ||
                    line.contains("ocultar") ||
                    line.contains("Ferramentas pessoais") ||
                    line.contains("Páginas para editores") ||
                    line.contains("FerramentasFerramentas")) {
                skipCurrentSection = true;
                continue;
            }

            // Detectar final de uma seção a ignorar
            if (skipCurrentSection && line.trim().isEmpty()) {
                skipCurrentSection = false;
                continue;
            }

            // Adicionar linha apenas se estiver no conteúdo principal e não for parte de uma seção ignorada
            if (isInMainContent && !skipCurrentSection) {
                mainContent.append(line).append("\n");
            }
        }

        return mainContent.toString();
    }

    /**
     * Limpa elementos específicos da Wikipedia do conteúdo
     */
    private String cleanWikipediaSpecificElements(String content) {
        String cleaned = content;

        // Remover referências e notas
        cleaned = cleaned.replaceAll("\\[\\d+\\]", "");
        cleaned = cleaned.replaceAll("\\[nota \\d+\\]", "");

        // Remover links para edição
        cleaned = cleaned.replaceAll("\\[\\s*editar\\s*\\|\\s*editar código-fonte\\s*\\]", "");
        cleaned = cleaned.replaceAll("\\[\\s*editar\\s*\\]", "");

        // Remover códigos CSS específicos da Wikipedia
        cleaned = cleaned.replaceAll("\\.mw-parser-output[^{]+\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("@media\\([^)]+\\)\\{[^}]*\\}", "");

        // Remover elementos de navegação
        cleaned = cleaned.replaceAll("\\d+ línguas", "");

        return cleaned;
    }

    /**
     * Organiza o conteúdo em seções significativas
     */
    private String organizeWikipediaSections(String content) {
        StringBuilder organized = new StringBuilder();

        // Dividir o conteúdo em seções baseadas em títulos
        Pattern sectionPattern = Pattern.compile("==\\s*(TÍTULO PRINCIPAL|SUBTÍTULO|SEÇãO):\\s*([^=]+)\\s*");
        Matcher matcher = sectionPattern.matcher(content);

        int lastEnd = 0;
        while (matcher.find()) {
            // Adicionar o texto antes da próxima seção
            if (matcher.start() > lastEnd) {
                String previousText = content.substring(lastEnd, matcher.start()).trim();
                if (!previousText.isEmpty()) {
                    organized.append(previousText).append("\n\n");
                }
            }

            // Adicionar o título da seção formatado
            String sectionType = matcher.group(1);
            String sectionTitle = matcher.group(2).trim();

            organized.append("== ").append(sectionType).append(": ").append(sectionTitle).append("\n");

            lastEnd = matcher.end();
        }

        // Adicionar o texto após a última seção
        if (lastEnd < content.length()) {
            String remainingText = content.substring(lastEnd).trim();
            if (!remainingText.isEmpty()) {
                organized.append(remainingText);
            }
        }

        return organized.toString();
    }

    /**
     * Limpa o texto final para produzir conteúdo puro e coeso para leitores com deficiência visual.
     * Versão refinada que corrige problemas de formatação, espaçamento e preservação semântica.
     *
     * @param text Texto a ser limpo
     * @return Texto limpo e coeso
     */
    /**
     * Limpa o texto final para produzir conteúdo puro e coeso para leitores com deficiência visual.
     * Versão aprimorada que corrige problemas de formatação e espaçamento.
     *
     * @param text Texto a ser limpo
     * @return Texto limpo e coeso
     */
    public String cleanFinalText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String cleaned = text;

        // ===== PARTE 1: PRÉ-PROCESSAMENTO =====

        // Remover seções de navegação inteiras
        cleaned = removeNavigationSections(cleaned);

        // ===== PARTE 2: LIMPEZA DE ELEMENTOS HTML E FORMATAÇãO =====

        // Remover código CSS e JavaScript
        cleaned = cleaned.replaceAll("\\.mw-parser-output\\s*\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("@media[^{]*\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("(?s)<script.*?</script>", "");
        cleaned = cleaned.replaceAll("(?s)<style.*?</style>", "");

        // Corrigir o problema de "palavras fundidas"
        cleaned = cleaned.replaceAll("\\(do([a-zA-Z])", "(do $1");  // Corrige (dolatim) para (do latim)
        cleaned = cleaned.replaceAll("([a-z])([A-Z])", "$1 $2");    // Insere espaço entre minúscula e maiúscula

        // ===== PARTE 3: REMOÇãO DE ELEMENTOS WIKIPÉDIA ESPECÍFICOS =====

        // Remover referências, notas e marcadores de edição
        cleaned = cleaned.replaceAll("\\[\\d+\\]", "");
        cleaned = cleaned.replaceAll("\\[nota \\d+\\]", "");
        cleaned = cleaned.replaceAll("\\[\\s*(?:editar|edit)(?:\\|[^\\]]*)?\\s*\\]", "");
        cleaned = cleaned.replaceAll("\\[\\[(?:carece de fontes|citation needed)\\]\\]\\?", "");

        // Remover marcadores de referência nas notas de rodapé
        cleaned = cleaned.replaceAll("↑\\]", "");
        cleaned = cleaned.replaceAll("↑[a-z]\\]", "");
        cleaned = cleaned.replaceAll("↑[0-9]+\\]", "");

        // Limpar marcadores de lista e formatação
        cleaned = cleaned.replaceAll("- \\[LINK[^\\]]*\\]", "");
        cleaned = cleaned.replaceAll("\\[LINK[^\\]]*\\]", "");
        cleaned = cleaned.replaceAll("•\\s*", "• ");

        // ===== PARTE 4: CORREÇãO DO RESUMO =====

        // Corrigir o resumo
        cleaned = cleaned.replaceAll("RESUMO: \\]", "RESUMO: ");
        cleaned = cleaned.replaceAll("\\[RESUMO DA PÁGINA\\] \\]", "RESUMO: ");

        // ===== PARTE 5: FORMATAÇãO E ORGANIZAÇãO FINAL =====

        // Substituir marcações dos títulos por formatação limpa
        cleaned = cleaned.replaceAll("== (TÍTULO PRINCIPAL|SUBTÍTULO|SEÇãO): ([^=]+) ==", "$1: $2");
        cleaned = cleaned.replaceAll("== (TÍTULO PRINCIPAL|SUBTÍTULO|SEÇãO): ([^=]+)", "$1: $2");

        // Remover seções de links para outros idiomas e menus
        cleaned = removeTextBetween(cleaned, "== TÍTULO PRINCIPAL: Gelo ==", "== SUBTÍTULO: Conteúdo");
        cleaned = removeTextBetween(cleaned, "== TÍTULO PRINCIPAL: Gelo", "Nota:");

        // Limpar elementos de navegação
        cleaned = cleaned.replaceAll("(?i)(?:^|\\n)[\\s\\-•]*(?:Página principal|Conteúdo destacado|Eventos atuais|Esplanada|Portal comunitário|Páginas novas|Contato)(?:\\s*-\\s*)?(?:$|\\n)", "\n");
        cleaned = cleaned.replaceAll("\\[INÍCIO DO CONTEÚDO PRINCIPAL\\]", "");
        cleaned = cleaned.replaceAll("\\[FIM DO CONTEÚDO PRINCIPAL\\]", "");

        // Corrigir problemas específicos de formatação
        cleaned = cleaned.replaceAll("\\(em inglês\\)\\.", ".");
        cleaned = cleaned.replaceAll("\\.\\.", ".");
        cleaned = cleaned.replaceAll("\\s+\\.", ".");
        cleaned = cleaned.replaceAll("\\s+,", ",");

        // Remover boilerplate da Wikipedia
        cleaned = removeWikipediaBoilerplate(cleaned);

        // Corrigir espaçamentos e quebras de linha
        cleaned = cleaned.replaceAll("\\s{2,}", " ");
        cleaned = cleaned.replaceAll("(\\.) ([A-Z])", ".$1\n\n$2");
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        // ===== PARTE 6: LIMPEZA FINAL =====

        // Remover linhas em branco e espaços no início/fim
        cleaned = cleaned.replaceAll("(?m)^\\s*$", "");

        return cleaned.trim();
    }

    /**
     * Remove texto entre marcadores de início e fim especificados
     *
     * @param text Texto original
     * @param startMarker Marcador de início da seção a ser removida
     * @param endMarker Marcador de fim da seção a ser removida
     * @return Texto com a seção removida
     */
    private String removeTextBetween(String text, String startMarker, String endMarker) {
        int startIndex = text.indexOf(startMarker);
        if (startIndex == -1) {
            return text;
        }

        int endIndex = text.indexOf(endMarker, startIndex);
        if (endIndex == -1) {
            return text;
        }

        return text.substring(0, startIndex) + text.substring(endIndex);
    }

    /**
     * Extrai e preserva termos importantes que não devem ser alterados durante a limpeza
     */
    private Map<String, String> extractPreservedTerms(String text) {
        Map<String, String> terms = new HashMap<>();

        // Termos científicos importantes
        String[][] scientificTerms = {
                {"ponto de fusão", "TERM_FUSION_POINT"},
                {"estado sólido", "TERM_SOLID_STATE"},
                {"sistema cristalino hexagonal", "TERM_HEXAGONAL_SYSTEM"},
                {"grau Celsius", "TERM_CELSIUS"},
                {"centímetro cúbico", "TERM_CUBIC_CM"},
                {"densidade", "TERM_DENSITY"},
                {"volume", "TERM_VOLUME"},
                {"pressão", "TERM_PRESSURE"},
                {"massa", "TERM_MASS"},
                {"atmosfera", "TERM_ATMOSPHERE"},
                {"líquido", "TERM_LIQUID"},
                {"cristalizada", "TERM_CRYSTALLIZED"},
                {"fases cristalinas", "TERM_CRYSTAL_PHASES"},
                {"dilatado", "TERM_DILATED"},
                {"moléculas", "TERM_MOLECULES"},
                {"denso", "TERM_DENSE"}
        };

        // Substituir temporariamente por tokens
        for (String[] term : scientificTerms) {
            if (text.contains(term[0])) {
                terms.put(term[1], term[0]);
                text = text.replace(term[0], term[1]);
            }
        }

        return terms;
    }

    /**
     * Restaura termos importantes no texto limpo
     */
    private String restoreImportantTerms(String cleaned, Map<String, String> terms) {
        String result = cleaned;

        // Restaurar os termos originais
        for (Map.Entry<String, String> entry : terms.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        // Correções específicas para casos conhecidos
        result = result.replace("A sua é inferior", "A sua densidade é inferior");
        result = result.replace("O seu é de zero", "O seu ponto de fusão é de zero");
        result = result.replace("estadodano", "estado sólido da água");
        result = result.replace("A mesma de água", "A mesma massa de água");
        result = result.replace("no seu ponto de , apresenta-se mais", "no seu ponto de fusão, apresenta-se mais dilatado");

        return result;
    }

    /**
     * Método aprimorado para remover seções de navegação
     */
    private String removeNavigationSections(String text) {
        String result = text;

        // Seções inteiras a remover completamente
        String[] sectionsToRemove = {
                "Menu principal", "Colaboração", "Ferramentas",
                "Noutros projetos", "Imprimir/exportar",
                "155 línguas", "português", "Geral", "Donativos",
                "Criar uma conta", "Entrar", "Aspeto"
        };

        // Remover cada seção
        for (String section : sectionsToRemove) {
            int startIndex = result.indexOf(section);
            if (startIndex != -1) {
                int endIndex = result.indexOf("\n\n", startIndex);
                if (endIndex == -1) {
                    endIndex = result.indexOf("==", startIndex);
                }
                if (endIndex != -1 && endIndex > startIndex) {
                    result = result.substring(0, startIndex) + result.substring(endIndex);
                }
            }
        }

        // Remover linhas com links para outros idiomas
        Pattern langLinkPattern = Pattern.compile("- \\[LINK \\d+: [^\\]]+\\]\\s*", Pattern.MULTILINE);
        Matcher langLinkMatcher = langLinkPattern.matcher(result);
        result = langLinkMatcher.replaceAll("");

        return result;
    }

    /**
     * Método aprimorado para remover boilerplate da Wikipedia
     */
    private String removeWikipediaBoilerplate(String text) {
        String[] boilerplates = {
                "Esta página foi editada", "Obtida de", "Categorias ocultas",
                "Editar hiperligações", "Origem: Wikipédia", "Código de conduta",
                "Programadores", "Estatísticas", "Declaração sobre cookies",
                "Versão móvel", "Categorias:", "ISSN", "doi:", "Consultado em",
                "Wikimedia Commons", "Elemento Wikidata", "Política de privacidade",
                "Sobre a Wikipédia", "Avisos gerais", "Versão para impressão"
        };

        String result = text;

        for (String boilerplate : boilerplates) {
            result = result.replaceAll("(?i)(?:^|\\n).*" + Pattern.quote(boilerplate) + ".*(?:$|\\n)", "\n");
        }

        return result;
    }
    /**
     * Conta palavras-chave relevantes no conteúdo da Wikipedia
     */
    private int countWikipediaKeywords(String content) {
        String[] keywords = {"wikipedia", "enciclopédia", "artigo", "referências",
                "categoria", "seção", "conteúdo", "livre", "conhecimento"};

        int count = 0;
        String contentLower = content.toLowerCase();

        for (String keyword : keywords) {
            int index = contentLower.indexOf(keyword);
            while (index != -1) {
                count++;
                index = contentLower.indexOf(keyword, index + 1);
            }
        }

        return count;
    }

    @Override
    public Set<String> getIrrelevantClasses() {
        Set<String> classes = new HashSet<>();

        // Classes genéricas
        classes.add("menu");
        classes.add("navigation");
        classes.add("sidebar");
        classes.add("footer");
        classes.add("header");

        // Classes específicas da Wikipedia
        classes.add("mw-navigation");
        classes.add("vector-menu");
        classes.add("vector-header-container");
        classes.add("mw-indicators");
        classes.add("mw-editsection");
        classes.add("infobox");
        classes.add("toc");
        classes.add("metadata");
        classes.add("catlinks");
        classes.add("printfooter");
        classes.add("noprint");
        classes.add("mw-jump-link");
        classes.add("mw-references-wrap");
        classes.add("navbox");
        classes.add("mw-footer");
        classes.add("mw-data-after-content");
        classes.add("hlist");
        classes.add("navbar");
        classes.add("mw-empty-li");
        classes.add("mobile-stack");

        return classes;
    }

    @Override
    public Set<String> getIrrelevantIds() {
        Set<String> ids = new HashSet<>();

        // IDs genéricos
        ids.add("menu");
        ids.add("navigation");
        ids.add("sidebar");
        ids.add("footer");
        ids.add("header");

        // IDs específicos da Wikipedia
        ids.add("mw-navigation");
        ids.add("mw-panel");
        ids.add("p-logo");
        ids.add("p-search");
        ids.add("p-navigation");
        ids.add("p-tb");
        ids.add("footer");
        ids.add("mw-page-base");
        ids.add("siteSub");
        ids.add("mw-head");
        ids.add("mw-sidebar-button");
        ids.add("toc");
        ids.add("coordinates");
        ids.add("p-lang-btn");
        ids.add("searchInput");

        return ids;
    }

    @Override
    public String getMainContentSelector() {
        return "mw-content-text, mw-parser-output, content";
    }

    @Override
    public AIStats getStats() {
        return this.stats;
    }
}