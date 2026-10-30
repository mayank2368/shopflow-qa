package com.shopflow.config;

import java.util.Arrays;

public enum Browser {
    CHROME,
    FIREFOX,
    EDGE;

    public static Browser from(String value) {
        return Arrays.stream(values())
                .filter(b -> b.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported browser '" + value + "'. Use chrome, firefox or edge."));
    }
}
