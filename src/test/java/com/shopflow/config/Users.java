package com.shopflow.config;

/** Named test accounts, read from config so credentials live in one place. */
public record Users(String username, String password) {

    public static Users standard() {
        return of("standard");
    }

    public static Users admin() {
        return of("admin");
    }

    public static Users locked() {
        return of("locked");
    }

    private static Users of(String key) {
        return new Users(Config.get("user." + key + ".username"), Config.get("user." + key + ".password"));
    }
}
