package com.rickenbazolo;

/**
 * Configuration options for TOON encoding and decoding operations.
 *
 * <p>This record class encapsulates all configurable parameters that control
 * how TOON format serialization and deserialization behaves. It provides
 * fine-grained control over formatting, delimiters, validation, and structure.</p>
 *
 * <p>The options include:</p>
 * <ul>
 *   <li><strong>indent:</strong> Number of spaces per indentation level</li>
 *   <li><strong>delimiter:</strong> Character used to separate array elements and tabular data</li>
 *   <li><strong>lengthMarker:</strong> Whether to include length markers in array headers</li>
 *   <li><strong>strict:</strong> Whether to enable strict validation during parsing</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Using default options
 * ToonOptions defaultOptions = ToonOptions.DEFAULT;
 *
 * // Creating custom options with builder
 * ToonOptions customOptions = ToonOptions.builder()
 *     .indent(4)
 *     .delimiter(ToonOptions.Delimiter.TAB)
 *     .lengthMarker(true)
 *     .strict(false)
 *     .build();
 *
 * // Using in encoder/decoder
 * ToonEncoder encoder = new ToonEncoder(customOptions);
 * ToonDecoder decoder = new ToonDecoder(customOptions);
 * }</pre>
 *
 * @param indent the number of spaces per indentation level (must be >= 0)
 * @param delimiter the delimiter to use for separating values (must not be null)
 * @param lengthMarker whether to include length markers in array headers
 * @param strict whether to enable strict validation during parsing
 *
 * @since 0.1.0
 * @author Ricken Bazolo
 * @see ToonEncoder
 * @see ToonDecoder
 * @see Delimiter
 */
public record ToonOptions(
    int indent,
    Delimiter delimiter,
    boolean lengthMarker,
    boolean strict
) {

    /**
     * Supported delimiters for array values and tabular data rows.
     *
     * <p>Delimiters are used to separate individual values within arrays and
     * to separate columns in tabular array representations. The choice of
     * delimiter can affect both readability and token efficiency.</p>
     *
     * <p>Available delimiters:</p>
     * <ul>
     *   <li><strong>COMMA:</strong> Most common, good balance of readability and efficiency</li>
     *   <li><strong>TAB:</strong> Excellent for tabular data, good token efficiency</li>
     *   <li><strong>PIPE:</strong> High readability, useful when data contains commas</li>
     * </ul>
     *
     * @since 0.1.0
     * @author Ricken Bazolo
     */
    public enum Delimiter {
        COMMA(',', ","),
        TAB('\t', "\t"),
        PIPE('|', "|");

        private final char character;
        private final String string;

        Delimiter(char character, String string) {
            this.character = character;
            this.string = string;
        }

        public char character() {
            return character;
        }

        public String string() {
            return string;
        }

        public static Delimiter fromChar(char c) {
            return switch (c) {
                case ',' -> COMMA;
                case '\t' -> TAB;
                case '|' -> PIPE;
                default -> throw new IllegalArgumentException("Unsupported delimiter: " + c);
            };
        }
    }

    public static final ToonOptions DEFAULT = new ToonOptions(2, Delimiter.COMMA, false, true);

    public ToonOptions {
        if (indent < 0) {
            throw new IllegalArgumentException("Indentation must be >= 0");
        }
        if (delimiter == null) {
            throw new IllegalArgumentException("Delimiter cannot be null");
        }
    }

    public static class Builder {
        private int indent = DEFAULT.indent;
        private Delimiter delimiter = DEFAULT.delimiter;
        private boolean lengthMarker = DEFAULT.lengthMarker;
        private boolean strict = DEFAULT.strict;

        public Builder indent(int indent) {
            this.indent = indent;
            return this;
        }

        public Builder delimiter(Delimiter delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public Builder lengthMarker(boolean lengthMarker) {
            this.lengthMarker = lengthMarker;
            return this;
        }

        public Builder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        public ToonOptions build() {
            return new ToonOptions(indent, delimiter, lengthMarker, strict);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
