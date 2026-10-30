package com.shopflow.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.shopflow.data.TestData;
import com.shopflow.pages.CartPage;
import com.shopflow.pages.ProductsPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

@Epic("ShopFlow")
@Feature("Shopping cart")
public class CartTests extends BaseTest {

    @Test(groups = {"smoke", "regression"}, description = "TC-20: adding a product updates the cart badge")
    @Story("Add to cart")
    public void addingAProductUpdatesTheBadge() {
        ProductsPage products = loginAsStandard().addToCart(TestData.NORDIC_SOFA);
        assertEquals(products.header().cartCount(), 1, "badge should show one item");
    }

    @Test(groups = "regression", description = "TC-21: adding from the product detail page works")
    @Story("Add to cart")
    public void addingFromDetailPageWorks() {
        var detail = loginAsStandard().openProduct(TestData.OAK_TABLE);
        detail.addToCart();
        assertEquals(detail.header().cartCount(), 1);
    }

    @Test(groups = "regression", description = "TC-22: several different products accumulate in the cart")
    @Story("Add to cart")
    public void severalProductsAccumulate() {
        ProductsPage products = loginAsStandard()
                .addToCart(TestData.NORDIC_SOFA)
                .addToCart(TestData.OAK_TABLE)
                .addToCart(TestData.OFFICE_CHAIR);
        assertEquals(products.header().cartCount(), 3);

        CartPage cart = products.header().openCart();
        assertEquals(cart.itemCount(), 3);
        assertTrue(cart.itemNames().containsAll(
                java.util.List.of(TestData.NORDIC_SOFA, TestData.OAK_TABLE, TestData.OFFICE_CHAIR)));
    }

    @Test(groups = {"smoke", "regression"}, description = "TC-23: a seeded cart shows its items on the cart page")
    @Story("Cart page")
    public void seededCartShowsItems() {
        CartPage cart = loginAsStandardWithCart(1, 2).header().openCart();
        assertEquals(cart.itemCount(), 2);
    }

    @Test(groups = "regression", description = "TC-24: changing the quantity updates the line and subtotal")
    @Story("Update quantity")
    public void changingQuantityUpdatesSubtotal() {
        CartPage cart = loginAsStandardWithCart(1).header().openCart();
        double unitSubtotal = cart.subtotal();

        cart.setQuantity(TestData.NORDIC_SOFA, 3);
        assertEquals(cart.subtotal(), unitSubtotal * 3, 0.01, "subtotal should triple");
    }

    @Test(groups = "regression", description = "TC-25: removing the last item empties the cart")
    @Story("Remove item")
    public void removingLastItemEmptiesCart() {
        CartPage cart = loginAsStandardWithCart(1).header().openCart();
        cart.removeItem(TestData.NORDIC_SOFA);
        assertTrue(cart.isEmpty(), "cart should be empty");
        assertEquals(cart.header().cartCount(), 0, "badge should be cleared");
    }
}
