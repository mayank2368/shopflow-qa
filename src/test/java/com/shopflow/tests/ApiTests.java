package com.shopflow.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.shopflow.api.ApiClient;
import com.shopflow.config.Users;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * API-level tests. These run without a browser — faster, and they pin down the
 * backend contract the UI relies on (auth, roles, validation). A good advanced
 * suite tests the API directly, not only through the UI.
 */
@Epic("ShopFlow")
@Feature("API")
public class ApiTests {

    private final ApiClient api = new ApiClient();

    @BeforeMethod(alwaysRun = true)
    public void resetState() {
        api.reset();
    }

    @Test(groups = {"smoke", "regression"}, description = "API-01: login returns a token for valid credentials")
    @Story("Auth")
    public void loginReturnsToken() {
        String token = api.loginToken(Users.standard());
        assertTrue(token != null && !token.isBlank(), "a token should be returned");
    }

    @Test(groups = "regression", description = "API-02: a locked user is refused with 403")
    @Story("Auth")
    public void lockedUserIsRefused() {
        ApiClient.Response res = api.login(Users.locked());
        assertEquals(res.status(), 403, "a locked user should get 403");
        assertTrue(res.text("error").toLowerCase().contains("locked"));
    }

    @Test(groups = "regression", description = "API-03: wrong credentials are refused with 401")
    @Story("Auth")
    public void wrongCredentialsAreRefused() {
        ApiClient.Response res = api.login("standard_user", "nope");
        assertEquals(res.status(), 401, "wrong password should get 401");
    }

    @Test(groups = {"smoke", "regression"}, description = "API-04: the catalogue returns all seeded products")
    @Story("Products")
    public void catalogueReturnsSeededProducts() {
        ApiClient.Response res = api.get("/api/products?limit=100");
        assertEquals(res.status(), 200);
        assertEquals(res.jsonArray("items").size(), 12, "there should be 12 seeded products");
    }

    @Test(groups = "regression", description = "API-05: category filtering is applied server-side")
    @Story("Products")
    public void categoryFilterIsApplied() {
        ApiClient.Response res = api.get("/api/products?category=Lamps&limit=100");
        assertEquals(res.jsonArray("items").size(), 3, "Lamps should have 3 products");
    }

    @Test(groups = "regression", description = "API-06: checkout with a missing field is rejected with 400")
    @Story("Checkout")
    public void checkoutMissingFieldIsRejected() {
        String token = api.loginToken(Users.standard());
        api.addToCart(token, 1, 1);
        ApiClient.Response res = api.checkout(token, "Max", "", "36037");
        assertEquals(res.status(), 400, "a missing last name should be rejected");
    }

    @Test(groups = {"smoke", "regression"}, description = "API-07: a non-admin is forbidden from admin endpoints")
    @Story("Access control")
    public void nonAdminIsForbidden() {
        String token = api.loginToken(Users.standard());
        ApiClient.Response res = api.post("/api/admin/products", token,
                Map.of("name", "X", "category", "Chairs", "price", 10));
        assertEquals(res.status(), 403, "a customer should be forbidden from creating products");
    }

    @Test(groups = "regression", description = "API-08: an admin can create a product via the API")
    @Story("Access control")
    public void adminCanCreateViaApi() {
        String token = api.loginToken(Users.admin());
        ApiClient.Response res = api.post("/api/admin/products", token,
                Map.of("name", "API Chair", "category", "Chairs", "price", 42));
        assertEquals(res.status(), 201, "an admin should be able to create a product");
    }
}
