package meuparser.ia;

import meuparser.MeuParser;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementação do AIParserIntegrator específica para o portal G1
 */
public class G1ParserIntegrator implements AIParserIntegrator {
    private final AIStats stats = new AIStats();
    private static final String[] CATEGORIAS_G1 = {
            "Política", "Economia", "Mundo", "Tecnologia", "Concursos", "Educação",
            "Saúde", "Ciência", "Meio Ambiente", "Esporte", "Entretenimento"
    };

    // Padrões para extração de informações específicas do G1
    private static final Pattern PATTERN_DATA = Pattern.compile("\\d{2}/\\d{2}/\\d{4} \\d{2}h\\d{2}");
    private static final Pattern PATTERN_AUTOR = Pattern.compile("Por ([\\p{L}\\s]+),");
    private static final Pattern PATTERN_CHAPEU = Pattern.compile("\\[([^\\]]+)\\]");

    public G1ParserIntegrator(String folha) {
    }

    @Override
    public Optional<String> processContent(MeuParser parser) {
        if (parser == null) {
            throw new IllegalArgumentException("O parser não pode ser nulo");
        }

        if (parser.getErro()) {
            return Optional.empty();
        }

        String originalContent = parser.getTexto();
        if (originalContent == null || originalContent.isEmpty()) {
            return Optional.empty();
        }

        // Registra o tempo de início do processamento
        long startTime = System.currentTimeMillis();

        try {
            // Extrair informações específicas do G1
            String chapeu = extrairChapeu(originalContent);
            String autor = extrairAutor(originalContent);
            String dataPublicacao = extrairDataPublicacao(originalContent);
            String[] palavrasChave = extrairPalavrasChaveG1(originalContent);
            String categoria = identificarCategoriaG1(originalContent, chapeu);
            String resumo = gerarResumoG1(originalContent);

            // Formatar o conteúdo processado
            String conteudoProcessado = formatarConteudoG1(
                    originalContent, chapeu, autor, dataPublicacao,
                    palavrasChave, categoria, resumo
            );

            // Registrar estatísticas
            long processingTime = System.currentTimeMillis() - startTime;
            registrarEstatisticasG1(
                    originalContent, conteudoProcessado, palavrasChave,
                    categoria, processingTime, autor, dataPublicacao
            );

            return Optional.of(conteudoProcessado);
        } catch (Exception e) {
            System.err.println("Erro ao processar conteúdo do G1: " + e.getMessage());
            stats.registrarErro("Falha no processamento: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public AIStats getStats() {
        return stats;
    }

    @Override
    public Set<String> getIrrelevantClasses() {
        Set<String> classes = new HashSet<>();
        // Classes específicas do G1 a serem ignoradas
        classes.add("menu-item");
        classes.add("barra-globocom");
        classes.add("header-globo");
        classes.add("glb-topo");
        classes.add("glb-menu");
        classes.add("glb-bloco");
        classes.add("publicidade");
        classes.add("comentarios");
        classes.add("rodape");
        classes.add("tags");
        classes.add("share-bar");
        classes.add("saibamais");
        classes.add("lista-de-entidades");
        classes.add("veja-tambem");
        classes.add("barra-ferramentas");
        classes.add("ultimas-regiao");
        classes.add("mais-do-g1");
        return classes;
    }

    @Override
    public Set<String> getIrrelevantIds() {
        Set<String> ids = new HashSet<>();
        // IDs específicos do G1 a serem ignorados
        ids.add("glb-topo");
        ids.add("glb-barra");
        ids.add("glb-rodape");
        ids.add("comentarios");
        ids.add("ultimas-noticias");
        ids.add("mais-lidas");
        ids.add("lista-de-entidades");
        ids.add("tags");
        ids.add("comentarios");
        ids.add("boxe-assinatura");
        ids.add("boxe-relacionadas");
        return ids;
    }

    @Override
    public String getMainContentSelector() {
        // Seletor CSS para o conteúdo principal no G1
        return ".materia-conteudo";
    }

    /**
     * Extrai o chapéu (categoria principal) do conteúdo do G1
     */
    private String extrairChapeu(String content) {
        Matcher matcher = PATTERN_CHAPEU.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * Extrai o autor da matéria do G1
     */
    private String extrairAutor(String content) {
        Matcher matcher = PATTERN_AUTOR.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "G1";
    }

    /**
     * Extrai a data de publicação da matéria do G1
     */
    private String extrairDataPublicacao(String content) {
        Matcher matcher = PATTERN_DATA.matcher(content);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH'h'mm")
                .format(LocalDateTime.now());
    }

    /**
     * Extrai palavras-chave específicas do conteúdo do G1
     */
    private String[] extrairPalavrasChaveG1(String content) {
        // Detectar palavras mais relevantes baseado no conteúdo e título

        // Implementação simplificada para demonstração
        // Aqui seria interessante utilizar NLP para extração de entidades

        String[] palavrasCandidatas = content.replaceAll("[^\\p{L}\\s]", " ")
                .toLowerCase()
                .split("\\s+");

        // Remover palavras comuns (implementação simplificada)
        Set<String> stopWords = Set.of("o", "a", "os", "as", "de", "da", "do",
                "para", "que", "em", "um", "uma");

        return Arrays.stream(palavrasCandidatas)
                .filter(p -> p.length() > 3)
                .filter(p -> !stopWords.contains(p))
                .distinct()
                .limit(5)
                .toArray(String[]::new);
    }

    /**
     * Identifica a categoria da matéria do G1
     */
    private String identificarCategoriaG1(String content, String chapeu) {
        // Primeira tentativa: usar o chapéu como categoria
        if (!chapeu.isEmpty()) {
            for (String categoria : CATEGORIAS_G1) {
                if (chapeu.contains(categoria)) {
                    return categoria;
                }
            }
        }

        // Segunda tentativa: verificar no conteúdo
        String contentLower = content.toLowerCase();
        for (String categoria : CATEGORIAS_G1) {
            if (contentLower.contains(categoria.toLowerCase())) {
                return categoria;
            }
        }

        // Padrão
        return "Notícias";
    }

    /**
     * Gera um resumo específico para matérias do G1
     */
    private String gerarResumoG1(String content) {
        // Implementação simplificada
        // Para uma implementação real, usar técnicas de NLP para sumarização

        // Pegar os primeiros 2-3 parágrafos (normalmente o lide da notícia)
        String[] paragrafos = content.split("\n\n");

        if (paragrafos.length > 0) {
            if (paragrafos.length >= 2) {
                return paragrafos[0] + "\n" + paragrafos[1];
            }
            return paragrafos[0];
        }

        // Caso não tenha parágrafos definidos, pegar as primeiras 300 caracteres
        if (content.length() > 300) {
            return content.substring(0, 300) + "...";
        }

        return content;
    }

    /**
     * Formata o conteúdo processado em um formato estruturado para o G1
     */
    private String formatarConteudoG1(String originalContent, String chapeu,
                                      String autor, String dataPublicacao,
                                      String[] palavrasChave, String categoria,
                                      String resumo) {
        StringBuilder formatted = new StringBuilder();

        // Cabeçalho com informações estruturadas
        formatted.append("===== MATÉRIA DO G1 PROCESSADA =====\n\n");

        if (!chapeu.isEmpty()) {
            formatted.append("[CHAPÉU: ").append(chapeu).append("]\n");
        }

        formatted.append("[CATEGORIA: ").append(categoria).append("]\n");
        formatted.append("[AUTOR: ").append(autor).append("]\n");
        formatted.append("[DATA: ").append(dataPublicacao).append("]\n\n");

        // Palavras-chave
        if (palavrasChave.length > 0) {
            formatted.append("[PALAVRAS-CHAVE: ");
            for (int i = 0; i < palavrasChave.length; i++) {
                if (i > 0) formatted.append(", ");
                formatted.append(palavrasChave[i]);
            }
            formatted.append("]\n\n");
        }

        // Resumo
        formatted.append("[RESUMO]\n").append(resumo).append("\n\n");

        // Conteúdo original
        formatted.append("[CONTEÚDO COMPLETO]\n").append(originalContent);

        return formatted.toString();
    }

    /**
     * Registra estatísticas específicas para o processamento de conteúdo do G1
     */
    private void registrarEstatisticasG1(String originalContent, String enhancedContent,
                                         String[] palavrasChave, String categoria,
                                         long processingTime, String autor, String data) {
        stats.registrarProcessamento(
                originalContent,
                enhancedContent,
                palavrasChave.length,
                categoria,
                processingTime
        );

        // Estatísticas específicas do G1 (exemplo)
        stats.adicionarInformacaoPersonalizada("Portal", "G1");
        stats.adicionarInformacaoPersonalizada("Autor", autor);
        stats.adicionarInformacaoPersonalizada("Data de Publicação", data);
    }
}