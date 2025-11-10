
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
 * Implementação do AIParserIntegrator específica para o portal Brasil Escola
 * Responsável por processar conteúdos educacionais do Brasil Escola
 */
public class BrasilEscolaParserIntegrator implements AIParserIntegrator {

    private final AIStats stats = new AIStats();
    private static final String NOME_INTEGRADOR = "BrasilEscolaParserIntegrator";

    // Padrões para extração de informações específicas do Brasil Escola
    private static final Pattern PATTERN_MATERIA = Pattern.compile("<h2[^>]*>([^<]+)</h2>");
    private static final Pattern PATTERN_AUTOR = Pattern.compile("Por\\s*:\\s*([^<]+)");
    private static final Pattern PATTERN_DISCIPLINA = Pattern.compile("Disciplina:([^<]+)");

    // Constantes para categorias do Brasil Escola
    private static final String[] DISCIPLINAS = {
            "Português", "Matemática", "História", "Geografia", "Biologia",
            "Física", "Química", "Filosofia", "Sociologia", "Artes",
            "Educação Física", "Literatura", "Inglês", "Espanhol"
    };

    @Override
    public Optional<String> processContent(MeuParser parser) {
        logInfo("Iniciando processamento de conteúdo do Brasil Escola");

        try {
            // Validação de entrada
            if (parser == null) {
                logErro("Parser nulo recebido, impossível processar conteúdo");
                throw new IllegalArgumentException("O parser não pode ser nulo");
            }

            if (parser.getErro()) {
                logErro("Parser contém erro, abortando processamento");
                return Optional.empty();
            }

            String originalContent = parser.getTexto();
            if (originalContent == null || originalContent.isEmpty()) {
                logErro("Conteúdo vazio ou nulo, abortando processamento");
                return Optional.empty();
            }

            // Registra o tempo de início do processamento
            long startTime = System.currentTimeMillis();
            logInfo("Iniciando etapas de processamento no conteúdo de tamanho: " + originalContent.length());

            // Extração de metadados específicos do Brasil Escola
            String materia = extrairMateria(originalContent);
            String autor = extrairAutor(originalContent);
            String disciplina = extrairDisciplina(originalContent);
            String[] palavrasChave = extrairPalavrasChave(originalContent);
            String resumo = gerarResumo(originalContent);

            // Formata o conteúdo processado
            String conteudoProcessado = formatarConteudo(
                    originalContent, materia, autor, disciplina,
                    palavrasChave, resumo
            );

            // Registra estatísticas
            long processingTime = System.currentTimeMillis() - startTime;
            registrarEstatisticas(
                    originalContent, conteudoProcessado, palavrasChave,
                    disciplina, processingTime, autor
            );

            // Atualiza o parser com o conteúdo processado
            parser.setTexto(conteudoProcessado);

            logInfo("Processamento concluído com sucesso em " + processingTime + "ms");
            return Optional.of(conteudoProcessado);
        } catch (Exception e) {
            logErro("Erro durante processamento: " + e.getMessage(), e);
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
        // Classes específicas do Brasil Escola a serem ignoradas
        classes.add("publicidade");
        classes.add("banner");
        classes.add("menu-principal");
        classes.add("rodape");
        classes.add("navegacao");
        classes.add("barra-topo");
        classes.add("recomendados");
        classes.add("veja-tambem");
        classes.add("box-ferramentas");
        classes.add("area-assinatura");
        classes.add("comentarios");
        classes.add("tags");
        classes.add("box-newsletter");
        classes.add("box-vestibular");
        classes.add("menu-fixo");
        return classes;
    }

    @Override
    public Set<String> getIrrelevantIds() {
        Set<String> ids = new HashSet<>();
        // IDs específicos do Brasil Escola a serem ignorados
        ids.add("topo");
        ids.add("menu");
        ids.add("rodape");
        ids.add("header");
        ids.add("publicidade");
        ids.add("lateral");
        ids.add("barra-ferramentas");
        ids.add("comentarios");
        ids.add("box-newsletter");
        ids.add("relacionadas");
        ids.add("mais-materias");
        return ids;
    }

    @Override
    public String getMainContentSelector() {
        // Seletor CSS para o conteúdo principal no Brasil Escola
        return ".texto-materia";
    }

    /**
     * Extrai o título da matéria/conteúdo
     */
    private String extrairMateria(String content) {
        try {
            Matcher matcher = PATTERN_MATERIA.matcher(content);
            if (matcher.find()) {
                logInfo("Matéria extraída com sucesso");
                return matcher.group(1).trim();
            }
            logAviso("Não foi possível extrair o título da matéria");
            return "";
        } catch (Exception e) {
            logErro("Erro ao extrair título da matéria", e);
            return "";
        }
    }

    /**
     * Extrai o autor do conteúdo
     */
    private String extrairAutor(String content) {
        try {
            Matcher matcher = PATTERN_AUTOR.matcher(content);
            if (matcher.find()) {
                logInfo("Autor extraído com sucesso");
                return matcher.group(1).trim();
            }
            logAviso("Não foi possível extrair o autor, usando valor padrão");
            return "Equipe Brasil Escola";
        } catch (Exception e) {
            logErro("Erro ao extrair autor", e);
            return "Equipe Brasil Escola";
        }
    }

    /**
     * Extrai a disciplina relacionada ao conteúdo
     */
    private String extrairDisciplina(String content) {
        try {
            // Primeiro tenta extrair usando o padrão específico
            Matcher matcher = PATTERN_DISCIPLINA.matcher(content);
            if (matcher.find()) {
                logInfo("Disciplina extraída com sucesso do padrão específico");
                return matcher.group(1).trim();
            }

            // Se não encontrar usando padrão, tenta identificar por menções no texto
            String contentLower = content.toLowerCase();
            for (String disciplina : DISCIPLINAS) {
                if (contentLower.contains(disciplina.toLowerCase())) {
                    logInfo("Disciplina identificada por menção no texto: " + disciplina);
                    return disciplina;
                }
            }

            logAviso("Não foi possível determinar a disciplina, usando valor padrão");
            return "Educação";
        } catch (Exception e) {
            logErro("Erro ao extrair disciplina", e);
            return "Educação";
        }
    }

    /**
     * Extrai palavras-chave do conteúdo
     */
    private String[] extrairPalavrasChave(String content) {
        try {
            // Implementação simplificada para extração de palavras-chave
            // Em uma implementação real, usaria técnicas de NLP mais avançadas

            // Remove tags HTML e caracteres especiais
            String textoLimpo = content.replaceAll("<[^>]*>", " ")
                    .replaceAll("[^\\p{L}\\s]", " ")
                    .toLowerCase();

            // Divide em palavras e remove palavras comuns
            Set<String> stopWords = Set.of("o", "a", "os", "as", "de", "da", "do",
                    "para", "que", "em", "um", "uma", "e", "é");

            String[] palavras = textoLimpo.split("\\s+");
            String[] resultado = Arrays.stream(palavras)
                    .filter(p -> p.length() > 3)
                    .filter(p -> !stopWords.contains(p))
                    .distinct()
                    .limit(7)
                    .toArray(String[]::new);

            logInfo("Extraídas " + resultado.length + " palavras-chave");
            return resultado;
        } catch (Exception e) {
            logErro("Erro ao extrair palavras-chave", e);
            return new String[]{"educação", "brasil", "escola"};
        }
    }

    /**
     * Gera um resumo do conteúdo
     */
    private String gerarResumo(String content) {
        try {
            // Remover tags HTML
            String textoLimpo = content.replaceAll("<[^>]*>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            // Para uma implementação simples, pega as primeiras 300 caracteres
            // Em implementação real, usaria técnicas de sumarização mais sofisticadas
            if (textoLimpo.length() > 300) {
                String resumo = textoLimpo.substring(0, 300) + "...";
                logInfo("Resumo gerado com sucesso");
                return resumo;
            }

            logInfo("Resumo gerado (conteúdo completo por ser pequeno)");
            return textoLimpo;
        } catch (Exception e) {
            logErro("Erro ao gerar resumo", e);
            return "Não foi possível gerar um resumo para este conteúdo.";
        }
    }

    /**
     * Formata o conteúdo processado em formato estruturado
     */
    private String formatarConteudo(String originalContent, String materia,
                                    String autor, String disciplina,
                                    String[] palavrasChave, String resumo) {
        try {
            StringBuilder formatted = new StringBuilder();

            // Cabeçalho
            formatted.append("===== CONTEÚDO BRASIL ESCOLA PROCESSADO =====\n\n");

            // Título da matéria
            if (!materia.isEmpty()) {
                formatted.append("[TÍTULO: ").append(materia).append("]\n");
            }

            // Metadados
            formatted.append("[DISCIPLINA: ").append(disciplina).append("]\n");
            formatted.append("[AUTOR: ").append(autor).append("]\n");
            formatted.append("[DATA DE PROCESSAMENTO: ")
                    .append(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                            .format(LocalDateTime.now()))
                    .append("]\n\n");

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

            // Conteúdo original limpo (poderia incluir aqui rotinas para remover ads, etc.)
            formatted.append("[CONTEÚDO COMPLETO]\n").append(originalContent);

            logInfo("Conteúdo formatado com sucesso");
            return formatted.toString();
        } catch (Exception e) {
            logErro("Erro ao formatar conteúdo", e);
            // Em caso de erro, retorna o conteúdo original
            return originalContent;
        }
    }

    /**
     * Registra estatísticas do processamento
     */
    private void registrarEstatisticas(String originalContent, String enhancedContent,
                                       String[] palavrasChave, String disciplina,
                                       long processingTime, String autor) {
        try {
            stats.registrarProcessamento(
                    originalContent,
                    enhancedContent,
                    palavrasChave.length,
                    disciplina,
                    processingTime
            );

            // Adiciona informações específicas do Brasil Escola
            stats.adicionarInformacaoPersonalizada("Portal", "Brasil Escola");
            stats.adicionarInformacaoPersonalizada("Autor", autor);
            stats.adicionarInformacaoPersonalizada("Disciplina", disciplina);

            logInfo("Estatísticas registradas com sucesso");
        } catch (Exception e) {
            logErro("Erro ao registrar estatísticas", e);
        }
    }

    // Métodos de log (a serem substituídos pela implementação Log4J)

    /**
     * Registra informação de log
     */
    private void logInfo(String mensagem) {
        // Implementação temporária até Log4J ser implementado
        System.out.println("[INFO] " + NOME_INTEGRADOR + " - " + mensagem);
    }

    /**
     * Registra aviso de log
     */
    private void logAviso(String mensagem) {
        // Implementação temporária até Log4J ser implementado
        System.out.println("[AVISO] " + NOME_INTEGRADOR + " - " + mensagem);
    }

    /**
     * Registra erro de log sem exceção
     */
    private void logErro(String mensagem) {
        // Implementação temporária até Log4J ser implementado
        System.err.println("[ERRO] " + NOME_INTEGRADOR + " - " + mensagem);
    }

    /**
     * Registra erro de log com exceção
     */
    private void logErro(String mensagem, Exception e) {
        // Implementação temporária até Log4J ser implementado
        System.err.println("[ERRO] " + NOME_INTEGRADOR + " - " + mensagem);
        e.printStackTrace();
    }
}