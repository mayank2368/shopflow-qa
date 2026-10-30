package com.shopflow.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Checkout step 2: order summary with subtotal, tax and total. */
public class CheckoutOverviewPage extends BasePage {

    private static final By SUBTOTAL = test("summary-subtotal");
    private static final By TAX = test("summary-tax");
    private static final By TOTAL = test("summary-total");
    private static final By FINISH = test("finish");

    public CheckoutOverviewPage(WebDriver driver) {
        super(driver);
        waits.visible(FINISH);
    }

    public double subtotal() {
        return parseMoney(text(SUBTOTAL));
    }

    public double tax() {
        return parseMoney(text(TAX));
    }

    public double total() {
        return parseMoney(text(TOTAL));
    }

    public CheckoutCompletePage finish() {
        click(FINISH);
        return new CheckoutCompletePage(driver);
    }
}
