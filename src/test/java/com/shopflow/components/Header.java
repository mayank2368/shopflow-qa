package com.shopflow.components;

import com.shopflow.pages.AccountPage;
import com.shopflow.pages.AdminPage;
import com.shopflow.pages.BasePage;
import com.shopflow.pages.CartPage;
import com.shopflow.pages.LoginPage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * The site header is a component shared by every logged-in page, so it lives in
 * its own class rather than being duplicated across page objects.
 */
public class Header extends BasePage {

    private static final By HEADER = test("header");
    private static final By CART_LINK = test("cart-link");
    private static final By CART_BADGE = test("cart-badge");
    private static final By ACCOUNT_MENU = test("account-menu");
    private static final By LOGOUT = test("logout");
    private static final By NAV_ADMIN = test("nav-admin");

    public Header(WebDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return isDisplayed(HEADER);
    }

    public int cartCount() {
        return isDisplayed(CART_BADGE) ? Integer.parseInt(text(CART_BADGE)) : 0;
    }

    public String accountName() {
        return text(ACCOUNT_MENU);
    }

    public boolean adminLinkVisible() {
        return isDisplayed(NAV_ADMIN);
    }

    @Step("Open the cart")
    public CartPage openCart() {
        click(CART_LINK);
        return new CartPage(driver);
    }

    @Step("Open the account page")
    public AccountPage openAccount() {
        click(ACCOUNT_MENU);
        return new AccountPage(driver).open();
    }

    @Step("Open the admin area")
    public AdminPage openAdmin() {
        click(NAV_ADMIN);
        return new AdminPage(driver).waitUntilLoaded();
    }

    public LoginPage logout() {
        click(LOGOUT);
        return new LoginPage(driver).waitUntilLoaded();
    }
}
