package meuparser.cache;


/**
 * Representa o conteudo armazenado em cache com informacoes de metadados(tempo)
 */
public class CachedContent {
    private final String content;
    private final long timestamp;
    private final long ttlMillis;


    /**
     * Construtor para criar um CachedContent com o conteudo, timestamp atual e TTL
     *
     * @param content   Conteudo a ser armazenado
     * @param timestamp Tempo de vida em milissegundos
     */

    public CachedContent(String content, long timestamp) {
        this(content, timestamp, 2 * 60 * 1000); // TTL padrã£o de 30 minutos
    }

    /**
     * Cria um novo conteãºdo em cache com TTL customizado
     *
     * @param content   O conteãºdo a ser armazenado
     * @param timestamp Timestamp de quando foi criado
     * @param ttlMillis Time to live em milissegundos
     */
    public CachedContent(String content, long timestamp, long ttlMillis) {
        this.content = content;
        this.timestamp = timestamp;
        this.ttlMillis = ttlMillis;
    }

    /**
     * Verifica se o conteãºdo expirou
     *
     * @return true se expirou, false caso contrã¡rio
     */
    public boolean isExpired() {
        return (System.currentTimeMillis() - timestamp) > ttlMillis;
    }


    /**
     * Obtã©m o conteãºdo armazenado
     *
     * @return O conteãºdo
     */
    public String getContent() {
        return content;
    }

    /**
     * Obtã©m o timestamp de criaã§ã£o
     *
     * @return Timestamp em milissegundos
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Obtã©m o TTL configurado
     *
     * @return TTL em milissegundos
     */
    public long getTtlMillis() {
        return ttlMillis;
    }

    /**
     * Calcula o tempo restante atã© expirar
     *
     * @return Tempo restante em milissegundos (negativo se jã¡ expirou)
     */
    public long getTimeToExpire() {
        return ttlMillis - (System.currentTimeMillis() - timestamp);
    }

}