package com.example.utils;

import com.cohere.api.Cohere;
import com.cohere.api.requests.ChatRequest;
import com.cohere.api.types.*;
import com.example.configurations.ConfigManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CohereLLM {
    private static final Logger logger = Logger.getLogger(CohereLLM.class.getName());
    private static final String API_KEY = System.getenv("COHERE_API_KEY");
    private static final String MODEL = ConfigManager.getProperty("cohere.model");
    private static final String TEMPERATURE = ConfigManager.getProperty("cohere.temperature");

    /**
     * Send request to Cohere API to generate locators based on failed locator and page source
     * @param failedLocatorPath - Path to the failed locator file
     * @param failedPageSourcePath - Path to the failed page source file
     * @param llmOutputPath - Path to store the generated locators
     */
    public static void sendRequestToCohere(String failedLocatorPath, String failedPageSourcePath, String llmOutputPath) {
        if (isApiKeyInvalid()) return;

        try {
            String failedLocatorContent = readFile(failedLocatorPath);
            String pageSourceContent = readFile(failedPageSourcePath);

            String prompt = createPrompt(failedLocatorContent, pageSourceContent);
            NonStreamedChatResponse response = sendCohereRequest(prompt);

            if (response != null) {
                processResponse(response.getText(), llmOutputPath);
            } else {
                logger.log(Level.SEVERE, "Failed to get response from API.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading files or sending request to Cohere", e);
        }
    }

    /**
     * Check if the API key is invalid
     * @return - True if the API key is invalid, false otherwise
     */
    private static boolean isApiKeyInvalid() {
        if (API_KEY == null || API_KEY.isEmpty()) {
            logger.log(Level.SEVERE, "Cohere API key is not set in environment variables.");
            return true;
        }
        return false;
    }

    /**
     * Read file content
     * @param filePath - Path to the file
     * @return - File content
     * @throws IOException - If an I/O error occurs
     */
    private static String readFile(String filePath) throws IOException {
        logger.log(Level.INFO, "Reading file: " + filePath);
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    /**
     * Read prompt template from file
     * @return - Prompt template
     * @throws IOException - If an I/O error occurs
     */
    private static String readPromptTemplate() throws IOException {
        String filePath = "src/main/resources/LLM_prompt.txt";
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    /**
     * Create prompt for Cohere API
     * @param failedLocatorContent - Content of the failed locator
     * @param pageSourceContent - Content of the page source
     * @return - Prompt for Cohere API
     */
    private static String createPrompt(String failedLocatorContent, String pageSourceContent) throws IOException {
        String promptTemplate = readPromptTemplate();
        return String.format(promptTemplate, escapeJson(failedLocatorContent), escapeJson(pageSourceContent));
    }

    /**
     * Send request to Cohere API
     * @param prompt - Prompt for the API
     * @return - Response from the API
     */
    public static NonStreamedChatResponse sendCohereRequest(String prompt) {
        try {
            Cohere cohere = Cohere.builder().token(API_KEY).clientName("snippet").build();
            return cohere.chat(ChatRequest.builder()
                    .message(prompt)
                    .model(MODEL)
                    .temperature(Double.parseDouble(TEMPERATURE))
                    .build());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Network error executing HTTP request", e);
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Process the response from Cohere API
     * @param responseBody - Response from the API
     * @param locatorScoresPath - Path to store locator scores
     */
    private static void processResponse(String responseBody, String locatorScoresPath) {
        logger.log(Level.INFO, "Request to Cohere API successful. " + responseBody);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(locatorScoresPath))) {
            extractAndWriteLocators(responseBody, writer);
            logger.log(Level.INFO, "Locator and score details stored in " + locatorScoresPath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write to file " + locatorScoresPath, e);
        }
    }

    /**
     * Extract locators and scores from the response and write to a file
     * @param responseBody - Response from the API
     * @param writer - BufferedWriter instance
     * @throws IOException - If an I/O error occurs
     */
    private static void extractAndWriteLocators(String responseBody, BufferedWriter writer) throws IOException {
        Pattern locatorPattern = Pattern.compile("\"locator\":\\s*\"(.*?)\"");
        Pattern scorePattern = Pattern.compile("\"score\":\\s*([\\d.]+)");
        Pattern explanationPattern = Pattern.compile("\"explanation\":\\s*\"(.*?)\"");

        Matcher locatorMatcher = locatorPattern.matcher(responseBody);
        Matcher scoreMatcher = scorePattern.matcher(responseBody);
        Matcher explanationMatcher = explanationPattern.matcher(responseBody);

        while (locatorMatcher.find() && scoreMatcher.find() && explanationMatcher.find()) {
            String locator = locatorMatcher.group(1);
            String score = scoreMatcher.group(1);
            String explanation = explanationMatcher.group(1);
            writer.write(String.format("Locator: %s, Score: %s, Explanation: %s%n%n", locator, score, explanation));
        }
    }

    /**
     * Escape JSON content
     * @param str - JSON content
     * @return - Escaped JSON content
     */
    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}