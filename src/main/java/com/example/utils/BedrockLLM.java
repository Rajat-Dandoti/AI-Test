package com.example.utils;

import com.example.configurations.ConfigManager;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedrockLLM {
    private static final Logger logger = Logger.getLogger(BedrockLLM.class.getName());

    private static final String MODEL_ID = ConfigManager.getProperty("bedrock.modelId");
    private static final String AWS_REGION = ConfigManager.getProperty("aws.region");
    private static final String AWS_PROFILE = ConfigManager.getProperty("aws.profile");
    private static final String TEMPERATURE = ConfigManager.getProperty("bedrock.temperature");
    private static final String TOP_P = ConfigManager.getProperty("bedrock.topP");
    private static final String GUARDRAIL_ID = ConfigManager.getProperty("bedrock.guardrailId");
    private static final String GUARDRAIL_VERSION = ConfigManager.getProperty("bedrock.guardrailVersion");

    /**
     * Private constructor to hide the implicit public one
     */
    private BedrockLLM() {}

    /**
     * Send request to Bedrock API to generate locators based on failed locator and page source
     * @param failedLocatorPath - Path to the failed locator file
     * @param failedPageSourcePath - Path to the failed page source file
     * @param llmOutputPath - Path to store the generated locators
     */
    public static void sendRequestToBedrock(String failedLocatorPath, String failedPageSourcePath, String llmOutputPath) {
        try {
            String failedLocatorContent = readFile(failedLocatorPath);
            String pageSourceContent = readFile(failedPageSourcePath);

            // Count tokens in input files
            int failedLocatorTokens = TokenCounter.countTokensInFile(new File(failedLocatorPath));
            int pageSourceTokens = TokenCounter.countTokensInFile(new File(failedPageSourcePath));

            if (failedLocatorTokens > 0 && pageSourceTokens > 0) {
                logger.log(Level.INFO, "Tokens in failed locator file: "+ failedLocatorTokens);
                logger.log(Level.INFO, "Tokens in page source file: "+ pageSourceTokens);
                String prompt = createPrompt(failedLocatorContent, pageSourceContent);
                String response = sendBedrockRequest(prompt);

                if (response != null) {
                    // Count tokens in the response
                    int responseTokens = new StringTokenizer(response).countTokens();
                    logger.log(Level.INFO, "Tokens in Bedrock response: "+ responseTokens);
                    processResponse(response, llmOutputPath);
                } else {
                    logger.log(Level.SEVERE, "Failed to get response from API.");
                }
            }
            else {
                throw new Exception("Failed to count tokens or no tokens in input files, Locator tokens: "+failedLocatorTokens+" Page Source tokens: "+pageSourceTokens);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending request to Bedrock", e);
        }
    }

    /**
     * Read file content
     * @param filePath - Path to the file
     * @return - Content of the file
     * @throws IOException - If an I/O error occurs
     */
    private static String readFile(String filePath) throws IOException {
        logger.log(Level.INFO, "Reading file: "+ filePath);
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
    private static String readPromptTemplate() throws IOException {
        String filePath = "src/main/resources/LLM_prompt.txt";
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    /**
     * Create prompt for Bedrock API
     * @param failedLocatorContent - Content of the failed locator
     * @param pageSourceContent - Content of the page source
     * @return - Prompt for Bedrock API
     */
    private static String createPrompt(String failedLocatorContent, String pageSourceContent) throws IOException {
        String promptTemplate = readPromptTemplate();
        return String.format(promptTemplate, escapeJson(failedLocatorContent), escapeJson(pageSourceContent));
    }

    /**
     * Send request to Bedrock API
     * @param prompt - Prompt for the API
     * @return - Response from the API
     */
    public static String sendBedrockRequest(String prompt) {

        try (BedrockRuntimeClient bedrockClient = BedrockRuntimeClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.builder().profileName(AWS_PROFILE).build())
                .region(Region.of(AWS_REGION))
                .build()) {

            var message = Message.builder()
                    .content(ContentBlock.fromText(prompt))
                    .role(ConversationRole.USER)
                    .build();

            ConverseResponse response = bedrockClient.converse(request -> request
                    .modelId(MODEL_ID)
                    .messages(message)
                    .inferenceConfig(config -> config
                            .temperature(Float.valueOf(TEMPERATURE))
                            .topP(Float.valueOf(TOP_P)))
                    .guardrailConfig(config -> config
                            .guardrailIdentifier(GUARDRAIL_ID)
                            .guardrailVersion(GUARDRAIL_VERSION))
            );

            return response.output().message().content().getFirst().text();
        } catch (SdkClientException e) {
            logger.log(Level.SEVERE, "Network error executing HTTP request", e);
            return null;
        }
    }

    /**
     * Process response from Bedrock API
     * @param responseBody - Response from the API
     * @param locatorScoresPath - Path to store the locators and scores
     */
    private static void processResponse(String responseBody, String locatorScoresPath) {
        logger.log(Level.INFO, "Request to Bedrock API successful: "+ responseBody);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(locatorScoresPath))) {
            extractAndWriteLocators(responseBody, writer);
            logger.log(Level.INFO, "Locator and score details stored in: "+ locatorScoresPath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write to file " + locatorScoresPath, e);
        }
    }

    /**
     * Extract locators and write to file
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
            writer.write(String.format("Locator: %s, Score: %s, Explanation: %s"+"\n", locator, score, explanation));
        }
    }

    /**
     * Escape special characters in JSON
     * @param str - String to escape
     * @return - Escaped string
     */
    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}