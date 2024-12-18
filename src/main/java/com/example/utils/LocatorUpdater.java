package com.example.utils;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class LocatorUpdater {
    private LocatorUpdater() {}
    private static final Logger logger = Logger.getLogger(LocatorUpdater.class.getName());

    /**
     * Update locators in the project
     * @param validatedLocPath - path to the validated locators file
     * @param failedLocatorPath - path to the failed locator file
     */
    public static void updateLocators(String validatedLocPath, String failedLocatorPath) {
        try {
            Properties properties = new Properties();
            try (InputStream input = new FileInputStream("src/main/resources/config.properties")) {
                properties.load(input);
            }

            String locatorPath = properties.getProperty("locatorPath");
            String projectRootPath = (locatorPath == null || locatorPath.isEmpty()) ? System.getProperty("user.dir") : locatorPath;
            String failedLocator = readFile(failedLocatorPath).trim();
            String newLocator = getFirstFoundLocator(validatedLocPath);

            if (newLocator != null) {
                updateLocatorInProject(failedLocator, newLocator, projectRootPath, locatorPath != null && !locatorPath.isEmpty());
            } else {
                logger.log(Level.WARNING, "No valid locator found in the validated locators file.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading files", e);
        }
    }

    /**
     * Get the first found locator from the validated locators file
     * @param validatedLocPath - path to the validated locators file
     * @return - first found locator
     * @throws IOException - if an I/O error occurs
     */
    public static String getFirstFoundLocator(String validatedLocPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(validatedLocPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Locator found: ")) {
                    return line.substring(15).trim();
                }
            }
        }
        return null;
    }

    /**
     * Update locators in the project
     * @param failedLocator - failed locator
     * @param newLocator - new locator
     * @param projectRootPath - project root path
     * @param checkAllFiles - check all files in the given path
     */
    public static void updateLocatorInProject(String failedLocator, String newLocator, String projectRootPath, boolean checkAllFiles) {
        logger.log(Level.INFO, "Starting to update locators in project. Root path: {0}, Check all files: {1}", new Object[]{projectRootPath, checkAllFiles});
        try (Stream<Path> paths = Files.walk(Paths.get(projectRootPath))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> checkAllFiles || isRelevantFile(path))
                    .forEach(path -> {
                        logger.log(Level.FINE, "Processing file: {0}", path);
                        updateLocatorInFile(path, failedLocator, newLocator);
                    });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error walking through project files at path: " + projectRootPath, e);
        }
        logger.log(Level.INFO, "Finished updating locators in project.");
    }

    /**
     * Check if the file is relevant for locator update
     * @param path - file path
     * @return - true if the file is relevant, false otherwise
     */
    private static boolean isRelevantFile(Path path) {
        String fileName = path.toString();
        return fileName.endsWith(".java") || fileName.endsWith(".loc") || fileName.endsWith(".properties");
    }

    /**
     * Update locator in the file
     * @param filePath - file path
     * @param failedLocator - failed locator
     * @param newLocator - new locator
     */
    private static void updateLocatorInFile(Path filePath, String failedLocator, String newLocator) {
        try {
            String content = new String(Files.readAllBytes(filePath));
            if (content.contains(failedLocator)) {
                String updatedContent = content.replace(failedLocator, newLocator);
                Files.write(filePath, updatedContent.getBytes());
                logger.log(Level.INFO, "Updated locator in file: "+ filePath);
            } else {
                logger.log(Level.INFO, "Locator not found in file: "+ filePath);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error updating locator in file: " + filePath, e);
        }
    }

    /**
     * Read file content
     * @param filePath - file path
     * @return - file content
     * @throws IOException - if an I/O error occurs
     */
    private static String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
}