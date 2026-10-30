package com.shopflow.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.shopflow.config.Users;
import com.shopflow.data.DataProviders;
import com.shopflow.pages.LoginPage;
import com.shopflow.pages.ProductsPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

@Epic("ShopFlow")
@Feature("Authentication")
public class LoginTests extends BaseTest {

    @Test(groups = {"smoke", "regression"}, description = "TC-01: a standard user can log in")
    @Story("Login")
    @Severity(SeverityLevel.BLOCKER)
    public void standardUserCanLogIn() {
        ProductsPage products = loginAsStandard();
        assertTrue(products.header().isDisplayed(), "header should be visible after login");
        assertEquals(products.pageTitleText(), "Products");
        assertTrue(products.currentUrl().contains("/products"));
    }

    @Test(groups = "regression", dataProvider = "invalidLogins", dataProviderClass = DataProviders.class,
            description = "TC-02: invalid or locked logins show the right error")
    @Story("Login validation")
    public void invalidLoginShowsError(String username, String password, String expectedError) {
        LoginPage login = openLogin().loginExpectingError(username, password);
        assertTrue(login.errorMessage().toLowerCase().contains(expectedError.toLowerCase()),
                "expected '" + expectedError + "' but was: " + login.errorMessage());
    }

    @Test(groups = {"smoke", "regression"}, description = "TC-03: a logged-in user can log out")
    @Story("Logout")
    public void userCanLogOut() {
        LoginPage login = loginAsStandard().header().logout();
        assertTrue(login.isLoaded(), "login form should be shown after logout");
    }

    @Test(groups = "regression", description = "TC-04: an admin sees the admin navigation, a customer does not")
    @Story("Role-based UI")
    public void adminSeesAdminNav() {
        assertTrue(loginAsAdmin().header().adminLinkVisible(), "admin should see the Admin link");
    }

    @Test(groups = "regression", description = "TC-05: a customer does not see the admin navigation")
    @Story("Role-based UI")
    public void customerDoesNotSeeAdminNav() {
        assertTrue(!loginAsStandard().header().adminLinkVisible(), "customer should not see the Admin link");
        assertTrue(Users.standard().username().equals("standard_user"));
    }
}
