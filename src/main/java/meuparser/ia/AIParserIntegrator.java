
package meuparser.ia;

import meuparser.MeuParser;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interface para integradores de IA que processam conteúdo web para usuários com deficiência visual
 */
public interface AIParserIntegrator {

    /**
     * Processa o conteúdo extraído de uma página web
     *
     * @param parser O parser HTML que contém o conteúdo extraído
     * @return O conteúdo processado ou empty se não for possível processar
     * @throws IllegalArgumentException se o parser for nulo
     */
    Optional<String> processContent(MeuParser parser);

    /**
     * Retorna as estatísticas de processamento
     *
     * @return O objeto de estatísticas
     */
    AIStats getStats();

    /**
     * Retorna o conjunto de classes CSS consideradas irrelevantes para extração de conteúdo
     *
     * @return Conjunto de strings com nomes de classes CSS
     */
    Set<String> getIrrelevantClasses();

    /**
     * Retorna o conjunto de IDs considerados irrelevantes para extração de conteúdo
     *
     * @return Conjunto de strings com nomes de IDs
     */
    Set<String> getIrrelevantIds();

    /**
     * Retorna o seletor CSS para identificar o conteúdo principal da página
     *
     * @return String contendo o seletor CSS
     */
    String getMainContentSelector();

    /**
     * Limpa o texto final antes de retorná-lo ao usuário
     *
     * @param text Texto a ser limpo
     * @return Texto limpo sem elementos HTML ou formatações desnecessárias
     */
    default String cleanFinalText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Remove todos os códigos CSS e HTML residuais
        String cleaned = text;
        cleaned = cleaned.replaceAll("\\.mw-parser-output\\s*\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("@media[^{]*\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("(?s)<script.*?</script>", "");
        cleaned = cleaned.replaceAll("(?s)<style.*?</style>", "");

        // Remove qualquer HTML inline
        cleaned = cleaned.replaceAll("<[^>]+>", "");
        cleaned = cleaned.replaceAll("<a[^>]*>(.*?)</a>", "$1");
        cleaned = cleaned.replaceAll("<i>(.*?)</i>", "$1");
        cleaned = cleaned.replaceAll("<b>(.*?)</b>", "$1");
        cleaned = cleaned.replaceAll("<span[^>]*>(.*?)</span>", "$1");
        cleaned = cleaned.replaceAll("<[^>]*>", " ");

        // Remove o conteúdo de tags script e style
        cleaned = cleaned.replaceAll("(?s)<script.*?</script>", "");
        cleaned = cleaned.replaceAll("(?s)<style.*?</style>", "");

        // Remove atributos CSS e classes
        cleaned = cleaned.replaceAll("\\.mw-parser-output\\s+\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("@media[^}]*\\{[^}]*\\}", "");

        // 3. Remover elementos de navegação
        cleaned = cleaned.replaceAll("(?i)Navega[çc][aã]o|Ferramentas|Categorias|Páginas para editores", "");


        // Remove códigos CSS específicos (como detectado nos logs)
        cleaned = cleaned.replaceAll("\\.mw-parser-output[^{]+\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("@media\\([^)]+\\)\\{[^}]*\\}", "");

        // Remove elementos de link e a href
        cleaned = cleaned.replaceAll("\\[LINK(?:[^\\]]*)?\\]", "");

        // Remove quaisquer caracteres de controle ou não-imprimíveis
        cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // Remover código de template detectado nos logs
        cleaned = cleaned.replaceAll("tmulti[^{]*\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("navbox[^{]*\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("mobile-stack[^{]*\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("hlist[^{]*\\{[^}]*\\}", "");

        //  Remover texto boilerplate específico da Wikipedia
        String[] boilerplateTexts = {
                "Esta página foi editada",
                "Obtida de",
                "Categorias ocultas",
                "Editar hiperligações",
                "Origem: Wikipédia, a enciclopédia livre",
                "Este texto é disponibilizado nos termos da licença",
                "Código de conduta",
                "Programadores",
                "Estatísticas",
                "Políticas de privacidade"
        };

        for (String t : boilerplateTexts) {
            cleaned = cleaned.replaceAll("(?i)" + Pattern.quote(t) + ".*?\\n", "");
        }

        // Remover links mantendo apenas o texto
        cleaned = cleaned.replaceAll("\\[LINK(?:\\s+\\d+)?:\\s*([^\\]]+)\\]", "$1");
        cleaned = cleaned.replaceAll("\\[LINK\\]", "");


        // Normaliza espaços em branco
        cleaned = cleaned.replaceAll("\\s+", " ");

        // Separa parágrafos com dupla quebra de linha
        cleaned = cleaned.replaceAll("(\\. )\\s*([A-Z])", "$1\n\n$2");

        // Limpa marcadores específicos detectados nos logs
        cleaned = cleaned.replaceAll("skin--responsive", "");
        cleaned = cleaned.replaceAll("html\\.skin-theme-clientpref-night", "");
        cleaned = cleaned.replaceAll("body\\.skin--responsive", "");
        cleaned = cleaned.replaceAll("body\\.ns-0", "");
        cleaned = cleaned.replaceAll("html\\.skin-theme", "");
        cleaned = cleaned.replaceAll("body\\.skin--responsive", "");

        // Mantém formatação básica para Braille
        cleaned = Pattern.compile("\\[([A-Z\\s]+)\\]\\s").matcher(cleaned)
                .replaceAll("[[$1]] ");

        return cleaned.trim();
    }

    /**
     * Extrai apenas o conteúdo principal da página, removendo elementos irrelevantes
     *
     * @param content O conteúdo HTML original
     * @return Apenas o conteúdo principal limpo
     */
    default String extractMainContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // Identifica e extrai apenas a seção principal
        String mainContent = content;

        // Tenta encontrar marcadores de seção principal
        int startIdx = content.indexOf("[INÍCIO DO CONTEÚDO PRINCIPAL]");
        int endIdx = content.indexOf("[FIM DO CONTEÚDO PRINCIPAL]");

        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            mainContent = content.substring(startIdx, endIdx);
        }

        return mainContent;
    }
}