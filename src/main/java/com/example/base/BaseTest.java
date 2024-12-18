package com.example.base;

import com.example.configurations.ConfigManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseTest {
    protected WebDriver driver;
    private static final Logger logger = Logger.getLogger(BaseTest.class.getName());

    @BeforeMethod
    public void setUp() {

        String browser = ConfigManager.getProperty("browser");
        String baseUrl = ConfigManager.getProperty("baseUrl");

        logger.log(Level.INFO, "Browser: " + browser);
        logger.log(Level.INFO, "Base URL: " + baseUrl);

        if ("chrome".equalsIgnoreCase(browser)) {
            // Use WebDriverManager to set up ChromeDriver
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver();
        } else if ("edge".equalsIgnoreCase(browser)) {
            // Use WebDriverManager to set up EdgeDriver
            WebDriverManager.edgedriver().setup();
            driver = new EdgeDriver();
        } else {
            throw new IllegalArgumentException("Unsupported browser: " + browser);
        }

        driver.manage().window().maximize();
        driver.get(baseUrl);
        int implicitWait = Integer.parseInt(ConfigManager.getProperty("implicitWait"));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}