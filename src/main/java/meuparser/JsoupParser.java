package meuparser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementação do MeuParser usando os métodos da bilbioteca Jsoup para extração de conteúdo
 */
public class JsoupParser extends MeuParser {

    private Document document;
    private StringBuilder textofinal;
    private boolean erro;
    private String currentUrl;
    private int elementosIgnorados;

    public JsoupParser() {
        super();
        this.textofinal = new StringBuilder();
        this.erro = false;
        this.elementosIgnorados = 0;
    }

    /**
     * Extrai texto de uma URL específica usando Jsoup
     * @param url URL da página a ser processada
     */
    @Override
    public void ExtraiTexto(String url) {
        this.currentUrl = url;
        this.textofinal = new StringBuilder();
        this.erro = false;
        this.elementosIgnorados = 0;

        try {
            // Conectar à URL e obter o documento
            document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();

            // Adicionar marcador de início de conteúdo principal
            textofinal.append("[INÍCIO DO CONTEÚDO PRINCIPAL]\n");

            // Extrair título da página
            String title = document.title();
            textofinal.append("TÍTULO PRINCIPAL: ").append(title).append("\n\n");

            // Tentar encontrar o conteúdo principal
            Elements mainContent = findMainContent();

            if (!mainContent.isEmpty()) {
                processMainContent(mainContent);
            } else {
                // Se não encontrar conteúdo principal, usar o body inteiro
                processMainContent(document.body().children());
            }

            // Adicionar marcador de fim de conteúdo principal
            textofinal.append("\n[FIM DO CONTEÚDO PRINCIPAL]");

        } catch (IOException e) {
            this.erro = true;
            this.textofinal.append("Erro ao conectar ao site: ").append(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Encontra os elementos que provavelmente contêm o conteúdo principal
     */
    private Elements findMainContent() {
        // Verificar se é Wikipedia - seletor específico
        if (currentUrl.contains("wikipedia.org")) {
            Elements wikipediaContent = document.select("#mw-content-text, .mw-parser-output");
            if (!wikipediaContent.isEmpty()) {
                return wikipediaContent;
            }
        }

        // Tentar seletores comuns para conteúdo principal
        String[] commonSelectors = {
                "article", "main", "#main-content", "#content", ".main-content",
                ".content", ".post", ".entry", ".entry-content",
                "[role=main]", "[itemprop=articleBody]"
        };

        for (String selector : commonSelectors) {
            Elements selected = document.select(selector);
            if (!selected.isEmpty()) {
                return selected;
            }
        }

        // Estratégia de fallback: verificar a densidade de texto
        return new Elements();
    }

    /**
     * Processa o conteúdo principal extraído
     */
    private void processMainContent(Elements content) {
        // Remover elementos irrelevantes antes do processamento
        removeIrrelevantElements(content);

        // Extrair texto formatado
        for (Element element : content) {
            processElement(element, 0);
        }
    }

    /**
     * Remove elementos irrelevantes do DOM antes de extrair o texto
     */
    private void removeIrrelevantElements(Elements content) {
        // Remover elementos de navegação, publicidade, etc.
        content.select("nav, header, footer, aside, .ad, .ads, .advertisement, .sidebar, .comments, .menu, .nav, script, style, meta").remove();

        // Remover elementos específicos irrelevantes
        for (String className : getIrrelevantClassNames()) {
            content.select("." + className).remove();
        }

        for (String id : getIrrelevantIds()) {
            content.select("#" + id).remove();
        }
    }

    /**
     * Processa um elemento e seus filhos recursivamente
     */
    private void processElement(Element element, int depth) {
        // Ignorar elementos ocultos
        if (isHiddenElement(element)) {
            elementosIgnorados++;
            return;
        }

        String tagName = element.tagName().toLowerCase();

        // Processar cabeçalhos
        if (tagName.matches("h[1-6]")) {
            String headerLevel = tagName.substring(1);
            int level = Integer.parseInt(headerLevel);

            if (level == 1) {
                textofinal.append("\n== TÍTULO PRINCIPAL: ").append(element.text()).append(" ==\n\n");
            } else if (level == 2) {
                textofinal.append("\n== SUBTÍTULO: ").append(element.text()).append(" ==\n\n");
            } else {
                textofinal.append("\n== SEÇãO: ").append(element.text()).append(" ==\n\n");
            }
            return;
        }

        // Processar parágrafos
        if (tagName.equals("p")) {
            String text = element.text().trim();
            if (!text.isEmpty()) {
                textofinal.append(text).append("\n\n");
            }
            return;
        }

        // Processar listas
        if (tagName.equals("ul") || tagName.equals("ol")) {
            for (Element li : element.select("li")) {
                textofinal.append("• ").append(li.text()).append("\n");
            }
            textofinal.append("\n");
            return;
        }

        // Processar tabelas de forma simplificada
        if (tagName.equals("table")) {
            textofinal.append("[TABELA]\n");
            Elements rows = element.select("tr");
            for (Element row : rows) {
                Elements cells = row.select("th, td");
                for (Element cell : cells) {
                    textofinal.append(cell.text()).append("\t");
                }
                textofinal.append("\n");
            }
            textofinal.append("[FIM TABELA]\n\n");
            return;
        }

        // Para elementos de texto simples, adicionar seu texto
        if (element.childrenSize() == 0) {
            String text = element.text().trim();
            if (!text.isEmpty()) {
                textofinal.append(text).append(" ");
            }
            return;
        }

        // Processar recursivamente os elementos filhos
        for (Element child : element.children()) {
            processElement(child, depth + 1);
        }
    }

    /**
     * Verifica se o elemento está oculto via CSS
     */
    private boolean isHiddenElement(Element element) {
        String style = element.attr("style").toLowerCase();
        String className = element.attr("class").toLowerCase();

        return style.contains("display: none") ||
                style.contains("visibility: hidden") ||
                className.contains("hidden") ||
                className.contains("hide") ||
                element.hasClass("sr-only");
    }

    /**
     * Retorna classes CSS consideradas irrelevantes
     */
    private Set<String> getIrrelevantClassNames() {
        Set<String> classes = new HashSet<>();

        // Classes genéricas
        classes.add("menu");
        classes.add("navigation");
        classes.add("sidebar");
        classes.add("footer");
        classes.add("header");
        classes.add("comments");
        classes.add("advertisement");

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

        return classes;
    }

    /**
     * Retorna IDs considerados irrelevantes
     */
    private Set<String> getIrrelevantIds() {
        Set<String> ids = new HashSet<>();

        // IDs genéricos
        ids.add("menu");
        ids.add("navigation");
        ids.add("sidebar");
        ids.add("footer");
        ids.add("header");
        ids.add("comments");

        // IDs específicos da Wikipedia
        ids.add("mw-navigation");
        ids.add("mw-panel");
        ids.add("p-logo");
        ids.add("p-search");
        ids.add("footer");
        ids.add("toc");

        return ids;
    }

    @Override
    public String getTexto() {
        return textofinal.toString();
    }

    @Override
    public void setTexto(String texto) {
        this.textofinal = new StringBuilder(texto);
    }

    @Override
    public boolean getErro() {
        return erro;
    }

    @Override
    public int getElementosIgnorados() {
        return elementosIgnorados;
    }

    @Override
    public String getCurrentUrl() {
        return this.currentUrl;
    }
}