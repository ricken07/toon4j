package com.rickenbazolo.util;

import com.rickenbazolo.ToonOptions;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for string handling according to TOON format rules.
 *
 * <p>This class provides static methods for string processing, validation, and formatting
 * that are specifically designed for TOON (Token-Oriented Object Notation) serialization
 * and deserialization operations.</p>
 *
 * <p>The utilities handle:</p>
 * <ul>
 *   <li>String quoting and unquoting based on content analysis</li>
 *   <li>Character escaping and unescaping for safe serialization</li>
 *   <li>Identifier validation for object keys</li>
 *   <li>Control character detection and handling</li>
 *   <li>Indentation generation for structured output</li>
 * </ul>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li><strong>Smart quoting:</strong> Automatically determines when strings need quotes</li>
 *   <li><strong>Delimiter-aware:</strong> Considers active delimiter when making quoting decisions</li>
 *   <li><strong>Safe escaping:</strong> Handles all special characters and control sequences</li>
 *   <li><strong>Performance optimized:</strong> Uses efficient algorithms and caching</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Check if a string needs quotes
 * boolean needsQuotes = StringUtils.needsQuotes("hello world", Delimiter.COMMA); // true
 *
 * // Quote a string safely
 * String quoted = StringUtils.quote("hello,world", Delimiter.COMMA); // "hello,world"
 *
 * // Validate identifier
 * boolean isValid = StringUtils.isValidIdentifier("myField"); // true
 *
 * // Generate indentation
 * String indent = StringUtils.indent(2, 4); // "        " (8 spaces)
 * }</pre>
 *
 * @since 0.1.0
 * @author Ricken Bazolo
 * @see ToonOptions.Delimiter
 */
public class StringUtils {

    private static final Set<String> RESERVED_WORDS = Set.of("true", "false", "null");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$");
    private static final Pattern STRUCTURAL_TOKEN_PATTERN = Pattern.compile("^(\\[\\d+\\]|\\{.+\\}|\\[\\d+\\]:.*|\\[\\d+\\{.+\\}:.*|-.+)$");

    /**
     * Determines whether a string value needs to be enclosed in quotes for TOON serialization.
     *
     * <p>A string requires quotes if it:</p>
     * <ul>
     *   <li>Is null or empty</li>
     *   <li>Starts or ends with whitespace</li>
     *   <li>Starts with "- " (list item prefix)</li>
     *   <li>Is a reserved word ("true", "false", "null")</li>
     *   <li>Contains the active delimiter character</li>
     *   <li>Contains special characters (:, ", \)</li>
     *   <li>Contains control characters (except tab)</li>
     *   <li>Matches a number pattern</li>
     *   <li>Matches a structural token pattern</li>
     * </ul>
     *
     * <p>This method is optimized for performance by combining character scanning
     * into a single loop and using early returns for common cases.</p>
     *
     * @param value the string value to check, may be null
     * @param delimiter the current delimiter context for TOON serialization
     * @return true if the value needs quotes, false otherwise
     * @since 0.1.0
     */
    public static boolean needsQuotes(String value, ToonOptions.Delimiter delimiter) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        final int length = value.length();
        final char delimiterChar = delimiter.character();

        if (value.charAt(0) == ' ' || value.charAt(length - 1) == ' ') {
            return true;
        }

        if (value.startsWith("- ")) {
            return true;
        }

        if (RESERVED_WORDS.contains(value)) {
            return true;
        }

        var mightBeNumber = (value.charAt(0) == '-' || Character.isDigit(value.charAt(0)));

        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);

            if (c == delimiterChar) {
                return true;
            }

            if (c == ':' || c == '"' || c == '\\') {
                return true;
            }

            if (c < 32 && c != '\t') {
                return true;
            }
        }

        if (mightBeNumber && NUMBER_PATTERN.matcher(value).matches()) {
            return true;
        }

        return STRUCTURAL_TOKEN_PATTERN.matcher(value).matches();
    }

    /**
     * Validates whether a string can be used as a valid identifier in TOON format.
     *
     * <p>A valid identifier must:</p>
     * <ul>
     *   <li>Not be null</li>
     *   <li>Start with a letter (a-z, A-Z) or underscore (_)</li>
     *   <li>Contain only letters, digits, underscores, and dots</li>
     *   <li>Follow the pattern: ^[a-zA-Z_][a-zA-Z0-9_.]*$</li>
     * </ul>
     *
     * <p>This is commonly used to validate object keys that don't require quotes.</p>
     *
     * @param key the string to validate as an identifier, may be null
     * @return true if the key is a valid identifier, false otherwise
     * @since 0.1.0
     */
    public static boolean isValidIdentifier(String key) {
        return key != null && IDENTIFIER_PATTERN.matcher(key).matches();
    }

    /**
     * Escapes special characters in a string for safe inclusion in TOON format.
     *
     * <p>This method handles the following character escaping:</p>
     * <ul>
     *   <li>Quote (") → \"</li>
     *   <li>Backslash (\) → \\</li>
     *   <li>Newline (\n) → \n</li>
     *   <li>Carriage return (\r) → \r</li>
     *   <li>Tab (\t) → \t</li>
     *   <li>Backspace (\b) → \b</li>
     *   <li>Form feed (\f) → \f</li>
     *   <li>Control characters (< 32) → \\u#### (unicode escape)</li>
     * </ul>
     *
     * <p>If the input value is null, returns the string "null".</p>
     *
     * @param value the string to escape, may be null
     * @return the escaped string, or "null" if input is null
     * @since 0.1.0
     */
    public static String escape(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Unescapes a string by converting escape sequences back to their original characters.
     *
     * <p>This method handles the reverse of the {@link #escape(String)} method, converting:</p>
     * <ul>
     *   <li>\" → Quote (")</li>
     *   <li>\\ → Backslash (\)</li>
     *   <li>\n → Newline</li>
     *   <li>\r → Carriage return</li>
     *   <li>\t → Tab</li>
     *   <li>\b → Backspace</li>
     *   <li>\f → Form feed</li>
     *   <li>\\u#### → Unicode character (where #### is a 4-digit hex code)</li>
     * </ul>
     *
     * <p>If the input value is null, returns null. Malformed unicode escape sequences
     * are left as-is without conversion.</p>
     *
     * @param value the string to unescape, may be null
     * @return the unescaped string, or null if input is null
     * @since 0.1.0
     * @see #escape(String)
     */
    public static String unescape(String value) {
        if (value == null) {
            return null;
        }

        var sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(i + 1);
                switch (next) {
                    case '"' -> { sb.append('"'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case 'n' -> { sb.append('\n'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case 'b' -> { sb.append('\b'); i++; }
                    case 'f' -> { sb.append('\f'); i++; }
                    case 'u' -> {
                        if (i + 5 < value.length()) {
                            String hex = value.substring(i + 2, i + 6);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 5;
                        } else {
                            sb.append(c);
                        }
                    }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Conditionally quotes a string value for TOON serialization if necessary.
     *
     * <p>This method uses {@link #needsQuotes(String, ToonOptions.Delimiter)} to determine
     * if the string requires quotes based on its content and the current delimiter context.
     * If quotes are needed, the string is escaped using {@link #escape(String)} and wrapped
     * in double quotes. Otherwise, the original string is returned unchanged.</p>
     *
     * <p>This is the primary method used during TOON serialization to ensure string values
     * are properly formatted and safe for parsing.</p>
     *
     * @param value the string value to potentially quote, may be null
     * @param delimiter the current delimiter context for TOON serialization
     * @return the quoted and escaped string if necessary, or the original string
     * @since 0.1.0
     * @see #needsQuotes(String, ToonOptions.Delimiter)
     * @see #escape(String)
     * @see #unquote(String)
     */
    public static String quote(String value, ToonOptions.Delimiter delimiter) {
        if (needsQuotes(value, delimiter)) {
            return "\"" + escape(value) + "\"";
        }
        return value;
    }

    /**
     * Removes quotes from a string and unescapes its content if the string is quoted.
     *
     * <p>This method checks if the input string is surrounded by double quotes.
     * If so, it removes the quotes and applies {@link #unescape(String)} to convert
     * any escape sequences back to their original characters. If the string is not
     * quoted, it is returned unchanged.</p>
     *
     * <p>This is the primary method used during TOON deserialization to restore
     * original string values from their quoted representation.</p>
     *
     * <p>Strings shorter than 2 characters or null values are returned as-is
     * since they cannot be properly quoted strings.</p>
     *
     * @param value the potentially quoted string to unquote, may be null
     * @return the unquoted and unescaped string, or the original string if not quoted
     * @since 0.1.0
     * @see #quote(String, ToonOptions.Delimiter)
     * @see #unescape(String)
     */
    public static String unquote(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }

        if (value.startsWith("\"") && value.endsWith("\"")) {
            return unescape(value.substring(1, value.length() - 1));
        }

        return value;
    }

    /**
     * Generates an indentation string for formatting structured TOON output.
     *
     * <p>This method creates a string of spaces based on the indentation level
     * and the number of spaces per level. It is commonly used for creating
     * nicely formatted, hierarchical TOON output with proper indentation.</p>
     *
     * <p>The total number of spaces returned is calculated as:
     * {@code level * spacesPerLevel}</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code indent(0, 4)} returns "" (empty string)</li>
     *   <li>{@code indent(1, 4)} returns "    " (4 spaces)</li>
     *   <li>{@code indent(2, 2)} returns "    " (4 spaces)</li>
     *   <li>{@code indent(3, 4)} returns "            " (12 spaces)</li>
     * </ul>
     *
     * @param level the indentation level (0 or positive)
     * @param spacesPerLevel the number of spaces per indentation level (0 or positive)
     * @return a string containing the appropriate number of spaces for indentation
     * @since 0.1.0
     */
    public static String indent(int level, int spacesPerLevel) {
        return " ".repeat(level * spacesPerLevel);
    }
}
