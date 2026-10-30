package com.shopflow.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class ProductDetailPage extends BasePage {

    private static final By NAME = test("detail-name");
    private static final By PRICE = test("detail-price");
    private static final By CATEGORY = test("detail-category");
    private static final By DESCRIPTION = test("detail-description");
    private static final By ADD = test("detail-add");
    private static final By BACK = test("back-to-products");

    public ProductDetailPage(WebDriver driver) {
        super(driver);
        waits.visible(NAME);
    }

    public String name() {
        return text(NAME);
    }

    public double price() {
        return parseMoney(text(PRICE));
    }

    public String category() {
        return text(CATEGORY);
    }

    public String description() {
        return text(DESCRIPTION);
    }

    @Step("Add the product to the cart from its detail page")
    public ProductDetailPage addToCart() {
        click(ADD);
        waits.until(d -> isDisplayed(test("cart-badge")));
        return this;
    }

    public ProductsPage backToProducts() {
        click(BACK);
        return new ProductsPage(driver).waitUntilLoaded();
    }
}
