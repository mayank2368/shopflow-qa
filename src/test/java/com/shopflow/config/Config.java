package com.shopflow.config;

import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/**
 * Loads config.properties and lets any value be overridden by a system property
 * (-Dkey=value) or an environment variable of the same name, in that order of
 * precedence. Keeps configuration in one place and out of the test code.
 */
public final class Config {

    private static final Properties PROPS = load();

    private Config() {
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not read config.properties", e);
        }
        return props;
    }

    public static String get(String key) {
        String sys = System.getProperty(key);
        if (isSet(sys)) {
            return sys.trim();
        }
        String env = System.getenv(key.replace('.', '_').toUpperCase());
        if (isSet(env)) {
            return env.trim();
        }
        String prop = PROPS.getProperty(key);
        return prop == null ? null : prop.trim();
    }

    public static String get(String key, String fallback) {
        String value = get(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static boolean isSet(String v) {
        return v != null && !v.isBlank() && !v.startsWith("${");
    }

    public static String baseUrl() {
        String url = get("baseUrl", "http://localhost:5000");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static Browser browser() {
        return Browser.from(get("browser", "chrome"));
    }

    /** Headless is forced on when running in CI (GitHub Actions sets CI=true). */
    public static boolean headless() {
        return "true".equalsIgnoreCase(System.getenv("CI")) || Boolean.parseBoolean(get("headless", "true"));
    }

    public static String remoteUrl() {
        return get("remoteUrl", "");
    }

    public static Duration timeout() {
        return Duration.ofSeconds(Long.parseLong(get("timeoutSeconds", "10")));
    }
}
