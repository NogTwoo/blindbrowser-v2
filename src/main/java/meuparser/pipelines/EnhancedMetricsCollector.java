package meuparser.pipelines;

/**
 * Versã£o aprimorada do MetricsCollector que inclui validaã§ã£o baseada em Ground Truth
 */

public class EnhancedMetricsCollector extends MetricsCollector {

    private final GroundTruthValidator validator;

    public EnhancedMetricsCollector() {
        super();
        this.validator = new GroundTruthValidator();
    }

    /**
     * Agrega mã©tricas incluindo validaã§ã£o por ground truth
     */
    @Override
    public SystemMetrics aggregateMetrics(java.util.List<PerformanceProfiler.ProcessingMetrics> performanceData) {
        // Chama metodo da classe pai
        SystemMetrics baseMetrics = super.aggregateMetrics(performanceData);

        // Adiciona validaã§ã£o por ground truth
        java.util.List<GroundTruthValidator.ReferenceData> groundTruth =
                GroundTruthValidator.createGroundTruth();

        GroundTruthValidator.PrecisionRecallResults validation =
                validator.validateExtraction(performanceData, groundTruth);

        // Atualiza mã©tricas com valores reais
        baseMetrics.avgPrecision = validation.avgPrecision;
        baseMetrics.avgRecall = validation.avgRecall;
        baseMetrics.avgF1Score = validation.avgF1Score;

        return baseMetrics;
    }

    /**
     * Gera relatã³rio detalhado incluindo validaã§ã£o por site
     */
    public void generateEnhancedReport(SystemMetrics metrics,
                                       java.util.List<PerformanceProfiler.ProcessingMetrics> performanceData) {
        // Gera relatã³rio base
        super.generateMetricsReport(metrics);

        // Adiciona anã¡lise detalhada por site
        System.out.println("\nðŸŽ¯ VALIDAã‡ãƒO DETALHADA POR SITE:");
        System.out.println("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");

        java.util.List<GroundTruthValidator.ReferenceData> groundTruth =
                GroundTruthValidator.createGroundTruth();

        GroundTruthValidator validator = new GroundTruthValidator();
        GroundTruthValidator.PrecisionRecallResults validation =
                validator.validateExtraction(performanceData, groundTruth);

        for (GroundTruthValidator.SiteValidation siteVal : validation.siteValidations) {
            System.out.printf("\nðŸ“Š %s:\n", siteVal.siteName);
            System.out.printf("  - Precisã£o: %.3f\n", siteVal.precision);
            System.out.printf("  - Recall: %.3f\n", siteVal.recall);
            System.out.printf("  - F1-Score: %.3f\n", siteVal.f1Score);
            System.out.printf("  - Remoã§ã£o de ruã­do: %.3f\n", siteVal.noiseRemovalEfficiency);
            System.out.printf("  - Cobertura de palavras-chave: %.3f\n", siteVal.keywordCoverage);
        }

        System.out.println("\nðŸ“ˆ INTERPRETAã‡ãƒO DOS RESULTADOS:");
        System.out.println("- Precisã£o > 0.7: Boa qualidade de extraã§ã£o");
        System.out.println("- Recall > 0.6: Boa cobertura do conteãºdo");
        System.out.println("- F1-Score > 0.65: Bom equilã­brio geral");
        System.out.println("- Remoã§ã£o de ruã­do > 0.5: Eficiente na limpeza");
    }
}