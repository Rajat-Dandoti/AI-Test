package com.example.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocatorValidator {
    private static final Logger logger = Logger.getLogger(LocatorValidator.class.getName());

    /**
     * Validate the locators from the LLM output file against the HTML file
     * @param llmOutputPath - the path to the LLM output file
     * @param htmlFilePath - the path to the HTML file
     * @param validatedLocPath - the path to the output file
     */
    public static void validateLocators(String llmOutputPath, String htmlFilePath, String validatedLocPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(validatedLocPath))) {
            String htmlContent = readFile(htmlFilePath);
            Document document = Jsoup.parse(htmlContent);

            try (BufferedReader reader = new BufferedReader(new FileReader(llmOutputPath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Locator: ")) {
                        String locator = line.substring(9, line.indexOf(", Score:"));
                        validateLocator(locator, document, writer);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading files", e);
        }
    }

    /**
     * Validate the locator against the HTML document
     * @param locator - the locator to validate
     * @param document - the HTML document
     * @param writer - the writer to write the results
     * @throws IOException - if an I/O error occurs
     */
    private static void validateLocator(String locator, Document document, BufferedWriter writer) throws IOException {
        Elements elements = document.selectXpath(locator);
        if (elements.isEmpty()) {
            logger.log(Level.WARNING, "Locator not found: " + locator);
            writer.write("Locator not found: " + locator + "\n");
        } else {
            logger.log(Level.INFO, "Locator found: " + locator);
            writer.write("Locator found: " + locator + "\n");
        }
    }

    /**
     * Read the content of a file
     * @param filePath - the path to the file
     * @return - the content of the file
     * @throws IOException - if an I/O error occurs
     */
    private static String readFile(String filePath) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                contentBuilder.append(currentLine).append("\n");
            }
        }
        return contentBuilder.toString();
    }
}