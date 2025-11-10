package meuparser.ia;

import meuparser.MeuParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Implementação do integrador IA otimizada com Jsoup
 */
public class JsoupAIIntegrator implements AIParserIntegrator {

    private final AIStats stats;
    private final ContentSummarizer summarizer;

    public JsoupAIIntegrator() {
        this.stats = new AIStats();
        this.summarizer = new ContentSummarizer();
    }

    @Override
    public Optional<String> processContent(MeuParser parser) {
        if (parser == null) {
            throw new IllegalArgumentException("O parser não pode ser nulo");
        }

        if (parser.getErro()) {
            stats.registrarErro("Erro no parser ao extrair conteúdo");
            return Optional.empty();
        }

        String originalContent = parser.getTexto();
        if (originalContent == null || originalContent.isEmpty()) {
            stats.registrarErro("Conteúdo original vazio");
            return Optional.empty();
        }

        long startTime = System.currentTimeMillis();

        try {
            // Tenta processar diretamente com Jsoup se o conteúdo parecer HTML
            String processedContent;
            if (originalContent.trim().startsWith("<")) {
                processedContent = processHtmlDirectly(originalContent);
            } else {
                // Se não for HTML, usa o conteúdo já extraído pelo parser
                processedContent = extractMainContent(originalContent);
            }
            
            // Gera um resumo do conteúdo
            String summary = summarizer.generateSummary(processedContent);
            
            // Limpa o texto final
            String finalContent = cleanFinalText(summary + processedContent);
            
            // Registra estatísticas
            long processingTime = System.currentTimeMillis() - startTime;
            stats.registrarProcessamento(
                    originalContent,
                    finalContent,
                    countKeywords(finalContent),
                    detectCategory(parser.getCurrentUrl()),
                    processingTime
            );
            
            return Optional.of(finalContent);
            
        } catch (Exception e) {
            stats.registrarErro("Erro ao processar conteúdo: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Processa HTML diretamente com Jsoup
     */
    private String processHtmlDirectly(String html) {
        try {
            Document doc = Jsoup.parse(html);
            
            // Remove elementos irrelevantes
            for (String className : getIrrelevantClasses()) {
                doc.select("." + className).remove();
            }
            
            for (String id : getIrrelevantIds()) {
                doc.select("#" + id).remove();
            }
            
            // Tenta encontrar o conteúdo principal
            Elements mainContent = doc.select(getMainContentSelector());
            
            if (!mainContent.isEmpty()) {
                return "[INÍCIO DO CONTEÚDO PRINCIPAL]\n" + 
                       mainContent.text() + 
                       "\n[FIM DO CONTEÚDO PRINCIPAL]";
            } else {
                // Se não encontrar o conteúdo principal, usa o body todo
                return doc.body().text();
            }
            
        } catch (Exception e) {
            // Se falhar, retorna o HTML original
            return html;
        }
    }

    /**
     * Conta palavras-chave relevantes no conteúdo
     */
    private int countKeywords(String content) {
        String[] keywords = {"artigo", "conteúdo", "informação", "texto",
                "principal", "resumo", "tópico", "tema"};
        
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
    
    /**
     * Detecta a categoria da página baseado na URL
     */
    private String detectCategory(String url) {
        if (url == null) {
            return "Desconhecido";
        }
        
        if (url.contains("wikipedia.org")) {
            return "Wikipedia";
        } else if (url.contains("gov")) {
            return "Governo";
        } else if (url.contains("edu")) {
            return "Educação";
        } else if (url.contains("noticias") || url.contains("news")) {
            return "Notícias";
        } else {
            return "Site Geral";
        }
    }

    @Override
    public AIStats getStats() {
        return this.stats;
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
        classes.add("comments");
        classes.add("advertisement");
        classes.add("nav");
        
        // Classes específicas da Wikipedia e outros sites comuns
        classes.add("mw-navigation");
        classes.add("vector-menu");
        classes.add("toc");
        classes.add("metadata");
        classes.add("social-media");
        classes.add("share-buttons");
        classes.add("related-content");
        
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
        ids.add("comments");
        
        // IDs específicos
        ids.add("toc");
        ids.add("breadcrumbs");
        ids.add("related-articles");
        ids.add("social-share");
        
        return ids;
    }

    @Override
    public String getMainContentSelector() {
        return "article, main, #content, .content, .post-content, .entry-content, [role=main]";
    }
}