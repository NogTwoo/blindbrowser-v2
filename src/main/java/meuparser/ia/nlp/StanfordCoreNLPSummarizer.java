package meuparser.ia.nlp;

import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.util.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sumarizador baseado em Stanford CoreNLP - VERSãƒO CORRIGIDA
 * Configuraã§ã£o simplificada que funciona sem modelos especã­ficos
 */
public class StanfordCoreNLPSummarizer implements INLPSummarizer {

    private StanfordCoreNLP pipeline;
    private boolean initialized = false;
    private final Object initLock = new Object();

    // Evita tentativas repetidas apã³s falha permanente
    private static volatile boolean permanentlyDisabled = false;

    /**
     * Configuraã§ã£o BãSICA que funciona sem modelos especã­ficos de portuguãªs
     */
    private Properties createPipelineProperties() {
        Properties props = new Properties();

        // REMOVE configuraã§ãµes problemã¡ticas de portuguãªs:
        // - NãƒO usar "tokenize.language=Portuguese"
        // - NãƒO usar modelos especã­ficos que podem nã£o existir

        // Pipeline mã­nimo mas funcional
        props.setProperty("annotators", "tokenize,ssplit");

        // Configuraã§ãµes bã¡sicas
        props.setProperty("threads", "1");
        props.setProperty("outputFormat", "text");

        return props;
    }

    @Override
    public void initialize() throws Exception {
        synchronized (initLock) {
            if (initialized || permanentlyDisabled) return;

            System.out.println("ðŸš€ Inicializando Stanford CoreNLP...");
            long startTime = System.currentTimeMillis();

            try {
                // Primeira tentativa: configuraã§ã£o bã¡sica
                Properties props = createPipelineProperties();
                pipeline = new StanfordCoreNLP(props);
                initialized = true;

                long initTime = System.currentTimeMillis() - startTime;
                System.out.printf("âœ… Stanford CoreNLP inicializado em %d ms\n", initTime);

            } catch (Exception e) {
                System.err.println("âŒ Falha ao inicializar Stanford CoreNLP: " + e.getMessage());

                // Se falhar, desabilita permanentemente para evitar logs repetidos
                permanentlyDisabled = true;
                pipeline = null;
                initialized = false;

                System.out.println("âš ï¸ Stanford CoreNLP desabilitado, usando algoritmo extrativo");
            }
        }
    }


    @Override
    public String summarize(String content, int maxSentences) {
        if (!ensureInitialized()) {
            return fallbackSummarize(content, maxSentences);
        }

        long startTime = System.currentTimeMillis();

        try {
            // Prã©-processamento
            String cleanContent = preprocessContent(content);
            if (cleanContent.length() < 100) return cleanContent;

            // Anã¡lise com Stanford CoreNLP (bã¡sica)
            Annotation document = new Annotation(cleanContent);
            pipeline.annotate(document);

            // Extraã§ã£o de sentenã§as bã¡sica
            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

            if (sentences == null || sentences.isEmpty()) {
                return fallbackSummarize(content, maxSentences);
            }

            // ðŸ”§ CORREã‡ãƒO CRãTICA: Calcular limite baseado no tamanho do conteãºdo
            int targetSentenceCount = calculateAdaptiveSentenceCount(content.length(), maxSentences);
            int limit = Math.min(targetSentenceCount, sentences.size());

            System.out.printf("ðŸŽ¯ Stanford CoreNLP: %d sentenã§as disponã­veis â†’ selecionando %d sentenã§as (maxSentences: %d)\n",
                    sentences.size(), limit, maxSentences);

            // âœ… Seleciona sentenã§as baseado no limite calculado
            String summary = sentences.stream()
                    .limit(limit)
                    .map(CoreMap::toString)
                    .collect(Collectors.joining(" "));

            long processingTime = System.currentTimeMillis() - startTime;
            System.out.printf("ðŸŽ¯ Stanford CoreNLP: %d chars â†’ %d chars em %d ms\n",
                    content.length(), summary.length(), processingTime);

            return summary;

        } catch (Exception e) {
            System.err.println("âŒ Erro no Stanford CoreNLP: " + e.getMessage());
            return fallbackSummarize(content, maxSentences);
        }
    }

    /**
     * CORRIGIDO: Calcula nãºmero mais conservador de sentenã§as para reduzir compressã£o excessiva
     */
    private int calculateAdaptiveSentenceCount(int contentLength, int maxSentences) {
        // âœ… NãšMEROS EXTREMAMENTE REDUZIDOS para usuã¡rios Braille
        if (contentLength > 30000) {
            // Wikipedia: apenas 2-3 sentenã§as para ~600 chars
            return Math.min(3, maxSentences);   // DRASTICAMENTE REDUZIDO
        } else if (contentLength > 5000) {
            // Brasil Escola: apenas 2-3 sentenã§as para ~600 chars
            return Math.min(3, maxSentences);   // DRASTICAMENTE REDUZIDO
        } else {
            // G1: apenas 1-2 sentenã§as para ~400 chars
            return Math.min(2, maxSentences);   // DRASTICAMENTE REDUZIDO
        }
    }


    @Override
    public List<String> extractKeysentences(String content, int count) {
        if (!ensureInitialized()) {
            return Arrays.asList(fallbackSummarize(content, count).split("\\. "));
        }

        try {
            Annotation doc = new Annotation(preprocessContent(content));
            pipeline.annotate(doc);

            List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
            if (sentences == null) return new ArrayList<>();

            return sentences.stream()
                    .limit(count)
                    .map(CoreMap::toString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Arrays.asList(fallbackSummarize(content, count).split("\\. "));
        }
    }

    @Override
    public double calculateSentenceRelevance(String sentence, String fullContext) {
        return 0.5; // Valor neutro, pois nã£o temos anã¡lise avanã§ada
    }

    @Override
    public NLPProviderInfo getProviderInfo() {
        return new NLPProviderInfo(
                "Stanford CoreNLP",
                "4.5.0",
                "Pipeline bã¡sico (tokenize, ssplit) - sem modelos PT",
                false, false, 256_000_000L
        );
    }

    @Override
    public boolean isReady() {
        return initialized && pipeline != null && !permanentlyDisabled;
    }

    @Override
    public void cleanup() {
        pipeline = null;
        initialized = false;
        System.out.println("ðŸ§¹ Stanford CoreNLP cleanup concluã­do");
    }

    // Mã©todos auxiliares
    private boolean ensureInitialized() {
        if (permanentlyDisabled) return false;

        if (!initialized) {
            try {
                initialize();
            } catch (Exception e) {
                return false;
            }
        }
        return initialized;
    }

    private String preprocessContent(String content) {
        return content
                .replaceAll("\\[.*?\\]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String fallbackSummarize(String content, int maxSentences) {
        String[] sentences = content.split("(?<=[.!?])\\s+");
        int limit = Math.min(maxSentences, sentences.length);
        return String.join(" ", Arrays.copyOf(sentences, limit));
    }
}