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
 * TTSManager CORRIGIDO - Versão Final
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

    // NOVO: Configuraçãµes de voz
    private String selectedVoice = "auto";
    private List<BrazilianVoice> availableVoices;

    // NOVO: Controle de pause manual
    private boolean manuallyPaused = false;

    // NOVO: Timeout para verificaçãµes
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

        availableVoices.add(new BrazilianVoice("default", "Padrão do Sistema", "Voz padrão do Windows", "F", "basic", "default"));
        availableVoices.add(new BrazilianVoice("helena", "Helena (SAPI)", "Microsoft Helena - SAPI", "F", "sapi", "helena"));
        availableVoices.add(new BrazilianVoice("francisca", "Francisca (Edge)", "Voz feminina brasileira neural", "F", "edge",
                "edge-tts --voice pt-BR-FranciscaNeural --text"));
        availableVoices.add(new BrazilianVoice("antonio", "Antônio (Edge)", "Voz masculina brasileira neural", "M", "edge",
                "edge-tts --voice pt-BR-AntonioNeural --text"));

        System.out.println("✅ " + availableVoices.size() + " vozes brasileiras registradas");
    }

    private void initializeTTSSafely() {
        useSystemTTS = true;
        selectedVoice = "default";

        System.out.println("⚙️ TTS inicializado com voz padrão do sistema");
        System.out.println("🔄 Verificação de Edge TTS será feita em background");

        CompletableFuture.runAsync(this::checkEdgeTTSInBackground);
    }

    private void checkEdgeTTSInBackground() {
        try {
            boolean edgeAvailable = isEdgeTTSAvailableWithTimeout();
            if (edgeAvailable) {
                System.out.println("✅ Edge TTS detectado em background - vozes neurais disponíveis");
                selectedVoice = "francisca";
            } else {
                System.out.println("⚠️ Edge TTS não disponível - usando SAPI/sistema");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erro na verificação de Edge TTS: " + e.getMessage());
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
                    "Não há texto para narrar",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Se estava pausado manualmente, continua de onde parou
        if (manuallyPaused) {
            manuallyPaused = false;
            System.out.println("▶️ Continuando narração...");
        }

        stopNarration(); // Para qualquer narração anterior
        isNarrating = true;
        isPaused = false;

        narrationThread = new Thread(() -> {
            try {
                narrateWithBrazilianVoice(text);
            } catch (Exception e) {
                System.err.println("❗ Erro durante narração: " + e.getMessage());
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

        System.out.println("🔄 Narrando com " + voice.name + ": " +
                cleanText.substring(0, Math.min(50, cleanText.length())) + "...");

        try {
            switch (voice.method) {
                case "edge":
                    if (isEdgeTTSAvailableWithTimeout()) {
                        narrateWithEdgeTTSTimeout(cleanText, voice);
                    } else {
                        System.out.println("🔄”„ Edge TTS indisponível, usando SAPI...");
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
            System.out.println("✅ Narração concluída com " + voice.name);
        } catch (Exception e) {
            System.err.println("❗ Erro com " + voice.name + ", usando fallback básico");
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
                isNarrating = false; // Para a narração atual
                System.out.println("⏹ Narração pausada");
            } else {
                System.out.println("▶️ Para continuar, pressione F4 novamente");
            }
        } else {
            System.out.println("ℹ️ Nenhuma narração ativa para pausar");
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

        // Interrompe thread de narração
        if (narrationThread != null && narrationThread.isAlive()) {
            narrationThread.interrupt();
            try {
                narrationThread.join(1000); // Aguarda até 1 segundo
            } catch (InterruptedException e) {
                // Thread interrompida durante join
            }
        }

        // Mata qualquer processo TTS em execução (Windows)
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // Mata processos PowerShell que podem estar executando TTS
                ProcessBuilder pb = new ProcessBuilder("taskkill", "/f", "/im", "powershell.exe");
                pb.start();
            }
        } catch (Exception e) {
            // Ignora erros de cleanup
        }

        System.out.println("⏹️Narração parada completamente");
    }

    // ========== MÉTODOS AUXILIARES ==========

    public void selectVoice() {
        BrazilianVoice[] voices = availableVoices.toArray(new BrazilianVoice[0]);

        BrazilianVoice selected = (BrazilianVoice) JOptionPane.showInputDialog(
                null,
                "Escolha a voz para narração:",
                "Seleção de Voz",
                JOptionPane.QUESTION_MESSAGE,
                null,
                voices,
                getVoiceById(selectedVoice)
        );

        if (selected != null) {
            selectedVoice = selected.id;
            System.out.println("🔄⚙️ Voz alterada para: " + selected.name);

            JOptionPane.showMessageDialog(null,
                    "Voz alterada para " + selected.name + "\n" +
                            "Pressione F4 para testar com um conteãºdo.");
        }
    }

    public void testCurrentVoice() {
        BrazilianVoice currentVoice = getVoiceById(selectedVoice);
        String testText = "Olá! Esta é a voz " + currentVoice.name +
                " narrando em portuguãªs brasileiro. A qualidade está boa?";
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
        System.out.println("🔄⚚️ " + message);

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
                .replaceAll("═+", " ")
                .replaceAll("─+", " ")
                .replaceAll("\\[.*?\\]", "")
                .replaceAll("\\s+", " ")
                .replaceAll("F\\d+", "")
                .replace("🔄“„", "modo resumido")
                .replace("🔄“–", "modo completo")
                .replaceAll("[✅❌🔄” 🔄” ⏸ ▶ ⏹]", "")
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
                    "Narração de Texto (TTS não disponível)",
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
        info.append("Método: ").append(getVoiceById(selectedVoice).method).append("\n");
        info.append("Velocidade: ").append(speechRate).append(" palavras/min\n");
        info.append("Narrando: ").append(isNarrating).append("\n");
        info.append("Pausado: ").append(isPaused).append("\n");
        info.append("Pausado manualmente: ").append(manuallyPaused).append("\n");
        info.append("Timeout configurado: ").append(COMMAND_TIMEOUT_SECONDS).append("s\n");
        info.append("\nVozes disponíveis:\n");
        for (BrazilianVoice voice : availableVoices) {
            info.append("- ").append(voice.name).append(" (").append(voice.method).append(")\n");
        }
        return info.toString();
    }

    public void cleanup() {
        stopNarration();
    }
}