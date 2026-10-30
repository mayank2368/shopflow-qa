package com.shopflow.pages;

import com.shopflow.config.Config;
import io.qameta.allure.Step;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class AdminPage extends BasePage {

    private static final By ADD_NAME = test("admin-add-name");
    private static final By ADD_PRICE = test("admin-add-price");
    private static final By ADD_CATEGORY = test("admin-add-category");
    private static final By CREATE = test("admin-create");
    private static final By ERROR = test("admin-error");
    private static final By ROW = test("admin-row");
    private static final By ROW_NAME = test("admin-row-name");
    private static final By DENIED = test("admin-denied");

    public AdminPage(WebDriver driver) {
        super(driver);
    }

    public AdminPage open() {
        driver.get(Config.baseUrl() + "/#/admin");
        waits.urlContains("/admin");
        return this;
    }

    public boolean accessDenied() {
        return isDisplayed(DENIED);
    }

    public AdminPage waitUntilLoaded() {
        waits.visible(CREATE);
        return this;
    }

    public int productRowCount() {
        return driver.findElements(ROW).size();
    }

    public List<String> productNames() {
        return driver.findElements(ROW_NAME).stream().map(e -> e.getText().trim()).toList();
    }

    @Step("Create product '{name}' priced {price} in {category}")
    public AdminPage createProduct(String name, String price, String category) {
        int before = productRowCount();
        type(ADD_NAME, name);
        type(ADD_PRICE, price);
        type(ADD_CATEGORY, category);
        click(CREATE);
        waits.until(d -> productRowCount() > before);
        return this;
    }

    @Step("Create product expecting a validation error")
    public AdminPage createExpectingError(String name, String price) {
        type(ADD_NAME, name);
        type(ADD_PRICE, price);
        click(CREATE);
        waits.visible(ERROR);
        return this;
    }

    public String errorMessage() {
        return text(ERROR);
    }

    @Step("Delete product '{name}'")
    public AdminPage deleteProduct(String name) {
        WebElement row = driver.findElements(ROW).stream()
                .filter(r -> r.findElement(ROW_NAME).getText().trim().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No admin row for: " + name));
        row.findElement(By.cssSelector("[data-test='admin-delete']")).click();
        waits.until(ExpectedConditions.stalenessOf(row));
        return this;
    }
}
