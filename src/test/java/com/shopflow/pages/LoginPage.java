package com.shopflow.pages;

import com.shopflow.config.Config;
import com.shopflow.config.Users;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private static final By USERNAME = test("username");
    private static final By PASSWORD = test("password");
    private static final By LOGIN = test("login");
    private static final By ERROR = test("login-error");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    @Step("Open the login page")
    public LoginPage open() {
        driver.get(Config.baseUrl() + "/#/login");
        return waitUntilLoaded();
    }

    public LoginPage waitUntilLoaded() {
        waits.visible(LOGIN);
        return this;
    }

    @Step("Log in as {user.username}")
    public ProductsPage loginAs(Users user) {
        submit(user.username(), user.password());
        return new ProductsPage(driver).waitUntilLoaded();
    }

    @Step("Attempt login expecting an error")
    public LoginPage loginExpectingError(String username, String password) {
        submit(username, password);
        waits.visible(ERROR);
        return this;
    }

    private void submit(String username, String password) {
        type(USERNAME, username);
        type(PASSWORD, password);
        click(LOGIN);
    }

    public String errorMessage() {
        return text(ERROR);
    }

    public boolean isLoaded() {
        return isDisplayed(LOGIN);
    }
}
