package com.shopflow.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CheckoutCompletePage extends BasePage {

    private static final By CONFIRMATION = test("order-confirmation");
    private static final By ORDER_ID = test("order-id");
    private static final By BACK_HOME = test("back-home");

    public CheckoutCompletePage(WebDriver driver) {
        super(driver);
        waits.visible(CONFIRMATION);
    }

    public boolean isConfirmed() {
        return isDisplayed(CONFIRMATION);
    }

    public String orderId() {
        return text(ORDER_ID);
    }

    public ProductsPage backToProducts() {
        click(BACK_HOME);
        return new ProductsPage(driver).waitUntilLoaded();
    }
}
