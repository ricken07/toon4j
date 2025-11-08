package com.rickenbazolo.example;

import com.rickenbazolo.Toon;
import com.rickenbazolo.ToonOptions;

import java.util.List;
import java.util.Map;

/**
 * Examples of TOON Java usage.
 * @author Ricken Bazolo
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== toon4j - examples ===\n");

        // Example 1: Simple object
        example1SimpleObject();

        // Example 2: Tabular array
        example2TabularArray();

        // Example 3: Tab delimiter
        example3TabDelimiter();

        // Example 4: Token savings estimation
        example4TokenSavings();

        // Example 5: JSON <-> TOON conversion
        example5JsonConversion();
    }

    private static void example1SimpleObject() {
        System.out.println("=== Example 1: Simple object ===");

        Map<String, Object> user = Map.of(
            "id", 123,
            "name", "Ada Lovelace",
            "email", "ada@example.com",
            "active", true
        );

        String toon = Toon.encode(user);
        System.out.println(toon);
        System.out.println();
    }

    private static void example2TabularArray() {
        System.out.println("=== Example 2: Tabular array ===");

        List<Map<String, Object>> users = List.of(
            Map.of("id", 1, "name", "Alice", "role", "admin"),
            Map.of("id", 2, "name", "Bob", "role", "user"),
            Map.of("id", 3, "name", "Charlie", "role", "user")
        );

        Map<String, Object> data = Map.of("users", users);

        String toon = Toon.encode(data);
        System.out.println(toon);
        System.out.println();
    }

    private static void example3TabDelimiter() {
        System.out.println("=== Example 3: TAB Delimiter ===");

        List<Map<String, Object>> items = List.of(
            Map.of("sku", "A1", "name", "Widget", "qty", 5, "price", 9.99),
            Map.of("sku", "B2", "name", "Gadget", "qty", 3, "price", 14.50)
        );

        Map<String, Object> data = Map.of("items", items);

        ToonOptions options = ToonOptions.builder()
            .delimiter(ToonOptions.Delimiter.TAB)
            .lengthMarker(true)
            .build();

        String toon = Toon.encode(data, options);
        System.out.println(toon);
        System.out.println();
    }

    private static void example4TokenSavings() {
        System.out.println("=== Example 4: Token savings estimation ===");

        List<Map<String, Object>> employees = List.of(
            Map.of("id", 1, "name", "Alice Smith", "department", "Engineering", "salary", 75000),
            Map.of("id", 2, "name", "Bob Jones", "department", "Sales", "salary", 65000),
            Map.of("id", 3, "name", "Charlie Brown", "department", "Marketing", "salary", 70000)
        );

        Map<String, Object> data = Map.of("employees", employees);

        Toon.TokenSavings savings = Toon.estimateSavings(data);
        System.out.println(savings);
        System.out.println();
    }

    private static void example5JsonConversion() {
        System.out.println("=== Example 5: JSON <-> TOON conversion ===");

        String json = """
            {
              "product": {
                "id": "P123",
                "name": "Laptop",
                "price": 999.99,
                "tags": ["electronics", "computers", "portable"]
              }
            }
            """;

        try {
            // JSON -> TOON
            String toon = Toon.fromJson(json);
            System.out.println("TOON:");
            System.out.println(toon);
            System.out.println();

            // TOON -> JSON
            String jsonBack = Toon.toJson(toon);
            System.out.println("JSON back:");
            System.out.println(jsonBack);
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
