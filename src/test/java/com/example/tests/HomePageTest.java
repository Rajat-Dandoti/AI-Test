package com.example.tests;

import com.example.base.BaseTest;
import com.example.pages.HomePage;
import org.testng.annotations.Test;

public class HomePageTest extends BaseTest {
    private HomePage homePage;

    @Test
    public void testSearch() {
        homePage = new HomePage(driver);
        homePage.search("Selenium");
        // Add assertions here
    }
}