package com.shopflow.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.shopflow.pages.AccountPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

@Epic("ShopFlow")
@Feature("Account")
public class AccountTests extends BaseTest {

    @Test(groups = {"smoke", "regression"}, description = "TC-40: a user can update their name and email")
    @Story("Edit profile")
    public void userCanUpdateProfile() {
        AccountPage account = loginAsStandard().header().openAccount();
        account.update("Maximilian", "max@example.com");
        assertTrue(account.savedConfirmationShown(), "a saved confirmation should appear");

        // The change should survive a reload (persisted server-side).
        AccountPage reopened = account.reload();
        assertEquals(reopened.name(), "Maximilian");
        assertEquals(reopened.email(), "max@example.com");
    }

    @Test(groups = "regression", description = "TC-41: an invalid email is rejected")
    @Story("Validation")
    public void invalidEmailIsRejected() {
        AccountPage account = loginAsStandard().header().openAccount();
        account.updateExpectingError("Max", "not-an-email");
        assertTrue(account.errorMessage().toLowerCase().contains("email"),
                "error should mention email: " + account.errorMessage());
    }

    @Test(groups = "regression", description = "TC-42: an empty name is rejected")
    @Story("Validation")
    public void emptyNameIsRejected() {
        AccountPage account = loginAsStandard().header().openAccount();
        account.updateExpectingError("", "max@example.com");
        assertTrue(!account.errorMessage().isBlank(), "an error should be shown for an empty name");
    }
}
