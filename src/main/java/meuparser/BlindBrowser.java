/*
 * BlindBrowser.java
 *
 * Created on 16 de Junho de 2008, 12:08
 */
package meuparser;

// Imports Java padrão (ordem alfabética)

import com.fazecast.jSerialComm.SerialPort;
import meuparser.cache.ContentCache;
import meuparser.ia.DualModeManager.DualModeContentManager;
import meuparser.tts.TTSManager;
import serialcomm.LeituraEscrita;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author  Administrator
 */

/**
 * Interface gré

á

fica com suporte a Dual Mode @Nicholas-15/07/25
 */
public class BlindBrowser extends JFrame implements Runnable {

    private final ContentCache contentCache = new ContentCache();
    private TTSManager ttsManager;

    // NOVO: Gerenciador de Dual Mode
    private final DualModeContentManager dualModeManager = new DualModeContentManager();
    private JLabel modeIndicatorLabel; // Indicador visual do modo
    private JLabel statusLabel;


    Configuracao config;
    int contaba = 1;
    JTextArea jTextAreaGeral;
    JScrollPane teste;

    /** Creates new form BlindBrowser */
    public BlindBrowser() {
        // NOVO: Inicializar indicador de modo
        initializeModeIndicator();


        initComponents();

        initializeTTS();

        // Habilitar comparação de cache
        contentCache.setComparisonEnabled(true);

        jButtonSerial.setVisible(false);
        jButtonAbas.setVisible(false);

        jButtonSerialMouseClicked(new MouseEvent(rootPane, WIDTH, WIDTH, WIDTH, WIDTH, WIDTH, WIDTH, rootPaneCheckingEnabled));

        Tratador trat = new Tratador();
        addKeyListener(trat);
        //cada textfield precisa desse keyListener, pois eles "roubam"
        //o evento do JFrame.
        jTextFieldURL.addKeyListener(trat);
        jButtonIr.addKeyListener(trat);
        jTextAreaConteudo.addKeyListener(trat);

        // WindowListener para fechar porta serial ao sair
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopSerialListener();
                // Fechar porta serial se estiver aberta
                if (portaSerial != null && portaSerial.isOpen()) {
                    System.out.println("Fechando porta serial: " + portaSelecionada);
                    portaSerial.closePort();
                }

                // Fechar TTS se existir
                if (ttsManager != null) {
                    ttsManager.stopNarration();
                }

                System.out.println("BlindBrowser encerrado.");
                System.exit(0);
            }
        });
    }

    // Comunicação Serial
    private JButton jButtonSerial;
    private JButton jButtonConfigSerial;
    private JLabel jLabelStatusSerial;
    private SerialPort portaSerial;
    private String portaSelecionada = "COM3"; // Padrão
    private boolean serialConectada = false;
    private Thread serialListenerThread;
    private volatile boolean isListening = false;
    private JTextArea jTextAreaSerialLog; // Para mostrar dados recebidos (opcional)


    /**
     * NOVO: Método para inicialização aprimorada do TTS no construtor
     * SUBSTITUA o método initializeTTS() existente por este:
     */
    private void initializeTTS() {
        try {
            ttsManager = new TTSManager(); // Usa a nova versão com vozes brasileiras
            System.out.println("✅ Sistema de narração brasileira inicializado");

            // Mostrar informação da voz detectada
            //   SwingUtilities.invokeLater(() -> {
            //     if (statusLabel != null) {
            //       statusLabel.setText("🎙️¸ Narração brasileira pronta");
            // }
            // });

            // Log das vozes disponíveis
            System.out.println(ttsManager.getDiagnosticInfo());

        } catch (Exception e) {
            System.err.println("⚠🎙️ Erro na inicialização TTS: " + e.getMessage());
            ttsManager = null;

            SwingUtilities.invokeLater(() -> {
                if (statusLabel != null) {
                    statusLabel.setText("⚠🎙️ TTS indisponível");
                }
            });
        }
    }

    /**
     * NOVO: Inicializa o indicador de modo na interface
     */
    private void initializeModeIndicator() {
        modeIndicatorLabel = new JLabel("📄 Modo: RESUMIDO");
        modeIndicatorLabel.setFont(new Font("Arial", Font.BOLD, 12));
        modeIndicatorLabel.setOpaque(true);
        modeIndicatorLabel.setBackground(new Color(200, 255, 200));
        modeIndicatorLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Adicionar ao painel superior (será feito no initComponents modificado)
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        statusLabel = new JLabel("");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        statusLabel.setFont(new Font("Tahoma", Font.PLAIN, 12));

        jPanel1 = new JPanel();
        jLabel1 = new JLabel();
        jTextFieldURL = new JTextField();
        jButtonIr = new JButton();
        jButtonToggleMode = new JButton();
        jPanel2 = new JPanel();
        jTabbedPane1 = new JTabbedPane();
        jScrollPane1 = new JScrollPane();
        jTextAreaConteudo = new JTextArea();
        jButtonSerial = new JButton();
        jMenuBarra1 = new JMenuBar();
        jMenuArquivo1 = new JMenu();
        jMenuItemSair1 = new JMenuItem();
        jMenuFerramentas1 = new JMenu();
        jMenuConfigurar1 = new JMenuItem();
        jButtonAbas = new JButton();

        // BOTÕES SERIAL
        jButtonConfigSerial = new JButton("Config Serial");
        jButtonSerial = new JButton("Enviar Serial");
        jLabelStatusSerial = new JLabel("Serial: OFF");

        jButtonConfigSerial.addActionListener(e -> configurarSerial());
        jButtonSerial.setEnabled(false);
        jButtonSerial.addActionListener(e -> enviarParaSerial());
        jLabelStatusSerial.setForeground(Color.RED);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("BLINDBROWSER V2.0 - DUAL MODE + SERIAL");

        // ============== USAR BORDERLAYOUT NO CONTENTPANE ==============
        getContentPane().setLayout(new BorderLayout());

        // PAINEL SUPERIOR (URL + botões)
        jPanel1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jLabel1.setFont(new Font("Tahoma", 0, 12));
        jLabel1.setText("Endereço:");

        jTextFieldURL.setText("http://");
        jTextFieldURL.setToolTipText("Digitar a URL da página");

        jButtonIr.setText("Ir");
        jButtonIr.setFocusable(false);
        jButtonIr.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                jButtonIrMouseClicked(evt);
            }
        });

        jButtonToggleMode.setText("Resumido/Completo (F2)");
        jButtonToggleMode.setToolTipText("Alternar entre Modo Resumido e Completo (F2)");
        jButtonToggleMode.setFocusable(false);
        jButtonToggleMode.addActionListener(evt -> toggleContentMode());

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldURL, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonIr)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonToggleMode)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(modeIndicatorLabel)
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel1)
                                .addComponent(jTextFieldURL, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButtonIr)
                                .addComponent(jButtonToggleMode)
                                .addComponent(modeIndicatorLabel))
        );

        // PAINEL CENTRAL (conteúdo web)
        jPanel2.setBorder(BorderFactory.createTitledBorder(null, "Página Web",
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font("Tahoma", 1, 12)));

        jTabbedPane1.setFocusable(false);
        jTextAreaConteudo.setColumns(20);
        jTextAreaConteudo.setEditable(false);
        jTextAreaConteudo.setFont(new Font("Monospaced", 1, 17));
        jTextAreaConteudo.setRows(5);
        jTextAreaConteudo.setFocusable(false);
        jScrollPane1.setViewportView(jTextAreaConteudo);
        jTabbedPane1.addTab("Aba 1", jScrollPane1);

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jTabbedPane1, GroupLayout.DEFAULT_SIZE, 873, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jTabbedPane1, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );

        // PAINEL INFERIOR (status + serial)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        bottomPanel.setPreferredSize(new Dimension(800, 35)); // FORÇAR ALTURA

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        statusBar.add(statusLabel);

        JPanel painelSerial = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        painelSerial.add(jButtonConfigSerial);
        painelSerial.add(jButtonSerial);
        painelSerial.add(jLabelStatusSerial);

        bottomPanel.add(statusBar, BorderLayout.WEST);
        bottomPanel.add(painelSerial, BorderLayout.EAST);

        // MENU
        jMenuArquivo1.setMnemonic(KeyEvent.VK_A);
        jMenuArquivo1.setText("Arquivo");
        jMenuItemSair1.setText("Sair");
        jMenuItemSair1.addActionListener(evt -> jMenuItemSairActionPerformed(evt));
        jMenuArquivo1.add(jMenuItemSair1);
        jMenuBarra1.add(jMenuArquivo1);

        jMenuFerramentas1.setMnemonic(KeyEvent.VK_F);
        jMenuFerramentas1.setText("Ferramentas");
        jMenuConfigurar1.setText("Configuração");
        jMenuConfigurar1.addActionListener(evt -> jMenuConfigurar1ActionPerformed(evt));
        jMenuFerramentas1.add(jMenuConfigurar1);
        jMenuBarra1.add(jMenuFerramentas1);

        setJMenuBar(jMenuBarra1);

        // ADICIONAR TUDO AO CONTENTPANE COM BORDERLAYOUT
        getContentPane().add(jPanel1, BorderLayout.NORTH);
        getContentPane().add(jPanel2, BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        jButtonSerial.setVisible(false);
        jButtonAbas.setVisible(false);

        pack();

// Adicionar painel inferior ao SOUTH da janela
        add(bottomPanel, BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    // NOVO: Declaração do botão toggle
    private JButton jButtonToggleMode;

    private void jButtonIrMouseClicked(MouseEvent evt) {
        try {
            // Salvar referência direta para o JTextArea antes de qualquer processamento
            JScrollPane scrollPane = (JScrollPane) jTabbedPane1.getSelectedComponent();
            final JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();

            // Verificar se os componentes estão acessíveis
            if (textArea == null) {
                System.err.println("ERRO: JTextArea não encontrado!");
                return;
            }

            // Feedback visual inicial para o usuário
            textArea.setText("Processando...");
            textArea.paintImmediately(textArea.getBounds());
            scrollPane.paintImmediately(scrollPane.getBounds());

            // Obter a URL
            String endereco = jTextFieldURL.getText().trim();
            System.out.println("Iniciando processamento da URL: " + endereco);

            // Adiciona http:// se necessário
            if (!endereco.startsWith("http://") && !endereco.startsWith("https://")) {
                endereco = "http://" + endereco;
                jTextFieldURL.setText(endereco);
            }

            final String urlFinal = endereco;

            // MODIFICADO: Usar DualModeContentManager
            try {
                // Carregar conteúdo no DualModeManager
                dualModeManager.loadContent(urlFinal);

                // Obter conteúdo inicial (modo resumido por padrão)
                String conteudoInicial = dualModeManager.getEssentialContent();

                // Armazenar no cache
                contentCache.put(urlFinal, conteudoInicial);

                // Salvar em arquivo
                ArmazenaArquivo armz = new ArmazenaArquivo();
                armz.salvar(conteudoInicial);
                System.out.println("Página salva em arquivo.");

                // Atualizar interface
                final String mensagemFinal = "Modo: RESUMIDO (F2 para alternar)\n" +
                        "─".repeat(50) + "\n\n" +
                        conteudoInicial;

                SwingUtilities.invokeLater(() -> {
                    textArea.setText(mensagemFinal);
                    textArea.setCaretPosition(0);
                    updateModeIndicator();

                    // Mostrar estatísticas
                    DualModeContentManager.ContentStats stats = dualModeManager.getContentStats();
                    System.out.println(stats.toString());
                });

            } catch (Exception e) {
                System.err.println("Erro ao processar com DualMode: " + e.getMessage());
                e.printStackTrace();

                // Fallback para método antigo
                processarMetodoAntigo(urlFinal, textArea, scrollPane);
            }

        } catch (Exception ex) {
            System.err.println("Erro geral no processamento: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Ocorreu um erro durante o processamento: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * NOVO: Método para alternar entre modos
     */
    private void toggleContentMode() {
        if (dualModeManager.getCurrentContent() == null) {
            JOptionPane.showMessageDialog(this,
                    "Carregue uma página primeiro",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            DualModeContentManager.ContentSwitchResult result = dualModeManager.toggleMode();

            // Atualizar TextArea
            JScrollPane scrollPane = (JScrollPane) jTabbedPane1.getSelectedComponent();
            JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();

            String mensagem = result.mode.name() + " (F2 para alternar)\n" +
                    "─".repeat(50) + "\n\n" +
                    result.content;

            textArea.setText(mensagem);
            textArea.setCaretPosition(0);

            // Atualizar indicador
            updateModeIndicator();

            // Beep de notificação
            Toolkit.getDefaultToolkit().beep();

            // Mostrar notificação
            JOptionPane.showMessageDialog(this,
                    result.message + "\n" +
                            "Caracteres: " + result.characterCount + "\n" +
                            "Tempo leitura Braille: ~" + result.readingTime + " min",
                    "Modo Alterado",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            System.err.println("Erro ao alternar modo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * NOVO: Buscar no conteúdo completo
     */
    private void searchInContent() {
        String keyword = JOptionPane.showInputDialog(this,
                "Digite o termo para buscar no conteúdo completo:");

        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }

        DualModeContentManager.SearchResult result = dualModeManager.searchInComplete(keyword);

        if (!result.found) {
            JOptionPane.showMessageDialog(this, result.message, "Busca",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Mostrar resultados
        StringBuilder sb = new StringBuilder();
        sb.append(result.message).append("\n\n");

        for (int i = 0; i < Math.min(3, result.matches.size()); i++) {
            DualModeContentManager.SearchMatch match = result.matches.get(i);
            sb.append("─".repeat(50)).append("\n");
            sb.append("Ocorrência ").append(i + 1).append(":\n");
            sb.append(match.context).append("\n\n");
        }

        JTextArea resultArea = new JTextArea(sb.toString());
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this, scrollPane,
                "Resultados da Busca", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * NOVO: Mostrar estatísticas do conteúdo
     */
    private void showContentStats() {
        DualModeContentManager.ContentStats stats = dualModeManager.getContentStats();
        JOptionPane.showMessageDialog(this,
                stats.toString(),
                "Estatísticas do Conteúdo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * NOVO: Atualizar indicador de modo
     */
    private void updateModeIndicator() {
        DualModeContentManager.ContentMode mode = dualModeManager.getCurrentMode();

        if (mode == DualModeContentManager.ContentMode.ESSENTIAL) {
            modeIndicatorLabel.setText("F2“„ Modo: RESUMIDO");
            modeIndicatorLabel.setBackground(new Color(200, 255, 200));
        } else {
            modeIndicatorLabel.setText("F2“– Modo: COMPLETO");
            modeIndicatorLabel.setBackground(new Color(255, 255, 200));
        }
    }

    /**
     * Método para configurar e conectar serial:
     */
    private void configurarSerial() {
        // Listar portas disponíveis
        SerialPort[] portas = SerialPort.getCommPorts();

        // DEBUG DETALHADO
        System.out.println("\n========== DEBUG SERIAL ==========");
        System.out.println("Total de portas: " + portas.length);
        for (int i = 0; i < portas.length; i++) {
            System.out.println("\n--- Porta " + (i+1) + " ---");
            System.out.println("Nome: " + portas[i].getSystemPortName());
            System.out.println("Descrição: " + portas[i].getDescriptivePortName());
            System.out.println("Port Description: " + portas[i].getPortDescription());
            System.out.println("Location: " + portas[i].getPortLocation());
        }
        System.out.println("==================================\n");

        if (portas.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Nenhuma porta serial detectada!\n" +
                            "Conecte o dispositivo USB e tente novamente.",
                    "Erro Serial",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Criar lista de nomes de portas
        String[] nomePortas = new String[portas.length];
        for (int i = 0; i < portas.length; i++) {
            nomePortas[i] = portas[i].getSystemPortName() + " - " +
                    portas[i].getDescriptivePortName();
        }

        // Dialog para escolher porta
        String portaEscolhida = (String) JOptionPane.showInputDialog(
                this,
                "Selecione a porta serial do dispositivo Braille:",
                "Configurar Serial",
                JOptionPane.QUESTION_MESSAGE,
                null,
                nomePortas,
                nomePortas[0]
        );

        if (portaEscolhida != null) {
            // Extrair nome da porta (ex: "COM3")
            portaSelecionada = portaEscolhida.split(" - ")[0];
            conectarSerial();
        }
    }

    private void conectarSerial() {
        try {
            // Desconectar porta anterior se existir
            if (portaSerial != null && portaSerial.isOpen()) {
                portaSerial.closePort();
            }

            // Abrir nova porta
            portaSerial = SerialPort.getCommPort(portaSelecionada);

            // Configurar parâmetros (padrão dispositivos Braille)
            portaSerial.setBaudRate(9600);
            portaSerial.setNumDataBits(8);
            portaSerial.setNumStopBits(SerialPort.ONE_STOP_BIT);
            portaSerial.setParity(SerialPort.NO_PARITY);
            portaSerial.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

            // Tentar abrir
            if (portaSerial.openPort()) {
                serialConectada = true;
                jButtonSerial.setEnabled(true);
                jLabelStatusSerial.setText("Serial: " + portaSelecionada + " @ 9600");
                jLabelStatusSerial.setForeground(new Color(0, 150, 0)); // Verde

                startSerialListener();// ✅ Inicia escuta automática

                JOptionPane.showMessageDialog(this,
                        "Conectado com sucesso!\n" +
                                "Porta: " + portaSelecionada + "\n" +
                                "Baudrate: 9600, 8N1",
                        "Serial OK",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                throw new Exception("Não foi possível abrir a porta " + portaSelecionada);
            }

        } catch (Exception ex) {
            serialConectada = false;
            jButtonSerial.setEnabled(false);
            jLabelStatusSerial.setText("Serial: Erro");
            jLabelStatusSerial.setForeground(Color.RED);

            JOptionPane.showMessageDialog(this,
                    "Erro ao conectar serial:\n" + ex.getMessage() + "\n\n" +
                            "Verifique:\n" +
                            "1. Dispositivo está conectado via USB\n" +
                            "2. Drivers instalados (se necessário)\n" +
                            "3. Porta não está sendo usada por outro programa",
                    "Erro Serial",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Inicia thread que fica ouvindo dados da porta serial
     */
    private void startSerialListener() {
        if (portaSerial == null || !portaSerial.isOpen()) {
            System.err.println("⚠️ Porta serial não está aberta");
            return;
        }

        isListening = true;
        serialListenerThread = new Thread(() -> {
            System.out.println("🎧 Iniciando escuta da porta " + portaSelecionada);

            byte[] buffer = new byte[1024];

            while (isListening && portaSerial.isOpen()) {
                try {
                    // Aguardar dados (timeout 100ms)
                    int bytesAvailable = portaSerial.bytesAvailable();

                    if (bytesAvailable > 0) {
                        int numRead = portaSerial.readBytes(buffer, Math.min(bytesAvailable, buffer.length));

                        if (numRead > 0) {
                            // Converter bytes para String
                            String dadosRecebidos = new String(buffer, 0, numRead, StandardCharsets.UTF_8);

                            // Processar comando recebido
                            processarComandoSerial(dadosRecebidos);

                            System.out.println("📥 Recebido (" + numRead + " bytes): " + dadosRecebidos);
                        }
                    }

                    // Pequena pausa para não sobrecarregar CPU
                    Thread.sleep(50);

                } catch (InterruptedException e) {
                    System.out.println("🛑 Listener interrompido");
                    break;
                } catch (Exception e) {
                    System.err.println("❌ Erro na leitura serial: " + e.getMessage());
                }
            }

            System.out.println("🔴 Listener finalizado");
        });

        serialListenerThread.setDaemon(true);
        serialListenerThread.start();
    }

    /**
     * Processa comandos vindos do dispositivo Braille
     */
    private void processarComandoSerial(String comando) {
        comando = comando.trim().toLowerCase();

        System.out.println("🔍 Processando comando: '" + comando + "'");

        String finalComando = comando;
        String finalComando1 = comando;
        SwingUtilities.invokeLater(() -> {
            try {
                switch (finalComando) {
                    case "a": // Comando "a" - enviar conteúdo atual
                        System.out.println("📤 Comando 'a' recebido - Enviando conteúdo");
                        enviarConteudoAtual();
                        break;

                    case "r": // Comando "r" - modo resumido
                        System.out.println("📝 Comando 'r' - Alternar para RESUMIDO");
                        if (dualModeManager.getCurrentMode() != DualModeContentManager.ContentMode.ESSENTIAL) {
                            toggleContentMode();
                        }
                        break;

                    case "c": // Comando "c" - modo completo
                        System.out.println("📄 Comando 'c' - Alternar para COMPLETO");
                        if (dualModeManager.getCurrentMode() != DualModeContentManager.ContentMode.COMPLETE) {
                            toggleContentMode();
                        }
                        break;

                    case "n": // Comando "n" - narrar
                        System.out.println("🎙️ Comando 'n' - Iniciar narração");
                        narrateCurrentContent();
                        break;

                    case "s": // Comando "s" - parar narração
                        System.out.println("⏹️ Comando 's' - Parar narração");
                        stopNarration();
                        break;

                    case "p": // Comando "p" - pausar/continuar
                        System.out.println("⏸️ Comando 'p' - Pausar/Continuar");
                        toggleNarration();
                        break;

                    default:
                        System.out.println("❓ Comando desconhecido: " + finalComando1);
                        // Enviar feedback ao dispositivo
                        enviarRespostaSerial("ERRO: Comando desconhecido\n");
                        break;
                }
            } catch (Exception e) {
                System.err.println("❌ Erro ao processar comando: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Envia o conteúdo atual pela porta serial (chamado por comando)
     */
    private void enviarConteudoAtual() {
        if (!serialConectada || portaSerial == null || !portaSerial.isOpen()) {
            System.err.println("⚠️ Serial não conectada");
            return;
        }

        try {
            String conteudo = jTextAreaConteudo.getText();

            if (conteudo == null || conteudo.trim().isEmpty()) {
                enviarRespostaSerial("ERRO: Sem conteúdo\n");
                return;
            }

            // Enviar cabeçalho
            enviarRespostaSerial("INICIO_CONTEUDO\n");

            // Enviar conteúdo em chunks (para não sobrecarregar buffer)
            byte[] bytes = conteudo.getBytes(StandardCharsets.UTF_8);
            int chunkSize = 512; // Bytes por vez

            for (int i = 0; i < bytes.length; i += chunkSize) {
                int end = Math.min(i + chunkSize, bytes.length);
                byte[] chunk = java.util.Arrays.copyOfRange(bytes, i, end);

                portaSerial.writeBytes(chunk, chunk.length);
                portaSerial.flushIOBuffers();

                // Pequena pausa entre chunks
                Thread.sleep(10);
            }

            // Enviar rodapé
            enviarRespostaSerial("\nFIM_CONTEUDO\n");

            System.out.println("✅ Conteúdo enviado: " + bytes.length + " bytes");

        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar conteúdo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envia mensagem de resposta para o dispositivo
     */
    private void enviarRespostaSerial(String mensagem) {
        if (portaSerial != null && portaSerial.isOpen()) {
            try {
                byte[] bytes = mensagem.getBytes(StandardCharsets.UTF_8);
                portaSerial.writeBytes(bytes, bytes.length);
                portaSerial.flushIOBuffers();
                System.out.println("📤 Resposta enviada: " + mensagem.trim());
            } catch (Exception e) {
                System.err.println("❌ Erro ao enviar resposta: " + e.getMessage());
            }
        }
    }

    /**
     * Para o listener serial
     */
    private void stopSerialListener() {
        isListening = false;

        if (serialListenerThread != null) {
            try {
                serialListenerThread.interrupt();
                serialListenerThread.join(1000); // Aguarda até 1 segundo
            } catch (InterruptedException e) {
                System.err.println("⚠️ Timeout ao parar listener");
            }
        }
    }

    /**
     * Método para enviar conteúdo
     */
    private void enviarParaSerial() {
        if (!serialConectada || portaSerial == null || !portaSerial.isOpen()) {
            JOptionPane.showMessageDialog(this,
                    "Serial não está conectada!\n" +
                            "Clique em 'Configurar Serial' primeiro.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Pegar conteúdo atual (resumido ou completo)
            String conteudo = jTextAreaConteudo.getText();

            if (conteudo == null || conteudo.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Não há conteúdo para enviar!\n" +
                                "Carregue uma página primeiro.",
                        "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Converter para bytes UTF-8
            byte[] bytes = conteudo.getBytes(StandardCharsets.UTF_8);

            // Enviar
            int bytesSent = portaSerial.writeBytes(bytes, bytes.length);

            // Aguardar transmissão completar
            portaSerial.flushIOBuffers();

            // Feedback
            JOptionPane.showMessageDialog(this,
                    "Conteúdo enviado com sucesso!\n\n" +
                            "Bytes transmitidos: " + bytesSent + "\n" +
                            "Porta: " + portaSelecionada + "\n" +
                            "Modo atual: " + (dualModeManager.getCurrentMode()),
                    "Envio Concluído",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao enviar dados:\n" + ex.getMessage(),
                    "Erro Serial",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Método fallback - processamento antigo
     */
    private void processarMetodoAntigo(String urlFinal, JTextArea textArea, JScrollPane scrollPane) {
        // Código original do processamento...
        // [Mantém o código existente como fallback]
    }

    // ... [resto dos métodos existentes permanecem inalterados] ...

    private void jButtonSerialMouseClicked(MouseEvent evt) {//GEN-FIRST:event_jButtonSerialMouseClicked
        // [Código existente mantido]
        SwingWorker a = new SwingWorker() {
            @Override
            public Object doInBackground() {
                LeituraEscrita serial = new LeituraEscrita(getThis());
                try {
                    serial.ComunicaSerial();
                } catch (IOException ex) {
                    Logger.getLogger(BlindBrowser.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }
        };
        a.execute();
    }//GEN-LAST:event_jButtonSerialMouseClicked

    // ... [outros métodos existentes] ...

    /**
     * MODIFICADO: Classe Tratador com novos atalhos
     */
    private class Tratador implements KeyListener {

        public void keyPressed(KeyEvent e) {
            // Atalhos existentes
            if (jTextFieldURL.isFocusOwner()) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    jButtonIrMouseClicked(new MouseEvent(rootPane, WIDTH, WIDTH, WIDTH, WIDTH, WIDTH, WIDTH, rootPaneCheckingEnabled));
                }
            }

            if (e.getKeyCode() == KeyEvent.VK_N && e.isAltDown()) {
                jTextFieldURL.requestFocus();
            } else if (e.getKeyCode() == KeyEvent.VK_T && e.isControlDown()) {
                jButtonAbasMouseClicked(new MouseEvent(rootPane, WIDTH, WIDTH, WIDTH, WIDTH, WIDTH, WIDTH, rootPaneCheckingEnabled));
            }

            // NOVO: Ctrl+R para relatório
            if (e.getKeyCode() == KeyEvent.VK_R && e.isControlDown()) {
                System.out.println("\n=== RELATÓRIO DE COMPARAã‡ãƒO SOLICITADO ===");
                printComparisonReport();
            }

            // NOVO: F1 - Ajuda
            if (e.getKeyCode() == KeyEvent.VK_F1) {
                showHelp();
            }

            // NOVO: F2 - Alternar modo
            if (e.getKeyCode() == KeyEvent.VK_F2) {
                toggleContentMode();
            }

            // NOVO: F3 - Estatísticas
            if (e.getKeyCode() == KeyEvent.VK_F3) {
                showContentStats();
            }

            // NOVO: Ctrl+F - Buscar
            if (e.getKeyCode() == KeyEvent.VK_F && e.isControlDown()) {
                searchInContent();
            }

            // NOVO: Ctrl+S - Salvar
            if (e.getKeyCode() == KeyEvent.VK_S && e.isControlDown()) {
                saveCurrentContent();
            }
            // NOVO: F4 - Narrar conteúdo atual
            if (e.getKeyCode() == KeyEvent.VK_F4) {
                narrateCurrentContent();
            }

            // NOVO: F5 - Pausar/Continuar narração
            if (e.getKeyCode() == KeyEvent.VK_F5) {
                toggleNarration();
            }

            // NOVO: F6 - Parar narração
            if (e.getKeyCode() == KeyEvent.VK_F6) {
                stopNarration();
            }

            // NOVO: + (mais) - Aumentar velocidade
            if (e.getKeyCode() == KeyEvent.VK_PLUS ||
                    (e.getKeyCode() == KeyEvent.VK_EQUALS && e.isShiftDown())) {
                increaseNarrationSpeed();
            }

            // NOVO: - (menos) - Diminuir velocidade
            if (e.getKeyCode() == KeyEvent.VK_MINUS) {
                decreaseNarrationSpeed();
            }
            // NOVO: F8 - Selecionar voz brasileira
            if (e.getKeyCode() == KeyEvent.VK_F8) {
                selectBrazilianVoice();
            }

// NOVO: F9 - Testar voz atual
            if (e.getKeyCode() == KeyEvent.VK_F9) {
                testCurrentVoice();
            }

// NOVO: Ctrl+L  Menu rápido de vozes
            if (e.getKeyCode() == KeyEvent.VK_L && e.isControlDown()) {
                showVoiceQuickMenu();
            }
        }

        public void keyReleased(KeyEvent e) {
            //rotulo.setText("Tecla liberada");
        }

        public void keyTyped(KeyEvent e) {
            //rotulo.setText("Tecla pressionada e liberada");
        }

    }

    /**
     * METODO NOVO 10/08/25: Permite selecionar voz brasileira
     */
    private void selectBrazilianVoice() {
        if (ttsManager != null) {
            try {
                ttsManager.selectVoice();
                updateNarrationStatus("🎙️ - Configuração de voz alterada");
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao selecionar voz: " + e.getMessage());
                updateNarrationStatus("⚠️ Erro na seleção de voz");
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Sistema de narração não disponível",
                    "TTS Indisponível",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
    /**
     * METODO QUE ESTAVA FALTANDO: Atualiza status da narração
     */
    private void updateNarrationStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(message);
                statusLabel.repaint();
            }
            System.out.println("📊 Status: " + message);
        });
    }


    /**
     *  Mostra diagnóstico do TTS
     */
    private void showTTSDiagnostic() {
        if (ttsManager != null) {
            String diagnosticInfo = ttsManager.getDiagnosticInfo();

            JTextArea textArea = new JTextArea(diagnosticInfo);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            textArea.setEditable(false);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));

            JOptionPane.showMessageDialog(this,
                    scrollPane,
                    "Diagnóstico do Sistema TTS",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Sistema de narração não inicializado.\n" +
                            "Verifique se as dependências estão instaladas.",
                    "TTS Indisponível",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * NOVO: Testa a voz atual com texto exemplo
     */
    private void testCurrentVoice() {
        if (ttsManager != null) {
            try {
                updateNarrationStatus("🎵 Testando voz atual...");
                ttsManager.testCurrentVoice();
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao testar voz: " + e.getMessage());
                updateNarrationStatus("⚠️ Erro no teste de voz");
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Sistema de narração não disponível",
                    "TTS Indisponível",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * NOVO: Menu rápido de opções de voz
     */
    private void showVoiceQuickMenu() {
        if (ttsManager == null) {
            JOptionPane.showMessageDialog(this,
                    "Sistema de narração não disponível",
                    "TTS Indisponível",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] options = {
                "→ Selecionar Voz Brasileira (F8)",
                "🎙️ Testar Voz Atual (F9)",
                "📊 Diagnóstico do Sistema (F7)",
                "🎙️ Instalar Edge TTS (Recomendado)",
                "❌ Cancelar"
        };

        int choice = JOptionPane.showOptionDialog(this,
                "Escolha uma opção de configuração de voz:",
                "Menu de Voz Brasileira",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);

        switch (choice) {
            case 0: selectBrazilianVoice(); break;
            case 1: testCurrentVoice(); break;
            case 2: showTTSDiagnostic(); break;
            case 3: showEdgeTTSInstallation(); break;
            default: break;
        }
    }

    /**
     * NOVO: Mostra instruções para instalar Edge TTS (voz mais natural)
     */
    private void showEdgeTTSInstallation() {
        String instructions = """
=================================================================
  COMO INSTALAR MICROSOFT EDGE TTS (Vozes Naturais Brasileiras)
=================================================================

        O Edge TTS oferece as vozes mais naturais em português brasileiro:
        - Francisca (Feminina) - Muito natural e clara
                - António (Masculina) - Excelente para leitura longa

        INSTALAÇãO:

        1. INSTALAR PYTHON (se não tiver):
   • Baixe em: https://python.org
   • Marque "Add Python to PATH" durante instalação

        2. INSTALAR EDGE-TTS:
   • Abra CMD ou PowerShell como administrador
   • Digite: pip install edge-tts
   • Aguarde download e instalação

        3. TESTAR INSTALAÇãO:
   • Digite: edge-tts --list-voices
   • Deve mostrar lista com vozes pt-BR

        4. REINICIAR BLINDBROWSER:
   • Feche e abra novamente o programa
   • Pressione F8 para selecionar nova voz

        DICA: Após instalação, a voz ficará MUITO mais natural!

                ALTERNATIVA: Use as vozes SAPI do Windows (menos naturais mas funcionais)
        """;

                JTextArea textArea = new JTextArea(instructions);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                textArea.setEditable(false);

                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(600, 500));

                JOptionPane.showMessageDialog(this,
                        scrollPane,
                        "Instalação Edge TTS - Vozes Brasileiras",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            //-----------------------------*---------------*----------------------------

            /**
             * METODO ANTIGO 08/08/25* Narra o conteúdo atual*
             */
            private void narrateCurrentContent() {
                if (ttsManager == null) {
                    JOptionPane.showMessageDialog(this,
                            "Sistema de narração não disponível",
                            "Aviso",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Obter texto atual
                JScrollPane scrollPane = (JScrollPane) jTabbedPane1.getSelectedComponent();
                JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();
                String text = textArea.getText();

                if (text == null || text.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Não há conteúdo para narrar",
                            "Aviso",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Indicador visual
                statusLabel.setText("🎙️ Narrando... (F5: pausar, F6: parar)");

                // Narrar em thread separada
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        ttsManager.narrate(text);
                        return null;
                    }

                    @Override
                    protected void done() {
                        statusLabel.setText("Narração concluída");
                    }
                };

                worker.execute();
            }

            /**
             * Pausa ou continua a narração
             */
            private void toggleNarration() {
                if (ttsManager != null) {
                    ttsManager.togglePause();

                    if (ttsManager.isPaused()) {
                        statusLabel.setText("⏸ Narração pausada (F5 para continuar)");
                    } else {
                        statusLabel.setText("⏯ Narração continuada");
                    }
                }
            }

            /**
             * Para a narração
             */
            private void stopNarration() {
                if (ttsManager != null) {
                    ttsManager.stopNarration();
                    statusLabel.setText("⏹️ Narração parada");
                }
            }

            /**
             * Aumenta velocidade da narração
             */
            private void increaseNarrationSpeed() {
                if (ttsManager != null) {
                    ttsManager.increaseSpeechRate();
                }
            }

            /**
             * Diminui velocidade da narração
             */
            private void decreaseNarrationSpeed() {
                if (ttsManager != null) {
                    ttsManager.decreaseSpeechRate();
                }
            }

            /**
             *  MENU DE AJUDA RAPIDO F1
             */
            private void showHelp() {
                String help = """
=================================================================
BLINDBROWSER v1.5 - AJUDA RÁPIDA
=================================================================

ATALHOS DE TECLADO:
- F1        - Esta ajuda
- F2        - Alternar entre modo resumido/completo
- F3        - Estatísticas do conteúdo
- F4        - NARRAR conteúdo atual
- F5        - Pausar/Continuar narração
- F6        - Parar narração
- F7        - Diagnóstico do sistema TTS
- F8        - Selecionar voz brasileira
- F9        - Testar voz atual
- +         - Aumentar velocidade da narração
- -         - Diminuir velocidade da narração
- Ctrl+F    - Buscar no conteúdo completo
- Ctrl+S    - Salvar conteúdo atual
- Ctrl+R    - Relatório de comparação de cache
- Alt+N     - Foco na barra de URL
- Ctrl+T    - Nova aba

MODOS DE LEITURA:
[R] RESUMIDO - Essência do conteúdo (~10 min leitura Braille)
[C] COMPLETO - Conteúdo integral

NARRAÇãO BRASILEIRA:
[F4] para narrar com voz brasileira
[F8] para escolher voz (Francisca, António, etc.)
[F9] para testar a voz selecionada
[+/-] para ajustar velocidade

VOZES RECOMENDADAS:
- Edge TTS: Francisca/António (Mais naturais)
- SAPI: Helena/Daniel (Windows padrão)

DICAS:
- Comece sempre pelo modo resumido
- Use F2 se precisar de mais detalhes
- F4 narra o conteúdo atual em voz alta
- Para melhor qualidade: instale Edge TTS (Ctrl+V)
- Use F8 para experimentar diferentes vozes
- F9 testa a voz antes de narrar conteúdo longo
""";
        JTextArea helpArea = new JTextArea(help);
        helpArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        helpArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(helpArea);
        scrollPane.setPreferredSize(new Dimension(550, 500));

        JOptionPane.showMessageDialog(this, scrollPane, "Ajuda",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Define o texto na é área de texto, garantindo atualização da interface.
     * Este método foi completamente reescrito para garantir maior robustez.
     */
    public void setTextoArea(String t) {
        if (t == null) {
            System.err.println("Texto nulo recebido para exibição");
            return;
        }

        // Usar invokeLater para garantir que a atualização da UI ocorra na thread de eventos
        SwingUtilities.invokeLater(() -> {
            try {
                JScrollPane scrollPane = (JScrollPane) jTabbedPane1.getSelectedComponent();
                if (scrollPane == null) {
                    scrollPane = jScrollPane1;
                }

                JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();
                if (textArea == null) {
                    textArea = jTextAreaConteudo;
                }

                // Preservar estado atual de edição
                boolean oldEditable = textArea.isEditable();
                textArea.setEditable(true);

                // Definir texto e posicionar cursor no início
                textArea.setText(t);
                textArea.setCaretPosition(0);

                // Restaurar estado de edição
                textArea.setEditable(oldEditable);

                // Forçar atualização visual
                textArea.repaint();
                scrollPane.repaint();
            } catch (Exception e) {
                System.err.println("Erro ao definir texto na área: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }


    public BlindBrowser getThis() {
        return this;
    }
    //GEN-LAST:event_jButtonSerialMouseClicked

    // Adicione ã  classe BlindBrowser
    private void simularEnvioSerial(String texto) {
        JOptionPane.showMessageDialog(this,
                "Simulação de envio ao dispositivo Braille:\n\n" +
                        (texto.length() > 200 ? texto.substring(0, 200) + "..." : texto),
                "Envio Serial Simulado",
                JOptionPane.INFORMATION_MESSAGE);

        // Sa]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]lvar em arquivo para demonstração
        try {
            FileWriter writer = new FileWriter("ultima_transmissao_braille.txt");
            writer.write(texto);
            writer.close();
            System.out.println("Conteúdo salvo em 'ultima_transmissao_braille.txt'");
        } catch (IOException e) {
            System.err.println("Erro ao salvar simulação: " + e.getMessage());
        }
    }

    // Modifique o método que envia dados seriais para usar esta simulação como fallback
// Na classe LeituraEscrita ou onde for apropriado
    public void enviarDadosSerial(String dados) {
        try {
            // Tentar enviar normalmente
            // Código original aqui...
        } catch (Exception e) {
            System.err.println("Erro ao enviar pela porta serial: " + e.getMessage());
            // Usar simulação como fallback
            this.simularEnvioSerial(dados);
        }
    }

    private void jMenuItemSairActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jMenuItemSairActionPerformed
// TODO add your handling code here:
        System.exit(1);
    }//GEN-LAST:event_jMenuItemSairActionPerformed

    private void jMenuConfigurar1ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jMenuConfigurar1ActionPerformed
// TODO add your handling code here:
        config = new Configuracao();


        config.setDefaultCloseOperation(1);
        config.setSize(351, 304);
        Dimension dim = config.getToolkit().getScreenSize();
        int x = (int) (dim.getWidth() - config.getSize().getWidth()) / 2;
        int y = (int) (dim.getHeight() - config.getSize().getHeight()) / 2;
        config.setLocation(x, y);
        config.setLocationRelativeTo(this);
        config.setVisible(true);
    }//GEN-LAST:event_jMenuConfigurar1ActionPerformed
    /**
     * NOVO: Salvar conteúdo atual
     */
    private void saveCurrentContent() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("conteudo_blindbrowser.txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                dualModeManager.exportCurrentContent(
                        fileChooser.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(this,
                        "Conteúdo salvo com sucesso!",
                        "Salvar",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Erro ao salvar: " + ex.getMessage(),
                        "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void jButtonAbasMouseClicked(MouseEvent evt) {//GEN-FIRST:event_jButtonAbasMouseClicked
// TODO add your handling code here:

        JScrollPane jScrollPane = new JScrollPane();

        JTextArea jTextAreaConteudo2 = new JTextArea();

        jTextAreaConteudo2.setColumns(20);
        jTextAreaConteudo2.setEditable(false);
        jTextAreaConteudo2.setFont(new Font("Monospaced", 1, 17)); // NOI18N

        jTextAreaConteudo2.setRows(5);
        jScrollPane.setViewportView(jTextAreaConteudo2);

        jTabbedPane1.addTab("Aba " + ++contaba, jScrollPane);
        jTabbedPane1.setSelectedComponent(jScrollPane);
        jTextFieldURL.setText("http://");


        jTabbedPane1.setFocusable(false);
        jTextAreaConteudo2.setFocusable(false);
    }//GEN-LAST:event_jButtonAbasMouseClicked

    private void jButtonAbasActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonAbasActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_jButtonAbasActionPerformed

    public static void main(String args[]) {
        /*JFrame.setDefaultLookAndFeelDecorated(true);
        try {
        UIManager.setLookAndFeel(new SubstanceOfficeSilver2007LookAndFeel());
        } catch (Exception e) {
        System.out.println("Substance Raven Graphite failed to initialize");
        }*/
        EventQueue.invokeLater(new Runnable() {

            public void run() {

                new BlindBrowser().setVisible(true);
            }
        });
    }
    //NEW(06/08): um metodo para imprimir relatório de comparação manualmente
    public void printComparisonReport() {
        String report = contentCache.getComparisonReport();
        System.out.println("\n" + report);
    }
    /**
     * Método que inicia a execução da aplicação.
     * Este método é chamado quando a aplicação é iniciada.
     */

    public void run() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void Abas() {
        jButtonAbasMouseClicked(new MouseEvent(rootPane, WIDTH, WIDTH, WIDTH, WIDTH, WIDTH, WIDTH, rootPaneCheckingEnabled));
    /*System.out.println("Apertou a tecla 'e' para abrir abas");
    //MeuParser parser = new MeuParser();

    JScrollPane jScrollPane = new javax.swing.JScrollPane();

    JTextArea jTextAreaConteudo2 = new javax.swing.JTextArea();

    jTextAreaConteudo2.setColumns(20);
    jTextAreaConteudo2.setEditable(false);
    jTextAreaConteudo2.setFont(new java.awt.Font("Monospaced", 1, 17)); // NOI18N

    jTextAreaConteudo2.setRows(5);
    jScrollPane.setViewportView(jTextAreaConteudo2);

    jTabbedPane1.addTab("Aba", jScrollPane);

    jTabbedPane1.setSelectedComponent(jScrollPane);

    //parser.setTexto(""); //limpa a variavel texto que contem o conteudo textual da pagina que fora aberta antes

    jTextFieldURL.setText("http://");*/

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton jButtonAbas;
    private JButton jButtonIr;
    private JLabel jLabel1;
    private JMenu jMenuArquivo;
    private JMenu jMenuArquivo1;
    private JMenuBar jMenuBarra;
    private JMenuBar jMenuBarra1;
    private JMenuItem jMenuConfigurar;
    private JMenuItem jMenuConfigurar1;
    private JMenu jMenuFerramentas;
    private JMenu jMenuFerramentas1;
    private JMenuItem jMenuItemSair;
    private JMenuItem jMenuItemSair1;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JScrollPane jScrollPane1;
    private JTabbedPane jTabbedPane1;
    private JTextArea jTextAreaConteudo;
    private JTextField jTextFieldURL;

    // End of variables declaration//GEN-END:variables
}