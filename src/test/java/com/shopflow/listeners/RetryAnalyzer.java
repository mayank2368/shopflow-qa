package com.shopflow.listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries a failed test once. UI tests can fail for transient reasons (a slow
 * render, a network blip); a single retry removes most false reds without
 * hiding real, repeatable failures. The retry count is intentionally low so a
 * genuinely broken test still fails fast.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRIES = Integer.parseInt(System.getProperty("retryCount", "1"));
    private int attempts = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (attempts < MAX_RETRIES) {
            attempts++;
            return true;
        }
        return false;
    }
}
