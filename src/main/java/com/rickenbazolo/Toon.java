package com.rickenbazolo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Main facade class for encoding and decoding TOON (Token-Oriented Object Notation) format.
 *
 * <p>TOON is a compact serialization format optimized for reducing token usage when
 * interacting with Large Language Models (LLMs). It provides 30-60% token reduction
 * compared to JSON while maintaining human readability.</p>
 *
 * <p>This class provides static methods for:</p>
 * <ul>
 *   <li>Encoding Java objects to TOON format</li>
 *   <li>Decoding TOON strings to Java objects</li>
 *   <li>Converting between JSON and TOON formats</li>
 *   <li>Estimating token savings compared to JSON</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Encoding
 * Map<String, Object> data = Map.of("name", "Alice", "age", 30);
 * String toon = Toon.encode(data);
 *
 * // Decoding
 * JsonNode node = Toon.decode(toon);
 * MyClass obj = Toon.decode(toon, MyClass.class);
 *
 * // JSON conversion
 * String toonFromJson = Toon.fromJson(jsonString);
 * String jsonFromToon = Toon.toJson(toonString);
 * }</pre>
 *
 * @since 0.1.0
 * @author Ricken Bazolo
 */
public class Toon {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Encodes a Java object to TOON format using default options.
     *
     * <p>Uses default configuration with 2-space indentation, comma delimiter,
     * no length markers, and strict mode enabled.</p>
     *
     * @param value the object to encode (can be null)
     * @return the TOON representation as a string
     * @throws RuntimeException if encoding fails
     *
     * @see #encode(Object, ToonOptions)
     * @see ToonOptions#DEFAULT
     */
    public static String encode(Object value) {
        return encode(value, ToonOptions.DEFAULT);
    }

    /**
     * Encodes a Java object to TOON format using custom options.
     *
     * <p>The encoder will convert the object to an intermediate JsonNode representation
     * before applying TOON formatting rules based on the provided options.</p>
     *
     * @param value the object to encode (can be null)
     * @param options the encoding options to use (must not be null)
     * @return the TOON representation as a string
     * @throws RuntimeException if encoding fails
     * @throws IllegalArgumentException if options is null
     *
     * @see ToonEncoder
     * @see ToonOptions
     */
    public static String encode(Object value, ToonOptions options) {
        var encoder = new ToonEncoder(options);
        return encoder.encode(value);
    }

    /**
     * Decodes a TOON string to a JsonNode using default options.
     *
     * <p>Uses default configuration with 2-space indentation, comma delimiter,
     * no length markers, and strict mode enabled.</p>
     *
     * @param toonString the TOON string to decode (must not be null)
     * @return the decoded JsonNode representation
     * @throws RuntimeException if decoding fails
     * @throws IllegalArgumentException if toonString is null
     *
     * @see #decode(String, ToonOptions)
     * @see ToonOptions#DEFAULT
     */
    public static JsonNode decode(String toonString) {
        return decode(toonString, ToonOptions.DEFAULT);
    }

    /**
     * Decodes a TOON string to a JsonNode using custom options.
     *
     * <p>The decoder will parse the TOON format according to the provided options
     * and return a JsonNode that can be further processed or converted.</p>
     *
     * @param toonString the TOON string to decode (must not be null)
     * @param options the decoding options to use (must not be null)
     * @return the decoded JsonNode representation
     * @throws RuntimeException if decoding fails
     * @throws IllegalArgumentException if toonString or options is null
     *
     * @see ToonDecoder
     * @see ToonOptions
     */
    public static JsonNode decode(String toonString, ToonOptions options) {
        var decoder = new ToonDecoder(options);
        return decoder.decode(toonString);
    }

    /**
     * Decodes a TOON string and converts it to a Java object of the specified type.
     *
     * <p>Uses default options for decoding, then converts the resulting JsonNode
     * to the target class using Jackson's ObjectMapper.</p>
     *
     * @param <T> the target type
     * @param toonString the TOON string to decode (must not be null)
     * @param targetClass the class to convert to (must not be null)
     * @return the decoded and converted object
     * @throws IOException if conversion fails
     * @throws RuntimeException if decoding fails
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see #decode(String, ToonOptions, Class)
     */
    public static <T> T decode(String toonString, Class<T> targetClass) throws IOException {
        return decode(toonString, ToonOptions.DEFAULT, targetClass);
    }

    /**
     * Decodes a TOON string and converts it to a Java object of the specified type using custom options.
     *
     * <p>First decodes the TOON string using the provided options, then converts
     * the resulting JsonNode to the target class using Jackson's ObjectMapper.</p>
     *
     * @param <T> the target type
     * @param toonString the TOON string to decode (must not be null)
     * @param options the decoding options to use (must not be null)
     * @param targetClass the class to convert to (must not be null)
     * @return the decoded and converted object
     * @throws IOException if conversion fails
     * @throws RuntimeException if decoding fails
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see ToonDecoder
     * @see ObjectMapper#treeToValue(com.fasterxml.jackson.core.TreeNode, Class)
     */
    public static <T> T decode(String toonString, ToonOptions options, Class<T> targetClass) throws IOException {
        var node = decode(toonString, options);
        return OBJECT_MAPPER.treeToValue(node, targetClass);
    }

    /**
     * Converts a JSON string to TOON format using default options.
     *
     * <p>Parses the JSON string into a JsonNode and then encodes it to TOON format
     * using default configuration.</p>
     *
     * @param jsonString the JSON string to convert (must be valid JSON)
     * @return the TOON representation of the JSON data
     * @throws IOException if JSON parsing fails
     * @throws RuntimeException if TOON encoding fails
     * @throws IllegalArgumentException if jsonString is null
     *
     * @see #fromJson(String, ToonOptions)
     */
    public static String fromJson(String jsonString) throws IOException {
        return fromJson(jsonString, ToonOptions.DEFAULT);
    }

    /**
     * Converts a JSON string to TOON format using custom options.
     *
     * <p>Parses the JSON string into a JsonNode and then encodes it to TOON format
     * using the specified options for formatting and delimiters.</p>
     *
     * @param jsonString the JSON string to convert (must be valid JSON)
     * @param options the encoding options to use (must not be null)
     * @return the TOON representation of the JSON data
     * @throws IOException if JSON parsing fails
     * @throws RuntimeException if TOON encoding fails
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see ToonEncoder
     * @see ObjectMapper#readTree(String)
     */
    public static String fromJson(String jsonString, ToonOptions options) throws IOException {
        var node = OBJECT_MAPPER.readTree(jsonString);
        var encoder = new ToonEncoder(options);
        return encoder.encode(node);
    }

    /**
     * Converts a TOON string to JSON format using default options.
     *
     * <p>Decodes the TOON string and then converts it to pretty-printed JSON format.</p>
     *
     * @param toonString the TOON string to convert (must not be null)
     * @return the JSON representation of the TOON data (pretty-printed)
     * @throws IOException if JSON serialization fails
     * @throws RuntimeException if TOON decoding fails
     * @throws IllegalArgumentException if toonString is null
     *
     * @see #toJson(String, ToonOptions)
     */
    public static String toJson(String toonString) throws IOException {
        return toJson(toonString, ToonOptions.DEFAULT);
    }

    /**
     * Converts a TOON string to JSON format using custom options.
     *
     * <p>Decodes the TOON string using the specified options and then converts
     * it to pretty-printed JSON format.</p>
     *
     * @param toonString the TOON string to convert (must not be null)
     * @param options the decoding options to use (must not be null)
     * @return the JSON representation of the TOON data (pretty-printed)
     * @throws IOException if JSON serialization fails
     * @throws RuntimeException if TOON decoding fails
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see ToonDecoder
     * @see ObjectMapper#writerWithDefaultPrettyPrinter()
     */
    public static String toJson(String toonString, ToonOptions options) throws IOException {
        var node = decode(toonString, options);
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    /**
     * Estimates the token savings between JSON and TOON formats for the given object.
     *
     * <p><strong>Note:</strong> This estimation is approximate and based on character count
     * rather than actual token count. Actual token savings may vary depending on the
     * tokenizer used by the LLM.</p>
     *
     * <p>The method encodes the object to both pretty-printed JSON and TOON formats,
     * then calculates the difference in character count and percentage savings.</p>
     *
     * @param value the object to analyze for savings (can be null)
     * @return a TokenSavings record containing the analysis results
     * @throws RuntimeException if encoding to either format fails
     *
     * @see TokenSavings
     */
    public static TokenSavings estimateSavings(Object value) {
        try {
            var json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
            var toon = encode(value);

            var jsonLength = json.length();
            var toonLength = toon.length();
            var savedChars = jsonLength - toonLength;
            var savingsPercent = (savedChars * 100.0) / jsonLength;

            return new TokenSavings(jsonLength, toonLength, savedChars, savingsPercent);
        } catch (Exception e) {
            throw new RuntimeException("Error while estimating savings", e);
        }
    }

    /**
     * Results of token savings estimation between JSON and TOON formats.
     *
     * <p>This record contains the comparison metrics between JSON and TOON representations
     * of the same data, including character counts and percentage savings.</p>
     *
     * @param jsonLength the character count of the JSON representation
     * @param toonLength the character count of the TOON representation
     * @param savedChars the number of characters saved (jsonLength - toonLength)
     * @param savingsPercent the percentage of characters saved ((savedChars / jsonLength) * 100)
     *
     * @since 0.1.0
     */
    public record TokenSavings(
        int jsonLength,
        int toonLength,
        int savedChars,
        double savingsPercent
    ) {
        /**
         * Returns a formatted string representation of the savings analysis.
         *
         * @return a string showing JSON length, TOON length, and savings with percentage
         */
        @Override
        public String toString() {
            return String.format(
                "JSON: %d chars | TOON: %d chars | Savings: %d chars (%.1f%%)",
                jsonLength, toonLength, savedChars, savingsPercent
            );
        }
    }
}
