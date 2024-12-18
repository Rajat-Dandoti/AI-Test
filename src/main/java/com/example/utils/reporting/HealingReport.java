package com.example.utils.reporting;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HealingReport {
    private static final Logger logger = Logger.getLogger(HealingReport.class.getName());
    private HealingReport() {}

    /**
     * Generate a healing report
     * @param failedLocatorPath - Path to the failed locator file
     * @param failedPageFilePath - Path to the failed page file
     * @param llmOutputPath - Path to the LLM output file
     * @param validatedLocPath - Path to the validated locators file
     * @param reportFilePath - Path to save the generated report
     */
    public static void generateReport(String failedLocatorPath, String failedPageFilePath, String llmOutputPath, String validatedLocPath, String reportFilePath) {
        try (FileWriter writer = new FileWriter(reportFilePath)) {
            HealingReportGenerator reportGenerator = new HealingReportGenerator();
            String reportContent = reportGenerator.generateHTMLReport(failedLocatorPath, failedPageFilePath, llmOutputPath, validatedLocPath);
            writer.write(reportContent);
            logger.log(Level.INFO, "Healing report generated at: " + reportFilePath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to generate healing report", e);
        }
    }
}