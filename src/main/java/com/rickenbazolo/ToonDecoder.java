package com.rickenbazolo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rickenbazolo.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decoder for converting TOON (Token-Oriented Object Notation) strings to Java objects.
 *
 * <p>This class provides functionality to parse TOON format strings and convert them
 * into Jackson JsonNode objects that can be further processed or converted to specific
 * Java types.</p>
 *
 * <p>The decoder supports:</p>
 * <ul>
 *   <li>Object parsing with key-value pairs</li>
 *   <li>Array parsing including tabular, primitive, and list formats</li>
 *   <li>Nested structures with proper indentation handling</li>
 *   <li>Primitive value parsing (strings, numbers, booleans, null)</li>
 *   <li>Configurable delimiters and options</li>
 * </ul>
 *
 * <p>The parsing process is context-aware and uses indentation levels to determine
 * the structure hierarchy. Arrays can be represented in multiple formats:</p>
 * <ul>
 *   <li><strong>Tabular:</strong> {@code [2]{id,name}: 1,Alice 2,Bob}</li>
 *   <li><strong>Primitive:</strong> {@code [3]: 1,2,3}</li>
 *   <li><strong>List:</strong> {@code [2]: - item1 - item2}</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ToonDecoder decoder = new ToonDecoder();
 * JsonNode result = decoder.decode("name: Alice\nage: 30");
 *
 * // With custom options
 * ToonOptions options = ToonOptions.builder()
 *     .delimiter(ToonOptions.Delimiter.TAB)
 *     .indent(4)
 *     .build();
 * ToonDecoder customDecoder = new ToonDecoder(options);
 * }</pre>
 *
 * @since 0.1.0
 * @author Ricken Bazolo
 * @see ToonEncoder
 * @see ToonOptions
 * @see JsonNode
 */
public class ToonDecoder {

    private final ToonOptions options;
    private final ObjectMapper objectMapper;

    private static final Pattern ARRAY_HEADER_PATTERN =
        Pattern.compile("^\\[(#)?(\\d+)([\\s|]?)\\](:)?(.*)$");
    private static final Pattern TABULAR_HEADER_PATTERN =
        Pattern.compile("^\\[(#)?(\\d+)([\\s|]?)\\]\\{([^}]+)\\}:$");
    private static final Pattern KEY_VALUE_PATTERN =
        Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_.]*|\"[^\"]+\"):\\s*(.*)$");
    private static final Pattern KEY_ARRAY_PATTERN =
        Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_.]*|\"[^\"]+\")(\\[.*)$");

    /**
     * Creates a new ToonDecoder with default options.
     *
     * <p>Uses default configuration with 2-space indentation, comma delimiter,
     * no length markers, and strict mode enabled.</p>
     *
     * @see ToonOptions#DEFAULT
     */
    public ToonDecoder() {
        this(ToonOptions.DEFAULT);
    }

    /**
     * Creates a new ToonDecoder with custom options.
     *
     * <p>The options control various aspects of parsing including indentation
     * handling, delimiter recognition, and validation strictness.</p>
     *
     * @param options the decoding options to use (must not be null)
     * @throws IllegalArgumentException if options is null
     *
     * @see ToonOptions
     */
    public ToonDecoder(ToonOptions options) {
        this.options = options;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Decodes a TOON string into a Java object (JsonNode).
     *
     * <p>Parses the input string according to TOON format rules and returns
     * a JsonNode that represents the decoded data structure. The parsing is
     * context-aware and handles nested objects and arrays with proper
     * indentation-based hierarchy.</p>
     *
     * <p>If the input is null or empty, returns an empty ObjectNode.</p>
     *
     * @param input the TOON string to decode (can be null or empty)
     * @return the decoded JsonNode representation
     * @throws ToonParseException if parsing fails due to invalid format
     * @throws RuntimeException if an unexpected error occurs during parsing
     *
     * @see JsonNode
     * @see ObjectNode
     */
    public JsonNode decode(String input) {
        if (input == null || input.trim().isEmpty()) {
            return objectMapper.createObjectNode();
        }

        var lines = Arrays.asList(input.split("\n"));
        var context = new ParseContext(lines);

        return parseValue(context, 0);
    }

    private JsonNode parseValue(ParseContext context, int baseIndent) {
        if (!context.hasMore()) {
            return objectMapper.nullNode();
        }

        var line = context.peek();
        var indent = getIndentLevel(line);

        if (indent < baseIndent) {
            return objectMapper.nullNode();
        }

        var trimmed = line.trim();

        if (trimmed.matches("^\\[.*\\]:.*")) {
            return parseArray(context, indent);
        }

        return parseObject(context, baseIndent);
    }

    /**
     * Parses a TOON object from the context starting at the specified indentation level.
     *
     * <p>This method processes key-value pairs to construct an ObjectNode. It handles:</p>
     * <ul>
     *   <li>Simple key-value pairs with inline values</li>
     *   <li>Complex nested structures with multi-line values</li>
     *   <li>Array values with key prefixes (e.g., "tags[3]: value1,value2,value3")</li>
     *   <li>Proper indentation-based hierarchy detection</li>
     * </ul>
     *
     * <p>The parsing continues until the indentation level drops below the base level
     * or no more lines are available in the context.</p>
     *
     * @param context the parsing context containing the lines to process
     * @param baseIndent the minimum indentation level for this object's content
     * @return a populated ObjectNode containing the parsed key-value pairs
     */
    private ObjectNode parseObject(ParseContext context, int baseIndent) {
        ObjectNode obj = objectMapper.createObjectNode();

        while (context.hasMore()) {
            var line = context.peek();
            var indent = getIndentLevel(line);

            if (indent < baseIndent) {
                break;
            }

            var trimmed = line.trim();

            if (trimmed.isEmpty()) {
                context.next();
                continue;
            }

            var arrayMatcher = KEY_ARRAY_PATTERN.matcher(trimmed);
            if (arrayMatcher.matches()) {
                var key = StringUtils.unquote(arrayMatcher.group(1));
                var arrayPart = arrayMatcher.group(2);

                var value = parseArrayWithPrefix(context, arrayPart, indent);
                obj.set(key, value);
                continue;
            }

            var matcher = KEY_VALUE_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                context.next();
                var key = StringUtils.unquote(matcher.group(1));
                var valuePart = matcher.group(2).trim();

                JsonNode value;
                if (valuePart.isEmpty()) {
                    value = parseValue(context, indent + 1);
                } else {
                    value = parsePrimitive(valuePart);
                }

                obj.set(key, value);
            } else {
                if (trimmed.matches("^\\[.*\\]:.*")) {
                    return obj;
                }
                context.next();
            }
        }

        return obj;
    }

    /**
     * Parses a TOON array when the array header is given as a prefix (inline with object key).
     *
     * <p>This method handles array parsing when the array declaration appears on the same line
     * as an object key, for example: "tags[3]: value1,value2,value3". It processes the array
     * header prefix to determine the array format and delegates to the appropriate parsing method.</p>
     *
     * <p>Supported array formats:</p>
     * <ul>
     *   <li><strong>Tabular:</strong> {@code key[2]{id,name}: (followed by data lines)}</li>
     *   <li><strong>Primitive inline:</strong> {@code key[3]: value1,value2,value3}</li>
     *   <li><strong>List format:</strong> {@code key[2]: (followed by - item lines)}</li>
     * </ul>
     *
     * @param context the parsing context containing the lines to process
     * @param arrayPrefix the array prefix string (e.g., "[3]:" or "[2]{id,name}:")
     * @param baseIndent the current indentation level for nested array content
     * @return the parsed JsonNode representing the array (ArrayNode)
     */
    private JsonNode parseArrayWithPrefix(ParseContext context, String arrayPrefix, int baseIndent) {
        context.next(); // Consommer la ligne courante
        var trimmed = arrayPrefix.trim();

        var tabularMatcher = TABULAR_HEADER_PATTERN.matcher(trimmed);
        if (tabularMatcher.matches()) {
            return parseTabularArray(context, tabularMatcher, baseIndent);
        }

        var arrayMatcher = ARRAY_HEADER_PATTERN.matcher(trimmed);
        if (arrayMatcher.matches()) {
            var length = Integer.parseInt(arrayMatcher.group(2));
            var delimiterIndicator = arrayMatcher.group(3);
            var valuePart = arrayMatcher.group(5).trim();

            var delimiter = determineDelimiter(delimiterIndicator);

            if (!valuePart.isEmpty()) {
                return parsePrimitiveArray(valuePart, delimiter, length);
            } else {
                return parseListArray(context, baseIndent, length);
            }
        }

        return objectMapper.createArrayNode();
    }

    /**
     * Parses a TOON array from the context at the specified indentation level.
     *
     * <p>This method handles standalone array parsing where the array declaration appears
     * on its own line. It analyzes the array header to determine the format and delegates
     * to the appropriate specialized parsing method.</p>
     *
     * <p>Supported array formats:</p>
     * <ul>
     *   <li><strong>Tabular:</strong> {@code [2]{id,name}: (followed by data rows)}</li>
     *   <li><strong>Primitive inline:</strong> {@code [3]: value1,value2,value3}</li>
     *   <li><strong>List format:</strong> {@code [2]: (followed by - item lines)}</li>
     *   <li><strong>Empty:</strong> {@code [0]:}</li>
     * </ul>
     *
     * <p>The method consumes the current line from the context and processes the array
     * content according to the detected format.</p>
     *
     * @param context the parsing context containing the lines to process
     * @param baseIndent the current indentation level for the array content
     * @return the parsed JsonNode representing the array (ArrayNode)
     */
    private JsonNode parseArray(ParseContext context, int baseIndent) {
        var line = context.next();
        var trimmed = line.trim();

        var tabularMatcher = TABULAR_HEADER_PATTERN.matcher(trimmed);
        if (tabularMatcher.matches()) {
            return parseTabularArray(context, tabularMatcher, baseIndent);
        }

        var arrayMatcher = ARRAY_HEADER_PATTERN.matcher(trimmed);
        if (arrayMatcher.matches()) {
            var length = Integer.parseInt(arrayMatcher.group(2));
            var delimiterIndicator = arrayMatcher.group(3);
            var valuePart = arrayMatcher.group(5).trim();

            var delimiter = determineDelimiter(delimiterIndicator);

            if (!valuePart.isEmpty()) {
                return parsePrimitiveArray(valuePart, delimiter, length);
            } else {
                return parseListArray(context, baseIndent, length);
            }
        }

        return objectMapper.createArrayNode();
    }

    /**
     * Parses a TOON array in tabular format where data is organized in rows and columns.
     *
     * <p>Tabular format is used for arrays of objects that share the same field structure.
     * The format declaration includes field names in curly braces, followed by data rows
     * where each row contains values in the same order as the field declaration.</p>
     *
     * <p>Example format:</p>
     * <pre>{@code
     * [2]{id,name,age}:
     *   1,Alice,25
     *   2,Bob,30
     * }</pre>
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Extracts field names from the header pattern</li>
     *   <li>Processes each data row according to the specified delimiter</li>
     *   <li>Creates objects with field names as keys and row values as values</li>
     *   <li>Validates row count and field count if strict mode is enabled</li>
     * </ul>
     *
     * @param context the parsing context containing the data lines
     * @param matcher the pattern matcher containing the tabular header information
     * @param baseIndent the current indentation level for the array content
     * @return an ArrayNode containing ObjectNodes representing the tabular data
     * @throws ToonParseException if strict mode is enabled and validation fails
     */
    private ArrayNode parseTabularArray(ParseContext context, Matcher matcher, int baseIndent) {
        var length = Integer.parseInt(matcher.group(2));
        var delimiterIndicator = matcher.group(3);
        var fieldsStr = matcher.group(4);

        var delimiter = determineDelimiter(delimiterIndicator);

        var fields = fieldsStr.split(delimiter == ToonOptions.Delimiter.COMMA ? "," :
                                         delimiter == ToonOptions.Delimiter.TAB ? "\\s+" : "\\|");

        var array = objectMapper.createArrayNode();

        for (var i = 0; i < length; i++) {
            if (!context.hasMore()) {
                if (options.strict()) {
                    throw new ToonParseException(
                        "Declared array length: " + length + ", but only " + i + " lines found"
                    );
                }
                break;
            }

            var dataLine = context.next();
            var indent = getIndentLevel(dataLine);

            if (indent < baseIndent) {
                if (options.strict()) {
                    throw new ToonParseException("Incorrect indentation in tabular array");
                }
                break;
            }

            var trimmedData = dataLine.trim();
            var values = splitByDelimiter(trimmedData, delimiter);

            if (values.length != fields.length && options.strict()) {
                throw new ToonParseException(
                    "Nombre de valeurs (" + values.length + ") ne correspond pas au nombre de champs (" + fields.length + ")"
                );
            }

            var obj = objectMapper.createObjectNode();
            for (var j = 0; j < Math.min(fields.length, values.length); j++) {
                obj.set(fields[j].trim(), parsePrimitive(values[j].trim()));
            }

            array.add(obj);
        }

        return array;
    }

    /**
     * Parses a TOON array containing primitive values in inline format.
     *
     * <p>Primitive arrays contain simple values (strings, numbers, booleans, null)
     * separated by delimiters, all on a single line. This is the most compact
     * array representation in TOON format.</p>
     *
     * <p>Example formats:</p>
     * <pre>{@code
     * [3]: Alice,Bob,Charlie        // comma-delimited
     * [3 ]: Alice Bob Charlie       // space-delimited
     * [3|]: Alice|Bob|Charlie       // pipe-delimited
     * }</pre>
     *
     * <p>This method splits the value string using the specified delimiter and
     * converts each part to a primitive JsonNode. In strict mode, it validates
     * that the actual number of values matches the declared array length.</p>
     *
     * @param valuePart the string containing all primitive values separated by delimiters
     * @param delimiter the delimiter used to separate the values
     * @param expectedLength the declared length from the array header
     * @return an ArrayNode containing the parsed primitive values
     * @throws ToonParseException if strict mode is enabled and value count doesn't match expected length
     */
    private ArrayNode parsePrimitiveArray(String valuePart, ToonOptions.Delimiter delimiter, int expectedLength) {
        var array = objectMapper.createArrayNode();
        var values = splitByDelimiter(valuePart, delimiter);

        if (options.strict() && values.length != expectedLength) {
            throw new ToonParseException(
                "Longueur de tableau déclarée: " + expectedLength + ", mais " + values.length + " valeurs trouvées"
            );
        }

        for (var value : values) {
            array.add(parsePrimitive(value.trim()));
        }

        return array;
    }

    /**
     * Parses a TOON array in list format where each element is prefixed with "- ".
     *
     * <p>List format is used for arrays containing complex or mixed-type elements
     * that cannot be efficiently represented in tabular or primitive format. Each
     * array element starts with a "- " prefix on its own line.</p>
     *
     * <p>Example formats:</p>
     * <pre>{@code
     * [2]:
     *   - Alice
     *   - Bob
     *
     * [2]:
     *   - name: Alice
     *     age: 25
     *   - name: Bob
     *     age: 30
     * }</pre>
     *
     * <p>This method handles both primitive values and complex objects as list items.
     * For object items, it parses the first key-value pair from the same line as the
     * "- " prefix, then continues parsing additional fields from subsequent indented lines.</p>
     *
     * @param context the parsing context containing the list item lines
     * @param baseIndent the current indentation level for the array content
     * @param expectedLength the declared length from the array header
     * @return an ArrayNode containing the parsed list elements
     * @throws ToonParseException if strict mode is enabled and element count doesn't match expected length
     */
    private ArrayNode parseListArray(ParseContext context, int baseIndent, int expectedLength) {
        var array = objectMapper.createArrayNode();

        while (context.hasMore() && array.size() < expectedLength) {
            var line = context.peek();
            var indent = getIndentLevel(line);

            if (indent <= baseIndent) {
                break;
            }

            var trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                context.next();
                var itemValue = trimmed.substring(2).trim();

                if (itemValue.contains(":")) {
                    var obj = objectMapper.createObjectNode();

                    var matcher = KEY_VALUE_PATTERN.matcher(itemValue);
                    if (matcher.matches()) {
                        String key = StringUtils.unquote(matcher.group(1));
                        String value = matcher.group(2).trim();
                        obj.set(key, parsePrimitive(value));
                    }

                    while (context.hasMore()) {
                        var nextLine = context.peek();
                        var nextIndent = getIndentLevel(nextLine);

                        if (nextIndent <= indent || nextLine.trim().startsWith("- ")) {
                            break;
                        }

                        context.next();
                        var nextTrimmed = nextLine.trim();
                        var nextMatcher = KEY_VALUE_PATTERN.matcher(nextTrimmed);
                        if (nextMatcher.matches()) {
                            String key = StringUtils.unquote(nextMatcher.group(1));
                            String value = nextMatcher.group(2).trim();
                            obj.set(key, parsePrimitive(value));
                        }
                    }

                    array.add(obj);
                } else {
                    array.add(parsePrimitive(itemValue));
                }
            } else {
                break;
            }
        }

        if (options.strict() && array.size() != expectedLength) {
            throw new ToonParseException(
                "Declared array length: " + expectedLength + ", but " + array.size() + " elements found"
            );
        }

        return array;
    }

    /**
     * Parses a primitive value from a string and converts it to the appropriate JsonNode type.
     *
     * <p>This method handles the conversion of string representations to their corresponding
     * JSON types based on content analysis and format detection. It supports automatic
     * type inference for common value patterns.</p>
     *
     * <p>Type conversion rules:</p>
     * <ul>
     *   <li><strong>Quoted strings:</strong> Values enclosed in quotes become string nodes</li>
     *   <li><strong>null:</strong> The literal "null" becomes a null node</li>
     *   <li><strong>Booleans:</strong> "true" and "false" become boolean nodes</li>
     *   <li><strong>Numbers:</strong> Numeric patterns become number nodes (Long or Double)</li>
     *   <li><strong>Unquoted strings:</strong> Everything else becomes a string node</li>
     * </ul>
     *
     * <p>Number detection supports integers, floating-point numbers, and scientific notation.
     * If number parsing fails, the value is treated as a string.</p>
     *
     * @param value the string value to parse, may be null or empty
     * @return a JsonNode representing the parsed primitive value (never null)
     */
    private JsonNode parsePrimitive(String value) {
        if (value == null || value.isEmpty()) {
            return objectMapper.nullNode();
        }

        var unquoted = StringUtils.unquote(value);

        if (!value.equals(unquoted)) {
            return objectMapper.valueToTree(unquoted);
        }

        switch (value) {
            case "null" -> {
                return objectMapper.nullNode();
            }
            case "true" -> {
                return objectMapper.valueToTree(true);
            }
            case "false" -> {
                return objectMapper.valueToTree(false);
            }
        }

        try {
            if (value.contains(".") || value.contains("e") || value.contains("E")) {
                return objectMapper.valueToTree(Double.parseDouble(value));
            } else {
                return objectMapper.valueToTree(Long.parseLong(value));
            }
        } catch (NumberFormatException e) {
            return objectMapper.valueToTree(value);
        }
    }

    /**
     * Determines the delimiter type based on the indicator character in the array header.
     *
     * <p>The delimiter indicator appears in array headers to specify which character
     * should be used to separate values. This method maps the indicator characters
     * to their corresponding {@link ToonOptions.Delimiter} enum values.</p>
     *
     * <p>Mapping rules:</p>
     * <ul>
     *   <li><strong>Space (" "):</strong> Maps to TAB delimiter</li>
     *   <li><strong>Pipe ("|"):</strong> Maps to PIPE delimiter</li>
     *   <li><strong>Empty or null:</strong> Defaults to COMMA delimiter</li>
     *   <li><strong>Any other value:</strong> Defaults to COMMA delimiter</li>
     * </ul>
     *
     * @param indicator the delimiter indicator character from the array header, may be null or empty
     * @return the corresponding ToonOptions.Delimiter enum value (never null)
     */
    private ToonOptions.Delimiter determineDelimiter(String indicator) {
        if (indicator == null || indicator.isEmpty()) {
            return ToonOptions.Delimiter.COMMA;
        }

        return switch (indicator) {
            case " " -> ToonOptions.Delimiter.TAB;
            case "|" -> ToonOptions.Delimiter.PIPE;
            case "," -> ToonOptions.Delimiter.COMMA;
            default -> throw new IllegalArgumentException("Unsupported delimiter indicator: " + indicator);
        };
    }

    /**
     * Splits a string by the specified delimiter while properly handling quoted segments.
     *
     * <p>This method performs delimiter-aware string splitting that respects quoted content.
     * Delimiter characters inside quoted strings are ignored, and escape sequences within
     * quotes are properly handled to prevent premature quote termination.</p>
     *
     * <p>Key features:</p>
     * <ul>
     *   <li><strong>Quote awareness:</strong> Delimiters inside quotes are preserved</li>
     *   <li><strong>Escape handling:</strong> Backslash escapes are processed correctly</li>
     *   <li><strong>Nested quotes:</strong> Properly handles escaped quotes within quoted strings</li>
     *   <li><strong>Flexible delimiters:</strong> Works with any single-character delimiter</li>
     * </ul>
     *
     * <p>Example: {@code "Alice,\"Bob,Jr\",Charlie"} with comma delimiter produces
     * {@code ["Alice", "\"Bob,Jr\"", "Charlie"]}.</p>
     *
     * @param input the input string to split, must not be null
     * @param delimiter the delimiter to use for splitting
     * @return an array of string segments (never null, at least one element)
     */
    private String[] splitByDelimiter(String input, ToonOptions.Delimiter delimiter) {
        var result = new ArrayList<>();
        var current = new StringBuilder();
        var inQuotes = false;
        var escaped = false;

        for (var i = 0; i < input.length(); i++) {
            var c = input.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                current.append(c);
                continue;
            }

            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
                continue;
            }

            if (!inQuotes && c == delimiter.character()) {
                result.add(current.toString());
                current = new StringBuilder();
                continue;
            }

            current.append(c);
        }

        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    /**
     * Calculates the indentation level of a line by counting leading spaces.
     *
     * <p>This method counts the number of leading space characters in a line
     * and divides by the configured indentation size to determine the logical
     * indentation level. This is used to understand the hierarchical structure
     * of TOON content during parsing.</p>
     *
     * <p>Only space characters are counted; tabs and other whitespace characters
     * are not considered part of the indentation. The calculation stops at the
     * first non-space character.</p>
     *
     * @param line the input line to analyze, must not be null
     * @return the indentation level (0-based, where 0 means no indentation)
     */
    private int getIndentLevel(String line) {
        var spaces = 0;
        for (var c : line.toCharArray()) {
            if (c == ' ') {
                spaces++;
            } else {
                break;
            }
        }
        return spaces / options.indent();
    }

    /**
     * Parsing context that tracks the current position within the input lines during decoding.
     *
     * <p>This utility class encapsulates the state needed for sequential line processing
     * during TOON parsing. It provides methods to peek at the current line, advance to
     * the next line, and check for remaining content.</p>
     *
     * <p>The context maintains an immutable list of lines and a mutable position pointer,
     * allowing parsers to navigate through the input in a controlled manner while
     * supporting lookahead operations.</p>
     *
     * @since 0.1.0
     */
    private static class ParseContext {
        private final List<String> lines;
        private int position;

        ParseContext(List<String> lines) {
            this.lines = lines;
            this.position = 0;
        }

        boolean hasMore() {
            return position < lines.size();
        }

        String peek() {
            return hasMore() ? lines.get(position) : "";
        }

        String next() {
            return hasMore() ? lines.get(position++) : "";
        }
    }

    /**
     * Exception thrown when TOON parsing encounters an error or invalid format.
     *
     * <p>This exception is raised during TOON decoding operations when the input
     * format is malformed, violates TOON syntax rules, or fails validation checks
     * in strict mode. It extends {@link RuntimeException} to indicate that parsing
     * errors are typically unrecoverable.</p>
     *
     * <p>Common scenarios that trigger this exception:</p>
     * <ul>
     *   <li>Invalid array length declarations vs. actual content</li>
     *   <li>Incorrect indentation in structured content</li>
     *   <li>Mismatched field counts in tabular arrays</li>
     *   <li>Malformed array headers or syntax errors</li>
     * </ul>
     *
     * @since 0.1.0
     */
    public static class ToonParseException extends RuntimeException {
        public ToonParseException(String message) {
            super(message);
        }

        public ToonParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
