package meuparser.ia;

import meuparser.ia.nlp.*;
import java.util.*;
import java.util.stream.Collectors;
import java.text.BreakIterator;

/**
 * ContentSummarizer com suporte adaptativo baseado em categorias de conteúdo
 * Sistema inteligente que ajusta a compressão conforme o tipo de página
 * VERSÃO ADAPTATIVA - Diferentes ratios por categoria para melhor UX
 */
public class ContentSummarizer {

    private INLPSummarizer nlpSummarizer;
    private boolean nlpEnabled = false;

    // =================== PARÂMETROS MÉTRICOS ADAPTATIVOS ===================
    private static final int DEFAULT_SUMMARY_SENTENCES = 4; // Aumentado de 3
    private static final int MIN_SENTENCE_LENGTH = 20;
    private static final int MAX_SENTENCE_LENGTH = 200;

    // 🎯 SISTEMA ADAPTATIVO - Diferentes ratios por tipo de conteúdo
    private static final Map<ContentClassifier.ContentCategory, Double> ADAPTIVE_COMPRESSION_RATIOS = new HashMap<ContentClassifier.ContentCategory, Double>() {{
        put(ContentClassifier.ContentCategory.NEWS, 0.55);           // 45% redução - mantém contexto jornalístico
        put(ContentClassifier.ContentCategory.ARTICLE, 0.65);        // 35% redução - preserva detalhes técnicos
        put(ContentClassifier.ContentCategory.EDUCATIONAL, 0.70);    // 30% redução - mantém conteúdo didático
        put(ContentClassifier.ContentCategory.E_COMMERCE, 0.80);     // 20% redução - preserva info de produtos
        put(ContentClassifier.ContentCategory.FORM, 0.90);           // 10% redução - mantém instruçãµes críticas
        put(ContentClassifier.ContentCategory.BLOG, 0.60);           // 40% redução - equilibra personalidade e info
        put(ContentClassifier.ContentCategory.NAVIGATION, 0.50);     // 50% redução - simplifica menus
        put(ContentClassifier.ContentCategory.UNKNOWN, 0.60);        // 40% redução - valor seguro padrão
    }};

    private static final double DEFAULT_COMPRESSION_RATIO = 0.60; // 40% redução (mais conservador)

    // Palavras vazias em portuguãªs (stopwords)
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "o", "a", "os", "as", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das",
            "em", "no", "na", "nos", "nas", "para", "por", "com", "sem", "sob", "sobre",
            "entre", "até", "desde", "durante", "apã³s", "antes", "depois", "contra",
            "e", "ou", "mas", "porém", "contudo", "entretanto", "todavia", "ainda", "já",
            "não", "sim", "também", "muito", "mais", "menos", "bem", "mal", "melhor", "pior",
            "que", "qual", "quando", "onde", "como", "porque", "se", "caso", "embora",
            "este", "esta", "estes", "estas", "esse", "essa", "esses", "essas",
            "aquele", "aquela", "aqueles", "aquelas", "isto", "isso", "aquilo",
            "eu", "tu", "ele", "ela", "nã³s", "vã³s", "eles", "elas", "me", "te", "se",
            "nos", "vos", "lhe", "lhes", "meu", "minha", "meus", "minhas", "seu", "sua",
            "seus", "suas", "nosso", "nossa", "nossos", "nossas", "vosso", "vossa",
            "vossos", "vossas", "ser", "estar", "ter", "haver", "fazer", "dizer", "ir", "ver"
    ));

    /**
     * Representa uma sentença com seu score de relevã¢ncia
     */
    private static class ScoredSentence {
        public String text;
        public double score;
        public int position;

        public ScoredSentence(String text, double score, int position) {
            this.text = text;
            this.score = score;
            this.position = position;
        }
    }

    public ContentSummarizer() {
        try {
            // Tenta usar NLP avançado
            nlpSummarizer = NLPProviderFactory.createSummarizer();

            if (nlpSummarizer != null) {
                nlpSummarizer.initialize();
                nlpEnabled = nlpSummarizer.isReady();

                if (nlpEnabled) {
                    System.out.println("✅ ContentSummarizer adaptativo inicializado com: " +
                            nlpSummarizer.getProviderInfo().toString());
                } else {
                    System.out.println("âš ï¸ Sumarizador NLP não está pronto, usando algoritmo extrativo adaptativo");
                }
            } else {
                System.out.println("âš ï¸ Nenhum provedor NLP disponível, usando algoritmo extrativo adaptativo interno");
            }

        } catch (Exception e) {
            System.err.println("âš ï¸ NLP avançado não disponível, usando algoritmo extrativo adaptativo: " + e.getMessage());
            nlpEnabled = false;
        }
    }

    /**
     * Método principal de sumarização com categoria automática
     */
    public String generateSummary(String content) {
        // Classifica automaticamente o conteúdo
        ContentClassifier classifier = new ContentClassifier();
        ContentClassifier.ContentCategory category;
        category = classifier.classifyContent(content);

        return generateSummaryWithCategory(content, category);
    }


    public String generateSummaryWithCategory(String content, ContentClassifier.ContentCategory category) {
        long startTime = System.currentTimeMillis();
        String result = null;
        String method = "desconhecido";

        try {
            // PRIMEIRA TENTATIVA: NLP Avançado COM CATEGORIA CORRETA
            if (nlpEnabled && nlpSummarizer != null && nlpSummarizer.isReady()) {
                try {
                    // ✅ Ajusta número de sentenças baseado na categoria CORRETA
                    int targetSentences = getEmergencyTargetSentences(category);
                    result = nlpSummarizer.summarize(content, targetSentences);
                    method = "NLP avançado (" + NLPProviderFactory.getCurrentProvider() + ")";

                    // ✅ Log correto da categoria
                    System.out.printf("🎯 %s: %d chars → %d chars em %d ms\n",
                            category.name(), content.length(), result.length(),
                            System.currentTimeMillis() - startTime);

                } catch (Exception e) {
                    System.err.println("❌ Falha no NLP avançado: " + e.getMessage());
                    nlpEnabled = false;
                }
            }

            // ... resto permanece igual ...

            // ✅ Log final correto
            // ✅ TRUNCAMENTO FORÇADO para limites Braille
            result = applyBrailleCharacterLimit(result, category);
            method += " + truncamento Braille";

            long processingTime = System.currentTimeMillis() - startTime;
            double compressionRatio = ADAPTIVE_COMPRESSION_RATIOS.getOrDefault(category, DEFAULT_COMPRESSION_RATIO);

            System.out.printf("🎯 Sumarização %s via %s: %d ms (Original: %d chars → Resumo: %d chars, Ratio: %.2f)\n",
                    category.name(), method, processingTime, content.length(), result.length(), compressionRatio);

            return result;
        } finally {

        }
    }

    /**
     * Método legado para compatibilidade
     */
    public String summarize(String content) {
        return generateSummary(content);
    }

    /**
     * ALGORITMO EXTRATIVO ADAPTATIVO
     * Ajusta a compressão baseado no tipo de conteúdo identificado
     */
    private String extractiveSummarizeWithCategory(String content, ContentClassifier.ContentCategory category) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        // 1. LIMPEZA INICIAL
        String cleanContent = preprocessContent(content);
        if (cleanContent.length() < 200) { // Aumentado threshold
            return cleanContent;
        }

        // 2. TOKENIZAÇÃO EM SENTENÇAS
        List<String> sentences = extractSentences(cleanContent);
        if (sentences.size() <= getMinSentencesForCategory(category)) {
            return cleanContent;
        }

        // 3. CÁLCULO DE FREQUÊNCIA DE PALAVRAS
        Map<String, Double> wordFrequencies = calculateWordFrequencies(cleanContent);

        // 4. SCORING DAS SENTENÇAS com bonus por categoria
        List<ScoredSentence> scoredSentences = scoreSentencesWithCategory(sentences, wordFrequencies, category);

        // 5. SELEÇÃO ADAPTATIVA DAS MELHORES SENTENÇAS
        List<ScoredSentence> topSentences = selectTopSentencesAdaptive(
                scoredSentences, cleanContent.length(), category);

        // 6. MONTAGEM DO RESUMO FINAL
        return buildFinalSummary(topSentences);
    }

    /**
     * Fallback para algoritmo extrativo sem categoria (compatibilidade)
     */
    private String extractiveSummarize(String content) {
        return extractiveSummarizeWithCategory(content, ContentClassifier.ContentCategory.UNKNOWN);
    }

    /**
     * Implementação básica como fallback final
     */
    private String basicSummarize(String content) {
        if (content == null || content.trim().isEmpty()) return "";

        String[] sentences = content.split("(?<=[.!?])\\s+");
        int limit = Math.min(4, sentences.length); // Aumentado de 3 para 4
        return String.join(" ", Arrays.copyOf(sentences, limit));
    }

    // ========================= MÉTODOS ADAPTATIVOS =========================

    /**
     * Define número alvo de sentenças por categoria
     */
    private int getEmergencyTargetSentences(ContentClassifier.ContentCategory category) {
        switch (category) {
            case FORM: return 2;              // REDUZIDO: Era 8, agora 2
            case E_COMMERCE: return 2;        // REDUZIDO: Era 6, agora 2
            case EDUCATIONAL: return 3;       // REDUZIDO: Era 10, agora 3
            case NEWS: return 2;              // REDUZIDO: Era 6, agora 2
            case ARTICLE: return 3;           // REDUZIDO: Era 10, agora 3
            case BLOG: return 2;              // REDUZIDO: Era 5, agora 2
            case NAVIGATION: return 1;        // REDUZIDO: Era 4, agora 1
            default: return 2;                //REDUZIDO: Era 5, agora 2
        }
    }

    /**
     * EMERGÊNCIA: Aplica truncamento FORÇADO por caracteres para usuários Braille
     */
    private String applyBrailleCharacterLimit(String summary, ContentClassifier.ContentCategory category) {
        // Limites RÍGIDOS por categoria (baseado em tempo de leitura Braille)
        int charLimit;
        switch (category) {
            case NEWS: charLimit = 400; break;           // 8 min MAX
            case EDUCATIONAL: charLimit = 600; break;    // 12 min MAX
            case ARTICLE: charLimit = 500; break;        // 10 min MAX
            case E_COMMERCE: charLimit = 300; break;     // 6 min MAX
            case FORM: charLimit = 200; break;           // 4 min MAX
            case BLOG: charLimit = 350; break;           // 7 min MAX
            case NAVIGATION: charLimit = 150; break;     // 3 min MAX
            default: charLimit = 400; break;             // 8 min MAX
        }

        if (summary.length() <= charLimit) {
            return summary;
        }

        // Trunca no último ponto antes do limite
        String truncated = summary.substring(0, charLimit);
        int lastDot = truncated.lastIndexOf('.');

        if (lastDot > charLimit * 0.7) { // Pelo menos 70% do limite
            return truncated.substring(0, lastDot + 1);
        } else {
            return truncated + "...";
        }
    }

    /**
     * Define limites mínimos de sentenças por categoria
     */
    private int getMinSentencesForCategory(ContentClassifier.ContentCategory category) {
        switch (category) {
            case FORM: return 5; // AUMENTADO: Era 4, agora 5
            case E_COMMERCE: return 4; // AUMENTADO: Era 3, agora 4
            case EDUCATIONAL: return 6; // AUMENTADO: Era 4, agora 6
            case NEWS: return 4;  // AUMENTADO: Era 2, agora 4
            case ARTICLE: return 5; // AUMENTADO: Era 3, agora 5
            case BLOG: return 3; // AUMENTADO: Era 2, agora 3
            case NAVIGATION: return 2; // MANTIDO
            default: return 3; // AUMENTADO: Era 2, agora 3
        }
    }

    /**
     * Define limites máximos de sentenças por categoria
     */
    private int getMaxSentencesForCategory(ContentClassifier.ContentCategory category) {
        switch (category) {
            case FORM: return 12;           // AUMENTADO: Era 8, agora 12
            case E_COMMERCE: return 10;        //AUMENTADO: Era 6, agora 10
            case EDUCATIONAL: return 15;       // // AUMENTADO: Era 7, agora 15
            case NEWS: return 8;              // AUMENTADO: Era 6, agora 8
            case ARTICLE: return 10;           // AUMENTADO: Era 6, agora 10
            case BLOG: return 7;              // AUMENTADO: ERA 4, agora 7
            case NAVIGATION: return 5;        // AUMENTADO: Era 3, agora 5
            default: return 8;                // AUMENTADO: Era 5, agora 8
        }
    }

    // ========================= MÉTODOS DO ALGORITMO EXTRATIVO ADAPTATIVO =========================


    /**
     * Pré-processa o conteúdo removendo ruído
     */
    private String preprocessContent(String content) {
        return content
                .replaceAll("\\[INÍCIO DO CONTEÊDO PRINCIPAL\\]", "")
                .replaceAll("\\[FIM DO CONTEÊDO PRINCIPAL\\]", "")
                .replaceAll("\\[TÍTULO.*?\\]", "")
                .replaceAll("\\[LINK.*?\\]", "")
                .replaceAll("\\[CAMPO.*?\\]", "")
                .replaceAll("\\[IMAGEM.*?\\]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Extrai sentenças usando BreakIterator
     */
    private List<String> extractSentences(String text) {
        List<String> sentences = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(new Locale("pt", "BR"));
        iterator.setText(text);

        int start = iterator.first();
        int end = iterator.next();

        while (end != BreakIterator.DONE) {
            String sentence = text.substring(start, end).trim();

            // Filtra sentenças válidas
            if (sentence.length() >= MIN_SENTENCE_LENGTH &&
                    sentence.length() <= MAX_SENTENCE_LENGTH &&
                    !sentence.matches("^[\\d\\s\\p{Punct}]+$")) {
                sentences.add(sentence);
            }

            start = end;
            end = iterator.next();
        }

        return sentences;
    }

    /**
     * Calcula frequãªncias das palavras significativas
     */
    private Map<String, Double> calculateWordFrequencies(String text) {
        Map<String, Integer> wordCounts = new HashMap<>();
        String[] words = text.toLowerCase().split("\\W+");

        for (String word : words) {
            if (word.length() >= 3 && !STOP_WORDS.contains(word) && !word.matches("\\d+")) {
                wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
            }
        }

        int maxCount = wordCounts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        Map<String, Double> frequencies = new HashMap<>();

        for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
            frequencies.put(entry.getKey(), (double) entry.getValue() / maxCount);
        }

        return frequencies;
    }

    /**
     * Atribui scores ã s sentenças com bonus específico por categoria
     */
    private List<ScoredSentence> scoreSentencesWithCategory(List<String> sentences,
                                                            Map<String, Double> wordFreq,
                                                            ContentClassifier.ContentCategory category) {
        List<ScoredSentence> scored = new ArrayList<>();

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            double score = calculateSentenceScoreWithCategory(sentence, wordFreq, i, sentences.size(), category);
            scored.add(new ScoredSentence(sentence, score, i));
        }

        return scored;
    }

    /**
     * Calcula score de uma sentença individual com bonus por categoria
     */
    private double calculateSentenceScoreWithCategory(String sentence, Map<String, Double> wordFreq,
                                                      int position, int totalSentences,
                                                      ContentClassifier.ContentCategory category) {
        String[] words = sentence.toLowerCase().split("\\W+");
        double totalScore = 0.0;
        int significantWords = 0;

        // 1. Score por frequãªncia de palavras
        for (String word : words) {
            if (word.length() >= 3 && !STOP_WORDS.contains(word) && wordFreq.containsKey(word)) {
                totalScore += wordFreq.get(word);
                significantWords++;
            }
        }

        if (significantWords == 0) return 0.0;

        double avgWordScore = totalScore / significantWords;

        // 2. Bonus por posição (adaptativo por categoria)
        double positionScore = getPositionScoreForCategory(position, totalSentences, category);

        // 3. Penalidade por sentenças muito curtas ou muito longas
        double lengthScore = 1.0;
        if (sentence.length() < 50) {
            lengthScore = 0.8;
        } else if (sentence.length() > 150) {
            lengthScore = 0.9;
        }

        // 4. Bonus específico por categoria
        double categoryBonus = getCategorySpecificBonus(sentence, category);

        return avgWordScore * positionScore * lengthScore * categoryBonus;
    }

    /**
     * Calcula score de posição adaptativo por categoria
     */
    private double getPositionScoreForCategory(int position, int totalSentences,
                                               ContentClassifier.ContentCategory category) {
        double positionScore = 1.0;

        switch (category) {
            case NEWS:
                // Notícias: Primeira sentença (lead) é crítica
                if (position == 0) positionScore = 2.0;
                else if (position == 1) positionScore = 1.5;
                else if (position < totalSentences * 0.3) positionScore = 1.3;
                break;

            case ARTICLE:
            case EDUCATIONAL:
                // Artigos: Início e conclusão importantes
                if (position == 0) positionScore = 1.8;
                else if (position < totalSentences * 0.2) positionScore = 1.4;
                else if (position > totalSentences * 0.8) positionScore = 1.3;
                break;

            case FORM:
                // Formulários: Instruçãµes iniciais são críticas
                if (position < totalSentences * 0.3) positionScore = 1.6;
                break;

            case E_COMMERCE:
                // E-commerce: Descrição principal no início
                if (position < totalSentences * 0.4) positionScore = 1.5;
                break;

            default:
                // Padrão original
                if (position == 0) positionScore = 1.5;
                else if (position < totalSentences * 0.2) positionScore = 1.3;
                else if (position > totalSentences * 0.8) positionScore = 1.2;
        }

        return positionScore;
    }

    /**
     * Bonus específico por categoria baseado no conteúdo da sentença
     */
    private double getCategorySpecificBonus(String sentence, ContentClassifier.ContentCategory category) {
        String lowerSentence = sentence.toLowerCase();

        switch (category) {
            case NEWS:
                // Bonus para sentenças jornalísticas
                if (lowerSentence.matches(".*\\b(segundo|de acordo|informou|declarou|anunciou)\\b.*")) return 1.2;
                if (lowerSentence.matches(".*\\d+.*")) return 1.1; // Dados numéricos
                break;

            case E_COMMERCE:
                // Bonus para informaçãµes de produto
                if (lowerSentence.matches(".*\\b(preço|valor|custo|desconto|promoção)\\b.*")) return 1.3;
                if (lowerSentence.matches(".*\\b(características|especificaçãµes|dimensãµes)\\b.*")) return 1.2;
                break;

            case FORM:
                // Bonus para instruçãµes críticas
                if (lowerSentence.matches(".*\\b(obrigatã³rio|necessário|importante|atenção)\\b.*")) return 1.4;
                if (lowerSentence.matches(".*\\b(preencha|digite|selecione|clique)\\b.*")) return 1.2;
                break;

            case EDUCATIONAL:
                // Bonus para conteúdo didático
                if (lowerSentence.matches(".*\\b(conceito|definição|exemplo|importante)\\b.*")) return 1.3;
                if (lowerSentence.matches(".*\\b(primeiro|segundo|terceiro|finalmente)\\b.*")) return 1.1;
                break;
        }

        // Bonus padrão para sentenças com números (dados específicos)
        return lowerSentence.matches(".*\\d+.*") ? 1.1 : 1.0;
    }

    /**
     * Seleção adaptativa baseada no tipo de conteúdo
     */
    private List<ScoredSentence> selectTopSentencesAdaptive(List<ScoredSentence> scoredSentences,
                                                            int originalLength,
                                                            ContentClassifier.ContentCategory category) {
        // Ordena por score decrescente
        scoredSentences.sort((a, b) -> Double.compare(b.score, a.score));

        // 🎯 COMPRESSÃO ADAPTATIVA por categoria
        double compressionRatio = ADAPTIVE_COMPRESSION_RATIOS.getOrDefault(category, DEFAULT_COMPRESSION_RATIO);

        // ðŸ“ Calcula tamanho alvo
        int targetLength = (int) (originalLength * compressionRatio);

        // ðŸ“Š Logging para análise
        System.out.printf("🎯 Compressão adaptativa: %s → ratio %.2f (alvo: %d chars de %d originais)\n",
                category.name(), compressionRatio, targetLength, originalLength);

        List<ScoredSentence> selected = new ArrayList<>();
        int currentLength = 0;
        int minSentences = getMinSentencesForCategory(category);

        for (ScoredSentence sentence : scoredSentences) {
            boolean shouldInclude = false;

            // Inclui se ainda há espaço OU se não atingiu o mínimo
            if (currentLength + sentence.text.length() <= targetLength || selected.size() < minSentences) {
                shouldInclude = true;
            }

            if (shouldInclude) {
                selected.add(sentence);
                currentLength += sentence.text.length();
            }

            // ðŸ›¡ï¸ Limite máximo baseado na categoria
            int maxSentences = getMaxSentencesForCategory(category);
            if (selected.size() >= maxSentences) break;
        }

        // Garante pelo menos o mínimo de sentenças
        while (selected.size() < minSentences && selected.size() < scoredSentences.size()) {
            for (ScoredSentence sentence : scoredSentences) {
                if (!selected.contains(sentence)) {
                    selected.add(sentence);
                    break;
                }
            }
        }

        // Reordena pelas posiçãµes originais
        selected.sort((a, b) -> Integer.compare(a.position, b.position));
        return selected;
    }

    /**
     * Constrã³i o resumo final juntando as sentenças selecionadas
     */
    private String buildFinalSummary(List<ScoredSentence> sentences) {
        if (sentences.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder();

        for (int i = 0; i < sentences.size(); i++) {
            summary.append(sentences.get(i).text);

            // Adiciona espaçamento entre sentenças
            if (i < sentences.size() - 1) {
                if (!sentences.get(i).text.endsWith(".") &&
                        !sentences.get(i).text.endsWith("!") &&
                        !sentences.get(i).text.endsWith("?")) {
                    summary.append(".");
                }
                summary.append(" ");
            }
        }

        return summary.toString().trim();
    }

    /**
     * Método utilitário para obter estatísticas de sumarização adaptativa
     */
    public AdaptiveSummarizationStats getAdaptiveStatistics(String original, String summary,
                                                            ContentClassifier.ContentCategory category) {
        double expectedRatio = ADAPTIVE_COMPRESSION_RATIOS.getOrDefault(category, DEFAULT_COMPRESSION_RATIO);
        double actualRatio = (double) summary.length() / original.length();
        double compressionEfficiency = actualRatio / expectedRatio;

        return new AdaptiveSummarizationStats(
                original.length(),
                summary.length(),
                actualRatio,
                extractSentences(original).size(),
                extractSentences(summary).size(),
                category,
                expectedRatio,
                compressionEfficiency
        );
    }

    /**
     * Classe para estatísticas de sumarização adaptativa
     */
    public static class AdaptiveSummarizationStats {
        public final int originalLength;
        public final int summaryLength;
        public final double actualCompressionRatio;
        public final int originalSentences;
        public final int summarySentences;
        public final ContentClassifier.ContentCategory category;
        public final double expectedCompressionRatio;
        public final double compressionEfficiency;

        public AdaptiveSummarizationStats(int originalLength, int summaryLength, double actualCompressionRatio,
                                          int originalSentences, int summarySentences,
                                          ContentClassifier.ContentCategory category,
                                          double expectedCompressionRatio, double compressionEfficiency) {
            this.originalLength = originalLength;
            this.summaryLength = summaryLength;
            this.actualCompressionRatio = actualCompressionRatio;
            this.originalSentences = originalSentences;
            this.summarySentences = summarySentences;
            this.category = category;
            this.expectedCompressionRatio = expectedCompressionRatio;
            this.compressionEfficiency = compressionEfficiency;
        }

        @Override
        public String toString() {
            return String.format("AdaptiveStats[%s: %d→%d chars (%.1f%% vs %.1f%% esperado), %d→%d sentences, Efficiency: %.2f]",
                    category.name(), originalLength, summaryLength,
                    actualCompressionRatio * 100, expectedCompressionRatio * 100,
                    originalSentences, summarySentences, compressionEfficiency);
        }
    }

    /**
     * Permite trocar provedor em runtime
     */
    public void switchProvider(NLPProviderFactory.NLPProvider provider) {
        try {
            if (nlpSummarizer != null) {
                nlpSummarizer.cleanup();
            }

            NLPProviderFactory.setProvider(provider);
            nlpSummarizer = NLPProviderFactory.createSummarizer();

            if (nlpSummarizer != null) {
                nlpSummarizer.initialize();
                nlpEnabled = nlpSummarizer.isReady();
                System.out.println("ðŸ”„ Provedor trocado para: " + nlpSummarizer.getProviderInfo().name);
            } else {
                nlpEnabled = false;
                System.out.println("âŒ Provedor não disponível, usando algoritmo extrativo adaptativo");
            }

        } catch (Exception e) {
            System.err.println("âŒ Falha ao trocar provedor: " + e.getMessage());
            nlpEnabled = false;
        }
    }

    public void cleanup() {
        if (nlpSummarizer != null) {
            nlpSummarizer.cleanup();
        }
    }
}

/** (MODELO ANTIGO SEM ADAPTATIVIDADE) package meuparser.ia;

 import meuparser.ia.nlp.*;
 import java.util.*;
 import java.util.stream.Collectors;
 import java.text.BreakIterator;

 /**
 * ContentSummarizer com suporte a múltiplos provedores de NLP
 * Automaticamente seleciona a melhor implementação disponível
 * COM ALGORITMO EXTRATIVO COMPLETO PARA FALLBACK
 */
/**public class ContentSummarizer {

 private INLPSummarizer nlpSummarizer;
 private boolean nlpEnabled = false;

 // =================== PARÂMETROS MÉTRICOS DO ALGORITMO EXTRATIVO ===================
 private static final int DEFAULT_SUMMARY_SENTENCES = 3;
 private static final int MIN_SENTENCE_LENGTH = 20;
 private static final int MAX_SENTENCE_LENGTH = 200;
 private static final double COMPRESSION_RATIO = 0.3; // 30% do texto original

 // Palavras vazias em portuguãªs (stopwords) - MANTÉM OS MESMOS PARÂMETROS
 private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
 "o", "a", "os", "as", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das",
 "em", "no", "na", "nos", "nas", "para", "por", "com", "sem", "sob", "sobre",
 "entre", "até", "desde", "durante", "apã³s", "antes", "depois", "contra",
 "e", "ou", "mas", "porém", "contudo", "entretanto", "todavia", "ainda", "já",
 "não", "sim", "também", "muito", "mais", "menos", "bem", "mal", "melhor", "pior",
 "que", "qual", "quando", "onde", "como", "porque", "se", "caso", "embora",
 "este", "esta", "estes", "estas", "esse", "essa", "esses", "essas",
 "aquele", "aquela", "aqueles", "aquelas", "isto", "isso", "aquilo",
 "eu", "tu", "ele", "ela", "nã³s", "vã³s", "eles", "elas", "me", "te", "se",
 "nos", "vos", "lhe", "lhes", "meu", "minha", "meus", "minhas", "seu", "sua",
 "seus", "suas", "nosso", "nossa", "nossos", "nossas", "vosso", "vossa",
 "vossos", "vossas", "ser", "estar", "ter", "haver", "fazer", "dizer", "ir", "ver"
 ));

 /**
 * Representa uma sentença com seu score de relevã¢ncia
 */
/** private static class ScoredSentence {
 public String text;
 public double score;
 public int position;

 public ScoredSentence(String text, double score, int position) {
 this.text = text;
 this.score = score;
 this.position = position;
 }
 }

 public ContentSummarizer() {
 try {
 // Tenta usar NLP avançado
 nlpSummarizer = NLPProviderFactory.createSummarizer();

 if (nlpSummarizer != null) {
 nlpSummarizer.initialize();
 nlpEnabled = nlpSummarizer.isReady();

 if (nlpEnabled) {
 System.out.println("✅ ContentSummarizer inicializado com: " +
 nlpSummarizer.getProviderInfo().toString());
 } else {
 System.out.println("âš ï¸ Sumarizador NLP não está pronto, usando algoritmo extrativo");
 }
 } else {
 System.out.println("âš ï¸ Nenhum provedor NLP disponível, usando algoritmo extrativo interno");
 }

 } catch (Exception e) {
 System.err.println("âš ï¸ NLP avançado não disponível, usando algoritmo extrativo: " + e.getMessage());
 nlpEnabled = false;
 }
 }

 /**
 * Método principal de sumarização com múltiplas estratégias de fallback
 */
/**  public String generateSummary(String content) {
 long startTime = System.currentTimeMillis();
 String result = null;
 String method = "desconhecido";

 try {
 // PRIMEIRA TENTATIVA: NLP Avançado
 if (nlpEnabled && nlpSummarizer != null && nlpSummarizer.isReady()) {
 try {
 result = nlpSummarizer.summarize(content, DEFAULT_SUMMARY_SENTENCES);
 method = "NLP avançado (" + NLPProviderFactory.getCurrentProvider() + ")";
 } catch (Exception e) {
 System.err.println("âŒ Falha no NLP avançado: " + e.getMessage());
 nlpEnabled = false; // Desabilita para prã³ximas tentativas
 }
 }

 // SEGUNDA TENTATIVA: Algoritmo Extrativo Completo (SEU Cã“DIGO ORIGINAL)
 if (result == null || result.trim().isEmpty()) {
 try {
 result = extractiveSummarize(content);
 method = "algoritmo extrativo avançado";
 } catch (Exception e) {
 System.err.println("âŒ Falha no algoritmo extrativo: " + e.getMessage());
 }
 }

 // TERCEIRA TENTATIVA: Fallback simples
 if (result == null || result.trim().isEmpty()) {
 result = basicSummarize(content);
 method = "fallback básico";
 }

 long processingTime = System.currentTimeMillis() - startTime;
 System.out.printf("🎯 Sumarização via %s: %d ms (Original: %d chars → Resumo: %d chars)\n",
 method, processingTime, content.length(), result.length());

 return result;

 } catch (Exception e) {
 System.err.println("âŒ Erro crítico na sumarização: " + e.getMessage());
 e.printStackTrace();
 // Fallback de emergãªncia
 return content.length() > 500 ? content.substring(0, 500) + "..." : content;
 }
 }

 /**
 * Método legado para compatibilidade
 */
/** public String summarize(String content) {
 return generateSummary(content);
 }

 /**
 * ALGORITMO EXTRATIVO COMPLETO (baseado no seu cã³digo original)
 * Implementa técnicas de scoring por frequãªncia de palavras e posição
 * MANTÉM TODOS OS PARÂMETROS MÉTRICOS ORIGINAIS
 */
/** private String extractiveSummarize(String content) {
 if (content == null || content.trim().isEmpty()) {
 return "";
 }

 // 1. LIMPEZA INICIAL
 String cleanContent = preprocessContent(content);
 if (cleanContent.length() < 100) {
 return cleanContent; // Conteúdo muito pequeno, retorna original
 }

 // 2. TOKENIZAÇÃO EM SENTENÇAS
 List<String> sentences = extractSentences(cleanContent);
 if (sentences.size() <= DEFAULT_SUMMARY_SENTENCES) {
 return cleanContent; // Já é pequeno suficiente
 }

 // 3. CÁLCULO DE FREQUÊNCIA DE PALAVRAS
 Map<String, Double> wordFrequencies = calculateWordFrequencies(cleanContent);

 // 4. SCORING DAS SENTENÇAS
 List<ScoredSentence> scoredSentences = scoreSentences(sentences, wordFrequencies);

 // 5. SELEÇÃO DAS MELHORES SENTENÇAS
 List<ScoredSentence> topSentences = selectTopSentences(scoredSentences, cleanContent.length());

 // 6. MONTAGEM DO RESUMO FINAL
 return buildFinalSummary(topSentences);
 }

 /**
 * Implementação básica como fallback final
 */
/** private String basicSummarize(String content) {
 if (content == null || content.trim().isEmpty()) return "";

 String[] sentences = content.split("(?<=[.!?])\\s+");
 int limit = Math.min(3, sentences.length);
 return String.join(" ", Arrays.copyOf(sentences, limit));
 }

 // ========================= MÉTODOS DO ALGORITMO EXTRATIVO (SEUS PARÂMETROS ORIGINAIS) =========================

 /**
 * Pré-processa o conteúdo removendo ruído
 */
/**  private String preprocessContent(String content) {
 return content
 .replaceAll("\\[INÍCIO DO CONTEÊDO PRINCIPAL\\]", "")
 .replaceAll("\\[FIM DO CONTEÊDO PRINCIPAL\\]", "")
 .replaceAll("\\[TÍTULO.*?\\]", "")
 .replaceAll("\\[LINK.*?\\]", "")
 .replaceAll("\\[CAMPO.*?\\]", "")
 .replaceAll("\\[IMAGEM.*?\\]", "")
 .replaceAll("\\s+", " ")
 .trim();
 }

 /**
 * Extrai sentenças usando BreakIterator
 */
/** private List<String> extractSentences(String text) {
 List<String> sentences = new ArrayList<>();
 BreakIterator iterator = BreakIterator.getSentenceInstance(new Locale("pt", "BR"));
 iterator.setText(text);

 int start = iterator.first();
 int end = iterator.next();

 while (end != BreakIterator.DONE) {
 String sentence = text.substring(start, end).trim();

 // Filtra sentenças válidas - MANTÉM PARÂMETROS ORIGINAIS
 if (sentence.length() >= MIN_SENTENCE_LENGTH &&
 sentence.length() <= MAX_SENTENCE_LENGTH &&
 !sentence.matches("^[\\d\\s\\p{Punct}]+$")) { // Não é sã³ números e pontuação
 sentences.add(sentence);
 }

 start = end;
 end = iterator.next();
 }

 return sentences;
 }

 /**
 * Calcula frequãªncias das palavras significativas
 */
/**private Map<String, Double> calculateWordFrequencies(String text) {
 Map<String, Integer> wordCounts = new HashMap<>();
 String[] words = text.toLowerCase().split("\\W+");

 // Conta palavras significativas
 for (String word : words) {
 if (word.length() >= 3 && !STOP_WORDS.contains(word) && !word.matches("\\d+")) {
 wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
 }
 }

 // Converte para frequãªncias normalizadas
 int maxCount = wordCounts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
 Map<String, Double> frequencies = new HashMap<>();

 for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
 frequencies.put(entry.getKey(), (double) entry.getValue() / maxCount);
 }

 return frequencies;
 }

 /**
 * Atribui scores ã s sentenças baseado em múltiplos fatores
 */
/**private List<ScoredSentence> scoreSentences(List<String> sentences, Map<String, Double> wordFreq) {
 List<ScoredSentence> scored = new ArrayList<>();

 for (int i = 0; i < sentences.size(); i++) {
 String sentence = sentences.get(i);
 double score = calculateSentenceScore(sentence, wordFreq, i, sentences.size());
 scored.add(new ScoredSentence(sentence, score, i));
 }

 return scored;
 }

 /**
 * Calcula score de uma sentença individual - MANTÉM TODOS OS PARÂMETROS ORIGINAIS
 */
/**private double calculateSentenceScore(String sentence, Map<String, Double> wordFreq,
 int position, int totalSentences) {
 String[] words = sentence.toLowerCase().split("\\W+");
 double totalScore = 0.0;
 int significantWords = 0;

 // 1. Score por frequãªncia de palavras
 for (String word : words) {
 if (word.length() >= 3 && !STOP_WORDS.contains(word) && wordFreq.containsKey(word)) {
 totalScore += wordFreq.get(word);
 significantWords++;
 }
 }

 if (significantWords == 0) return 0.0;

 double avgWordScore = totalScore / significantWords;

 // 2. Bonus por posição (início e fim tãªm maior relevã¢ncia) - PARÂMETROS ORIGINAIS
 double positionScore = 1.0;
 if (position == 0) {
 positionScore = 1.5; // Primeira sentença
 } else if (position < totalSentences * 0.2) {
 positionScore = 1.3; // 20% iniciais
 } else if (position > totalSentences * 0.8) {
 positionScore = 1.2; // 20% finais
 }

 // 3. Penalidade por sentenças muito curtas ou muito longas - PARÂMETROS ORIGINAIS
 double lengthScore = 1.0;
 if (sentence.length() < 50) {
 lengthScore = 0.8;
 } else if (sentence.length() > 150) {
 lengthScore = 0.9;
 }

 // 4. Bonus para sentenças com números (dados específicos) - PARÂMETRO ORIGINAL
 double numberBonus = sentence.matches(".*\\d+.*") ? 1.1 : 1.0;

 return avgWordScore * positionScore * lengthScore * numberBonus;
 }

 /**
 * Seleciona as melhores sentenças baseado no score e taxa de compressão
 */
/**private List<ScoredSentence> selectTopSentences(List<ScoredSentence> scoredSentences, int originalLength) {
 // Ordena por score decrescente
 scoredSentences.sort((a, b) -> Double.compare(b.score, a.score));

 // Calcula quantas sentenças incluir baseado na taxa de compressão - PARÂMETRO ORIGINAL
 int targetLength = (int) (originalLength * COMPRESSION_RATIO);
 List<ScoredSentence> selected = new ArrayList<>();
 int currentLength = 0;

 for (ScoredSentence sentence : scoredSentences) {
 if (currentLength + sentence.text.length() <= targetLength || selected.size() < 2) {
 selected.add(sentence);
 currentLength += sentence.text.length();
 }

 if (selected.size() >= DEFAULT_SUMMARY_SENTENCES * 2) break; // Limite máximo
 }

 // Reordena pelas posiçãµes originais
 selected.sort((a, b) -> Integer.compare(a.position, b.position));

 return selected;
 }

 /**
 * Constrã³i o resumo final juntando as sentenças selecionadas
 */
/** private String buildFinalSummary(List<ScoredSentence> sentences) {
 if (sentences.isEmpty()) {
 return "";
 }

 StringBuilder summary = new StringBuilder();

 for (int i = 0; i < sentences.size(); i++) {
 summary.append(sentences.get(i).text);

 // Adiciona espaçamento entre sentenças
 if (i < sentences.size() - 1) {
 if (!sentences.get(i).text.endsWith(".") &&
 !sentences.get(i).text.endsWith("!") &&
 !sentences.get(i).text.endsWith("?")) {
 summary.append(".");
 }
 summary.append(" ");
 }
 }

 return summary.toString().trim();
 }

 /**
 * Método utilitário para obter estatísticas de sumarização - MANTÉM ORIGINAL
 */
/** public SummarizationStats getStatistics(String original, String summary) {
 return new SummarizationStats(
 original.length(),
 summary.length(),
 (double) summary.length() / original.length(),
 extractSentences(original).size(),
 extractSentences(summary).size()
 );
 }

 /**
 * Classe para estatísticas de sumarização - MANTÉM ORIGINAL
 */
/** public static class SummarizationStats {
 public final int originalLength;
 public final int summaryLength;
 public final double compressionRatio;
 public final int originalSentences;
 public final int summarySentences;

 public SummarizationStats(int originalLength, int summaryLength, double compressionRatio,
 int originalSentences, int summarySentences) {
 this.originalLength = originalLength;
 this.summaryLength = summaryLength;
 this.compressionRatio = compressionRatio;
 this.originalSentences = originalSentences;
 this.summarySentences = summarySentences;
 }

 @Override
 public String toString() {
 return String.format("Stats[%d→%d chars (%.1f%%), %d→%d sentences]",
 originalLength, summaryLength, compressionRatio * 100,
 originalSentences, summarySentences);
 }
 }

 /**
  * Permite trocar provedor em runtime
 */
/** public void switchProvider(NLPProviderFactory.NLPProvider provider) {
 try {
 if (nlpSummarizer != null) {
 nlpSummarizer.cleanup();
 }

 NLPProviderFactory.setProvider(provider);
 nlpSummarizer = NLPProviderFactory.createSummarizer();

 if (nlpSummarizer != null) {
 nlpSummarizer.initialize();
 nlpEnabled = nlpSummarizer.isReady();
 System.out.println("ðŸ”„ Provedor trocado para: " + nlpSummarizer.getProviderInfo().name);
 } else {
 nlpEnabled = false;
 System.out.println("âŒ Provedor não disponível, usando algoritmo extrativo");
 }

 } catch (Exception e) {
 System.err.println("âŒ Falha ao trocar provedor: " + e.getMessage());
 nlpEnabled = false;
 }
 }

 public void cleanup() {
 if (nlpSummarizer != null) {
 nlpSummarizer.cleanup();
 }
 }
 }
 */