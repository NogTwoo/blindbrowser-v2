package meuparser.ia.nlp;

import java.lang.reflect.Constructor;

/**
 * Factory para diferentes provedores de NLP
 * Permite alternar entre Stanford CoreNLP, OpenNLP, DL4J, etc.
 * USA APENAS REFLEXãƒO - NãƒO QUEBRA COMPILAã‡ãƒO
 */
public class NLPProviderFactory {

    public enum NLPProvider {
        STANFORD_CORENLP,    // Mais robusto, melhor para anã¡lise sintã¡tica
        APACHE_OPENNLP,      // Mais leve, boa performance
        DL4J_NEURAL,         // Deep Learning, melhor para tarefas complexas
        HUGGINGFACE_TRANSFORMERS, // Estado da arte, BERT/GPT
        HYBRID              // Combina mãºltiplos provedores
    }

    private static NLPProvider currentProvider = NLPProvider.STANFORD_CORENLP;

    /**
     * Cria instã¢ncia do sumarizador baseado no provedor configurado
     * USA APENAS REFLEXãƒO PARA EVITAR ERROS DE COMPILAã‡ãƒO
     */
    public static INLPSummarizer createSummarizer() {
        try {
            String className = getClassNameForProvider(currentProvider);

            if (className != null) {
                INLPSummarizer summarizer = createSummarizerByReflection(className);
                if (summarizer != null) {
                    return summarizer;
                }
            }

            // Fallback: tenta Stanford CoreNLP
            if (currentProvider != NLPProvider.STANFORD_CORENLP) {
                System.err.println("âš ï¸  Provedor " + currentProvider + " nã£o disponã­vel, tentando Stanford CoreNLP...");
                INLPSummarizer fallback = createSummarizerByReflection(
                        "meuparser.ia.nlp.StanfordCoreNLPSummarizer");
                if (fallback != null) {
                    return fallback;
                }
            }

            // ãšltimo recurso: retorna null para usar fallback bã¡sico
            System.err.println("âŒ Nenhum provedor NLP avanã§ado disponã­vel, usando fallback bã¡sico");
            return null;

        } catch (Exception e) {
            System.err.println("âŒ Erro ao criar sumarizador NLP: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cria sumarizador usando APENAS reflexã£o (nã£o quebra compilaã§ã£o)
     */
    private static INLPSummarizer createSummarizerByReflection(String className) {
        try {
            Class<?> clazz = Class.forName(className);

            // Verifica se implementa INLPSummarizer
            if (!INLPSummarizer.class.isAssignableFrom(clazz)) {
                System.err.println("âŒ Classe " + className + " nã£o implementa INLPSummarizer");
                return null;
            }

            Constructor<?> constructor = clazz.getDeclaredConstructor();
            Object instance = constructor.newInstance();

            System.out.println("âœ… Sumarizador criado via reflexã£o: " + className);
            return (INLPSummarizer) instance;

        } catch (ClassNotFoundException e) {
            System.out.println("âš ï¸  Classe nã£o encontrada: " + className);
            return null;
        } catch (Exception e) {
            System.err.println("âŒ Erro ao instanciar " + className + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Mapeia enum para nome da classe
     */
    private static String getClassNameForProvider(NLPProvider provider) {
        switch (provider) {
            case STANFORD_CORENLP:
                return "meuparser.ia.nlp.StanfordCoreNLPSummarizer";
            case APACHE_OPENNLP:
                return "meuparser.ia.nlp.OpenNLPSummarizer";
            case DL4J_NEURAL:
                return "meuparser.ia.nlp.DL4JNeuralSummarizer";
            case HUGGINGFACE_TRANSFORMERS:
                return "meuparser.ia.nlp.HuggingFaceTransformerSummarizer";
            case HYBRID:
                return "meuparser.ia.nlp.HybridNLPSummarizer";
            default:
                return null;
        }
    }

    /**
     * Verifica se uma classe estã¡ disponã­vel no classpath
     */
    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }

    /**
     * Configura o provedor de NLP a ser usado
     */
    public static void setProvider(NLPProvider provider) {
        System.out.println("ðŸ”„ Alterando provedor NLP para: " + provider);
        currentProvider = provider;
    }

    /**
     * Retorna o provedor atualmente configurado
     */
    public static NLPProvider getCurrentProvider() {
        return currentProvider;
    }

    /**
     * Lista provedores disponã­veis no sistema atual
     */
    public static NLPProvider[] getAvailableProviders() {
        java.util.List<NLPProvider> available = new java.util.ArrayList<>();

        for (NLPProvider provider : NLPProvider.values()) {
            String className = getClassNameForProvider(provider);
            if (className != null && isClassAvailable(className)) {
                available.add(provider);
            }
        }

        return available.toArray(new NLPProvider[0]);
    }

    /**
     * Detecta automaticamente o melhor provedor baseado no hardware disponã­vel
     * E que esteja EFETIVAMENTE disponã­vel
     */
    public static NLPProvider detectBestProvider() {
        NLPProvider[] available = getAvailableProviders();

        if (available.length == 0) {
            System.err.println("âŒ Nenhum provedor NLP disponã­vel! Usando Stanford CoreNLP como padrã£o");
            return NLPProvider.STANFORD_CORENLP;
        }

        // Prioriza baseado no que estã¡ disponã­vel
        NLPProvider[] priorities = {
                NLPProvider.STANFORD_CORENLP,  // Mova Stanford para primeiro
                NLPProvider.HUGGINGFACE_TRANSFORMERS,
                NLPProvider.DL4J_NEURAL,
                NLPProvider.APACHE_OPENNLP,
                NLPProvider.HYBRID
        };

        for (NLPProvider preferred : priorities) {
            if (java.util.Arrays.asList(available).contains(preferred)) {
                System.out.println("âœ… Usando provedor disponã­vel: " + preferred);
                return preferred;
            }
        }

        // Retorna o primeiro disponã­vel se nada mais funcionar
        System.out.println("âœ… Usando primeiro provedor disponã­vel: " + available[0]);
        return available[0];
    }

    /**
     * Inicializaã§ã£o automã¡tica baseada no ambiente
     */
    static {
        try {
            String configuredProvider = System.getProperty("blindbrowser.nlp.provider");
            if (configuredProvider != null) {
                try {
                    NLPProvider provider = NLPProvider.valueOf(configuredProvider.toUpperCase());
                    NLPProvider[] availableProviders = getAvailableProviders();

                    if (java.util.Arrays.asList(availableProviders).contains(provider)) {
                        setProvider(provider);
                    } else {
                        System.err.println("âš ï¸  Provedor configurado nã£o disponã­vel: " + configuredProvider);
                        setProvider(detectBestProvider());
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("âš ï¸  Provedor NLP invã¡lido: " + configuredProvider);
                    setProvider(detectBestProvider());
                }
            } else {
                // Auto-detecã§ã£o
                setProvider(detectBestProvider());
            }
        } catch (Exception e) {
            System.err.println("âŒ Erro na inicializaã§ã£o do NLPProviderFactory: " + e.getMessage());
            currentProvider = NLPProvider.STANFORD_CORENLP;
        }
    }
}
