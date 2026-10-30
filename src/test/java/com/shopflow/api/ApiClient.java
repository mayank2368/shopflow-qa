package com.shopflow.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.config.Config;
import com.shopflow.config.Users;
import io.qameta.allure.Step;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Thin REST client for the ShopFlow API, built on Java's HttpClient.
 *
 * Used for two things:
 *   1. Test data management — reset/seed the app and build state (e.g. a cart)
 *      quickly via the API instead of clicking through the UI. This makes UI
 *      tests faster and focused on the behaviour under test.
 *   2. API-level functional tests (see tests/api).
 */
public class ApiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String baseUrl = Config.baseUrl();

    public record Response(int status, JsonNode body) {
        public String text(String field) {
            return body != null && body.has(field) ? body.get(field).asText() : null;
        }

        /** Returns the array under {@code field}, or an empty array if absent. */
        public JsonNode jsonArray(String field) {
            return body != null && body.has(field) ? body.get(field) : MAPPER.createArrayNode();
        }
    }

    public Response send(String method, String path, String token, Object body) {
        try {
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body));
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .method(method, publisher);
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> res = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode json = res.body() == null || res.body().isBlank() ? null : MAPPER.readTree(res.body());
            return new Response(res.statusCode(), json);
        } catch (Exception e) {
            throw new RuntimeException(method + " " + path + " failed: " + e.getMessage(), e);
        }
    }

    public Response get(String path) {
        return send("GET", path, null, null);
    }

    public Response get(String path, String token) {
        return send("GET", path, token, null);
    }

    public Response post(String path, String token, Object body) {
        return send("POST", path, token, body);
    }

    // ---- auth / checkout (used by the API test suite) ----

    @Step("Log in via API as {user.username}")
    public Response login(Users user) {
        return login(user.username(), user.password());
    }

    public Response login(String username, String password) {
        return post("/api/login", null, Map.of("username", username, "password", password));
    }

    @Step("Place an order via API")
    public Response checkout(String token, String firstName, String lastName, String postalCode) {
        return post("/api/orders", token, Map.of(
                "firstName", firstName, "lastName", lastName, "postalCode", postalCode));
    }

    // ---- test-data management ----

    @Step("Reset application data to the seed state")
    public void reset() {
        int status = post("/api/test/reset", null, null).status();
        if (status != 204) {
            throw new IllegalStateException("Reset hook returned " + status + " — is ALLOW_TEST_RESET enabled?");
        }
    }

    @Step("Seed application data")
    public void seed(Object seedPayload) {
        post("/api/test/seed", null, seedPayload);
    }

    @Step("Log in via API as {user.username}")
    public String loginToken(Users user) {
        Response res = post("/api/login", null, Map.of("username", user.username(), "password", user.password()));
        if (res.status() != 200) {
            throw new IllegalStateException("API login failed for " + user.username() + ": " + res.status());
        }
        return res.text("token");
    }

    @Step("Add product {productId} to the cart via API")
    public void addToCart(String token, int productId, int quantity) {
        post("/api/cart/items", token, Map.of("productId", productId, "quantity", quantity));
    }
}
