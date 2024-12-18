package com.example.utils.reporting;

import com.example.utils.CohereLLM;
import com.example.utils.LocatorUpdater;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.internal.text.StringEscapeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HealingReportGenerator {
    private static final Logger logger = Logger.getLogger(HealingReportGenerator.class.getName());

    /**
     * Generate an HTML report
     * @param failedLocatorPath - Path to the failed locator file
     * @param failedPageFilePath - Path to the failed page file
     * @param llmOutputPath - Path to the LLM output file
     * @param validatedLocPath - Path to the validated locators file
     * @return - HTML report content
     * @throws IOException - If an I/O error occurs
     */
    public String generateHTMLReport(String failedLocatorPath, String failedPageFilePath, String llmOutputPath, String validatedLocPath) throws IOException {
        String failedLocatorContent = readFileContent(failedLocatorPath);
        String failedPageContent = readFileContent(failedPageFilePath);
        String llmOutputContent = readFileContent(llmOutputPath);
        String validatedLocContent = readFileContent(validatedLocPath);
        String firstFoundLocator = LocatorUpdater.getFirstFoundLocator(validatedLocPath);
        String aiSummary = getAISummary(failedLocatorContent, failedPageContent, firstFoundLocator);

        return "<html>" +
                "<head>" +
                "<title>Healing Report</title>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; background: #f0f0f0; }" +
                "h1 { color: #333; text-align: center; }" +
                "pre { background: #f4f4f4; padding: 10px; border: 1px solid #ddd; border-radius: 5px; white-space: pre-wrap; word-wrap: break-word; }" +
                ".section { margin-bottom: 20px; }" +
                ".accordion { cursor: pointer; padding: 15px; border: none; text-align: left; outline: none; font-size: 18px; transition: background-color 0.4s ease; background-color: #007bff; color: white; border-radius: 5px; display: flex; justify-content: space-between; align-items: center; }" +
                ".accordion:hover { background-color: #0056b3; }" +
                ".accordion:after { content: '\\002B'; font-size: 24px; transition: transform 0.4s ease; }" +
                ".accordion.active:after { content: '\\2212'; transform: rotate(180deg); }" +
                ".section-content { max-height: 0; overflow: hidden; transition: max-height 0.4s ease; padding: 0 18px; background-color: white; border-radius: 5px; margin-top: 10px; }" +
                ".section-content.show { max-height: 500px; padding: 15px; }" +
                ".summary { background: linear-gradient(135deg, #e0f7fa, #80deea); padding: 20px; border-radius: 10px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); }" +
                ".summary h2 { color: #00796b; }" +
                ".summary p { color: #004d40; font-size: 16px; }" +
                "</style>" +
                "<script>" +
                "document.addEventListener('DOMContentLoaded', function() {" +
                "  var acc = document.getElementsByClassName('accordion');" +
                "  for (var i = 0; i < acc.length; i++) {" +
                "    acc[i].addEventListener('click', function() {" +
                "      this.classList.toggle('active');" +
                "      var panel = this.nextElementSibling;" +
                "      if (panel.style.maxHeight) {" +
                "        panel.style.maxHeight = null;" +
                "        panel.classList.remove('show');" +
                "      } else {" +
                "        panel.style.maxHeight = panel.scrollHeight + 'px';" +
                "        panel.classList.add('show');" +
                "      }" +
                "    });" +
                "  }" +
                "});" +
                "</script>" +
                "</head>" +
                "<body>" +
                "<h1>Healing Report</h1>" +
                "<div class='section summary'>" +
                "<h2>Summary</h2>" +
                "<p><strong>Failed Locator:</strong> <code>" + escapeHtml(failedLocatorContent) + "</code></p>" +
                "<p><strong>Replaced Locator:</strong> <code>" + escapeHtml(firstFoundLocator) + "</code></p>" +
                "<p><strong>AI Summary:</strong> " + escapeHtml(aiSummary) + "</p>" +
                "</div>" +
                "<div class='section'>" +
                "<button class='accordion'>Failed Locator </button>" +
                "<div id='failedLocator' class='section-content'><pre>" + escapeHtml(failedLocatorContent) + "</pre></div>" +
                "</div>" +
                "<div class='section'>" +
                "<button class='accordion'>Processed HTML </button>" +
                "<div id='processedHtml' class='section-content'><pre>" + escapeHtml(failedPageContent) + "</pre></div>" +
                "</div>" +
                "<div class='section'>" +
                "<button class='accordion'>LLM Output </button>" +
                "<div id='llmOutput' class='section-content'><pre>" + escapeHtml(llmOutputContent) + "</pre></div>" +
                "</div>" +
                "<div class='section'>" +
                "<button class='accordion'>Validated Locators </button>" +
                "<div id='validatedLocators' class='section-content'><pre>" + escapeHtml(validatedLocContent) + "</pre></div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Read file content
     * @param filePath - Path to the file
     * @return - File content
     * @throws IOException - If an I/O error occurs
     */
    private String readFileContent(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    /**
     * Escape HTML content
     * @param content - HTML content
     * @return - Escaped HTML content
     */
    private String escapeHtml(String content) {
        return StringEscapeUtils.escapeHtml4(content);
    }

    /**
     * Get AI summary
     * @param failedLocatorContent - Failed locator content
     * @param pageSourceContent    - Page source content
     * @param firstFoundLocator    - First found locator
     * @return - AI summary
     */
    private String getAISummary(String failedLocatorContent, String pageSourceContent, String firstFoundLocator) {
        String prompt = "Summarize the following content in 3-4 lines:\n" +
                "Failed Locator: " + failedLocatorContent + "\n" +
                "Page Source: " + pageSourceContent + "\n" +
                "Replaced Locator: " + firstFoundLocator;
        String jsonResponse = String.valueOf(CohereLLM.sendCohereRequest(prompt));

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            return jsonNode.get("text").asText();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing AI summary response", e);
            return "Error parsing AI summary response";
        }
    }
}