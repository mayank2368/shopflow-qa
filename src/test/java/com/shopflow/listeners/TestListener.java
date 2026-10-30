package com.shopflow.listeners;

import com.shopflow.driver.DriverFactory;
import com.shopflow.utils.ScreenshotUtil;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * On failure, captures a screenshot into the Allure report. Logs test start/
 * end to the console for live visibility in CI.
 */
public class TestListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        System.out.printf("▶ %s.%s%n", result.getTestClass().getRealClass().getSimpleName(), result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (DriverFactory.hasDriver()) {
            ScreenshotUtil.attachToAllure(DriverFactory.getDriver(), "failure-" + result.getMethod().getMethodName());
        }
        System.out.printf("✗ %s — %s%n", result.getMethod().getMethodName(),
                result.getThrowable() == null ? "" : result.getThrowable().getMessage());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.printf("✓ %s%n", result.getMethod().getMethodName());
    }
}
