package meuparser;

import meuparser.ia.AIParserIntegrator;
import meuparser.ia.AIParserIntegratorFactory;
import meuparser.ia.WikipediaParserIntegrator;
import meuparser.ia.JsoupAIIntegrator;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;


/**
 * Metodo Parser(ATUALMENTE NãO ESTÁ SENDO UTILIZADO) de conteúdo HTML que extrai texto semântico de páginas web
 */
public class MeuParser extends HTMLEditorKit.ParserCallback {

    private StringBuilder textofinal;
    private boolean erro;
    private boolean dentroConteudoPrincipal;
    private boolean dentroElementoIrrelevante;
    private int profundidadeElemento;
    private int elementosIgnorados;
    private String currentUrl;
    public static final String ATTR_PLACEHOLDER = "placeholder";


    // Elementos considerados irrelevantes para usuários com deficiência visual
    private final Set<String> ELEMENTOS_IRRELEVANTES = new HashSet<>();

    // Classe para o parser delegator
    private static class ParserGetter extends HTMLEditorKit {
        @Override
        public HTMLEditorKit.Parser getParser() {
            return new ParserDelegator();
        }
    }

    /**
     * Construtor padrão
     */
    public MeuParser() {
        textofinal = new StringBuilder();
        erro = false;
        dentroConteudoPrincipal = false;
        dentroElementoIrrelevante = false;
        profundidadeElemento = 0;
        elementosIgnorados = 0;

        // Inicializa elementos irrelevantes
        ELEMENTOS_IRRELEVANTES.add("script");
        ELEMENTOS_IRRELEVANTES.add("style");
        ELEMENTOS_IRRELEVANTES.add("noscript");
        ELEMENTOS_IRRELEVANTES.add("iframe");
        ELEMENTOS_IRRELEVANTES.add("svg");
        ELEMENTOS_IRRELEVANTES.add("path");
        ELEMENTOS_IRRELEVANTES.add("meta");
        ELEMENTOS_IRRELEVANTES.add("head");
    }

    /**
     * Extrai texto de uma URL específica
     * @param url URL da página a ser processada
     */
    public void ExtraiTexto(String url) {
        // Salvar a URL atual
        this.currentUrl = url;
        System.out.println("DEBUG: MeuParser.ExtraiTexto - Iniciando extração para URL: " + url);

        // Limpar o texto final antes de começar
        textofinal = new StringBuilder();
        dentroConteudoPrincipal = false;
        dentroElementoIrrelevante = false;
        profundidadeElemento = 0;
        elementosIgnorados = 0;

        try {
            // Obter a página
            URL endereco = new URL(url);
            URLConnection conexao = endereco.openConnection();
            conexao.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");

            // Configurar o parser
            ParserGetter kit = new ParserGetter();
            HTMLEditorKit.Parser parser = kit.getParser();

            // Iniciar com marcador de conteúdo principal
            textofinal.append("[INÍCIO DO CONTEÚDO PRINCIPAL]");

            // Realizar o parsing
            System.out.println("DEBUG: MeuParser.ExtraiTexto - Iniciando parsing do HTML");
            InputStreamReader isr = new InputStreamReader(conexao.getInputStream(), "UTF-8");
            parser.parse(isr, this, true);
            isr.close();

            System.out.println("DEBUG: MeuParser.ExtraiTexto - Parsing concluído, tamanho inicial: " + textofinal.length() + " caracteres");

            // Determinar qual integrador usar com base na URL
            AIParserIntegrator integrator = AIParserIntegratorFactory.createIntegrator(this.currentUrl);
            System.out.println("DEBUG: MeuParser.ExtraiTexto - Usando integrador: " + integrator.getClass().getSimpleName());

            // Processar o conteúdo com o integrador específico
            Optional<String> processedContent = integrator.processContent(this);

            if (processedContent.isPresent()) {
                // Substituir o conteúdo original pelo processado
                String newContent = processedContent.get();
                textofinal = new StringBuilder(newContent);
                System.out.println("DEBUG: MeuParser.ExtraiTexto - Conteúdo processado pelo integrador específico");
            } else {
                System.out.println("DEBUG: MeuParser.ExtraiTexto - Integrador não processou o conteúdo, mantendo original");
            }

            erro = false;

        } catch (MalformedURLException ex) {
            System.err.println("ERRO: URL mal formada - " + ex.getMessage());
            erro = true;
        } catch (IOException ex) {
            System.err.println("ERRO: Problema de IO - " + ex.getMessage());
            erro = true;
        } catch (Exception ex) {
            System.err.println("ERRO: Exceção geral - " + ex.getMessage());
            ex.printStackTrace();
            erro = true;
        }
    }

    /**
     * Processa o início de uma tag HTML
     */
    @Override
    public void handleStartTag(HTML.Tag tag, MutableAttributeSet atributos, int pos) {
        profundidadeElemento++;

        String tagName = tag.toString().toLowerCase();

        // Verifica se é um elemento irrelevante
        if (ELEMENTOS_IRRELEVANTES.contains(tagName)) {
            dentroElementoIrrelevante = true;
            elementosIgnorados++;
            return;
        }

        // Verifica se estamos numa div de conteúdo principal
        if (tag == HTML.Tag.DIV && atributos.containsAttribute("class", "content")) {
            dentroConteudoPrincipal = true;
        }

        // Processa cabeçalhos
        if (tag == HTML.Tag.H1) {
            textofinal.append("\n\n== TÍTULO PRINCIPAL: ");
        } else if (tag == HTML.Tag.H2) {
            textofinal.append("\n\n== SUBTÍTULO: ");
        } else if (tag == HTML.Tag.H3 ||
                tag == HTML.Tag.H4 ||
                tag == HTML.Tag.H5 ||
                tag == HTML.Tag.H6) {
            textofinal.append("\n\n== SEÇãO: ");
        }

        // Processa listas
        else if (tag == HTML.Tag.LI) {
            textofinal.append("\n- ");
        }

        // Processa links
        else if (tag == HTML.Tag.A) {
            String href = (String) atributos.getAttribute(HTML.Attribute.HREF);
            if (href != null && !href.startsWith("javascript:") && !href.startsWith("#")) {
                textofinal.append("[LINK: ");
            }
        }

        // Processa entrada de formulários
        else if (tag == HTML.Tag.INPUT ||
                tag == HTML.Tag.TEXTAREA ||
                tag == HTML.Tag.SELECT) {
            String name = (String) atributos.getAttribute(HTML.Attribute.NAME);
            String type = (String) atributos.getAttribute(HTML.Attribute.TYPE);
            String placeholder = (String) atributos.getAttribute(ATTR_PLACEHOLDER);

            textofinal.append("\n[CAMPO DE ENTRADA");
            if (name != null) textofinal.append(" nome=\"").append(name).append("\"");
            if (type != null) textofinal.append(" tipo=\"").append(type).append("\"");
            if (placeholder != null) textofinal.append(" dica=\"").append(placeholder).append("\"");
            textofinal.append("]\n");
        }
    }

    /**
     * Processa o fim de uma tag HTML
     */
    @Override
    public void handleEndTag(javax.swing.text.html.HTML.Tag tag, int pos) {
        profundidadeElemento--;

        String tagName = tag.toString().toLowerCase();

        // Verifica se saímos de um elemento irrelevante
        if (ELEMENTOS_IRRELEVANTES.contains(tagName)) {
            dentroElementoIrrelevante = false;
            return;
        }

        // Verifica se saímos da div de conteúdo principal
        if (tag == javax.swing.text.html.HTML.Tag.DIV && dentroConteudoPrincipal && profundidadeElemento < 1) {
            dentroConteudoPrincipal = false;
        }

        // Processa links
        if (tag == javax.swing.text.html.HTML.Tag.A) {
            // Fechamos o marcador de link apenas se o texto termina com "LINK: "
            String content = textofinal.toString();
            if (content.endsWith("[LINK: ")) {
                // Remover o marcador de link incompleto
                textofinal.delete(textofinal.length() - 7, textofinal.length());
            } else if (content.contains("[LINK: ") && !content.endsWith("]")) {
                textofinal.append("]");
            }
        }

        // Adiciona quebras de linha para elementos de bloco
        if (tag == javax.swing.text.html.HTML.Tag.P ||
                tag == javax.swing.text.html.HTML.Tag.DIV ||
                tag == javax.swing.text.html.HTML.Tag.H1 ||
                tag == javax.swing.text.html.HTML.Tag.H2 ||
                tag == javax.swing.text.html.HTML.Tag.H3 ||
                tag == javax.swing.text.html.HTML.Tag.H4 ||
                tag == javax.swing.text.html.HTML.Tag.H5 ||
                tag == javax.swing.text.html.HTML.Tag.H6 ||
                tag == javax.swing.text.html.HTML.Tag.BR) {
            textofinal.append("\n");
        }
    }

    /**
     * Processa texto simples
     */
    @Override
    public void handleText(char[] text, int position) {
        if (!dentroElementoIrrelevante) {
            String textContent = new String(text).trim();
            if (!textContent.isEmpty()) {
                // Remover espaços múltiplos
                textContent = textContent.replaceAll("\\s+", " ");
                textofinal.append(textContent).append(" ");

                // Adicionar quebra de linha para textos longos ou frases completas
                if (textContent.endsWith(".") || textContent.endsWith("?") || textContent.endsWith("!")) {
                    textofinal.append("\n");
                }
            }
        }
    }


    /**
     * Processa um comentário HTML
     */
    @Override
    public void handleComment(char[] dados, int pos) {
        // Ignoramos comentários HTML
    }

    /**
     * Processa uma entidade HTML
     */
    @Override
    public void handleSimpleTag(javax.swing.text.html.HTML.Tag t, javax.swing.text.MutableAttributeSet a, int pos) {
        if (t == javax.swing.text.html.HTML.Tag.BR) {
            textofinal.append("\n");
        } else if (t == javax.swing.text.html.HTML.Tag.IMG) {
            String alt = (String) a.getAttribute(javax.swing.text.html.HTML.Attribute.ALT);
            if (alt != null && !alt.isEmpty()) {
                textofinal.append(" [IMAGEM: ").append(alt).append("] ");
            } else {
                textofinal.append(" [IMAGEM] ");
            }
        }
    }

    /**
     * Define o texto extraído
     * @param texto Texto a ser definido
     */
    public void setTexto(String texto) {
        this.textofinal = new StringBuilder(texto);
    }

    /**
     * Obtém o texto extraído
     * @return Texto extraído
     */
    public String getTexto() {
        return textofinal.toString();
    }

    /**
     * Verifica se ocorreu erro durante a extração
     * @return true se ocorreu erro, false caso contrário
     */
    public boolean getErro() {
        return erro;
    }

    /**
     * Obtém o número de elementos ignorados durante a extração
     * @return Número de elementos ignorados
     */
    public int getElementosIgnorados() {
        return elementosIgnorados;
    }

    /**
     * Retorna a URL atual sendo processada
     * @return URL atual
     */
    public String getCurrentUrl() {
        return this.currentUrl;
    }

    /**
     * Utilitário para extrair texto de strings usando expressões regulares
     * @param content Conteúdo a ser analisado
     * @param pattern Padrão de regex
     * @return Texto extraído ou string vazia se não encontrado
     */
    public String extractPattern(String content, String pattern) {
        if (content == null || pattern == null) {
            return "";
        }

        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "";
    }

    /**
     * Utilitário para extrair conteúdo delimitado por tags
     * @param content Conteúdo HTML
     * @param startTag Tag de início
     * @param endTag Tag de fim
     * @return Conteúdo entre as tags ou string vazia se não encontrado
     */
    public String extractBetweenTags(String content, String startTag, String endTag) {
        if (content == null || startTag == null || endTag == null) {
            return "";
        }

        String pattern = Pattern.quote(startTag) + "(.*?)" + Pattern.quote(endTag);
        Pattern regexPattern = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher matcher = regexPattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "";
    }

    /**
     * Divide o conteúdo em linhas e remove espaços em branco
     * @param content Conteúdo a ser processado
     * @return Array de linhas processadas
     */
    public String[] splitIntoCleanLines(String content) {
        if (content == null) {
            return new String[0];
        }

        Scanner scanner = new Scanner(content);
        StringBuilder cleanContent = new StringBuilder();

        while (scanner.hasNext()) {
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                cleanContent.append(line).append("\n");
            }
        }

        scanner.close();
        return cleanContent.toString().split("\n");
    }
}

/*
public class MeuParser extends HTMLEditorKit.ParserCallback {
    BlindBrowser formulario;
    private StringBuilder textofinal = new StringBuilder();-->
    boolean erro;
    private boolean dentroConteudoPrincipal = false;
    private int relevanciaAtual = 0;
    private String currentUrl = "";
    /**
     * Retorna a URL atual sendo processada
     * @return URL atual
   
    private String getCurrentUrl() {
        return this.currentUrl;
    }


    // Variáveis para controle de elementos irrelevantes
    private boolean dentroElementoIrrelevante = false;
    private int profundidadeElemento = 0;
    private int elementosIgnorados = 0;

    // Conjuntos de tags irrelevantes
    private static final Set<HTML.Tag> tagsIrrelevantes = new HashSet<>();
    private static final Set<String> tagNamesIrrelevantes = new HashSet<>();
    private static final Set<String> classesIrrelevantes = new HashSet<>();
    private static final Set<String> idsIrrelevantes = new HashSet<>();

    static {
        // Tags que geralmente contêm conteúdo não essencial
        tagsIrrelevantes.add(HTML.Tag.SCRIPT);
        tagsIrrelevantes.add(HTML.Tag.STYLE);
        tagsIrrelevantes.add(HTML.Tag.ADDRESS);

        // HTML5 tags que não existem no Java Swing (verificadas por nome)
        tagNamesIrrelevantes.add("nav");
        tagNamesIrrelevantes.add("footer");
        tagNamesIrrelevantes.add("aside");
        tagNamesIrrelevantes.add("main");
        tagNamesIrrelevantes.add("article");
        tagNamesIrrelevantes.add("noscript");
        tagNamesIrrelevantes.add("iframe");

        // Classes comuns para menus, banners, anúncios, etc.
        classesIrrelevantes.add("menu");
        classesIrrelevantes.add("navigation");
        classesIrrelevantes.add("sidebar");
        classesIrrelevantes.add("banner");
        classesIrrelevantes.add("advertisement");
        classesIrrelevantes.add("ads");
        classesIrrelevantes.add("social-media");
        classesIrrelevantes.add("cookie-notice");
        classesIrrelevantes.add("header-menu");
        classesIrrelevantes.add("nav-menu");
        classesIrrelevantes.add("main-menu");
        classesIrrelevantes.add("top-menu");
        classesIrrelevantes.add("navbar");
        classesIrrelevantes.add("widget");
        classesIrrelevantes.add("popup");
        classesIrrelevantes.add("modal");
        classesIrrelevantes.add("cookie-policy");
        classesIrrelevantes.add("newsletter");
        classesIrrelevantes.add("share-buttons");
        classesIrrelevantes.add("related-posts");
        classesIrrelevantes.add("comments-section");
        classesIrrelevantes.add("footer-links");
        classesIrrelevantes.add("author-bio");
        classesIrrelevantes.add("recommended-posts");
        classesIrrelevantes.add("recommended-content");
        classesIrrelevantes.add("recommended");
        classesIrrelevantes.add("pagination");
        classesIrrelevantes.add("search-form");
        classesIrrelevantes.add("share");
        classesIrrelevantes.add("related-content");
        classesIrrelevantes.add("related");
        classesIrrelevantes.add("subscribe");
        classesIrrelevantes.add("tag-cloud");
        classesIrrelevantes.add("trending-posts");
        classesIrrelevantes.add("trending-content");
        classesIrrelevantes.add("trending");

        // IDs comuns para elementos irrelevantes
        idsIrrelevantes.add("menu");
        idsIrrelevantes.add("navigation");
        idsIrrelevantes.add("sidebar");
        idsIrrelevantes.add("footer");
        idsIrrelevantes.add("header");
        idsIrrelevantes.add("topnav");
        idsIrrelevantes.add("navbar");
        idsIrrelevantes.add("breadcrumbs");
        idsIrrelevantes.add("comments");
        idsIrrelevantes.add("related-content");
        idsIrrelevantes.add("newsletter-form");
        idsIrrelevantes.add("cookie-banner");
        idsIrrelevantes.add("popup-overlay");
        idsIrrelevantes.add("author-infor");
        idsIrrelevantes.add("search-overlay");
        idsIrrelevantes.add("suggested-content");
        idsIrrelevantes.add("social-media");
        idsIrrelevantes.add("cookie-notice");
        idsIrrelevantes.add("cookie-policy");
        idsIrrelevantes.add("newsletter");
        idsIrrelevantes.add("share-buttons");
        idsIrrelevantes.add("related-posts");
        idsIrrelevantes.add("side-menu");

    }

    public MeuParser() {
        this.textofinal = new StringBuilder();
    }

    // Verifica se o elemento atual é irrelevante com base em suas tags, classes ou IDs
    private boolean elementoEhIrrelevante(HTML.Tag tag, MutableAttributeSet atributos) {
        // Verifica tag diretamente
        if (tagsIrrelevantes.contains(tag)) {
            elementosIgnorados++;
            return true;
        }

        // Verifica nome da tag para HTML5 tags não existentes no Java Swing
        String tagName = tag.toString().toLowerCase();
        if (tagNamesIrrelevantes.contains(tagName)) {
            elementosIgnorados++;
            return true;
        }

        if (atributos != null) {
            // Verifica classes
            String classValue = (String) atributos.getAttribute(HTML.Attribute.CLASS);
            if (classValue != null) {
                String[] classes = classValue.toLowerCase().split("\\s+");
                for (String cls : classes) {
                    if (classesIrrelevantes.contains(cls) || cls.contains("menu") ||
                            cls.contains("nav") || cls.contains("share") ||
                            cls.contains("header") || cls.contains("footer") ||
                            cls.contains("comment") || cls.contains("cookie") ||
                            cls.contains("banner") || cls.contains("ad-") ||
                            cls.contains("popup") || cls.contains("sidebar")) {
                        elementosIgnorados++;
                        return true;

                    }
                }
            }

            // Verifica ID
                String idValue = (String) atributos.getAttribute(HTML.Attribute.ID);
                if (idValue != null) {
                    idValue = idValue.toLowerCase();
                    if (idsIrrelevantes.contains(idValue) || idValue.contains("menu") ||
                            idValue.contains("nav") || idValue.contains("share") ||
                            idValue.contains("header") || idValue.contains("footer") ||
                            idValue.contains("comment") || idValue.contains("cookie") ||
                            idValue.contains("banner") || idValue.contains("ad-") ||
                            idValue.contains("popup") || idValue.contains("sidebar")) {
                        elementosIgnorados++;
                        return true;
                    }

            }

            // Verifica atributos de acessibilidade (novo)
            String ariaHidden = (String) atributos.getAttribute("aria-hidden");
            if (ariaHidden != null && ariaHidden.equals("true")) {
                elementosIgnorados++;
                return true;
            }


            // Verifica atributo role para elementos de navegação - usando nome do atributo como string
            String ariaRole = (String) atributos.getAttribute("role");
            if (ariaRole != null && (ariaRole.equals("banner") || ariaRole.equals("navigation") ||
                    ariaRole.equals("complementary") || ariaRole.equals("contentinfo") ||
                    ariaRole.equals("search") || ariaRole.equals("dialog"))) {
                elementosIgnorados++;
                return true;
            }
        }

        return false;
    }


    //Métodos para coletar o texto e manipular as TAGS    
    @Override
    public void handleText(char[] text, int position) {
        // Ignora o texto se estiver dentro de um elemento irrelevante
        if (!dentroElementoIrrelevante) {
            // Limpa e formata melhor o texto
            String textContent = new String(text).trim();

            // Evita adicionar espaços vazios ou quebras de linha para texto em branco
            if (!textContent.isEmpty()) {
                // Aplica priorização com base na relevância do elemento atual
                String processedText = textContent.replaceAll("\\s+", " ");

                // Verifica se o texto parece ser conteúdo principal da Wikipedia
                boolean isWikipediaContent = currentUrl.contains("wikipedia.org") && 
                                            (processedText.length() > 30 || 
                                             processedText.startsWith("[") || 
                                             Character.isUpperCase(processedText.charAt(0)));

                // Se estiver dentro do conteúdo principal, destaca mais
                if (dentroConteudoPrincipal && relevanciaAtual > 0) {
                    textofinal.append(processedText).append(" ");
                } else if (relevanciaAtual > 0) {
                    // Conteúdo relevante fora da área principal
                    textofinal.append(processedText).append(" ");
                } else if (processedText.length() > 50) {
                    // Conteúdo longo fora de elementos relevantes pode ser importante
                    textofinal.append(processedText).append(" ");
                } else if (isWikipediaContent) {
                    // Conteúdo da Wikipedia que parece relevante
                    textofinal.append(processedText).append(" ");
                } else if (!isBoilerplateText(processedText)) {
                    // Adiciona apenas se não parecer texto boilerplate
                    textofinal.append(processedText).append(" ");
                }

                // Log para depuração de conteúdo da Wikipedia
                if (currentUrl.contains("wikipedia.org") && processedText.length() > 50) {
                    System.out.println("DEBUG: MeuParser.handleText - Texto Wikipedia coletado: " + 
                                      processedText.substring(0, Math.min(50, processedText.length())) + "...");
                }
            }
        }
    }

    /**
     * Verifica se o texto parece ser boilerplate (texto padrão repetitivo)
     
    private boolean isBoilerplateText(String text) {
        String lowerText = text.toLowerCase();

        // Frases comuns de boilerplate
        String[] boilerplatePatterns = {
                "aceitar cookies", "política de privacidade", "termos de uso",
                "todos os direitos reservados", "copyright", "newsletter",
                "inscreva-se", "cadastre-se", "entre em contato", "siga-nos"
        };

        for (String pattern : boilerplatePatterns) {
            if (lowerText.contains(pattern)) {
                return true;
            }
        }

        // Verifica se parece um texto muito curto e não essencial
        if (text.length() < 15 && !Character.isUpperCase(text.charAt(0))) {
            return true;
        }

        return false;
    }


    @Override
    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        // Identifica se está entrando em uma área de conteúdo principal
        if (!dentroConteudoPrincipal) {
            if (t.toString().toLowerCase().equals("main") || t.toString().toLowerCase().equals("article")) {
                dentroConteudoPrincipal = true;
                textofinal = new StringBuilder(textofinal + "\n[INÍCIO DO CONTEÚDO PRINCIPAL]\n");
            } else if (a != null) {
                String role = (String) a.getAttribute("role");
                String id = (String) a.getAttribute(HTML.Attribute.ID);
                String className = (String) a.getAttribute(HTML.Attribute.CLASS);

                if ((role != null && role.equals("main")) ||
                        (id != null && (id.equals("content") || id.equals("main-content") || id.equals("article"))) ||
                        (className != null && (className.contains("content") || className.contains("article")))) {
                    dentroConteudoPrincipal = true;
                    textofinal = new StringBuilder(textofinal + "\n[INÍCIO DO CONTEÚDO PRINCIPAL]\n");
                }
            }
        }

        // Verifica se está entrando em um elemento irrelevante
        if (elementoEhIrrelevante(t, a)) {
            dentroElementoIrrelevante = true;
            profundidadeElemento++;
        } else if (dentroElementoIrrelevante) {
            // Se já está dentro de elemento irrelevante, apenas incrementa profundidade
            profundidadeElemento++;
        } else {
            // Aumenta relevância para cabeçalhos e elementos importantes
            if (t == HTML.Tag.H1) {
                relevanciaAtual += 5;
                textofinal = new StringBuilder(textofinal + "\n[TÍTULO PRINCIPAL] ");
            } else if (t == HTML.Tag.H2) {
                relevanciaAtual += 4;
                textofinal = new StringBuilder(textofinal + "\n[SUBTÍTULO] ");
            } else if (t == HTML.Tag.H3) {
                relevanciaAtual += 3;
                textofinal = new StringBuilder(textofinal + "\n[TÓPICO] ");
            } else if (t == HTML.Tag.P && dentroConteudoPrincipal) {
                relevanciaAtual += 2;
            }

            // Processa normalmente elementos relevantes
            if (t == HTML.Tag.A && a != null) {
                String hrefValue = (String) a.getAttribute(HTML.Attribute.HREF);
                if (hrefValue != null && !hrefValue.startsWith("#") && !hrefValue.startsWith("javascript:")) {
                    textofinal = new StringBuilder(textofinal + "<Link: " + hrefValue + ">" + " ");
                }
            }

            // Destaque para listas
            if (t == HTML.Tag.LI) {
                textofinal = new StringBuilder(textofinal + "\n * ");
            }
        }
    }


    @Override
    public void handleEndTag(HTML.Tag t, int pos) {
        // Se estiver saindo de um elemento irrelevante
        if (dentroElementoIrrelevante) {
            profundidadeElemento--;
            if (profundidadeElemento == 0) {
                dentroElementoIrrelevante = false;
            }
        }
        // Se estiver saindo da área de conteúdo principal
        if (dentroConteudoPrincipal) {
            if (t.toString().toLowerCase().equals("main") || t.toString().toLowerCase().equals("article")) {
                dentroConteudoPrincipal = false;
                textofinal = new StringBuilder(textofinal + "\n[FIM DO CONTEÚDO PRINCIPAL]\n");
            }

        }

        // Diminui relevância ao sair de elementos importantes
        if (t == HTML.Tag.H1) {
            relevanciaAtual -= 5;
        } else if (t == HTML.Tag.H2) {
            relevanciaAtual -= 4;
        } else if (t == HTML.Tag.H3) {
            relevanciaAtual -= 3;
        } else if (t == HTML.Tag.P && dentroConteudoPrincipal) {
            relevanciaAtual -= 2;
        }
    }


    @Override
    public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        if (!dentroElementoIrrelevante) {
            if (t == HTML.Tag.IMG && a != null) {
                String altValue = (String) a.getAttribute(HTML.Attribute.ALT);
                if (altValue != null && !altValue.trim().isEmpty()) {
                    textofinal = new StringBuilder(textofinal + "<Nome da imagem: " + altValue + ">" + " ");
                }
            } else if (t == HTML.Tag.BR) {
                textofinal = new StringBuilder(textofinal + "\n");
            } else if (t == HTML.Tag.HR) {
                textofinal = new StringBuilder(textofinal + "\n-----------------------------------------\n");
            }
        }
    }

    /**
     * Processa o texto final antes de retorná-lo, aplicando formatação adicional
     * e garantindo que as partes mais relevantes sejam enfatizadas
    
    private String processFinalText(String rawText) {
        // Dividir em parágrafos
        String[] paragraphs = rawText.split("\n{2,}");
        StringBuilder processedText = new StringBuilder();

        // Filtrar parágrafos muito curtos ou com pouca informação
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();

            // Ignora parágrafos vazios
            if (paragraph.isEmpty()) {
                continue;
            }

            // Ignora parágrafos que são apenas navegação ou elementos irrelevantes
            if (paragraph.length() < 20 && !paragraph.contains("[TÍTULO") &&
                    !paragraph.contains("PRINCIPAL]") && !paragraph.contains("Link:")) {
                continue;
            }

            // Remove espaços duplicados
            paragraph = paragraph.replaceAll("\\s+", " ");

            processedText.append(paragraph).append("\n\n");
        }

        return processedText.toString().trim();
    }


    public void ExtraiTexto(String url) {
        // Salvar a URL atual
        this.currentUrl = url;
        System.out.println("DEBUG: MeuParser.ExtraiTexto - Iniciando extração para URL: " + url);
        
        // Limpar o texto final antes de começar
        textofinal = new StringBuilder();
        dentroConteudoPrincipal = false;
        dentroElementoIrrelevante = false;
        profundidadeElemento = 0;
        elementosIgnorados = 0;
        
        try {
            // Obter a página
            URL endereco = new URL(url);
            URLConnection conexao = endereco.openConnection();
            conexao.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
            
            // Configurar o parser
            ParserGetter kit = new ParserGetter();
            HTMLEditorKit.Parser parser = kit.getParser();
            
            // Iniciar com marcador de conteúdo principal
            textofinal.append("[INÍCIO DO CONTEÚDO PRINCIPAL]");
            
            // Realizar o parsing
            System.out.println("DEBUG: MeuParser.ExtraiTexto - Iniciando parsing do HTML");
            InputStreamReader isr = new InputStreamReader(conexao.getInputStream(), "UTF-8");
            parser.parse(isr, this, true);
            isr.close();
            
            System.out.println("DEBUG: MeuParser.ExtraiTexto - Parsing concluído, tamanho inicial: " + textofinal.length() + " caracteres");
            
            // Determinar qual integrador usar com base na URL
            AIParserIntegrator integrator = AIParserIntegratorFactory.createIntegrator(this.currentUrl);
            System.out.println("DEBUG: MeuParser.ExtraiTexto - Usando integrador: " + integrator.getClass().getSimpleName());
            
            // Processar o conteúdo com o integrador específico
            Optional<String> processedContent = integrator.processContent(this);
            
            if (processedContent.isPresent()) {
                // Substituir o conteúdo original pelo processado
                textofinal = new StringBuilder(processedContent.get());
                System.out.println("DEBUG: MeuParser.ExtraiTexto - Conteúdo processado pelo integrador específico");
            } else {
                System.out.println("DEBUG: MeuParser.ExtraiTexto - Integrador não processou o conteúdo, mantendo original");
            }
            
            erro = false;
            
        } catch (MalformedURLException ex) {
            System.err.println("ERRO: URL mal formada - " + ex.getMessage());
            erro = true;
        } catch (IOException ex) {
            System.err.println("ERRO: Problema de IO - " + ex.getMessage());
            erro = true;
        } catch (Exception ex) {
            System.err.println("ERRO: Exceção geral - " + ex.getMessage());
            ex.printStackTrace();
            erro = true;
        }
    }
    /**
     * Processa o texto extraído usando inteligência artificial para melhorar ainda mais
     * a relevância e compreensão do conteúdo
     
    @Override
    public Optional<String> processContent(MeuParser parser) {
        System.out.println("DEBUG: WikipediaParserIntegrator.processContent - Iniciando processamento específico para Wikipedia");

        if (parser == null) {
            System.err.println("ERRO: WikipediaParserIntegrator recebeu parser nulo");
            return Optional.empty();
        }

        // Verificar URL
        String url = parser.getCurrentUrl();
        if (url == null || !url.toLowerCase().contains("wikipedia")) {
            System.err.println("ERRO: URL não é da Wikipedia: " + url);
            return Optional.empty();
        }

        System.out.println("DEBUG: WikipediaParserIntegrator.processContent - Processando URL Wikipedia: " + url);

        try {
            // Obter conteúdo da página usando técnica específica para Wikipedia
            String wikiContent = extrairConteudoWikipedia(url);

            if (wikiContent != null && !wikiContent.isEmpty()) {
                System.out.println("DEBUG: WikipediaParserIntegrator.processContent - Extração bem-sucedida, tamanho: " + wikiContent.length());

                // Pré-processar o conteúdo para melhor formatação
                String processedContent = preprocessWikipediaContent(wikiContent);

                // Formatar com SmartFormatter
                String formattedContent = formatter.format(processedContent);

                // Registrar estatísticas
                registerProcessingStats(wikiContent, formattedContent, System.currentTimeMillis());

                return Optional.of(formattedContent);
            } else {
                System.err.println("ERRO: WikipediaParserIntegrator não conseguiu extrair conteúdo");
                return Optional.empty();
            }
        } catch (Exception e) {
            System.err.println("ERRO: Falha ao processar conteúdo da Wikipedia: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Método especializado para extrair conteúdo da Wikipedia usando técnicas específicas
     
    private String extrairConteudoWikipedia(String url) {
        try {
            // Configurar conexão
            URL wikiUrl = new URL(url);
            URLConnection conexao = wikiUrl.openConnection();
            conexao.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");

            // Obter conteúdo HTML
            InputStream is = conexao.getInputStream();
            Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
            String htmlContent = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
            is.close();

            // Extrair conteúdo principal usando expressões regulares (simplificado)
            // Na implementação real, seria melhor usar JSoup ou similar
            String content = extrairConteudoComRegex(htmlContent);

            // Se a extração com regex falhar, tentar método alternativo
            if (content == null || content.trim().isEmpty()) {
                System.out.println("DEBUG: Extração com regex falhou, tentando método alternativo");
                content = extrairConteudoAlternativo(htmlContent);
            }

            return content;
        } catch (Exception e) {
            System.err.println("ERRO ao extrair conteúdo da Wikipedia: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extrai conteúdo usando expressões regulares
     
    private String extrairConteudoComRegex(String html) {
        try {
            // Padrão para capturar o conteúdo principal da Wikipedia
            Pattern pattern = Pattern.compile("<div id=\"mw-content-text\"[^>]*>(.*?)<div class=\"printfooter\">", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
                String content = matcher.group(1);

                // Limpar tags HTML (implementação simplificada)
                content = content.replaceAll("<script.*?</script>", "");
                content = content.replaceAll("<style.*?</style>", "");
                content = content.replaceAll("<.*?>", " ");
                content = content.replaceAll("\\s+", " ");

                return content.trim();
            }
            return "";
        } catch (Exception e) {
            System.err.println("Erro na extração com regex: " + e.getMessage());
            return "";
        }
    }

    /**
     * Método alternativo de extração para caso o regex falhe
     
    private String extrairConteudoAlternativo(String html) {
        try {
            // Identificar seções chave da Wikipedia
            int startIndex = html.indexOf("<div id=\"mw-content-text\"");
            int endIndex = html.indexOf("<div class=\"printfooter\">");

            if (startIndex > 0 && endIndex > startIndex) {
                String content = html.substring(startIndex, endIndex);

                // Remover tags HTML
                content = content.replaceAll("<script.*?</script>", "");
                content = content.replaceAll("<style.*?</style>", "");
                content = content.replaceAll("<[^>]*>", " ");
                content = content.replaceAll("\\s+", " ");

                return content.trim();
            }
            return "Não foi possível extrair o conteúdo da Wikipedia.";
        } catch (Exception e) {
            System.err.println("Erro na extração alternativa: " + e.getMessage());
            return "Erro ao processar página da Wikipedia: " + e.getMessage();
        }



    }


    public String getTexto() {
        // Processa o texto final antes de retorná-lo
        return processFinalText(textofinal.toString());
    }

    public void setTexto(String t) {
        textofinal = new StringBuilder(t);
    }

    public boolean getErro() {
        return erro;
    }

    public void setErro(boolean e) {
        this.erro = e;
    }

    // Método para registrar estatísticas de elementos ignorados
    public int getElementosIgnorados() {
        return elementosIgnorados;
    }
}

class ParserGetter extends HTMLEditorKit {
    public HTMLEditorKit.Parser getParser() {
        return super.getParser();
    }
}
// No MeuParser.java, método ExtraiTexto ou onde o conteúdo é processado
public void ExtraiTexto(String endereco) {
    this.currentUrl = endereco; // Armazenar URL atual
    // Código existente para extrair texto...
    
    // Após extrair o conteúdo básico, verificar se temos apenas o marcador de início
    if (texto.trim().equals("[INÍCIO DO CONTEÚDO PRINCIPAL]")) {
        System.out.println("DEBUG: Texto extraído contém apenas marcador de início, aplicando processamento específico...");
    }
    
    // Aplicar integração com IA
    try {
        // Criar uma instância do integrador de IA usando a URL atual
        AIParserIntegrator aiIntegrator = AIParserIntegratorFactory.createIntegrator(this.currentUrl);
        System.out.println("DEBUG: Usando integrador: " + aiIntegrator.getClass().getSimpleName());
        
        // Verificar se é uma instância de WikipediaParserIntegrator
        if (aiIntegrator instanceof WikipediaParserIntegrator) {
            System.out.println("DEBUG: Ativando processamento específico para Wikipedia");
            
            // Se necessário, recarregar a página ou fazer um processamento específico para Wikipedia
            // Exemplo: recarregar a página com parâmetros específicos que funcionem melhor para Wikipedia
        }
        
        // Processar o conteúdo com o integrador
        Optional<String> processedContent = aiIntegrator.processContent(this);
        
        // Se o processamento foi bem-sucedido, usar o conteúdo processado
        if (processedContent.isPresent()) {
            String finalContent = processedContent.get();
            if (!finalContent.trim().isEmpty() && finalContent.length() > texto.length()) {
                System.out.println("DEBUG: Usando conteúdo processado pelo integrador. Tamanho: " + finalContent.length());
                texto = finalContent;
            } else {
                System.out.println("DEBUG: Integrador não melhorou o conteúdo, mantendo original");
            }
        } else {
            System.out.println("DEBUG: Integrador não retornou conteúdo processado");
        }
    } catch (Exception e) {
        System.err.println("ERRO no processamento de IA: " + e.getMessage());
        e.printStackTrace();
    }
}
 */
