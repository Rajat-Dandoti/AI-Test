package com.example.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessHTML {
    private static final Logger logger = Logger.getLogger(ProcessHTML.class.getName());

    private ProcessHTML() {}

    /**
     * Extract failed part of HTML based on locator
     * @param htmlFilePath - the path to the HTML file
     * @param txtFilePath - the path to the text file containing the locator
     * @param failedPageFilePath - the path to the output file
     */
    public static void extractFailedPartOfHTML(String htmlFilePath, String txtFilePath, String failedPageFilePath) {
        try {
            String pageSource = readFileContent(htmlFilePath);
            String locator = readFileContent(txtFilePath).trim();

            try (FileWriter failedPageWriter = new FileWriter(failedPageFilePath, false)) {
                processLocator(pageSource, locator, failedPageWriter);
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "Failed to capture failed part of HTML", e);
        }
    }

    /**
     * Read file content
     * @param filePath - the path to the file
     * @return - the content of the file
     * @throws Exception - if an I/O error occurs
     */
    private static String readFileContent(String filePath) throws Exception {
        return new String(Files.readAllBytes(new File(filePath).toPath()));
    }

    /**
     * Process locator
     * @param pageSource - the HTML content
     * @param locator - the locator
     * @param failedPageWriter - the writer to write the results
     */
    private static void processLocator(String pageSource, String locator, FileWriter failedPageWriter) {
        try {
            Pattern pattern = Pattern.compile("//(\\w+)");
            Matcher matcher = pattern.matcher(locator);
            if (matcher.find()) {
                String tagName = matcher.group(1);
                logger.info("Tag name: " + tagName);

                Document doc = Jsoup.parse(pageSource);
                List<Element> matchingTags = doc.getElementsByTag(tagName);
                logger.info("Found " + matchingTags.size() + " tags with name " + tagName);

                writeTagsToFile(matchingTags, failedPageWriter);
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "Failed to capture snippet for locator " + locator, e);
        }
    }

    /**
     * Write tags to file
     * @param tags - the list of tags
     * @param writer - the writer to write the results
     * @throws Exception - if an I/O error occurs
     */
    private static void writeTagsToFile(List<Element> tags, FileWriter writer) throws Exception {
        for (Element tag : tags) {
            writer.write(tag.outerHtml() + "\n");
            logger.info("Tag HTML: " + tag + "\n");
        }
    }
}