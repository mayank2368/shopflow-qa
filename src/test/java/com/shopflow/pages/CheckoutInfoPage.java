package com.shopflow.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Checkout step 1: customer information with validation. */
public class CheckoutInfoPage extends BasePage {

    private static final By FIRST_NAME = test("first-name");
    private static final By LAST_NAME = test("last-name");
    private static final By POSTAL_CODE = test("postal-code");
    private static final By CONTINUE = test("continue");
    private static final By CANCEL = test("cancel");
    private static final By ERROR = test("checkout-error");

    public CheckoutInfoPage(WebDriver driver) {
        super(driver);
        waits.visible(CONTINUE);
    }

    @Step("Enter checkout information: {firstName} {lastName}, {postalCode}")
    public CheckoutInfoPage fillInformation(String firstName, String lastName, String postalCode) {
        type(FIRST_NAME, firstName);
        type(LAST_NAME, lastName);
        type(POSTAL_CODE, postalCode);
        return this;
    }

    public CheckoutOverviewPage continueToOverview() {
        click(CONTINUE);
        return new CheckoutOverviewPage(driver);
    }

    @Step("Continue expecting a validation error")
    public CheckoutInfoPage continueExpectingError() {
        click(CONTINUE);
        waits.visible(ERROR);
        return this;
    }

    public String errorMessage() {
        return text(ERROR);
    }

    public CartPage cancel() {
        click(CANCEL);
        return new CartPage(driver);
    }
}
