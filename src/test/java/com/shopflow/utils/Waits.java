package com.shopflow.utils;

import com.shopflow.config.Config;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Explicit-wait helpers. The project uses explicit waits only — never implicit
 * waits or Thread.sleep — because mixing wait strategies causes unpredictable
 * timeouts, and sleeps make suites slow and flaky.
 */
public class Waits {

    private final WebDriverWait wait;

    public Waits(WebDriver driver) {
        this(driver, Config.timeout());
    }

    public Waits(WebDriver driver, Duration timeout) {
        this.wait = new WebDriverWait(driver, timeout);
    }

    public WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement clickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public List<WebElement> allVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public boolean invisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean textToBe(By locator, String text) {
        return wait.until(ExpectedConditions.textToBe(locator, text));
    }

    public boolean urlContains(String fraction) {
        return wait.until(ExpectedConditions.urlContains(fraction));
    }

    public <T> T until(Function<WebDriver, T> condition) {
        return wait.until(condition);
    }
}
