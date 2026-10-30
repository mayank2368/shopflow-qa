package com.shopflow.data;

import com.shopflow.pages.SortOption;
import org.testng.annotations.DataProvider;

/** Data-driven inputs. One test method runs once per row. */
public final class DataProviders {

    private DataProviders() {
    }

    /** username, password, expected error fragment */
    @DataProvider(name = "invalidLogins")
    public static Object[][] invalidLogins() {
        return new Object[][] {
                {"", "Pass123!", "match any user"},
                {"standard_user", "", "match any user"},
                {"standard_user", "wrong", "match any user"},
                {"ghost_user", "Pass123!", "match any user"},
                {"STANDARD_USER", "Pass123!", "match any user"},
                {"locked_user", "Pass123!", "locked"},
        };
    }

    /** firstName, lastName, postalCode, expected error */
    @DataProvider(name = "missingCheckoutFields")
    public static Object[][] missingCheckoutFields() {
        return new Object[][] {
                {"", "Mustermann", "36037", "First name is required"},
                {"Max", "", "36037", "Last name is required"},
                {"Max", "Mustermann", "", "Postal code is required"},
        };
    }

    @DataProvider(name = "sortOptions")
    public static Object[][] sortOptions() {
        return new Object[][] {
                {SortOption.NAME_ASC},
                {SortOption.NAME_DESC},
                {SortOption.PRICE_ASC},
                {SortOption.PRICE_DESC},
        };
    }

    @DataProvider(name = "categories")
    public static Object[][] categories() {
        return new Object[][] {{"Sofas", 3}, {"Tables", 3}, {"Chairs", 3}, {"Lamps", 3}};
    }
}
