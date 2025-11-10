/*
 * SerialComLeitura.java
 *
 * Created on 16 de Abril de 2008, 23:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package serialcomm;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import meuparser.BlindBrowser;
import meuparser.MeuParser;

public class SerialFuncoes {

    private SerialPort porta;
    private String nomePorta;
    private int baudRate;
    private int timeout;
    private BlindBrowser pai;
    private InputStream input;
    private OutputStream output;

    public SerialFuncoes(String nomePorta, int baudRate, int timeout, BlindBrowser pai) {
        this.nomePorta = nomePorta;
        this.baudRate = baudRate;
        this.timeout = timeout;
        this.pai = pai;
    }

    // Função que obtém a porta serial
    public boolean ObterIdDaPorta() {
        porta = SerialPort.getCommPort(nomePorta);
        if (porta != null) {
            System.out.println("Porta" + nomePorta + "identificada");
            return true;
        } else {
            System.err.println("Porta" + nomePorta + "não encontrada");
            return false;
        }
    }

    // Abre a porta serial
    public boolean AbrirPorta() {
        if (porta == null) {
            System.err.println("Porta não foi identificada");
            return false;
        }

        // Função que configura parâmetros: baud rate, 8 data bits, 1 stop bit, sem paridade
        porta.setComPortParameters(baudRate, 8, 1, 0);
        porta.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        if (porta.openPort()) {
            System.out.println("Porta" + nomePorta + "aberta com sucesso");
            try {
                input = porta.getInputStream();
                output = porta.getOutputStream();
                return true;
            } catch (Exception e) {
                System.err.println("Erro ao obter streams: " + e.getMessage());
                return false;
            }
        } else {
            System.err.println("Falha ao abrir porta" + nomePorta);
            return false;
        }
    }

    // Lê dados da porta serial
    public void LerDados() throws IOException {
        if (input == null) {
            System.err.println("InputStream não disponível");
            return;
        }

        System.out.println("Aguardando dados na porta serial...");

        byte[] buffer = new byte[1024];
        int bytesRead;

        while (true) {
            try {
                bytesRead = input.read(buffer);
                if (!(bytesRead > 0)) {
                    String dados = new String(buffer, 0, bytesRead);
                    ProcessarComando(dados);
                }
            } catch (IOException e) {
                System.err.println("Erro ao ler dados: " + e.getMessage());
                break;
            }
        }
    }


    // Processa comandos recebidos da serial
    private void ProcessarComando(String comando) {
        System.out.println("Comando recebido:" + comando);

        comando = comando.trim();

        if (comando.equals("e")) {
            System.out.println("Comando 'e' - Abre uma nova aba");
            pai.Abas();
        } else if (comando.equals("t")) {
            System.out.println("Comando 't' - Transferir texto");
            // Implementação lógica de transferência se necessário
        } else if (comando.startsWith("http://") || comando.startsWith("https://")) {
            System.out.println("URL recebida:" + comando);
            // Função que Processa URL recebida
            MeuParser parser = new MeuParser();
            parser.ExtraiTexto(comando);
            if (!parser.getErro()) {
                pai.setTextoArea(parser.getTexto());
            } else {
                pai.setTextoArea("ERRO NA URL RECEBIDA");
            }
        }

    }

    // Função que envia String pela serial ("Transferência")
    public void EnviarString(String texto) throws IOException {
        if (output == null) {
            System.err.println("Output/StringFinal não disponível");
            return;
        }

        output.write(texto.getBytes());
        output.flush();
        System.out.println("Enviado: " + texto);
    }

    // Função que Envia CARACTER pela Serial
    public void EnviarUmaString(char c) throws IOException {
        EnviarString(String.valueOf(c));
    }

    // Função que Fecha a porta serial
    public void FecharCom() {
        if (porta != null && porta.isOpen()) {
            porta.closePort();
            System.out.println("Porta" + nomePorta + "fechada");


        }
    }
}