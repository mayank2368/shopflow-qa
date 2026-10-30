package com.shopflow.utils;

import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public final class ScreenshotUtil {

    private ScreenshotUtil() {
    }

    /** Attaches a PNG of the current page to the Allure report (used on failure). */
    public static void attachToAllure(WebDriver driver, String name) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), ".png");
        } catch (RuntimeException e) {
            System.err.println("Could not capture screenshot: " + e.getMessage());
        }
    }
}
