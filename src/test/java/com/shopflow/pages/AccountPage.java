package com.shopflow.pages;

import com.shopflow.config.Config;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class AccountPage extends BasePage {

    private static final By NAME = test("account-name");
    private static final By EMAIL = test("account-email");
    private static final By SAVE = test("account-save");
    private static final By SAVED = test("account-saved");
    private static final By ERROR = test("account-error");

    public AccountPage(WebDriver driver) {
        super(driver);
    }

    public AccountPage open() {
        driver.get(Config.baseUrl() + "/#/account");
        waits.visible(SAVE);
        return this;
    }

    public AccountPage reload() {
        driver.navigate().refresh();
        waits.visible(SAVE);
        return this;
    }

    public String name() {
        return waits.visible(NAME).getDomProperty("value");
    }

    public String email() {
        return waits.visible(EMAIL).getDomProperty("value");
    }

    @Step("Update account: name='{name}', email='{email}'")
    public AccountPage update(String name, String email) {
        type(NAME, name);
        type(EMAIL, email);
        click(SAVE);
        return this;
    }

    @Step("Save account expecting a validation error")
    public AccountPage updateExpectingError(String name, String email) {
        type(NAME, name);
        type(EMAIL, email);
        click(SAVE);
        waits.visible(ERROR);
        return this;
    }

    public boolean savedConfirmationShown() {
        return isDisplayed(SAVED);
    }

    public String errorMessage() {
        return text(ERROR);
    }
}
