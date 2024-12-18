package com.example.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractFailedLoc {
    /**
     * Private constructor to hide the implicit public one
     */
    private ExtractFailedLoc() {}

    /**
     * Extract the locator from the exception message
     * @param exceptionMessage - the exception message
     * @return - the locator
     */
    public static String extractLocator(String exceptionMessage) {
        Matcher matcher = Pattern.compile("\"selector\":\"(.*?)\"").matcher(exceptionMessage);
        return matcher.find() ? matcher.group(1) : "Locator not found in exception message";
    }
}