package com.shopflow.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.shopflow.data.DataProviders;
import com.shopflow.data.TestData;
import com.shopflow.pages.CartPage;
import com.shopflow.pages.CheckoutCompletePage;
import com.shopflow.pages.CheckoutInfoPage;
import com.shopflow.pages.CheckoutOverviewPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

@Epic("ShopFlow")
@Feature("Checkout")
public class CheckoutTests extends BaseTest {

    @Test(groups = {"smoke", "regression"}, description = "TC-30: a full purchase completes and returns an order id")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void fullPurchaseCompletes() {
        CartPage cart = loginAsStandardWithCart(1, 2).header().openCart();

        CheckoutInfoPage info = cart.checkout();
        CheckoutOverviewPage overview = info
                .fillInformation(TestData.FIRST_NAME, TestData.LAST_NAME, TestData.POSTAL_CODE)
                .continueToOverview();

        CheckoutCompletePage complete = overview.finish();
        assertTrue(complete.isConfirmed(), "a confirmation should be shown");
        assertTrue(complete.orderId().contains("ORD-"), "an order id should be shown: " + complete.orderId());
        assertEquals(complete.header().cartCount(), 0, "cart should be emptied after purchase");
    }

    @Test(groups = "regression", description = "TC-31: the overview totals equal subtotal plus 19% tax")
    @Story("Totals")
    public void overviewTotalsAddUp() {
        CartPage cart = loginAsStandardWithCart(1, 2, 3).header().openCart();
        double cartSubtotal = cart.subtotal();

        CheckoutOverviewPage overview = cart.checkout()
                .fillInformation(TestData.FIRST_NAME, TestData.LAST_NAME, TestData.POSTAL_CODE)
                .continueToOverview();

        double expectedTax = round(cartSubtotal * 0.19);
        assertEquals(overview.subtotal(), cartSubtotal, 0.01, "overview subtotal should match the cart");
        assertEquals(overview.tax(), expectedTax, 0.01, "tax should be 19%");
        assertEquals(overview.total(), round(cartSubtotal + expectedTax), 0.01, "total = subtotal + tax");
    }

    @Test(groups = "regression", dataProvider = "missingCheckoutFields", dataProviderClass = DataProviders.class,
            description = "TC-32: missing checkout fields are rejected with a message")
    @Story("Validation")
    public void missingCheckoutFieldsAreRejected(String first, String last, String zip, String expectedError) {
        CheckoutInfoPage info = loginAsStandardWithCart(1).header().openCart().checkout();
        info.fillInformation(first, last, zip).continueExpectingError();
        assertTrue(info.errorMessage().contains(expectedError),
                "expected '" + expectedError + "' but was: " + info.errorMessage());
    }

    @Test(groups = "regression", description = "TC-33: cancelling checkout returns to the cart with items intact")
    @Story("Cancel")
    public void cancellingReturnsToCart() {
        CartPage cart = loginAsStandardWithCart(1, 2).header().openCart();
        CartPage back = cart.checkout().cancel();
        assertEquals(back.itemCount(), 2, "items should still be in the cart");
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
