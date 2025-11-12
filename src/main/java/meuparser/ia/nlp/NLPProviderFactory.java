package meuparser.ia.nlp;

import java.lang.reflect.Constructor;

/**
 * Factory para diferentes provedores de NLP
 * Permite alternar entre Stanford CoreNLP, OpenNLP, DL4J, etc.
 * USA APENAS REFLEXãO - NãO QUEBRA COMPILAçãO
 */
public class NLPProviderFactory {

    public enum NLPProvider {
        STANFORD_CORENLP,    // Mais robusto, melhor para análise sintática
        APACHE_OPENNLP,      // Mais leve, boa performance
        DL4J_NEURAL,         // Deep Learning, melhor para tarefas complexas
        HUGGINGFACE_TRANSFORMERS, // Estado da arte, BERT/GPT
        HYBRID              // Combina múltiplos provedores
    }

    private static NLPProvider currentProvider = NLPProvider.STANFORD_CORENLP;

    /**
     * Cria instância do sumarizador baseado no provedor configurado
     * USA APENAS REFLEXãO PARA EVITAR ERROS DE COMPILAçãO
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
                System.err.println("⚠️  Provedor " + currentProvider + " não disponível, tentando Stanford CoreNLP...");
                INLPSummarizer fallback = createSummarizerByReflection(
                        "meuparser.ia.nlp.StanfordCoreNLPSummarizer");
                if (fallback != null) {
                    return fallback;
                }
            }

            // último recurso: retorna null para usar fallback básico
            System.err.println("❌ Nenhum provedor NLP avançado disponível, usando fallback básico");
            return null;

        } catch (Exception e) {
            System.err.println("❌ Erro ao criar sumarizador NLP: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cria sumarizador usando APENAS reflexão (não quebra compilação)
     */
    private static INLPSummarizer createSummarizerByReflection(String className) {
        try {
            Class<?> clazz = Class.forName(className);

            // Verifica se implementa INLPSummarizer
            if (!INLPSummarizer.class.isAssignableFrom(clazz)) {
                System.err.println("❌ Classe " + className + " não implementa INLPSummarizer");
                return null;
            }

            Constructor<?> constructor = clazz.getDeclaredConstructor();
            Object instance = constructor.newInstance();

            System.out.println("✅ Sumarizador criado via reflexão: " + className);
            return (INLPSummarizer) instance;

        } catch (ClassNotFoundException e) {
            System.out.println("⚠️  Classe não encontrada: " + className);
            return null;
        } catch (Exception e) {
            System.err.println("❌ Erro ao instanciar " + className + ": " + e.getMessage());
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
     * Verifica se uma classe está disponível no classpath
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
        System.out.println("📄 Alterando provedor NLP para: " + provider);
        currentProvider = provider;
    }

    /**
     * Retorna o provedor atualmente configurado
     */
    public static NLPProvider getCurrentProvider() {
        return currentProvider;
    }

    /**
     * Lista provedores disponíveis no sistema atual
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
     * Detecta automaticamente o melhor provedor baseado no hardware disponível
     * E que esteja EFETIVAMENTE disponível
     */
    public static NLPProvider detectBestProvider() {
        NLPProvider[] available = getAvailableProviders();

        if (available.length == 0) {
            System.err.println("❌ Nenhum provedor NLP disponível! Usando Stanford CoreNLP como padrão");
            return NLPProvider.STANFORD_CORENLP;
        }

        // Prioriza baseado no que está disponível
        NLPProvider[] priorities = {
                NLPProvider.STANFORD_CORENLP,  // Mova Stanford para primeiro
                NLPProvider.HUGGINGFACE_TRANSFORMERS,
                NLPProvider.DL4J_NEURAL,
                NLPProvider.APACHE_OPENNLP,
                NLPProvider.HYBRID
        };

        for (NLPProvider preferred : priorities) {
            if (java.util.Arrays.asList(available).contains(preferred)) {
                System.out.println("✅ Usando provedor disponível: " + preferred);
                return preferred;
            }
        }

        // Retorna o primeiro disponível se nada mais funcionar
        System.out.println("✅ Usando primeiro provedor disponível: " + available[0]);
        return available[0];
    }

    /**
     * Inicialização automática baseada no ambiente
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
                        System.err.println("⚠️  Provedor configurado não disponível: " + configuredProvider);
                        setProvider(detectBestProvider());
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("⚠️  Provedor NLP inválido: " + configuredProvider);
                    setProvider(detectBestProvider());
                }
            } else {
                // Auto-detecção
                setProvider(detectBestProvider());
            }
        } catch (Exception e) {
            System.err.println("❌ Erro na inicialização do NLPProviderFactory: " + e.getMessage());
            currentProvider = NLPProvider.STANFORD_CORENLP;
        }
    }
}
