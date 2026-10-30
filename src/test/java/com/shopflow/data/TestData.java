package com.shopflow.data;

/** Product names and checkout data used across tests (match the app's seed data). */
public final class TestData {

    private TestData() {
    }

    // Seed products (see app/src/data.js)
    public static final String NORDIC_SOFA = "Nordic Sofa";
    public static final String OAK_TABLE = "Oak Table";
    public static final String OFFICE_CHAIR = "Office Chair";
    public static final String ARC_LAMP = "Arc Lamp";
    public static final String DESK_LAMP = "Desk Lamp";

    public static final int TOTAL_PRODUCTS = 12;
    public static final int PAGE_SIZE = 6;

    // Checkout
    public static final String FIRST_NAME = "Max";
    public static final String LAST_NAME = "Mustermann";
    public static final String POSTAL_CODE = "36037";
}
