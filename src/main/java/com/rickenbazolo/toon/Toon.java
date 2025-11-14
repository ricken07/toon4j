package com.rickenbazolo.toon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickenbazolo.toon.config.ToonOptions;
import com.rickenbazolo.toon.converter.json.JsonToToonConverter;
import com.rickenbazolo.toon.converter.json.JsonToToonOptions;
import com.rickenbazolo.toon.converter.json.ToonToJsonConverter;
import com.rickenbazolo.toon.converter.json.ToonToJsonOptions;
import com.rickenbazolo.toon.converter.xml.ToonToXmlConverter;
import com.rickenbazolo.toon.converter.xml.ToonToXmlOptions;
import com.rickenbazolo.toon.converter.xml.XmlToToonConverter;
import com.rickenbazolo.toon.converter.xml.XmlToToonOptions;
import com.rickenbazolo.toon.core.ToonDecoder;
import com.rickenbazolo.toon.core.ToonEncoder;
import com.rickenbazolo.toon.exception.XmlException;
import com.rickenbazolo.toon.exception.XmlParseException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

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
 *   <li>Converting XML to TOON format</li>
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
 *
 * // XML conversion
 * String toonFromXml = Toon.fromXml(xmlString);
 * String xmlFromToon = Toon.toXml(toonString);
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
     * <p>Uses the JsonToToonConverter to parse JSON and encode to TOON format.</p>
     *
     * @param jsonString the JSON string to convert (must be valid JSON)
     * @return the TOON representation of the JSON data
     * @throws IOException if JSON parsing fails
     * @throws IllegalArgumentException if jsonString is null
     *
     * @see #fromJson(String, JsonToToonOptions)
     * @see JsonToToonConverter
     * @since 0.1.0
     */
    public static String fromJson(String jsonString) throws IOException {
        return fromJson(jsonString, JsonToToonOptions.DEFAULT);
    }

    /**
     * Converts a JSON string to TOON format using custom TOON options.
     *
     * <p>Uses the JsonToToonConverter with the specified TOON encoding options.</p>
     *
     * @param jsonString the JSON string to convert (must be valid JSON)
     * @param options the TOON encoding options to use (must not be null)
     * @return the TOON representation of the JSON data
     * @throws IOException if JSON parsing fails
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see #fromJson(String, JsonToToonOptions)
     * @since 0.1.0
     */
    public static String fromJson(String jsonString, ToonOptions options) throws IOException {
        return fromJson(jsonString, new JsonToToonOptions(options));
    }

    /**
     * Converts a JSON string to TOON format using custom JSON-to-TOON options.
     *
     * <p>Uses the JsonToToonConverter with the specified options.</p>
     *
     * @param jsonString the JSON string to convert (must be valid JSON)
     * @param options the conversion options to use (must not be null)
     * @return the TOON representation of the JSON data
     * @throws IOException if JSON parsing fails
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see JsonToToonConverter
     * @see JsonToToonOptions
     * @since 0.2.0
     */
    public static String fromJson(String jsonString, JsonToToonOptions options) throws IOException {
        var converter = new JsonToToonConverter(options);
        return converter.convert(jsonString);
    }

    /**
     * Converts a TOON string to JSON format using default options.
     *
     * <p>Uses the ToonToJsonConverter with pretty-printing enabled.</p>
     *
     * @param toonString the TOON string to convert (must not be null)
     * @return the JSON representation of the TOON data (pretty-printed)
     * @throws IOException if JSON serialization fails
     * @throws ToonDecoder.ToonParseException if TOON decoding fails
     * @throws IllegalArgumentException if toonString is null
     *
     * @see #toJson(String, ToonToJsonOptions)
     * @see ToonToJsonConverter
     * @since 0.1.0
     */
    public static String toJson(String toonString) throws IOException {
        return toJson(toonString, ToonToJsonOptions.DEFAULT);
    }

    /**
     * Converts a TOON string to JSON format using custom TOON options.
     *
     * <p>Uses the ToonToJsonConverter with the specified TOON decoding options and pretty-printing.</p>
     *
     * @param toonString the TOON string to convert (must not be null)
     * @param options the TOON decoding options to use (must not be null)
     * @return the JSON representation of the TOON data (pretty-printed)
     * @throws IOException if JSON serialization fails
     * @throws ToonDecoder.ToonParseException if TOON decoding fails
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see #toJson(String, ToonToJsonOptions)
     * @since 0.1.0
     */
    public static String toJson(String toonString, ToonOptions options) throws IOException {
        return toJson(toonString, new ToonToJsonOptions(options, true));
    }

    /**
     * Converts a TOON string to JSON format using custom TOON-to-JSON options.
     *
     * <p>Uses the ToonToJsonConverter with the specified options.</p>
     *
     * @param toonString the TOON string to convert (must not be null)
     * @param options the conversion options to use (must not be null)
     * @return the JSON representation of the TOON data
     * @throws IOException if JSON serialization fails
     * @throws ToonDecoder.ToonParseException if TOON decoding fails
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see ToonToJsonConverter
     * @see ToonToJsonOptions
     * @since 0.2.0
     */
    public static String toJson(String toonString, ToonToJsonOptions options) throws IOException {
        var converter = new ToonToJsonConverter(options);
        return converter.convert(toonString);
    }

    /**
     * Converts an XML string to TOON format using default options.
     *
     * <p>Uses default configuration with attributes included with "@" prefix,
     * text content as "#text" field, and automatic array detection.</p>
     *
     * @param xmlString the XML string to convert (must be valid XML)
     * @return the TOON representation of the XML data
     * @throws XmlParseException if XML parsing fails
     * @throws IllegalArgumentException if xmlString is null
     *
     * @see #fromXml(String, XmlToToonOptions)
     * @see XmlToToonConverter
     * @since 0.2.0
     */
    public static String fromXml(String xmlString) throws XmlParseException {
        return fromXml(xmlString, XmlToToonOptions.DEFAULT);
    }

    /**
     * Converts an XML string to TOON format using custom options.
     *
     * <p>Uses the XmlToToonConverter with the specified options for attribute handling,
     * text content handling, and array detection.</p>
     *
     * @param xmlString the XML string to convert (must be valid XML)
     * @param xmlOptions the XML parsing options to use (must not be null)
     * @return the TOON representation of the XML data
     * @throws XmlParseException if XML parsing fails
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see XmlToToonConverter
     * @see XmlToToonOptions
     * @since 0.2.0
     */
    public static String fromXml(String xmlString, XmlToToonOptions xmlOptions) throws XmlParseException {
        var converter = new XmlToToonConverter(xmlOptions);
        return converter.convert(xmlString);
    }

    /**
     * Converts an XML file to TOON format using default options.
     *
     * <p>Reads the XML file content and converts it to TOON format using default configuration.</p>
     *
     * @param xmlFile the XML file to parse (must exist and be readable)
     * @return the TOON representation of the XML data
     * @throws IOException if file reading fails
     * @throws XmlParseException if XML parsing fails
     * @throws IllegalArgumentException if xmlFile is null
     *
     * @see #fromXml(String)
     * @since 0.2.0
     */
    public static String fromXml(File xmlFile) throws IOException, XmlParseException {
        return fromXml(xmlFile, XmlToToonOptions.DEFAULT);
    }

    /**
     * Converts an XML file to TOON format using custom options.
     *
     * <p>Reads the XML file content and converts it to TOON format using the specified options.</p>
     *
     * @param xmlFile the XML file to parse (must exist and be readable)
     * @param xmlOptions the XML parsing options to use (must not be null)
     * @return the TOON representation of the XML data
     * @throws IOException if file reading fails
     * @throws XmlParseException if XML parsing fails
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see #fromXml(String, XmlToToonOptions)
     * @since 0.2.0
     */
    public static String fromXml(File xmlFile, XmlToToonOptions xmlOptions) throws IOException, XmlParseException {
        var xmlString = Files.readString(xmlFile.toPath());
        return fromXml(xmlString, xmlOptions);
    }

    /**
     * Converts an XML input stream to TOON format using default options.
     *
     * <p>Reads the XML content from the stream and converts it to TOON format.</p>
     *
     * @param xmlStream the XML input stream (must not be null)
     * @return the TOON representation of the XML data
     * @throws IOException if stream reading fails
     * @throws XmlParseException if XML parsing fails
     * @throws IllegalArgumentException if xmlStream is null
     *
     * @see #fromXml(String)
     * @since 0.2.0
     */
    public static String fromXml(InputStream xmlStream) throws IOException, XmlParseException {
        return fromXml(xmlStream, XmlToToonOptions.DEFAULT);
    }

    /**
     * Converts an XML input stream to TOON format using custom options.
     *
     * <p>Reads the XML content from the stream and converts it to TOON format using the specified options.</p>
     *
     * @param xmlStream the XML input stream (must not be null)
     * @param xmlOptions the XML parsing options to use (must not be null)
     * @return the TOON representation of the XML data
     * @throws IOException if stream reading fails
     * @throws XmlParseException if XML parsing fails
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see #fromXml(String, XmlToToonOptions)
     * @since 0.2.0
     */
    public static String fromXml(InputStream xmlStream, XmlToToonOptions xmlOptions) throws IOException, XmlParseException {
        var xmlString = new String(xmlStream.readAllBytes());
        return fromXml(xmlString, xmlOptions);
    }

    /**
     * Converts a TOON string to XML format using default options.
     *
     * <p>Uses default configuration with "root" as root element name,
     * "@" as attribute prefix, "#text" as text node key, and pretty-printing enabled.</p>
     *
     * @param toonString the TOON string to convert (must not be null)
     * @return the XML representation of the TOON data
     * @throws XmlException if conversion fails
     * @throws IllegalArgumentException if toonString is null
     *
     * @see #toXml(String, ToonToXmlOptions)
     * @see ToonToXmlConverter
     * @since 0.2.0
     */
    public static String toXml(String toonString) throws XmlException {
        return toXml(toonString, ToonToXmlOptions.DEFAULT);
    }

    /**
     * Converts a TOON string to XML format using custom options.
     *
     * <p>Uses the ToonToXmlConverter with the specified options for attribute handling,
     * text content handling, root element naming, and formatting.</p>
     *
     * @param toonString the TOON string to convert (must not be null)
     * @param xmlOptions the XML generation options to use (must not be null)
     * @return the XML representation of the TOON data
     * @throws XmlException if conversion fails
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see ToonToXmlConverter
     * @see ToonToXmlOptions
     * @since 0.2.0
     */
    public static String toXml(String toonString, ToonToXmlOptions xmlOptions) throws XmlException {
        var converter = new ToonToXmlConverter(xmlOptions);
        return converter.convert(toonString);
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
