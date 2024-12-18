package com.example.listeners;

import com.example.configurations.ConfigManager;
import com.example.utils.*;
import com.example.utils.reporting.HealingReport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomTestListener implements ITestListener {
    private static final Logger logger = Logger.getLogger(CustomTestListener.class.getName());

    private static final String DRIVER_KEY = "driver";
    private String htmlFilePath;
    private String failedLocatorPath;
    private String failedPageFilePath;
    private String llmOutputPath;
    private String validatedLocPath;
    private String reportFilePath;

    /**
     * This method is invoked before the test starts
     * @param result - ITestResult instance of the test method that is about to start
     */
    @Override
    public void onTestStart(ITestResult result) {
        WebDriver driver = getWebDriver(result);
        if (driver != null) {
            result.getTestContext().setAttribute(DRIVER_KEY, driver);
        }
    }

    /**
     * This method is invoked when a test fails
     * @param result - ITestResult instance of the test method that failed
     */
    @Override
    public void onTestFailure(ITestResult result) {
        WebDriver driver = (WebDriver) result.getTestContext().getAttribute(DRIVER_KEY);
        if (driver != null) {
            Throwable throwable = result.getThrowable();
            if (throwable instanceof NoSuchElementException) {
                try {
                    handleTestFailure(result, driver);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error handling test failure", e);
                }
            }
        }
    }

    /**
     * This method handles the test failure
     * @param result - ITestResult instance of the test method that failed
     * @param driver - WebDriver instance
     */
    private void handleTestFailure(ITestResult result, WebDriver driver) {
        try {
            boolean isHealingEnabled = Boolean.parseBoolean(ConfigManager.getProperty("healing.solution.enabled"));
            if(isHealingEnabled) {
                String exceptionMessage = result.getThrowable().toString();
                String pageSource = getPageSource(driver);
                String locator = ExtractFailedLoc.extractLocator(exceptionMessage);
                createFiles(locator, pageSource);
                TokenCounter.countTokens(failedLocatorPath, htmlFilePath);
                ProcessHTML.extractFailedPartOfHTML(htmlFilePath, failedLocatorPath, failedPageFilePath);
                //BedrockLLM.sendRequestToBedrock(failedLocatorPath, failedPageFilePath, llmOutputPath);
                CohereLLM.sendRequestToCohere(failedLocatorPath, failedPageFilePath, llmOutputPath);
                LocatorValidator.validateLocators(llmOutputPath, htmlFilePath, validatedLocPath);
                LocatorUpdater.updateLocators(validatedLocPath, failedLocatorPath);
                HealingReport.generateReport(failedLocatorPath, failedPageFilePath, llmOutputPath, validatedLocPath, reportFilePath);
            }
            else {
                logger.log(Level.SEVERE, "Auto-Healing is disabled.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling test failure ", e);
        }
    }

    /**
     * This method gets the WebDriver instance from the ITestResult
     * @param result - ITestResult instance
     * @return - WebDriver instance
     */
    private WebDriver getWebDriver(ITestResult result) {
        WebDriver driver = (WebDriver) result.getTestContext().getAttribute(DRIVER_KEY);
        if (driver == null) {
            try {
                Field driverField = result.getInstance().getClass().getSuperclass().getDeclaredField(DRIVER_KEY);
                driverField.setAccessible(true);
                driver = (WebDriver) driverField.get(result.getInstance());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.log(Level.SEVERE, "Error setting WebDriver in ITestContext", e);
            }
        }
        return driver;
    }

    /**
     * This method gets the page source of the current page
     * @param driver - WebDriver instance
     * @return - Page source
     */
    private String getPageSource(WebDriver driver) {
        return driver.getPageSource();
        //return (String) ((JavascriptExecutor) driver).executeScript("return document.documentElement.outerHTML;");
    }

    /**
     * This method creates the necessary files
     * @param locator - Failed locator
     * @param pageSource - Page source
     * @throws IOException - Exception
     */
    private void createFiles(String locator, String pageSource) throws IOException {
        File[] folders = getFolders();
        String timestamp = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());

        failedLocatorPath = new File(folders[1], "failed_locator_" + timestamp + ".txt").getAbsolutePath();
        try (FileWriter writer = new FileWriter(failedLocatorPath)) {
            writer.write(locator);
        }

        Document doc = Jsoup.parse(pageSource);
        String prettyHtml = doc.outerHtml();

        htmlFilePath = new File(folders[0], "page_source_" + timestamp + ".html").getAbsolutePath();
        try (FileWriter writer = new FileWriter(htmlFilePath)) {
            writer.write(prettyHtml);
        }

        failedPageFilePath = new File(folders[2], "processed_page_source_" + timestamp + ".html").getAbsolutePath();
        llmOutputPath = new File(folders[3], "llm_output_" + timestamp + ".txt").getAbsolutePath();
        validatedLocPath = new File(folders[4], "validated_locators_" + timestamp + ".txt").getAbsolutePath();
        reportFilePath = new File(folders[5], "healing_report_" + timestamp + ".html").getAbsolutePath();

        logger.log(Level.SEVERE, "Test failed. Exception: " + locator);
        logger.log(Level.SEVERE, "Page source captured.");
    }

    /**
     * This method gets the folders for storing the files
     * @return - Array of folders
     */
    private static File[] getFolders() {
        File parentFolder = new File("Healing_Docs");
        if (!parentFolder.exists()) parentFolder.mkdirs();

        File[] folders = {
                new File(parentFolder, "html"),
                new File(parentFolder, "locator"),
                new File(parentFolder, "processed_html"),
                new File(parentFolder, "llm_output"),
                new File(parentFolder, "validated_locators"),
                new File(parentFolder, "healing_report")
        };

        for (File folder : folders) {
            if (!folder.exists()) folder.mkdirs();
        }
        return folders;
    }
}