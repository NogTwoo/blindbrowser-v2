
package meuparser.ia;

import java.util.ArrayList;
import java.util.List;

/**
 * Armazena estatísticas de processamento de conteúdo com IA
 */
public class AIStats {

    private int totalProcessamentos;
    private int processamentosComErro;
    private long tempoTotalProcessamento;
    private String ultimoErro;
    private List<ProcessamentoInfo> historico;

    public AIStats() {
        totalProcessamentos = 0;
        processamentosComErro = 0;
        tempoTotalProcessamento = 0;
        ultimoErro = "";
        historico = new ArrayList<>();
    }

    /**
     * Registra informações sobre um processamento bem-sucedido
     *
     * @param conteudoOriginal Conteúdo antes do processamento
     * @param conteudoProcessado Conteúdo após o processamento
     * @param palavrasChaveEncontradas Número de palavras-chave encontradas
     * @param categoriaPagina Categoria identificada da página
     * @param tempoProcessamento Tempo em ms que levou o processamento
     */
    public void registrarProcessamento(String conteudoOriginal, String conteudoProcessado,
                                       int palavrasChaveEncontradas, String categoriaPagina,
                                       long tempoProcessamento) {

        totalProcessamentos++;
        tempoTotalProcessamento += tempoProcessamento;

        ProcessamentoInfo info = new ProcessamentoInfo(
                System.currentTimeMillis(),
                conteudoOriginal.length(),
                conteudoProcessado.length(),
                palavrasChaveEncontradas,
                categoriaPagina,
                tempoProcessamento
        );

        historico.add(info);

        // Limitar o histórico a 20 entradas para não consumir muita memória
        if (historico.size() > 20) {
            historico.remove(0);
        }
    }

    /**
     * Registra um erro ocorrido durante o processamento
     *
     * @param mensagemErro Descrição do erro
     */
    public void registrarErro(String mensagemErro) {
        processamentosComErro++;
        ultimoErro = mensagemErro;
    }

    /**
     * Retorna o tempo médio de processamento em ms
     *
     * @return Tempo médio ou 0 se não houver processamentos
     */
    public double getTempoMedioProcessamento() {
        if (totalProcessamentos > 0) {
            return (double) tempoTotalProcessamento / totalProcessamentos;
        }
        return 0;
    }

    /**
     * Retorna a taxa de erros
     *
     * @return Taxa de erros como valor entre 0 e 1
     */
    public double getTaxaErros() {
        if (totalProcessamentos > 0) {
            return (double) processamentosComErro / totalProcessamentos;
        }
        return 0;
    }

    public void adicionarInformacaoPersonalizada(String portal, String brasilEscola) {
    }

    /**
     * Informações de um processamento específico
     */
    public static class ProcessamentoInfo {
        private final long timestamp;
        private final int tamanhoOriginal;
        private final int tamanhoProcessado;
        private final int palavrasChave;
        private final String categoria;
        private final long tempoProcessamento;

        public ProcessamentoInfo(long timestamp, int tamanhoOriginal, int tamanhoProcessado,
                                 int palavrasChave, String categoria, long tempoProcessamento) {
            this.timestamp = timestamp;
            this.tamanhoOriginal = tamanhoOriginal;
            this.tamanhoProcessado = tamanhoProcessado;
            this.palavrasChave = palavrasChave;
            this.categoria = categoria;
            this.tempoProcessamento = tempoProcessamento;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getTamanhoOriginal() {
            return tamanhoOriginal;
        }

        public int getTamanhoProcessado() {
            return tamanhoProcessado;
        }

        public int getPalavrasChave() {
            return palavrasChave;
        }

        public String getCategoria() {
            return categoria;
        }

        public long getTempoProcessamento() {
            return tempoProcessamento;
        }
    }
}