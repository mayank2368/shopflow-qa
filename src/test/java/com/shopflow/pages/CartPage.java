package com.shopflow.pages;

import com.shopflow.config.Config;
import io.qameta.allure.Step;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class CartPage extends BasePage {

    private static final By ITEM = test("cart-item");
    private static final By ITEM_NAME = test("item-name");
    private static final By ITEM_PRICE = test("item-price");
    private static final By SUBTOTAL = test("cart-subtotal");
    private static final By COUNT = test("cart-count");
    private static final By EMPTY = test("cart-empty");
    private static final By CHECKOUT = test("checkout");

    public CartPage(WebDriver driver) {
        super(driver);
        waits.urlContains("/cart");
    }

    public CartPage open() {
        driver.get(Config.baseUrl() + "/#/cart");
        waits.urlContains("/cart");
        return this;
    }

    public int itemCount() {
        return driver.findElements(ITEM).size();
    }

    public List<String> itemNames() {
        return driver.findElements(ITEM_NAME).stream().map(e -> e.getText().trim()).toList();
    }

    public boolean isEmpty() {
        return isDisplayed(EMPTY);
    }

    public double subtotal() {
        return parseMoney(text(SUBTOTAL));
    }

    @Step("Remove '{productName}' from the cart")
    public CartPage removeItem(String productName) {
        WebElement row = row(productName);
        int before = itemCount();
        row.findElement(By.cssSelector("[data-test='item-remove']")).click();
        waits.until(d -> driver.findElements(ITEM).size() == before - 1);
        return this;
    }

    @Step("Set quantity of '{productName}' to {quantity}")
    public CartPage setQuantity(String productName, int quantity) {
        WebElement qty = row(productName).findElement(By.cssSelector("[data-test='item-qty']"));
        qty.clear();
        qty.sendKeys(String.valueOf(quantity));
        qty.sendKeys(org.openqa.selenium.Keys.TAB);
        waits.until(ExpectedConditions.stalenessOf(qty));
        return this;
    }

    public CheckoutInfoPage checkout() {
        click(CHECKOUT);
        return new CheckoutInfoPage(driver);
    }

    private WebElement row(String productName) {
        return driver.findElements(ITEM).stream()
                .filter(r -> r.findElement(ITEM_NAME).getText().trim().equals(productName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not in cart: " + productName));
    }
}
