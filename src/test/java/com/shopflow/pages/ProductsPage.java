package com.shopflow.pages;

import com.shopflow.components.Header;
import io.qameta.allure.Step;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class ProductsPage extends BasePage {

    private static final By GRID = test("product-grid");
    private static final By CARD = test("product-card");
    private static final By NAME = test("product-name");
    private static final By PRICE = test("product-price");
    private static final By SEARCH = test("search");
    private static final By SORT = test("sort");
    private static final By CATEGORY = test("category-filter");
    private static final By RESULT_COUNT = test("result-count");
    private static final By NO_RESULTS = test("no-results");
    private static final By PAGE_INFO = test("page-info");
    private static final By NEXT = test("page-next");
    private static final By PREV = test("page-prev");

    private final Header header;

    public ProductsPage(WebDriver driver) {
        super(driver);
        this.header = new Header(driver);
    }

    public ProductsPage waitUntilLoaded() {
        waits.urlContains("/products");
        waits.visible(RESULT_COUNT);
        return this;
    }

    public Header header() {
        return header;
    }

    public int productCount() {
        return driver.findElements(CARD).size();
    }

    public List<String> productNames() {
        return driver.findElements(NAME).stream().map(e -> e.getText().trim()).toList();
    }

    public List<Double> productPrices() {
        return driver.findElements(PRICE).stream().map(e -> parseMoney(e.getText())).toList();
    }

    @Step("Search for '{query}'")
    public ProductsPage search(String query) {
        WebElement oldGrid = currentGrid();
        type(SEARCH, query);
        waits.visible(SEARCH).sendKeys(org.openqa.selenium.Keys.ENTER);
        return waitForReload(oldGrid);
    }

    @Step("Sort by {option}")
    public ProductsPage sortBy(SortOption option) {
        WebElement oldGrid = currentGrid();
        new Select(waits.visible(SORT)).selectByValue(option.value());
        return waitForReload(oldGrid);
    }

    @Step("Filter by category '{category}'")
    public ProductsPage filterByCategory(String category) {
        WebElement oldGrid = currentGrid();
        new Select(waits.visible(CATEGORY)).selectByVisibleText(category);
        return waitForReload(oldGrid);
    }

    public String selectedSort() {
        return new Select(waits.visible(SORT)).getFirstSelectedOption().getDomAttribute("value");
    }

    @Step("Add '{productName}' to the cart")
    public ProductsPage addToCart(String productName) {
        card(productName).findElement(By.cssSelector("[data-test='add-to-cart']")).click();
        // wait until the cart badge reflects the add (the toast appears too)
        waits.until(d -> isDisplayed(test("cart-badge")));
        return this;
    }

    public ProductDetailPage openProduct(String productName) {
        card(productName).findElement(NAME).click();
        return new ProductDetailPage(driver);
    }

    public String resultCountText() {
        return text(RESULT_COUNT);
    }

    public boolean hasResults() {
        return !isDisplayed(NO_RESULTS);
    }

    // ---- pagination ----
    public String pageInfo() {
        return text(PAGE_INFO);
    }

    public boolean nextEnabled() {
        return waits.visible(NEXT).isEnabled();
    }

    public boolean prevEnabled() {
        return waits.visible(PREV).isEnabled();
    }

    public ProductsPage nextPage() {
        WebElement oldGrid = currentGrid();
        click(NEXT);
        return waitForReload(oldGrid);
    }

    /**
     * Finds a product's card by name. The catalogue is paginated and sorted, so
     * the product may not be on the current page — if it isn't, we search for it
     * first. That makes {@code addToCart}/{@code openProduct} work regardless of
     * default sort or which page is showing.
     */
    private WebElement card(String productName) {
        List<WebElement> matches = cardsNamed(productName);
        if (matches.isEmpty()) {
            search(productName);
            matches = cardsNamed(productName);
        }
        return matches.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not found in catalogue: " + productName));
    }

    private List<WebElement> cardsNamed(String productName) {
        return driver.findElements(CARD).stream()
                .filter(c -> c.findElement(NAME).getText().trim().equals(productName))
                .toList();
    }

    /** The grid element currently on the page, or null if none is shown (e.g. a no-results state). */
    private WebElement currentGrid() {
        return driver.findElements(GRID).stream().findFirst().orElse(null);
    }

    /**
     * Waits for a filter/sort/pagination action to finish. The SPA re-renders the
     * product list in place, replacing the grid node, so we wait for the grid that
     * existed <em>before</em> the action (captured by the caller) to go stale. The
     * stale reference must be captured before triggering the action — capturing it
     * afterwards races the re-render and can grab the new grid, which never goes
     * stale. If the grid isn't replaced (e.g. the result set is unchanged) the wait
     * falls back to the result-count being present.
     */
    private ProductsPage waitForReload(WebElement oldGrid) {
        if (oldGrid != null) {
            try {
                waits.until(ExpectedConditions.stalenessOf(oldGrid));
            } catch (org.openqa.selenium.TimeoutException ignored) {
                // Grid node was reused rather than replaced — nothing to wait on.
            }
        }
        waits.visible(RESULT_COUNT);
        return this;
    }
}
