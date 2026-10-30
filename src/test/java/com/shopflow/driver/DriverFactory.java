package com.shopflow.driver;

import java.net.URL;
import java.time.Duration;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.shopflow.config.Config;

/**
 * Creates and stores one WebDriver per thread.
 *
 * ThreadLocal is what makes parallel execution safe: every TestNG test thread
 * gets its own isolated browser. A remoteUrl in the config switches every
 * browser to a Selenium Grid node (RemoteWebDriver) for distributed runs.
 */
public final class DriverFactory {

	private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

	private DriverFactory() {
	}

	public static void createDriver() {
		boolean headless = Config.headless();
		System.out.printf("[DriverFactory] launching %s | headless=%b | CI=%s%n", Config.browser(), headless,
				System.getenv("CI"));
		MutableCapabilities options = switch (Config.browser()) {
		case CHROME -> chromeOptions(headless);
		case FIREFOX -> firefoxOptions(headless);
		case EDGE -> edgeOptions(headless);
		};

		WebDriver driver;
		String remote = Config.remoteUrl();
		try {
			if (remote != null && !remote.isBlank()) {
				driver = new RemoteWebDriver(new URL(remote), options);
			} else {
				driver = switch (Config.browser()) {
				case CHROME -> new org.openqa.selenium.chrome.ChromeDriver((ChromeOptions) options);
				case FIREFOX -> new org.openqa.selenium.firefox.FirefoxDriver((FirefoxOptions) options);
				case EDGE -> new org.openqa.selenium.edge.EdgeDriver((EdgeOptions) options);
				};
			}
		} catch (Exception e) {
			throw new IllegalStateException("Could not start WebDriver (" + Config.browser() + ")", e);
		}

		driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
		driver.manage().window().setSize(new org.openqa.selenium.Dimension(1440, 900));
		DRIVER.set(driver);
	}

	public static WebDriver getDriver() {
		WebDriver driver = DRIVER.get();
		if (driver == null) {
			throw new IllegalStateException("WebDriver not started. Does the test extend BaseTest?");
		}
		return driver;
	}

	public static boolean hasDriver() {
		return DRIVER.get() != null;
	}

	public static void quitDriver() {
		WebDriver driver = DRIVER.get();
		if (driver != null) {
			try {
				driver.quit();
			} finally {
				DRIVER.remove();
			}
		}
	}

	private static ChromeOptions chromeOptions(boolean headless) {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--incognito", "--disable-notifications");
		if (headless) {
			options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1440,900");
		}
		return options;
	}

	private static FirefoxOptions firefoxOptions(boolean headless) {
		FirefoxOptions options = new FirefoxOptions();
		if (headless) {
			options.addArguments("-headless");
		}
		return options;
	}

	private static EdgeOptions edgeOptions(boolean headless) {
		EdgeOptions options = new EdgeOptions();
		options.addArguments("--inprivate");
		if (headless) {
			options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
		}
		return options;
	}
}
