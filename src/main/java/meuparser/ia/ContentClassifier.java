package meuparser.ia;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifica o conteúdo da página com base em suas características
 * Ajuda usuários deficientes visuais a entenderem rapidamente o tipo de conteúdo
 */
public class ContentClassifier {

    // Possíveis categorias de conteúdo
    public enum ContentCategory {
        ARTICLE,
        NEWS,
        BLOG,
        FORM,
        NAVIGATION,
        E_COMMERCE,
        EDUCATIONAL,
        UNKNOWN
    }
    
    // Padrões que indicam cada categoria
    private static final Map<ContentCategory, Pattern> CATEGORY_PATTERNS = new HashMap<>();
    
    static {
        // Inicializar padrões para cada categoria
        CATEGORY_PATTERNS.put(ContentCategory.ARTICLE, 
                Pattern.compile("(?i)\\b(artigo|article|publicado|published|autor|author)\\b"));
        CATEGORY_PATTERNS.put(ContentCategory.NEWS, 
                Pattern.compile("(?i)\\b(notícia|news|reportagem|jornal|newspaper|data de publicação|publication date)\\b"));
        CATEGORY_PATTERNS.put(ContentCategory.BLOG, 
                Pattern.compile("(?i)\\b(blog|post|publicação|comentários|comments|postar|postado)\\b"));
        CATEGORY_PATTERNS.put(ContentCategory.FORM, 
                Pattern.compile("(?i)(\\[INÍCIO DE FORMULÁRIO\\]|\\[CAMPO DE ENTRADA\\]|formulário|form|inscreva-se|cadastro|login|submit)"));
        CATEGORY_PATTERNS.put(ContentCategory.NAVIGATION, 
                Pattern.compile("(?i)(\\[LINK:.*\\]|menu|navegação|navigation|sitemap|mapa do site)"));
        CATEGORY_PATTERNS.put(ContentCategory.E_COMMERCE, 
                Pattern.compile("(?i)\\b(comprar|compra|preço|price|carrinho|cart|produto|product|loja|store|checkout|pagamento|payment)\\b"));
        CATEGORY_PATTERNS.put(ContentCategory.EDUCATIONAL, 
                Pattern.compile("(?i)\\b(aprender|learn|curso|course|aula|lesson|educação|education|tutorial|estudante|student)\\b"));
    }
    
    /**
     * Classifica o conteúdo da página baseado em seus padrões textuais
     *
     * @param pageContent O conteúdo textual da página
     * @return A categoria de conteúdo mais provável
     */
    public ContentCategory classifyContent(String pageContent) {
        if (pageContent == null || pageContent.trim().isEmpty()) {
            return ContentCategory.UNKNOWN;
        }
        
        Map<ContentCategory, Integer> categoryScores = new HashMap<>();
        
        // Inicializa pontuações
        for (ContentCategory category : ContentCategory.values()) {
            categoryScores.put(category, 0);
        }
        
        // Pontuação para cada categoria
        for (Map.Entry<ContentCategory, Pattern> entry : CATEGORY_PATTERNS.entrySet()) {
            ContentCategory category = entry.getKey();
            Pattern pattern = entry.getValue();
            
            Matcher matcher = pattern.matcher(pageContent);
            while (matcher.find()) {
                // Incrementa pontuação ao encontrar padrão
                categoryScores.put(category, categoryScores.get(category) + 1);
            }
        }
        
        // Identifica categoria com maior pontuação
        ContentCategory bestCategory = ContentCategory.UNKNOWN;
        int maxScore = 0;
        
        for (Map.Entry<ContentCategory, Integer> entry : categoryScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }
        
        return bestCategory;
    }
    
    /**
     * Retorna um prefixo descritivo com base na categoria do conteúdo
     * @param category A categoria do conteúdo
     * @return Uma descrição amigável da categoria
     */
    public String getCategoryDescription(ContentCategory category) {
        switch (category) {
            case ARTICLE:
                return "ARTIGO: ";
            case NEWS:
                return "NOTÍCIA: ";
            case BLOG:
                return "BLOG: ";
            case FORM:
                return "FORMULÁRIO: ";
            case NAVIGATION:
                return "PÁGINA DE NAVEGAÇãO: ";
            case E_COMMERCE:
                return "LOJA VIRTUAL: ";
            case EDUCATIONAL:
                return "CONTEÚDO EDUCATIVO: ";
            case UNKNOWN:
            default:
                return "";
        }
    }
}