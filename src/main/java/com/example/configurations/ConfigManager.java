package com.example.configurations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigManager {
    private static final Logger logger = Logger.getLogger(ConfigManager.class.getName());
    private static final Properties properties = new Properties();
    private static final Properties locators = new Properties();

    private ConfigManager() {}

    static {
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.log(Level.SEVERE, "Sorry, unable to find config.properties");
            } else {
                properties.load(input);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error loading config.properties", ex);
        }

        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("locators.loc")) {
            if (input == null) {
                logger.log(Level.SEVERE, "Sorry, unable to find locators.loc");
            } else {
                locators.load(input);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error loading locators.loc", ex);
        }
    }

    /**
     * Get property value from config.properties
     * @param key - property key
     * @return - property value
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get locator value from locators.loc
     * @param key - locator key
     * @return - locator value
     */
    public static String getLocator(String key) {
        return locators.getProperty(key);
    }
}