package com.shopflow.pages;

/** The values of the sort dropdown on the products page. */
public enum SortOption {
    NAME_ASC("name-asc"),
    NAME_DESC("name-desc"),
    PRICE_ASC("price-asc"),
    PRICE_DESC("price-desc");

    private final String value;

    SortOption(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
