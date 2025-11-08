package com.rickenbazolo;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class ToonEncoderTest {

    @Test
    void testEncodeSimpleObject() {
        Map<String, Object> data = Map.of(
            "id", 123,
            "name", "Ada",
            "active", true
        );

        String toon = Toon.encode(data);

        assertThat(toon).contains("id: 123");
        assertThat(toon).contains("name: Ada");
        assertThat(toon).contains("active: true");
    }

    @Test
    void testEncodeNestedObject() {
        Map<String, Object> data = Map.of(
            "user", Map.of(
                "id", 123,
                "name", "Ada"
            )
        );

        String toon = Toon.encode(data);

        assertThat(toon).contains("user:");
        assertThat(toon).contains("id: 123");
        assertThat(toon).contains("name: Ada");
    }

    @Test
    void testEncodePrimitiveArray() {
        Map<String, Object> data = Map.of(
            "tags", List.of("admin", "ops", "dev")
        );

        String toon = Toon.encode(data);

        assertThat(toon).contains("tags[3]: admin,ops,dev");
    }

    @Test
    void testEncodeTabularArray() {
        List<Map<String, Object>> items = List.of(
            Map.of("sku", "A1", "qty", 2, "price", 9.99),
            Map.of("sku", "B2", "qty", 1, "price", 14.5)
        );

        Map<String, Object> data = Map.of("items", items);
        String toon = Toon.encode(data);

        assertThat(toon).contains("items[2]{");
        // Fields are sorted alphabetically: price, qty, sku
        assertThat(toon).contains("9.99,2,A1");
        assertThat(toon).contains("14.5,1,B2");
    }

    @Test
    void testEncodeWithTabDelimiter() {
        List<Map<String, Object>> items = List.of(
            Map.of("sku", "A1", "qty", 2),
            Map.of("sku", "B2", "qty", 1)
        );

        Map<String, Object> data = Map.of("items", items);

        ToonOptions options = ToonOptions.builder()
            .delimiter(ToonOptions.Delimiter.TAB)
            .build();

        String toon = Toon.encode(data, options);

        assertThat(toon).contains("items[2 ]{");
        // Fields are sorted alphabetically: qty, sku
        assertThat(toon).contains("2\tA1");
    }

    @Test
    void testEncodeWithLengthMarker() {
        Map<String, Object> data = Map.of(
            "tags", List.of("a", "b", "c")
        );

        ToonOptions options = ToonOptions.builder()
            .lengthMarker(true)
            .build();

        String toon = Toon.encode(data, options);

        assertThat(toon).contains("tags[#3]:");
    }

    @Test
    void testEncodeEmptyArray() {
        Map<String, Object> data = Map.of(
            "items", List.of()
        );

        String toon = Toon.encode(data);

        assertThat(toon).contains("items[0]:");
    }

    @Test
    void testEncodeEmptyObject() {
        Map<String, Object> data = Map.of();

        String toon = Toon.encode(data);

        assertThat(toon).isEmpty();
    }

    @Test
    void testEncodeStringQuoting() {
        Map<String, Object> data = Map.of(
            "simple", "hello",
            "withSpaces", "hello world",
            "leadingSpace", " hello",
            "keyword", "true",
            "number", "42",
            "withComma", "a,b"
        );

        String toon = Toon.encode(data);

        assertThat(toon).contains("simple: hello"); // No quotes needed
        assertThat(toon).contains("withSpaces: hello world"); // Internal spaces OK
        assertThat(toon).contains("leadingSpace: \" hello\""); // Quotes necessary
        assertThat(toon).contains("keyword: \"true\""); // Quotes to avoid confusion
        assertThat(toon).contains("number: \"42\""); // Quotes to avoid confusion
        assertThat(toon).contains("withComma: \"a,b\""); // Quotes for delimiter
    }

    @Test
    void testEncodeNullValue() {
        Map<String, Object> data = new HashMap<>();
        data.put("value", null);

        String toon = Toon.encode(data);

        assertThat(toon).contains("value: null");
    }

    @Test
    void testEstimateSavings() {
        List<Map<String, Object>> users = List.of(
            Map.of("id", 1, "name", "Alice", "role", "admin"),
            Map.of("id", 2, "name", "Bob", "role", "user")
        );

        Map<String, Object> data = Map.of("users", users);

        Toon.TokenSavings savings = Toon.estimateSavings(data);

        assertThat(savings.jsonLength()).isGreaterThan(0);
        assertThat(savings.toonLength()).isGreaterThan(0);
        assertThat(savings.savedChars()).isGreaterThan(0);
        assertThat(savings.savingsPercent()).isGreaterThan(0);
    }
}
