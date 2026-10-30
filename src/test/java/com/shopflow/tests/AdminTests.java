package com.shopflow.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.shopflow.data.TestData;
import com.shopflow.pages.AdminPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

@Epic("ShopFlow")
@Feature("Admin")
public class AdminTests extends BaseTest {

    @Test(groups = {"smoke", "regression"}, description = "TC-50: an admin can create a product")
    @Story("Create product")
    @Severity(SeverityLevel.CRITICAL)
    public void adminCanCreateProduct() {
        AdminPage admin = loginAsAdmin().header().openAdmin();
        int before = admin.productRowCount();

        admin.createProduct("Velvet Stool", "89.00", "Chairs");
        assertEquals(admin.productRowCount(), before + 1, "a row should be added");
        assertTrue(admin.productNames().contains("Velvet Stool"), "the new product should be listed");
    }

    @Test(groups = "regression", description = "TC-51: an admin can delete a product")
    @Story("Delete product")
    public void adminCanDeleteProduct() {
        AdminPage admin = loginAsAdmin().header().openAdmin();
        int before = admin.productRowCount();

        admin.deleteProduct(TestData.DESK_LAMP);
        assertEquals(admin.productRowCount(), before - 1, "a row should be removed");
        assertTrue(!admin.productNames().contains(TestData.DESK_LAMP), "the product should be gone");
    }

    @Test(groups = "regression", description = "TC-52: creating a product with no name is rejected")
    @Story("Validation")
    public void creatingWithNoNameIsRejected() {
        AdminPage admin = loginAsAdmin().header().openAdmin();
        int before = admin.productRowCount();

        admin.createExpectingError("", "50.00");
        assertTrue(!admin.errorMessage().isBlank(), "an error should be shown");
        assertEquals(admin.productRowCount(), before, "no row should be added");
    }

    @Test(groups = {"smoke", "regression"}, description = "TC-53: a customer cannot reach the admin area")
    @Story("Access control")
    @Severity(SeverityLevel.CRITICAL)
    public void customerCannotReachAdmin() {
        loginAsStandard();
        AdminPage admin = new AdminPage(driver()).open();
        assertTrue(admin.accessDenied(), "a customer should be denied access to the admin area");
    }
}
