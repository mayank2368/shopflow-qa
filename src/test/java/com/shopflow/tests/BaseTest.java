package com.shopflow.tests;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.shopflow.api.ApiClient;
import com.shopflow.config.Users;
import com.shopflow.driver.DriverFactory;
import com.shopflow.pages.LoginPage;
import com.shopflow.pages.ProductsPage;

/**
 * Base for all tests.
 *
 * - A fresh browser per test (fresh session) → tests are independent and
 * parallel-safe. - The app's data is reset to the seed state before each test
 * via the API, so every test starts from a known, identical state.
 */
public abstract class BaseTest {

	protected final ApiClient api = new ApiClient();

	@BeforeMethod(alwaysRun = true)
	public void setUp() {
		api.reset();
		DriverFactory.createDriver();
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException ignored) {
		}
		DriverFactory.quitDriver();
	}

	protected WebDriver driver() {
		return DriverFactory.getDriver();
	}

	protected LoginPage openLogin() {
		return new LoginPage(driver()).open();
	}

	protected ProductsPage loginAsStandard() {
		return openLogin().loginAs(Users.standard());
	}

	protected ProductsPage loginAsAdmin() {
		return openLogin().loginAs(Users.admin());
	}

	/**
	 * Seeds a cart for the standard user via the API, then opens the app already
	 * logged in — so cart/checkout tests don't repeat the add-to-cart clicks.
	 */
	protected ProductsPage loginAsStandardWithCart(int... productIds) {
		String token = api.loginToken(Users.standard());
		for (int id : productIds) {
			api.addToCart(token, id, 1);
		}
		ProductsPage products = loginAsStandard();
		driver().navigate().refresh();
		return products.waitUntilLoaded();
	}
}
