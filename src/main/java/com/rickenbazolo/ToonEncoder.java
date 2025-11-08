package com.rickenbazolo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rickenbazolo.util.StringUtils;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Encoder for converting Java objects to TOON (Token-Oriented Object Notation) format.
 *
 * <p>This class provides functionality to serialize Java objects into TOON format strings.
 * The encoder handles various data types and structures, optimizing the output for token
 * efficiency while maintaining human readability.</p>
 *
 * <p>The encoder supports:</p>
 * <ul>
 *   <li>Object serialization with key-value pairs</li>
 *   <li>Array serialization in multiple optimized formats (tabular, primitive, list)</li>
 *   <li>Primitive type handling (strings, numbers, booleans, null)</li>
 *   <li>Special type normalization (Date, Instant, BigInteger)</li>
 *   <li>Configurable formatting options and delimiters</li>
 *   <li>Automatic format selection for optimal token usage</li>
 * </ul>
 *
 * <p>Array encoding is automatically optimized based on content:</p>
 * <ul>
 *   <li><strong>Tabular format:</strong> Used for arrays of objects with identical keys</li>
 *   <li><strong>Primitive format:</strong> Used for arrays of simple values</li>
 *   <li><strong>List format:</strong> Used for complex nested arrays</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ToonEncoder encoder = new ToonEncoder();
 *
 * Map<String, Object> data = Map.of(
 *     "name", "Alice",
 *     "age", 30,
 *     "skills", List.of("Java", "Python", "JavaScript")
 * );
 *
 * String toon = encoder.encode(data);
 * // Result: name: Alice\nage: 30\nskills[3]: Java,Python,JavaScript
 *
 * // With custom options
 * ToonOptions options = ToonOptions.builder()
 *     .delimiter(ToonOptions.Delimiter.PIPE)
 *     .indent(4)
 *     .build();
 * ToonEncoder customEncoder = new ToonEncoder(options);
 * }</pre>
 *
 * @since 0.1.0
 * @author Ricken Bazolo
 * @see ToonDecoder
 * @see ToonOptions
 * @see JsonNode
 */
public class ToonEncoder {

    private final ToonOptions options;
    private final ObjectMapper objectMapper;

    public ToonEncoder() {
        this(ToonOptions.DEFAULT);
    }

    public ToonEncoder(ToonOptions options) {
        this.options = options;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Encodes a Java object into a TOON format string.
     *
     * <p>This is the main entry point for TOON encoding. It converts any Java object
     * to its TOON string representation by first normalizing the object to a JsonNode
     * and then applying TOON formatting rules based on the configured options.</p>
     *
     * <p>The encoding process:</p>
     * <ul>
     *   <li>Normalizes special types (Date, Instant, BigInteger) to standard representations</li>
     *   <li>Converts the object to an intermediate JsonNode structure</li>
     *   <li>Applies TOON formatting with appropriate indentation and delimiters</li>
     *   <li>Optimizes array representations for token efficiency</li>
     * </ul>
     *
     * <p>Null values are encoded as empty objects, and the method handles all
     * standard Java types including collections, maps, primitives, and custom objects.</p>
     *
     * @param value the Java object to encode, may be null
     * @return the TOON string representation of the object (never null)
     * @throws RuntimeException if encoding fails due to unsupported object structure
     */
    public String encode(Object value) {
        var node = normalizeValue(value);
        var sb = new StringBuilder();
        encodeValue(node, sb, 0);
        return sb.toString();
    }

    /**
     * Normalizes Java objects to JsonNode representations for consistent TOON encoding.
     *
     * <p>This method converts various Java types to their JsonNode equivalents while applying
     * special handling for certain types to ensure proper TOON format compatibility:</p>
     * <ul>
     *   <li><strong>Date objects:</strong> Converted to ISO-8601 instant strings</li>
     *   <li><strong>Instant objects:</strong> Converted to ISO-8601 strings</li>
     *   <li><strong>BigInteger:</strong> Converted to long if within range, otherwise to string</li>
     *   <li><strong>Special float values:</strong> NaN and Infinite values become null</li>
     *   <li><strong>Null values:</strong> Converted to JsonNode null representation</li>
     * </ul>
     *
     * <p>All other objects are converted using Jackson's standard object mapping.</p>
     *
     * @param value the Java object to normalize (can be null)
     * @return a JsonNode representation suitable for TOON encoding (never null)
     */
    private JsonNode normalizeValue(Object value) {
        if (value == null) {
            return objectMapper.nullNode();
        }

        if (value instanceof Date date) {
            return objectMapper.valueToTree(date.toInstant().toString());
        }

        if (value instanceof Instant instant) {
            return objectMapper.valueToTree(instant.toString());
        }

        if (value instanceof BigInteger bigInt) {
            if (bigInt.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0
                && bigInt.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0) {
                return objectMapper.valueToTree(bigInt.longValue());
            }
            return objectMapper.valueToTree(bigInt.toString());
        }

        if (value instanceof Double d) {
            if (d.isNaN() || d.isInfinite()) {
                return objectMapper.nullNode();
            }
        }

        if (value instanceof Float f) {
            if (f.isNaN() || f.isInfinite()) {
                return objectMapper.nullNode();
            }
        }

        return objectMapper.valueToTree(value);
    }

    /**
     * Encodes a JsonNode value into TOON format by delegating to the appropriate encoding method.
     *
     * <p>This method serves as the central dispatcher for encoding different types of JsonNode values.
     * It examines the node type and delegates to specialized encoding methods:</p>
     * <ul>
     *   <li><strong>Null nodes:</strong> Encoded as "null" literal</li>
     *   <li><strong>Object nodes:</strong> Delegated to {@link #encodeObject(ObjectNode, StringBuilder, int)}</li>
     *   <li><strong>Array nodes:</strong> Delegated to {@link #encodeArray(ArrayNode, StringBuilder, int)}</li>
     *   <li><strong>Primitive nodes:</strong> Delegated to {@link #encodePrimitive(JsonNode, StringBuilder)}</li>
     * </ul>
     *
     * @param node the JsonNode to encode (can be null)
     * @param sb the StringBuilder to append the encoded content to
     * @param level the current indentation level for nested structures
     */
    private void encodeValue(JsonNode node, StringBuilder sb, int level) {
        if (node == null || node.isNull()) {
            sb.append("null");
        } else if (node.isObject()) {
            encodeObject((ObjectNode) node, sb, level);
        } else if (node.isArray()) {
            encodeArray((ArrayNode) node, sb, level);
        } else {
            encodePrimitive(node, sb);
        }
    }

    /**
     * Encodes an ObjectNode into TOON object format with proper indentation and key-value pairs.
     *
     * <p>This method processes each field in the ObjectNode and formats them according to TOON
     * object syntax rules. It handles different value types appropriately:</p>
     * <ul>
     *   <li><strong>Empty objects:</strong> No output is generated</li>
     *   <li><strong>Nested objects:</strong> Formatted with increased indentation on new lines</li>
     *   <li><strong>Array values:</strong> Processed inline with the key using {@link #encodeArrayInline}</li>
     *   <li><strong>Primitive values:</strong> Formatted inline after the key with ": " separator</li>
     * </ul>
     *
     * <p>Keys are quoted only if they contain special characters or are not valid identifiers.
     * Multiple fields are separated by newlines with appropriate indentation.</p>
     *
     * @param obj the ObjectNode to encode
     * @param sb the StringBuilder to append the encoded content to
     * @param level the current indentation level for this object
     */
    private void encodeObject(ObjectNode obj, StringBuilder sb, int level) {
        if (obj.isEmpty()) {
            return;
        }

        var fields = obj.fields();
        var first = true;

        while (fields.hasNext()) {
            var entry = fields.next();
            var key = entry.getKey();
            var value = entry.getValue();

            if (!first) {
                sb.append("\n");
            }
            first = false;

            sb.append(StringUtils.indent(level, options.indent()));

            if (StringUtils.isValidIdentifier(key)) {
                sb.append(key);
            } else {
                sb.append("\"").append(StringUtils.escape(key)).append("\"");
            }

            if (value.isObject() && !value.isEmpty()) {
                sb.append(":\n");
                encodeValue(value, sb, level + 1);
            } else if (value.isArray()) {
                encodeArrayInline((ArrayNode) value, sb, level);
            } else {
                sb.append(": ");
                encodePrimitive(value, sb);
            }
        }
    }

    /**
     * Encodes an ArrayNode inline as part of an object key-value pair.
     *
     * <p>This method determines the optimal array format for inline encoding based on the array's
     * content and structure. It automatically selects from three possible formats:</p>
     * <ul>
     *   <li><strong>Tabular format:</strong> For arrays of objects with identical keys</li>
     *   <li><strong>Primitive format:</strong> For arrays containing only primitive values</li>
     *   <li><strong>List format:</strong> For complex arrays with mixed or nested content</li>
     * </ul>
     *
     * <p>Empty arrays are encoded with just their length marker and a colon.</p>
     *
     * @param array the ArrayNode to encode inline
     * @param sb the StringBuilder to append the encoded content to
     * @param level the current indentation level for nested content
     */
    private void encodeArrayInline(ArrayNode array, StringBuilder sb, int level) {
        var length = array.size();

        if (length == 0) {
            sb.append(formatArrayLength(0));
            sb.append(":");
            return;
        }

        if (canUseTabularFormat(array)) {
            encodeTabularArrayInline(array, sb, level);
        } else if (areAllPrimitives(array)) {
            encodePrimitiveArrayInline(array, sb);
        } else {
            encodeListArrayInline(array, sb, level);
        }
    }

    /**
     * Encodes an ArrayNode as a standalone array with proper indentation and format selection.
     *
     * <p>This method handles standalone array encoding (not inline with object keys) and
     * automatically determines the most efficient format based on the array's content:</p>
     * <ul>
     *   <li><strong>Tabular format:</strong> Objects with identical keys are encoded in table format</li>
     *   <li><strong>Primitive format:</strong> Simple values are encoded as comma/delimiter-separated lists</li>
     *   <li><strong>List format:</strong> Complex structures use bullet-point list format</li>
     * </ul>
     *
     * <p>Empty arrays are encoded with appropriate indentation, length marker, and colon.</p>
     *
     * @param array the ArrayNode to encode
     * @param sb the StringBuilder to append the encoded content to
     * @param level the current indentation level for this array
     */
    private void encodeArray(ArrayNode array, StringBuilder sb, int level) {
        var length = array.size();

        if (length == 0) {
            sb.append(StringUtils.indent(level, options.indent()));
            sb.append(formatArrayLength(0));
            sb.append(":");
            return;
        }

        if (canUseTabularFormat(array)) {
            encodeTabularArray(array, sb, level);
        } else if (areAllPrimitives(array)) {
            encodePrimitiveArray(array, sb, level);
        } else {
            encodeListArray(array, sb, level);
        }
    }

    private boolean canUseTabularFormat(ArrayNode array) {
        if (array.isEmpty()) {
            return false;
        }

        for (var node : array) {
            if (!node.isObject()) {
                return false;
            }
        }

        var firstKeys = getObjectKeys((ObjectNode) array.get(0));
        for (var i = 1; i < array.size(); i++) {
            var keys = getObjectKeys((ObjectNode) array.get(i));
            if (!firstKeys.equals(keys)) {
                return false;
            }
        }

        for (var node : array) {
            var obj = (ObjectNode) node;
            var values = obj.elements();
            while (values.hasNext()) {
                var value = values.next();
                if (value.isObject() || value.isArray()) {
                    return false;
                }
            }
        }

        return true;
    }

    private void encodeTabularArray(ArrayNode array, StringBuilder sb, int level) {
        sb.append(StringUtils.indent(level, options.indent()));
        sb.append(formatArrayLength(array.size()));

        var first = (ObjectNode) array.get(0);
        var fields = new ArrayList<String>();
        first.fields().forEachRemaining(entry -> fields.add(entry.getKey()));
        Collections.sort(fields);

        sb.append("{");
        var delimiter = options.delimiter() == ToonOptions.Delimiter.COMMA ? "," :
                          options.delimiter() == ToonOptions.Delimiter.TAB ? " " : "|";
        sb.append(String.join(delimiter, fields));
        sb.append("}:\n");

        for (var node : array) {
            sb.append(StringUtils.indent(level, options.indent()));
            ObjectNode obj = (ObjectNode) node;

            var values = new ArrayList<String>();
            for (var field : fields) {
                var value = obj.get(field);
                values.add(formatPrimitiveValue(value));
            }

            sb.append(String.join(options.delimiter().string(), values));
            sb.append("\n");
        }

        if (sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
    }

    /**
     * Determines if all elements in an array are primitive values.
     *
     * <p>This method checks whether an array contains only primitive JsonNode types
     * (strings, numbers, booleans, null) and no complex structures (objects or arrays).
     * Arrays that pass this test can be encoded using the primitive array format with
     * delimiter-separated values.</p>
     *
     * <p>Primitive types include:</p>
     * <ul>
     *   <li>Text nodes (strings)</li>
     *   <li>Numeric nodes (integers, floats)</li>
     *   <li>Boolean nodes</li>
     *   <li>Null nodes</li>
     * </ul>
     *
     * @param array the ArrayNode to evaluate
     * @return true if all elements are primitive values, false if any element is an object or array
     */
    private boolean areAllPrimitives(ArrayNode array) {
        for (var node : array) {
            if (node.isObject() || node.isArray()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Encodes an array of primitive values using delimiter-separated format.
     *
     * <p>This method handles arrays containing only primitive values (strings, numbers, booleans, null)
     * by formatting them as a single line with the configured delimiter. The output format is:</p>
     * <pre>
     * [length]: value1,value2,value3
     * </pre>
     *
     * <p>Each value is formatted according to its type:</p>
     * <ul>
     *   <li><strong>Strings:</strong> Quoted if they contain delimiter characters or special characters</li>
     *   <li><strong>Numbers:</strong> Formatted as integers or doubles based on type</li>
     *   <li><strong>Booleans:</strong> Formatted as "true" or "false"</li>
     *   <li><strong>Null:</strong> Formatted as "null"</li>
     * </ul>
     *
     * @param array the ArrayNode containing primitive values to encode
     * @param sb the StringBuilder to append the encoded content to
     * @param level the current indentation level
     */
    private void encodePrimitiveArray(ArrayNode array, StringBuilder sb, int level) {
        sb.append(StringUtils.indent(level, options.indent()));
        sb.append(formatArrayLength(array.size()));
        sb.append(": ");

        var values = StreamSupport.stream(array.spliterator(), false)
            .map(this::formatPrimitiveValue)
            .toList();

        sb.append(String.join(options.delimiter().string(), values));
    }

    /**
     * Encodes an array using the list format with bullet points for complex structures.
     *
     * <p>The list format is used for arrays that contain complex nested structures or mixed content
     * that cannot be efficiently represented in tabular or primitive formats. Each array element
     * is prefixed with a bullet point ("- ") and formatted according to its type:</p>
     * <ul>
     *   <li><strong>Objects:</strong> Key-value pairs with proper indentation</li>
     *   <li><strong>Arrays:</strong> Nested arrays with length markers</li>
     *   <li><strong>Primitives:</strong> Simple values directly after the bullet point</li>
     * </ul>
     *
     * <p>Example output:</p>
     * <pre>
     * [2]:
     * - name: Alice
     *   age: 30
     * - name: Bob
     *   age: 25
     * </pre>
     *
     * @param array the ArrayNode to encode in list format
     * @param sb the StringBuilder to append the encoded content to
     * @param level the current indentation level
     */
    private void encodeListArray(ArrayNode array, StringBuilder sb, int level) {
        sb.append(StringUtils.indent(level, options.indent()));
        sb.append(formatArrayLength(array.size()));
        sb.append(":\n");

        for (var node : array) {
            sb.append(StringUtils.indent(level, options.indent()));
            sb.append("- ");

            if (node.isObject() && !node.isEmpty()) {
                var obj = (ObjectNode) node;
                var fields = obj.fields();
                var first = true;

                while (fields.hasNext()) {
                    var entry = fields.next();
                    if (!first) {
                        sb.append("\n");
                        sb.append(StringUtils.indent(level + 1, options.indent()));
                    }
                    first = false;

                    var key = entry.getKey();
                    var value = entry.getValue();

                    if (StringUtils.isValidIdentifier(key)) {
                        sb.append(key);
                    } else {
                        sb.append("\"").append(StringUtils.escape(key)).append("\"");
                    }

                    sb.append(": ");
                    encodePrimitive(value, sb);
                }
            } else if (node.isArray()) {
                sb.append(formatArrayLength(node.size()));
                sb.append(": ");
                if (areAllPrimitives((ArrayNode) node)) {
                    var values = StreamSupport.stream(node.spliterator(), false)
                        .map(this::formatPrimitiveValue)
                        .toList();
                    sb.append(String.join(options.delimiter().string(), values));
                }
            } else {
                encodePrimitive(node, sb);
            }

            sb.append("\n");
        }

        if (sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
    }

    /**
     * Encodes an array of primitive values inline without indentation.
     *
     * <p>This is the inline variant of {@link #encodePrimitiveArray} used when encoding arrays
     * as part of object key-value pairs. The format is identical to the standalone version
     * but without leading indentation:</p>
     * <pre>
     * [length]: value1,value2,value3
     * </pre>
     *
     * <p>Each primitive value is formatted using the same rules as the standalone version,
     * with proper quoting and delimiter handling based on the configured options.</p>
     *
     * @param array the ArrayNode containing primitive values to encode
     * @param sb the StringBuilder to append the encoded content to
     */
    private void encodePrimitiveArrayInline(ArrayNode array, StringBuilder sb) {
        sb.append(formatArrayLength(array.size()));
        sb.append(": ");

        var values = StreamSupport.stream(array.spliterator(), false)
            .map(this::formatPrimitiveValue)
            .toList();

        sb.append(String.join(options.delimiter().string(), values));
    }

    /**
     * Encodes an array using the tabular format inline as part of an object key-value pair.
     *
     * <p>This is the inline variant of {@link #encodeTabularArray} used when encoding tabular
     * arrays as object values. The format is identical to the standalone version but integrates
     * seamlessly with object key encoding:</p>
     * <pre>
     * users[2]{name,age}:
     * Alice,30
     * Bob,25
     * </pre>
     *
     * <p>The method handles the same tabular format structure with header and data rows,
     * maintaining proper indentation for the data rows based on the current level.</p>
     *
     * @param array the ArrayNode to encode in tabular format
     * @param sb the StringBuilder to append the encoded content to
     * @param level the current indentation level for data rows
     */
    private void encodeTabularArrayInline(ArrayNode array, StringBuilder sb, int level) {
        sb.append(formatArrayLength(array.size()));

        var first = (ObjectNode) array.get(0);
        var fields = new ArrayList<String>();
        first.fields().forEachRemaining(entry -> fields.add(entry.getKey()));
        Collections.sort(fields);

        sb.append("{");
        var delimiter = options.delimiter() == ToonOptions.Delimiter.COMMA ? "," :
                          options.delimiter() == ToonOptions.Delimiter.TAB ? " " : "|";
        sb.append(String.join(delimiter, fields));
        sb.append("}:\n");

        for (var node : array) {
            sb.append(StringUtils.indent(level, options.indent()));
            ObjectNode obj = (ObjectNode) node;

            var values = new ArrayList<String>();
            for (var field : fields) {
                var value = obj.get(field);
                values.add(formatPrimitiveValue(value));
            }

            sb.append(String.join(options.delimiter().string(), values));
            sb.append("\n");
        }

        if (sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
    }

    /**
     * Encodes an array using the list format inline as part of an object key-value pair.
     *
     * <p>This is the inline variant of {@link #encodeListArray} used when encoding list-format
     * arrays as object values. The format uses bullet points for each element with proper
     * indentation handling:</p>
     * <pre>
     * items[2]:
     * - name: Alice
     *   age: 30
     * - name: Bob
     *   age: 25
     * </pre>
     *
     * <p>Each array element is prefixed with "- " and complex objects are formatted with
     * appropriate indentation. The method handles the same element types as the standalone
     * version but integrates with object key encoding.</p>
     *
     * @param array the ArrayNode to encode in list format
     * @param sb the StringBuilder to append the encoded content to
     * @param level the current indentation level for list items
     */
    private void encodeListArrayInline(ArrayNode array, StringBuilder sb, int level) {
        sb.append(formatArrayLength(array.size()));
        sb.append(":\n");

        for (var node : array) {
            sb.append(StringUtils.indent(level, options.indent()));
            sb.append("- ");

            if (node.isObject() && !node.isEmpty()) {
                var obj = (ObjectNode) node;
                var fields = obj.fields();
                var first = true;

                while (fields.hasNext()) {
                    var entry = fields.next();

                    if (!first) {
                        sb.append("\n");
                        sb.append(StringUtils.indent(level + 1, options.indent()));
                    }
                    first = false;

                    var key = entry.getKey();
                    var value = entry.getValue();

                    if (StringUtils.isValidIdentifier(key)) {
                        sb.append(key);
                    } else {
                        sb.append("\"").append(StringUtils.escape(key)).append("\"");
                    }

                    sb.append(": ");
                    encodePrimitive(value, sb);
                }
            } else if (node.isArray()) {
                sb.append(formatArrayLength(node.size()));
                sb.append(": ");
                if (areAllPrimitives((ArrayNode) node)) {
                    var values = StreamSupport.stream(node.spliterator(), false)
                        .map(this::formatPrimitiveValue)
                        .toList();
                    sb.append(String.join(options.delimiter().string(), values));
                }
            } else {
                encodePrimitive(node, sb);
            }

            sb.append("\n");
        }

        if (sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
    }

    /**
     * Encodes a primitive JsonNode value into its TOON string representation.
     *
     * <p>This method handles the encoding of primitive value types according to TOON format rules:</p>
     * <ul>
     *   <li><strong>Null values:</strong> Encoded as the literal "null"</li>
     *   <li><strong>Boolean values:</strong> Encoded as "true" or "false"</li>
     *   <li><strong>Integer numbers:</strong> Encoded as long values without decimal points</li>
     *   <li><strong>Floating-point numbers:</strong> Encoded as double values with decimal points</li>
     *   <li><strong>Text values:</strong> Quoted if necessary based on delimiter conflicts</li>
     * </ul>
     *
     * <p>String quoting is handled intelligently - values are only quoted when they contain
     * the configured delimiter character or other special characters that would cause parsing ambiguity.</p>
     *
     * @param node the JsonNode representing a primitive value to encode
     * @param sb the StringBuilder to append the encoded value to
     */
    private void encodePrimitive(JsonNode node, StringBuilder sb) {
        if (node.isNull()) {
            sb.append("null");
        } else if (node.isBoolean()) {
            sb.append(node.asBoolean());
        } else if (node.isNumber()) {
            if (node.isIntegralNumber()) {
                sb.append(node.asLong());
            } else {
                sb.append(node.asDouble());
            }
        } else if (node.isTextual()) {
            var value = node.asText();
            sb.append(StringUtils.quote(value, options.delimiter()));
        }
    }

    /**
     * Formats a primitive JsonNode value as a string for use in delimiter-separated contexts.
     *
     * <p>This method is similar to {@link #encodePrimitive} but returns a string instead of
     * appending to a StringBuilder. It's primarily used when building lists of values for
     * primitive arrays and tabular data rows. The formatting rules are identical:</p>
     * <ul>
     *   <li><strong>Null values:</strong> Returned as "null"</li>
     *   <li><strong>Boolean values:</strong> Returned as "true" or "false"</li>
     *   <li><strong>Integer numbers:</strong> Returned as string representation of long value</li>
     *   <li><strong>Floating-point numbers:</strong> Returned as string representation of double value</li>
     *   <li><strong>Text values:</strong> Quoted if necessary based on delimiter conflicts</li>
     * </ul>
     *
     * <p>If the node is not recognized as a valid primitive type, "null" is returned as a fallback.</p>
     *
     * @param node the JsonNode representing a primitive value to format
     * @return the formatted string representation of the primitive value
     */
    private String formatPrimitiveValue(JsonNode node) {
        if (node.isNull()) {
            return "null";
        } else if (node.isBoolean()) {
            return String.valueOf(node.asBoolean());
        } else if (node.isNumber()) {
            if (node.isIntegralNumber()) {
                return String.valueOf(node.asLong());
            } else {
                return String.valueOf(node.asDouble());
            }
        } else if (node.isTextual()) {
            return StringUtils.quote(node.asText(), options.delimiter());
        }
        return "null";
    }

    /**
     * Formats an array length marker according to the configured TOON options.
     *
     * <p>This method generates the array length marker that appears before array content
     * in TOON format. The marker format varies based on configuration options:</p>
     * <ul>
     *   <li><strong>Length marker:</strong> Optional "#" prefix when length markers are enabled</li>
     *   <li><strong>Delimiter suffix:</strong> Added based on the configured delimiter:
     *       <ul>
     *         <li>TAB delimiter: adds a space suffix</li>
     *         <li>PIPE delimiter: adds a "|" suffix</li>
     *         <li>COMMA delimiter: no suffix</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * <p>Examples of formatted output:</p>
     * <ul>
     *   <li>Default: "[3]"</li>
     *   <li>With length marker: "[#3]"</li>
     *   <li>With pipe delimiter: "[3|]"</li>
     *   <li>With tab delimiter: "[3 ]"</li>
     * </ul>
     *
     * @param length the array length to format
     * @return the formatted array length marker string
     */
    private String formatArrayLength(int length) {
        var marker = options.lengthMarker() ? "#" : "";
        var delimiterSuffix = "";

        if (options.delimiter() == ToonOptions.Delimiter.TAB) {
            delimiterSuffix = " ";
        } else if (options.delimiter() == ToonOptions.Delimiter.PIPE) {
            delimiterSuffix = "|";
        }

        return "[" + marker + length + delimiterSuffix + "]";
    }

    /**
     * Extracts the field names from an ObjectNode as an ordered set.
     *
     * <p>This utility method retrieves all field names (keys) from an ObjectNode and returns
     * them as a LinkedHashSet to preserve insertion order. This is used primarily in tabular
     * format validation to ensure that all objects in an array have identical key structures.</p>
     *
     * <p>The LinkedHashSet maintains the original field order from the ObjectNode, which is
     * important for consistent tabular format output and reliable comparison between objects.</p>
     *
     * @param obj the ObjectNode to extract keys from
     * @return a LinkedHashSet containing all field names in insertion order
     */
    private Set<String> getObjectKeys(ObjectNode obj) {
        var keys = new LinkedHashSet<String>();
        obj.fieldNames().forEachRemaining(keys::add);
        return keys;
    }
}
