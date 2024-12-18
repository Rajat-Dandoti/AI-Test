package com.example.pages;

import com.example.base.BasePage;
import com.example.configurations.LocatorFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.testng.Assert;

public class HomePage extends BasePage {

    public HomePage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    private WebElement getSubmitButton() {
        return LocatorFactory.getLocator(driver, "submitButton");
    }

    private WebElement getSearchBox() {
        return LocatorFactory.getLocator(driver, "searchBox");
    }

    private WebElement getStoreLink() {
        return LocatorFactory.getLocator(driver, "storeLink");
    }

    private WebElement getSearchResult() {
        return LocatorFactory.getLocator(driver, "selenium_sr");
    }

    private WebElement getAppliLink() {
        return LocatorFactory.getLocator(driver, "selenium_appliTools");
    }

    public void search(String query) {
        WebElement storeLink = getStoreLink();
        Assert.assertTrue(storeLink.isDisplayed(), "Store link is not displayed");
        WebElement searchBox = getSearchBox();
        searchBox.sendKeys(query);
        WebElement submitButton = getSubmitButton();
        submitButton.submit();

        WebElement searchResult = getSearchResult();
        Assert.assertTrue(searchResult.isDisplayed(), "Search result is not displayed");
        searchResult.click();

        Actions actions = new Actions(driver);
        WebElement learnMoreLink = getAppliLink();
        actions.moveToElement(learnMoreLink).perform();
    }
}