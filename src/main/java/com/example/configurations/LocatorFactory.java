package com.example.configurations;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LocatorFactory {

    private LocatorFactory() {}

    /**
     * Get locator from locators.loc file
     * @param driver - WebDriver instance
     * @param key - locator key
     * @return - WebElement
     */
    public static WebElement getLocator(WebDriver driver, String key) {
        String locator = ConfigManager.getLocator(key);
        return driver.findElement(By.xpath(locator));
    }
}