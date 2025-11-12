package meuparser.pipelines;

/**
 * Versão aprimorada do MetricsCollector que inclui validação baseada em Ground Truth
 */

public class EnhancedMetricsCollector extends MetricsCollector {

    private final GroundTruthValidator validator;

    public EnhancedMetricsCollector() {
        super();
        this.validator = new GroundTruthValidator();
    }

    /**
     * Agrega métricas incluindo validação por ground truth
     */
    @Override
    public SystemMetrics aggregateMetrics(java.util.List<PerformanceProfiler.ProcessingMetrics> performanceData) {
        // Chama metodo da classe pai
        SystemMetrics baseMetrics = super.aggregateMetrics(performanceData);

        // Adiciona validação por ground truth
        java.util.List<GroundTruthValidator.ReferenceData> groundTruth =
                GroundTruthValidator.createGroundTruth();

        GroundTruthValidator.PrecisionRecallResults validation =
                validator.validateExtraction(performanceData, groundTruth);

        // Atualiza métricas com valores reais
        baseMetrics.avgPrecision = validation.avgPrecision;
        baseMetrics.avgRecall = validation.avgRecall;
        baseMetrics.avgF1Score = validation.avgF1Score;

        return baseMetrics;
    }

    /**
     * Gera relatório detalhado incluindo validação por site
     */
    public void generateEnhancedReport(SystemMetrics metrics,
                                       java.util.List<PerformanceProfiler.ProcessingMetrics> performanceData) {
        // Gera relatório base
        super.generateMetricsReport(metrics);

        // Adiciona análise detalhada por site
        System.out.println("🎯 VALIDAÇÃO DETALHADA POR SITE:");
        System.out.println("================================");

        java.util.List<GroundTruthValidator.ReferenceData> groundTruth =
                GroundTruthValidator.createGroundTruth();

        GroundTruthValidator validator = new GroundTruthValidator();
        GroundTruthValidator.PrecisionRecallResults validation =
                validator.validateExtraction(performanceData, groundTruth);

        for (GroundTruthValidator.SiteValidation siteVal : validation.siteValidations) {
            System.out.printf("\n📊 %s:\n", siteVal.siteName);
            System.out.printf("  - Precisão: %.3f\n", siteVal.precision);
            System.out.printf("  - Recall: %.3f\n", siteVal.recall);
            System.out.printf("  - F1-Score: %.3f\n", siteVal.f1Score);
            System.out.printf("  - Remoção de ruído: %.3f\n", siteVal.noiseRemovalEfficiency);
            System.out.printf("  - Cobertura de palavras-chave: %.3f\n", siteVal.keywordCoverage);
        }

        System.out.println("\n📈 INTERPRETAÇÃO DOS RESULTADOS:");
        System.out.println("- Precisão > 0.7: Boa qualidade de extração");
        System.out.println("- Recall > 0.6: Boa cobertura do conteãºdo");
        System.out.println("- F1-Score > 0.65: Bom equilíbrio geral");
        System.out.println("- Remoção de ruído > 0.5: Eficiente na limpeza");
    }
}