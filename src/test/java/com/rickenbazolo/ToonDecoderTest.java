package com.rickenbazolo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToonDecoderTest {

    @Test
    void testDecodeSimpleObject() {
        String toon = """
            id: 123
            name: Ada
            active: true
            """;

        JsonNode node = Toon.decode(toon);

        assertThat(node.get("id").asInt()).isEqualTo(123);
        assertThat(node.get("name").asText()).isEqualTo("Ada");
        assertThat(node.get("active").asBoolean()).isTrue();
    }

    @Test
    void testDecodeNestedObject() {
        String toon = """
            user:
              id: 123
              name: Ada
            """;

        JsonNode node = Toon.decode(toon);

        assertThat(node.get("user").get("id").asInt()).isEqualTo(123);
        assertThat(node.get("user").get("name").asText()).isEqualTo("Ada");
    }

    @Test
    void testDecodePrimitiveArray() {
        String toon = "tags[3]: admin,ops,dev";

        JsonNode node = Toon.decode(toon);

        assertThat(node.get("tags").isArray()).isTrue();
        assertThat(node.get("tags").size()).isEqualTo(3);
        assertThat(node.get("tags").get(0).asText()).isEqualTo("admin");
        assertThat(node.get("tags").get(1).asText()).isEqualTo("ops");
        assertThat(node.get("tags").get(2).asText()).isEqualTo("dev");
    }

    @Test
    void testDecodeTabularArray() {
        String toon = """
            items[2]{sku,qty,price}:
            A1,2,9.99
            B2,1,14.5
            """;

        JsonNode node = Toon.decode(toon);

        assertThat(node.get("items").isArray()).isTrue();
        assertThat(node.get("items").size()).isEqualTo(2);

        JsonNode first = node.get("items").get(0);
        assertThat(first.get("sku").asText()).isEqualTo("A1");
        assertThat(first.get("qty").asInt()).isEqualTo(2);
        assertThat(first.get("price").asDouble()).isEqualTo(9.99);

        JsonNode second = node.get("items").get(1);
        assertThat(second.get("sku").asText()).isEqualTo("B2");
        assertThat(second.get("qty").asInt()).isEqualTo(1);
        assertThat(second.get("price").asDouble()).isEqualTo(14.5);
    }

    @Test
    void testDecodeTabularArrayWithTabDelimiter() {
        String toon = """
            items[2 ]{sku qty}:
            A1\t2
            B2\t1
            """;

        ToonOptions options = ToonOptions.builder()
            .delimiter(ToonOptions.Delimiter.TAB)
            .build();

        JsonNode node = Toon.decode(toon, options);

        assertThat(node.get("items").size()).isEqualTo(2);
        assertThat(node.get("items").get(0).get("sku").asText()).isEqualTo("A1");
        assertThat(node.get("items").get(0).get("qty").asInt()).isEqualTo(2);
    }

    @Test
    void testDecodeWithLengthMarker() {
        String toon = "tags[#3]: a,b,c";

        JsonNode node = Toon.decode(toon);

        assertThat(node.get("tags").size()).isEqualTo(3);
    }

    @Test
    void testDecodeEmptyArray() {
        String toon = "items[0]:";

        JsonNode node = Toon.decode(toon);

        assertThat(node.get("items").isArray()).isTrue();
        assertThat(node.get("items").size()).isEqualTo(0);
    }

    @Test
    void testDecodeQuotedStrings() {
        String toon = """
            simple: hello
            withSpaces: hello world
            leadingSpace: " hello"
            keyword: "true"
            number: "42"
            """;

        JsonNode node = Toon.decode(toon);

        assertThat(node.get("simple").asText()).isEqualTo("hello");
        assertThat(node.get("withSpaces").asText()).isEqualTo("hello world");
        assertThat(node.get("leadingSpace").asText()).isEqualTo(" hello");
        assertThat(node.get("keyword").asText()).isEqualTo("true");
        assertThat(node.get("number").asText()).isEqualTo("42");
    }

    @Test
    void testDecodeNull() {
        String toon = "value: null";

        JsonNode node = Toon.decode(toon);

        assertThat(node.get("value").isNull()).isTrue();
    }

    @Test
    void testDecodeNumbers() {
        String toon = """
            integer: 42
            negative: -10
            decimal: 3.14
            scientific: 1e6
            """;

        JsonNode node = Toon.decode(toon);

        assertThat(node.get("integer").asLong()).isEqualTo(42);
        assertThat(node.get("negative").asInt()).isEqualTo(-10);
        assertThat(node.get("decimal").asDouble()).isEqualTo(3.14);
        assertThat(node.get("scientific").asDouble()).isEqualTo(1000000.0);
    }

    @Test
    void testRoundTrip() {
        var original = java.util.Map.of(
            "id", 123,
            "name", "Ada",
            "tags", java.util.List.of("admin", "ops"),
            "items", java.util.List.of(
                java.util.Map.of("sku", "A1", "qty", 2),
                java.util.Map.of("sku", "B2", "qty", 1)
            )
        );

        String toon = Toon.encode(original);
        JsonNode decoded = Toon.decode(toon);

        assertThat(decoded.get("id").asInt()).isEqualTo(123);
        assertThat(decoded.get("name").asText()).isEqualTo("Ada");
        assertThat(decoded.get("tags").size()).isEqualTo(2);
        assertThat(decoded.get("items").size()).isEqualTo(2);
    }
}
