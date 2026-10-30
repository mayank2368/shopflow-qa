package com.shopflow.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.shopflow.data.DataProviders;
import com.shopflow.data.TestData;
import com.shopflow.pages.ProductDetailPage;
import com.shopflow.pages.ProductsPage;
import com.shopflow.pages.SortOption;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.testng.annotations.Test;

@Epic("ShopFlow")
@Feature("Product catalogue")
public class ProductsTests extends BaseTest {

    @Test(groups = {"smoke", "regression"}, description = "TC-10: the first page shows a full page of products")
    @Story("Listing")
    public void firstPageShowsAFullPage() {
        ProductsPage products = loginAsStandard();
        assertEquals(products.productCount(), TestData.PAGE_SIZE, "first page should be full");
        assertTrue(products.resultCountText().contains(String.valueOf(TestData.TOTAL_PRODUCTS)),
                "result count should mention the total");
    }

    @Test(groups = "regression", dataProvider = "sortOptions", dataProviderClass = DataProviders.class,
            description = "TC-11: sorting orders the visible products correctly")
    @Story("Sorting")
    public void sortingOrdersProducts(SortOption option) {
        ProductsPage products = loginAsStandard().sortBy(option);
        assertEquals(products.selectedSort(), option.value(), "dropdown should reflect the chosen sort");

        switch (option) {
            case NAME_ASC -> assertSortedBy(products.productNames(), Comparator.naturalOrder());
            case NAME_DESC -> assertSortedBy(products.productNames(), Comparator.reverseOrder());
            case PRICE_ASC -> assertSortedBy(products.productPrices(), Comparator.naturalOrder());
            case PRICE_DESC -> assertSortedBy(products.productPrices(), Comparator.reverseOrder());
        }
    }

    @Test(groups = {"smoke", "regression"}, description = "TC-12: search narrows the catalogue to matching products")
    @Story("Search")
    public void searchNarrowsResults() {
        ProductsPage products = loginAsStandard().search("sofa");
        assertTrue(products.hasResults(), "sofa should return results");
        assertTrue(products.productNames().stream().allMatch(n -> n.toLowerCase().contains("sofa")),
                "every result should match the query: " + products.productNames());
    }

    @Test(groups = "regression", description = "TC-13: a search with no matches shows an empty state")
    @Story("Search")
    public void searchWithNoMatchesShowsEmptyState() {
        ProductsPage products = loginAsStandard().search("xyz-no-such-product");
        assertTrue(!products.hasResults(), "there should be no results");
        assertEquals(products.productCount(), 0);
    }

    @Test(groups = "regression", dataProvider = "categories", dataProviderClass = DataProviders.class,
            description = "TC-14: filtering by category shows only that category")
    @Story("Filtering")
    public void filterByCategory(String category, int expectedCount) {
        ProductsPage products = loginAsStandard().filterByCategory(category);
        assertEquals(products.productCount(), expectedCount, category + " should have " + expectedCount + " products");
    }

    @Test(groups = {"smoke", "regression"}, description = "TC-15: pagination moves between pages of results")
    @Story("Pagination")
    public void paginationMovesBetweenPages() {
        ProductsPage products = loginAsStandard();
        assertTrue(!products.prevEnabled(), "prev should be disabled on page 1");
        assertTrue(products.nextEnabled(), "next should be enabled on page 1");
        List<String> firstPage = products.productNames();

        products.nextPage();
        assertEquals(products.productCount(), TestData.TOTAL_PRODUCTS - TestData.PAGE_SIZE, "page 2 has the remainder");
        assertTrue(!products.nextEnabled(), "next should be disabled on the last page");
        assertTrue(products.prevEnabled(), "prev should be enabled on page 2");
        assertTrue(firstPage.stream().noneMatch(products.productNames()::contains),
                "page 2 should not repeat page 1 items");
    }

    @Test(groups = "regression", description = "TC-16: opening a product shows its detail")
    @Story("Product detail")
    public void openingAProductShowsDetail() {
        ProductDetailPage detail = loginAsStandard().openProduct(TestData.NORDIC_SOFA);
        assertEquals(detail.name(), TestData.NORDIC_SOFA);
        assertEquals(detail.category(), "Sofas");
        assertTrue(detail.price() > 0, "price should be shown");
        assertTrue(!detail.description().isBlank(), "description should be shown");
    }

    private static <T> void assertSortedBy(List<T> actual, Comparator<T> comparator) {
        List<T> expected = new ArrayList<>(actual);
        expected.sort(comparator);
        assertEquals(actual, expected, "items are not in the expected order");
    }
}
