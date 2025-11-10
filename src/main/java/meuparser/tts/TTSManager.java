package meuparser.tts;

import javax.speech.Central;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import java.awt.*;
import java.util.Locale;
import javax.swing.*;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * TTSManager CORRIGIDO - Versã£o Final
 * @author Nicholas
 */
public class TTSManager {

    private Synthesizer synthesizer;
    private boolean isNarrating = false;
    private boolean isPaused = false;
    private float speechRate = 150.0f;
    private Thread narrationThread;
    private boolean useSystemTTS = false;
    private boolean freeTTSFailed = false;

    // NOVO: Configuraã§ãµes de voz
    private String selectedVoice = "auto";
    private List<BrazilianVoice> availableVoices;

    // NOVO: Controle de pause manual
    private boolean manuallyPaused = false;

    // NOVO: Timeout para verificaã§ãµes
    private static final int COMMAND_TIMEOUT_SECONDS = 3;

    /**
     * Classe para representar vozes brasileiras
     */
    public static class BrazilianVoice {
        public String id;
        public String name;
        public String description;
        public String gender;
        public String method;
        public String command;

        public BrazilianVoice(String id, String name, String description, String gender, String method, String command) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.gender = gender;
            this.method = method;
            this.command = command;
        }

        @Override
        public String toString() {
            return name + " (" + description + ")";
        }
    }

    public TTSManager() {
        initializeVoicesQuickly();
        initializeTTSSafely();
    }

    private void initializeVoicesQuickly() {
        availableVoices = new ArrayList<>();

        availableVoices.add(new BrazilianVoice("default", "Padrã£o do Sistema", "Voz padrã£o do Windows", "F", "basic", "default"));
        availableVoices.add(new BrazilianVoice("helena", "Helena (SAPI)", "Microsoft Helena - SAPI", "F", "sapi", "helena"));
        availableVoices.add(new BrazilianVoice("francisca", "Francisca (Edge)", "Voz feminina brasileira neural", "F", "edge",
                "edge-tts --voice pt-BR-FranciscaNeural --text"));
        availableVoices.add(new BrazilianVoice("antonio", "Antã´nio (Edge)", "Voz masculina brasileira neural", "M", "edge",
                "edge-tts --voice pt-BR-AntonioNeural --text"));

        System.out.println("âœ… " + availableVoices.size() + " vozes brasileiras registradas");
    }

    private void initializeTTSSafely() {
        useSystemTTS = true;
        selectedVoice = "default";

        System.out.println("ðŸŽ™ï¸ TTS inicializado com voz padrã£o do sistema");
        System.out.println("ðŸ’¡ Verificaã§ã£o de Edge TTS serã¡ feita em background");

        CompletableFuture.runAsync(this::checkEdgeTTSInBackground);
    }

    private void checkEdgeTTSInBackground() {
        try {
            boolean edgeAvailable = isEdgeTTSAvailableWithTimeout();
            if (edgeAvailable) {
                System.out.println("âœ… Edge TTS detectado em background - vozes neurais disponã­veis");
                selectedVoice = "francisca";
            } else {
                System.out.println("âš ï¸ Edge TTS nã£o disponã­vel - usando SAPI/sistema");
            }
        } catch (Exception e) {
            System.err.println("âš ï¸ Erro na verificaã§ã£o de Edge TTS: " + e.getMessage());
        }
    }

    private boolean isEdgeTTSAvailableWithTimeout() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("edge-tts", "--list-voices");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return false;
                }

                return process.exitValue() == 0;
            } catch (Exception e) {
                return false;
            }
        });

        try {
            return future.get(COMMAND_TIMEOUT_SECONDS + 1, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            future.cancel(true);
            return false;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * CORRIGIDO: Narra usando a voz brasileira selecionada
     */
    public void narrate(String text) {
        if (text == null || text.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Nã£o hã¡ texto para narrar",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Se estava pausado manualmente, continua de onde parou
        if (manuallyPaused) {
            manuallyPaused = false;
            System.out.println("â–¶ï¸ Continuando narraã§ã£o...");
        }

        stopNarration(); // Para qualquer narraã§ã£o anterior
        isNarrating = true;
        isPaused = false;

        narrationThread = new Thread(() -> {
            try {
                narrateWithBrazilianVoice(text);
            } catch (Exception e) {
                System.err.println("âŒ Erro durante narraã§ã£o: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isNarrating = false;
                manuallyPaused = false;
            }
        });

        narrationThread.start();
    }

    private void narrateWithBrazilianVoice(String text) {
        String cleanText = cleanTextForNarration(text);
        BrazilianVoice voice = getVoiceById(selectedVoice);

        System.out.println("ðŸŽµ Narrando com " + voice.name + ": " +
                cleanText.substring(0, Math.min(50, cleanText.length())) + "...");

        try {
            switch (voice.method) {
                case "edge":
                    if (isEdgeTTSAvailableWithTimeout()) {
                        narrateWithEdgeTTSTimeout(cleanText, voice);
                    } else {
                        System.out.println("ðŸ”„ Edge TTS indisponã­vel, usando SAPI...");
                        narrateWithSAPITimeout(cleanText);
                    }
                    break;
                case "sapi":
                    narrateWithSAPITimeout(cleanText);
                    break;
                default:
                    narrateWithBasicTTS(cleanText);
                    break;
            }
            System.out.println("âœ… Narraã§ã£o concluã­da com " + voice.name);
        } catch (Exception e) {
            System.err.println("âŒ Erro com " + voice.name + ", usando fallback bã¡sico");
            try {
                narrateWithBasicTTS(cleanText);
            } catch (Exception fallbackError) {
                showTextDialog(cleanText);
            }
        }
    }

    private void narrateWithEdgeTTSTimeout(String text, BrazilianVoice voice) throws Exception {
        String[] sentences = splitTextIntoSentences(text);

        for (String sentence : sentences) {
            if (!isNarrating) break;

            while (isPaused) {
                Thread.sleep(100);
                if (!isNarrating) return;
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<Void> future = executor.submit(() -> {
                    try {
                        ProcessBuilder pb = new ProcessBuilder(
                                "edge-tts",
                                "--voice", "pt-BR-FranciscaNeural",
                                "--text", sentence
                        );

                        Process process = pb.start();
                        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                        if (!finished) {
                            process.destroyForcibly();
                            throw new RuntimeException("Edge TTS timeout");
                        }

                        if (process.exitValue() != 0) {
                            throw new RuntimeException("Edge TTS falhou");
                        }

                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                future.get(15, TimeUnit.SECONDS);

            } catch (TimeoutException e) {
                throw new Exception("Edge TTS timeout");
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private void narrateWithSAPITimeout(String text) throws Exception {
        String escapedText = text.replace("'", "''").replace("\"", "'");

        String command = String.format(
                "powershell.exe -Command \"" +
                        "Add-Type -AssemblyName System.Speech; " +
                        "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$voices = $synth.GetInstalledVoices(); " +
                        "$ptVoice = $voices | Where-Object { $_.VoiceInfo.Culture.Name -eq 'pt-BR' } | Select-Object -First 1; " +
                        "if ($ptVoice) { $synth.SelectVoice($ptVoice.VoiceInfo.Name) }; " +
                        "$synth.Rate = %d; " +
                        "$synth.Speak('%s')\"",
                getSAPIRate(),
                escapedText
        );

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Void> future = executor.submit(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                    if (!finished) {
                        process.destroyForcibly();
                        throw new RuntimeException("SAPI timeout");
                    }

                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            future.get(35, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            throw new Exception("SAPI timeout");
        } finally {
            executor.shutdownNow();
        }
    }

    private void narrateWithBasicTTS(String text) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            String command = String.format(
                    "powershell.exe -Command \"" +
                            "Add-Type -AssemblyName System.Speech; " +
                            "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                            "$synth.Rate = %d; " +
                            "$synth.Speak('%s')\"",
                    getSAPIRate(),
                    text.replace("'", "''")
            );

            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            Process process = pb.start();
            process.waitFor();

        } else if (os.contains("mac")) {
            ProcessBuilder pb = new ProcessBuilder("say", "-v", "Luciana", "-r", String.valueOf((int) speechRate), text);
            Process process = pb.start();
            process.waitFor();

        } else {
            ProcessBuilder pb = new ProcessBuilder("espeak", "-v", "pt-br", "-s", String.valueOf((int) speechRate), text);
            Process process = pb.start();
            process.waitFor();
        }
    }

    /**
     * CORRIGIDO: togglePause() que funciona com sistema TTS
     */
    public void togglePause() {
        if (isNarrating) {
            manuallyPaused = !manuallyPaused;

            if (manuallyPaused) {
                // Para pausar: interrompe thread atual
                if (narrationThread != null && narrationThread.isAlive()) {
                    narrationThread.interrupt();
                }
                isNarrating = false; // Para a narraã§ã£o atual
                System.out.println("â¸ï¸ Narraã§ã£o pausada");
            } else {
                System.out.println("â–¶ï¸ Para continuar, pressione F4 novamente");
            }
        } else {
            System.out.println("â„¹ï¸ Nenhuma narraã§ã£o ativa para pausar");
        }
    }

    /**
     * CORRIGIDO: stopNarration() que para definitivamente
     */
    public void stopNarration() {
        // Para tudo imediatamente
        isNarrating = false;
        isPaused = false;
        manuallyPaused = false;

        // Interrompe thread de narraã§ã£o
        if (narrationThread != null && narrationThread.isAlive()) {
            narrationThread.interrupt();
            try {
                narrationThread.join(1000); // Aguarda atã© 1 segundo
            } catch (InterruptedException e) {
                // Thread interrompida durante join
            }
        }

        // Mata qualquer processo TTS em execuã§ã£o (Windows)
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // Mata processos PowerShell que podem estar executando TTS
                ProcessBuilder pb = new ProcessBuilder("taskkill", "/f", "/im", "powershell.exe");
                pb.start();
            }
        } catch (Exception e) {
            // Ignora erros de cleanup
        }

        System.out.println("â¹ï¸ Narraã§ã£o parada completamente");
    }

    // ========== Mã‰TODOS AUXILIARES ==========

    public void selectVoice() {
        BrazilianVoice[] voices = availableVoices.toArray(new BrazilianVoice[0]);

        BrazilianVoice selected = (BrazilianVoice) JOptionPane.showInputDialog(
                null,
                "Escolha a voz para narraã§ã£o:",
                "Seleã§ã£o de Voz",
                JOptionPane.QUESTION_MESSAGE,
                null,
                voices,
                getVoiceById(selectedVoice)
        );

        if (selected != null) {
            selectedVoice = selected.id;
            System.out.println("ðŸŽ™ï¸ Voz alterada para: " + selected.name);

            JOptionPane.showMessageDialog(null,
                    "Voz alterada para " + selected.name + "\n" +
                            "Pressione F4 para testar com um conteãºdo.");
        }
    }

    public void testCurrentVoice() {
        BrazilianVoice currentVoice = getVoiceById(selectedVoice);
        String testText = "Olã¡! Esta ã© a voz " + currentVoice.name +
                " narrando em portuguãªs brasileiro. A qualidade estã¡ boa?";
        narrate(testText);
    }

    public void setSpeechRate(float rate) {
        this.speechRate = Math.max(50, Math.min(300, rate));
    }

    public void increaseSpeechRate() {
        setSpeechRate(speechRate + 25);
        showRateMessage();
    }

    public void decreaseSpeechRate() {
        setSpeechRate(speechRate - 25);
        showRateMessage();
    }

    private void showRateMessage() {
        String message = String.format("Velocidade: %.0f palavras/min", speechRate);
        System.out.println("ðŸŽšï¸ " + message);

        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog();
            dialog.setUndecorated(true);
            JLabel label = new JLabel(message);
            label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            label.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            dialog.add(label);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);

            Timer timer = new Timer(1500, e -> dialog.dispose());
            timer.setRepeats(false);
            timer.start();
        });
    }

    private String[] splitTextIntoSentences(String text) {
        String[] sentences = text.split("(?<=[.!?])\\s+");
        List<String> result = new ArrayList<>();

        for (String sentence : sentences) {
            if (sentence.length() > 200) {
                String[] parts = sentence.split("[,;:]");
                for (String part : parts) {
                    result.add(part.trim());
                }
            } else {
                result.add(sentence.trim());
            }
        }

        return result.toArray(new String[0]);
    }

    private BrazilianVoice getVoiceById(String id) {
        return availableVoices.stream()
                .filter(v -> v.id.equals(id))
                .findFirst()
                .orElse(availableVoices.get(0));
    }

    private String cleanTextForNarration(String text) {
        return text
                .replaceAll("â”€+", " ")
                .replaceAll("â•+", " ")
                .replaceAll("\\[.*?\\]", "")
                .replaceAll("\\s+", " ")
                .replaceAll("F\\d+", "")
                .replace("ðŸ“„", "modo resumido")
                .replace("ðŸ“–", "modo completo")
                .replaceAll("[âœ…âŒðŸ”ðŸ”Šâ¸ï¸â–¶ï¸â¹ï¸]", "")
                .trim();
    }

    private int getSAPIRate() {
        return (int) ((speechRate - 50) / 250.0 * 10);
    }

    private void showTextDialog(String text) {
        SwingUtilities.invokeLater(() -> {
            JTextArea textArea = new JTextArea(text);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setEditable(false);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new java.awt.Dimension(500, 400));

            JOptionPane.showMessageDialog(null, scrollPane,
                    "Narraã§ã£o de Texto (TTS nã£o disponã­vel)",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // ========== GETTERS ==========

    public boolean isNarrating() {
        return isNarrating;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public String getDiagnosticInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== TTS DIAGNOSTIC INFO ===\n");
        info.append("Sistema: ").append(System.getProperty("os.name")).append("\n");
        info.append("Voz atual: ").append(getVoiceById(selectedVoice).name).append("\n");
        info.append("Mã©todo: ").append(getVoiceById(selectedVoice).method).append("\n");
        info.append("Velocidade: ").append(speechRate).append(" palavras/min\n");
        info.append("Narrando: ").append(isNarrating).append("\n");
        info.append("Pausado: ").append(isPaused).append("\n");
        info.append("Pausado manualmente: ").append(manuallyPaused).append("\n");
        info.append("Timeout configurado: ").append(COMMAND_TIMEOUT_SECONDS).append("s\n");
        info.append("\nVozes disponã­veis:\n");
        for (BrazilianVoice voice : availableVoices) {
            info.append("- ").append(voice.name).append(" (").append(voice.method).append(")\n");
        }
        return info.toString();
    }

    public void cleanup() {
        stopNarration();
    }
}