package meuparser.ia;

/**
 * Factory responsável por criar a implementação apropriada de AIParserIntegrator
 * baseado na URL da página sendo processada
 */
public class AIParserIntegratorFactory {

    /**
     * Cria a implementação apropriada de AIParserIntegrator com base na URL
     *
     * @return Uma implementação específica para o site ou o integrador genérico
     */
    public static AIParserIntegrator createIntegrator(String url) {
        if (url == null || url.isEmpty()) {
            System.out.println("DEBUG: URL nula ou vazia, usando integrador genérico");
            return new GenericParserIntegrator();
        }

        String urlLower = url.toLowerCase();

        // Integrador específico para Wikipedia - prioridade mais alta devido aos logs mostrando problemas
        if (urlLower.contains("wikipedia.org") || urlLower.contains("wikimedia.org")) {
            System.out.println("DEBUG: Detectada página da Wikipedia, usando integrador especializado");
            return new WikipediaParserIntegrator(); // Nova implementação melhorada
        }

        // Sites de notícias brasileiros
        if (urlLower.contains("g1.globo.com") || urlLower.contains("globo.com")) {
            return new G1ParserIntegrator("globo");
        } else if (urlLower.contains("uol.com.br") && !urlLower.contains("brasilescola")) {
            return new UOLParserIntegrator("uol");
        } else if (urlLower.contains("folha.uol.com.br")) {
            return new G1ParserIntegrator("folha");
        }

        // Sites educacionais
        if (urlLower.contains("brasilescola.uol.com.br") ||
                urlLower.contains("educacao.uol.com.br") ||
                urlLower.contains("mundoeducacao") ||
                urlLower.contains("infoescola")) {
            return new BrasilEscolaParserIntegrator();
        }

        // Para qualquer outro site, usar o integrador genérico melhorado
        System.out.println("DEBUG: Usando integrador genérico para: " + urlLower);
        return new GenericParserIntegrator();
    }
}