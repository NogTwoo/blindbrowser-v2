package meuparser.pipelines;

/**
 * Runner para execução do benchmark adaptativo
 * Testa o sistema de compressão inteligente por categoria
 */
public class BenchmarkRunner {
    public static void main(String[] args) {
        System.out.println("⚡ BENCHMARK ADAPTATIVO - SISTEMA DE COMPRESSãO INTELIGENTE");
        System.out.println("══════════════════════════════════════════════════════════");

        // 🔍 PRIMEIRO: Debug do sistema adaptativo
        System.out.println("🔧 EXECUTANDO DEBUG DO SISTEMA ADAPTATIVO:");
        PerformanceProfiler.debugTokenCountAdaptive();
        System.out.println("\n" + "═".repeat(60) + "\n");

        // URLs organizadas por categoria para teste completo
        String[] diverseUrls = {
                // Notícias (45% redução esperada)
                "https://g1.globo.com/tecnologia/",

                // Educacional (30% redução esperada)
                "https://pt.wikipedia.org/wiki/Java_(linguagem_de_programação)",
                "https://brasilescola.uol.com.br/informatica/",

                // E-commerce seria testado se houvesse URLs
                // Formulários seria testado se houvesse URLs
        };

        PerformanceProfiler profiler = new PerformanceProfiler();
        var results = profiler.runBenchmark(diverseUrls);

        System.out.println("\n🎯 ANÁLISE FINAL:");
        System.out.println("- O sistema adaptativo ajusta a compressão automaticamente");
        System.out.println("- Notícias mantêm mais contexto (45% redução)");
        System.out.println("- Conteúdo educativo preserva detalhes (30% redução)");
        System.out.println("- E-commerce manteria informações críticas (20% redução)");

        System.out.println("\n✅ Benchmark adaptativo concluído!");
        System.out.println("Execute RelatorioDataGenerator para relatório completo.");
    }
}

/** (BENCHMARK SEM ADAPTATIVIDADE)package meuparser.pipelines;

 /**
 * Runner simplificado para execução rápida de benchmarks
 */
/**public class BenchmarkRunner {
 public static void main(String[] args) {
 System.out.println("⚡ BENCHMARK RÁPIDO - DADOS PARA RELATÓRIO");
 System.out.println("═══════════════════════════════════════════");

 // URLs de teste rápido
 String[] quickUrls = {
 "https://pt.wikipedia.org/wiki/Java_(linguagem_de_programação)",
 "https://brasilescola.uol.com.br/informatica/",
 };

 PerformanceProfiler profiler = new PerformanceProfiler();
 var results = profiler.runBenchmark(quickUrls);

 System.out.println("✅ Benchmark concluído! Execute RelatorioDataGenerator para dados completos.");
 }
 }
 */