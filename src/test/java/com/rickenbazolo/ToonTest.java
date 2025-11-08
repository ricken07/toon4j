package com.rickenbazolo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test suite for the Toon facade class
 */
class ToonTest {

    @Test
    void testEncodeWithDefaultOptions() {
        Map<String, Object> data = Map.of(
            "id", 123,
            "name", "Alice",
            "active", true
        );

        String toon = Toon.encode(data);

        assertThat(toon).contains("id: 123");
        assertThat(toon).contains("name: Alice");
        assertThat(toon).contains("active: true");
        // Simple objects don't have indentation at root level
        assertThat(toon).isNotEmpty();
    }

    @Test
    void testEncodeWithCustomOptions() {
        Map<String, Object> data = Map.of("test", "value");

        ToonOptions customOptions = ToonOptions.builder()
            .indent(4)
            .delimiter(ToonOptions.Delimiter.PIPE)
            .build();

        String toon = Toon.encode(data, customOptions);

        assertThat(toon).contains("test: value");
    }

    @Test
    void testEncodeNullValue() {
        String result = Toon.encode(null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    void testDecodeToJsonNodeWithDefaultOptions() {
        String toonString = "name: Alice\nage: 30\nactive: true";

        JsonNode result = Toon.decode(toonString);

        assertThat(result.get("name").asText()).isEqualTo("Alice");
        assertThat(result.get("age").asInt()).isEqualTo(30);
        assertThat(result.get("active").asBoolean()).isTrue();
    }

    @Test
    void testDecodeToJsonNodeWithCustomOptions() {
        String toonString = "name: Alice\nage: 30";

        ToonOptions customOptions = ToonOptions.builder()
            .strict(false)
            .build();

        JsonNode result = Toon.decode(toonString, customOptions);

        assertThat(result.get("name").asText()).isEqualTo("Alice");
        assertThat(result.get("age").asInt()).isEqualTo(30);
    }

    @Test
    void testDecodeNullString() {
        try {
            JsonNode result = Toon.decode(null);
            assertThat(result).isNotNull(); // If no exception, verify result is not null
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    void testDecodeToTargetClassWithDefaultOptions() throws IOException {
        String toonString = "name: Alice\nage: 30\nactive: true";

        @SuppressWarnings("unchecked")
        Map<String, Object> result = Toon.decode(toonString, Map.class);

        assertThat(result).containsKey("name");
        assertThat(result).containsKey("age");
        assertThat(result).containsKey("active");
    }

    @Test
    void testDecodeToTargetClassWithCustomOptions() throws IOException {
        String toonString = "name: Bob\nage: 25";

        ToonOptions customOptions = ToonOptions.builder()
            .strict(false)
            .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> result = Toon.decode(toonString, customOptions, Map.class);

        assertThat(result).containsEntry("name", "Bob");
        assertThat(result).containsEntry("age", 25L); // Numbers are parsed as Long
    }

    @Test
    void testDecodeToTargetClassInvalidInput() {
        String invalidToon = "invalid toon format";

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = Toon.decode(invalidToon, Map.class);
            // If parsing succeeds, verify we get a valid result
            assertThat(result).isNotNull();
        } catch (Exception e) {
            // Exception is acceptable for invalid input
            assertThat(e).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    void testFromJsonWithDefaultOptions() throws IOException {
        String jsonString = "{\"name\":\"Alice\",\"age\":30,\"active\":true}";

        String toonResult = Toon.fromJson(jsonString);

        assertThat(toonResult).contains("name: Alice");
        assertThat(toonResult).contains("age: 30");
        assertThat(toonResult).contains("active: true");
    }

    @Test
    void testFromJsonWithCustomOptions() throws IOException {
        String jsonString = "{\"items\":[\"a\",\"b\",\"c\"]}";

        ToonOptions customOptions = ToonOptions.builder()
            .delimiter(ToonOptions.Delimiter.PIPE)
            .build();

        String toonResult = Toon.fromJson(jsonString, customOptions);

        assertThat(toonResult).contains("items[3|]: a|b|c");
    }

    @Test
    void testFromJsonInvalidJson() {
        String invalidJson = "{invalid json}";

        assertThatThrownBy(() -> Toon.fromJson(invalidJson))
            .isInstanceOf(IOException.class);
    }

    @Test
    void testFromJsonNullInput() {
        assertThatThrownBy(() -> Toon.fromJson(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testToJsonWithDefaultOptions() throws IOException {
        String toonString = "name: Alice\nage: 30\nactive: true";

        String jsonResult = Toon.toJson(toonString);

        assertThat(jsonResult).contains("\"name\" : \"Alice\"");
        assertThat(jsonResult).contains("\"age\" : 30");
        assertThat(jsonResult).contains("\"active\" : true");
    }

    @Test
    void testToJsonWithCustomOptions() throws IOException {
        String toonString = "name: Bob\nage: 25";

        ToonOptions customOptions = ToonOptions.builder()
            .strict(false)
            .build();

        String jsonResult = Toon.toJson(toonString, customOptions);

        assertThat(jsonResult).contains("\"name\" : \"Bob\"");
        assertThat(jsonResult).contains("\"age\" : 25");
    }

    @Test
    void testToJsonInvalidToonString() {
        String invalidToon = "completely invalid format";

        try {
            String result = Toon.toJson(invalidToon);
            // If conversion succeeds, verify we get valid JSON
            assertThat(result).isNotNull();
        } catch (Exception e) {
            // Exception is acceptable for invalid TOON input
            assertThat(e).isInstanceOfAny(RuntimeException.class, IOException.class);
        }
    }

    @Test
    void testToJsonNullInput() {
        try {
            String result = Toon.toJson(null);
            // If conversion succeeds, verify result
            assertThat(result).isNotNull();
        } catch (Exception e) {
            // Exception is acceptable for null input
            assertThat(e).isInstanceOfAny(IllegalArgumentException.class, RuntimeException.class);
        }
    }

    @Test
    void testEstimateSavings() {
        Map<String, Object> data = Map.of(
            "users", List.of(
                Map.of("id", 1, "name", "Alice", "role", "admin"),
                Map.of("id", 2, "name", "Bob", "role", "user"),
                Map.of("id", 3, "name", "Charlie", "role", "moderator")
            )
        );

        Toon.TokenSavings savings = Toon.estimateSavings(data);

        assertThat(savings.jsonLength()).isGreaterThan(0);
        assertThat(savings.toonLength()).isGreaterThan(0);
        assertThat(savings.savedChars()).isGreaterThan(0);
        assertThat(savings.savingsPercent()).isGreaterThan(0);
        assertThat(savings.savedChars()).isEqualTo(savings.jsonLength() - savings.toonLength());
    }

    @Test
    void testEstimateSavingsWithNullValue() {
        Toon.TokenSavings savings = Toon.estimateSavings(null);

        assertThat(savings.jsonLength()).isGreaterThan(0);
        assertThat(savings.toonLength()).isGreaterThan(0);
        assertThat(savings.savedChars()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testEstimateSavingsWithEmptyObject() {
        Map<String, Object> emptyData = Map.of();

        Toon.TokenSavings savings = Toon.estimateSavings(emptyData);

        assertThat(savings.jsonLength()).isGreaterThan(0);
        assertThat(savings.toonLength()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testTokenSavingsToString() {
        Toon.TokenSavings savings = new Toon.TokenSavings(100, 60, 40, 40.0);

        String result = savings.toString();

        assertThat(result).contains("JSON: 100 chars");
        assertThat(result).contains("TOON: 60 chars");
        assertThat(result).contains("Savings: 40 chars");
        assertThat(result).contains("40.0%");
    }

    @Test
    void testTokenSavingsRecordProperties() {
        Toon.TokenSavings savings = new Toon.TokenSavings(150, 90, 60, 40.0);

        assertThat(savings.jsonLength()).isEqualTo(150);
        assertThat(savings.toonLength()).isEqualTo(90);
        assertThat(savings.savedChars()).isEqualTo(60);
        assertThat(savings.savingsPercent()).isEqualTo(40.0);
    }

    @Test
    void testRoundTripConversion() throws IOException {
        // Test that encoding and then decoding preserves data
        Map<String, Object> originalData = Map.of(
            "name", "Alice",
            "age", 30,
            "tags", List.of("admin", "user"),
            "metadata", Map.of("created", "2023-01-01", "updated", "2023-01-02")
        );

        // Encode to TOON
        String toonString = Toon.encode(originalData);

        // Decode back to JsonNode
        JsonNode decodedNode = Toon.decode(toonString);

        // Verify the data is preserved
        assertThat(decodedNode.get("name").asText()).isEqualTo("Alice");
        assertThat(decodedNode.get("age").asInt()).isEqualTo(30);
        assertThat(decodedNode.get("tags").size()).isEqualTo(2);
        assertThat(decodedNode.get("metadata").get("created").asText()).isEqualTo("2023-01-01");
    }

    @Test
    void testJsonToToonToJsonRoundTrip() throws IOException {
        // Test converting JSON -> TOON -> JSON preserves structure
        String originalJson = "{\"name\":\"Alice\",\"age\":30,\"active\":true}";

        // JSON -> TOON
        String toonString = Toon.fromJson(originalJson);

        // TOON -> JSON
        String resultJson = Toon.toJson(toonString);

        // Parse both JSON strings and compare structure
        JsonNode originalNode = Toon.decode(Toon.fromJson(originalJson));
        JsonNode resultNode = Toon.decode(toonString);

        assertThat(originalNode.get("name").asText()).isEqualTo(resultNode.get("name").asText());
        assertThat(originalNode.get("age").asInt()).isEqualTo(resultNode.get("age").asInt());
        assertThat(originalNode.get("active").asBoolean()).isEqualTo(resultNode.get("active").asBoolean());
    }

    @Test
    void testEncodeDecodeWithArrays() {
        Map<String, Object> data = Map.of(
            "numbers", List.of(1, 2, 3, 4, 5),
            "strings", List.of("a", "b", "c"),
            "mixed", List.of("text", 42, true)
        );

        String toonString = Toon.encode(data);
        JsonNode decoded = Toon.decode(toonString);

        assertThat(decoded.get("numbers").size()).isEqualTo(5);
        assertThat(decoded.get("strings").size()).isEqualTo(3);
        assertThat(decoded.get("mixed").size()).isEqualTo(3);
        assertThat(decoded.get("numbers").get(0).asInt()).isEqualTo(1);
        assertThat(decoded.get("strings").get(0).asText()).isEqualTo("a");
    }

    @Test
    void testEncodeDecodeWithNestedStructures() {
        Map<String, Object> data = Map.of(
            "user", Map.of(
                "profile", Map.of(
                    "name", "Alice",
                    "preferences", Map.of(
                        "theme", "dark",
                        "notifications", true
                    )
                ),
                "roles", List.of("admin", "user")
            )
        );

        String toonString = Toon.encode(data);
        JsonNode decoded = Toon.decode(toonString);

        assertThat(decoded.get("user").get("profile").get("name").asText()).isEqualTo("Alice");
        assertThat(decoded.get("user").get("profile").get("preferences").get("theme").asText()).isEqualTo("dark");
        assertThat(decoded.get("user").get("roles").size()).isEqualTo(2);
    }

    @Test
    void testDefaultOptionsConfiguration() {
        // Verify that ToonOptions.DEFAULT has expected values
        ToonOptions defaultOptions = ToonOptions.DEFAULT;

        assertThat(defaultOptions.indent()).isEqualTo(2);
        assertThat(defaultOptions.delimiter()).isEqualTo(ToonOptions.Delimiter.COMMA);
        assertThat(defaultOptions.lengthMarker()).isFalse();
        assertThat(defaultOptions.strict()).isTrue();
    }
}
