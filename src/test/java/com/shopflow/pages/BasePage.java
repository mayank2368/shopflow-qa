package com.shopflow.pages;

import com.shopflow.components.Header;
import com.shopflow.utils.Waits;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Base for every page object. Holds the driver and wait helpers and the small
 * set of actions all pages share. Tests never touch the WebDriver directly —
 * they go through page objects, which keeps locators out of the tests.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final Waits waits;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.waits = new Waits(driver);
    }

    /** data-test attribute selector — the stable hook the app exposes for tests. */
    protected static By test(String name) {
        return By.cssSelector("[data-test='" + name + "']");
    }

    protected void click(By locator) {
        waits.clickable(locator).click();
    }

    protected void type(By locator, String value) {
        WebElement field = waits.visible(locator);
        field.clear();
        field.sendKeys(value);
    }

    protected String text(By locator) {
        return waits.visible(locator).getText().trim();
    }

    protected boolean isDisplayed(By locator) {
        List<WebElement> found = driver.findElements(locator);
        return !found.isEmpty() && found.get(0).isDisplayed();
    }

    /** The shared site header, available from every logged-in page. */
    public Header header() {
        return new Header(driver);
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public String pageTitleText() {
        return text(test("page-title"));
    }

    /**
     * Parses a money value out of a label. Money is always rendered as "€NN.NN",
     * so we take the part after the last '€' — this avoids picking up stray
     * digits in a label such as "Tax (19%): €19.00".
     */
    protected static double parseMoney(String raw) {
        String afterSymbol = raw.substring(raw.lastIndexOf('€') + 1);
        return Double.parseDouble(afterSymbol.replaceAll("[^0-9.]", ""));
    }
}
