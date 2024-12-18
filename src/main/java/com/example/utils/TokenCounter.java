package com.example.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TokenCounter {
    private TokenCounter() {}
    private static final Logger logger = Logger.getLogger(TokenCounter.class.getName());

    /**
     * Count the number of tokens in a file
     * @param file - the file to count the tokens
     * @return - the number of tokens in the file
     * @throws IOException - if an I/O error occurs
     */
    public static int countTokensInFile(File file) throws IOException {
        String content = new String(Files.readAllBytes(file.toPath()));
        StringTokenizer tokenizer = new StringTokenizer(content);
        return tokenizer.countTokens();
    }

    /**
     * Count the number of tokens in .txt and .html files
     * @param failedLocatorPath - the path to the .txt file
     * @param htmlFilePath - the path to the .html file
     */
    public static void countTokens(String failedLocatorPath, String htmlFilePath) {
        try {
            if (failedLocatorPath != null && htmlFilePath != null) {
                File txtFile = new File(failedLocatorPath);
                File htmlFile = new File(htmlFilePath);

                int txtTokenCount = countTokensInFile(txtFile);
                int htmlTokenCount = countTokensInFile(htmlFile);

                logger.log(Level.INFO, "Number of tokens in .txt file: " + txtTokenCount);
                logger.log(Level.INFO, "Number of tokens in .html file: " + htmlTokenCount);
            } else {
                logger.log(Level.WARNING, "File paths are not available.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error counting tokens", e);
        }
    }
}