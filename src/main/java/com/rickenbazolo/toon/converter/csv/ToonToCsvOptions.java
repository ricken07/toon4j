package com.rickenbazolo.toon.converter.csv;

import com.rickenbazolo.toon.config.ToonOptions;

import java.util.List;

/**
 * Configuration options for converting TOON to CSV format.
 *
 * <p>This record class encapsulates all configuration options needed to decode
 * TOON documents and convert them to CSV format. It includes options for:</p>
 * <ul>
 *   <li>CSV generation (delimiter, quote character, escape character)</li>
 *   <li>Header handling (include/exclude, custom column ordering)</li>
 *   <li>Array extraction strategies (auto-detect, explicit path)</li>
 *   <li>Nested data handling (flatten, JSON string, error)</li>
 *   <li>Null value and quoting strategies</li>
 *   <li>TOON decoding options (via ToonOptions)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Use default options
 * ToonToCsvOptions options = ToonToCsvOptions.DEFAULT;
 *
 * // Custom configuration
 * ToonToCsvOptions custom = ToonToCsvOptions.builder()
 *     .delimiter('\t')
 *     .includeHeader(true)
 *     .quoteMode(QuoteMode.ALL)
 *     .nestedDataHandling(NestedDataHandling.FLATTEN)
 *     .build();
 * }</pre>
 *
 * @param toonOptions the TOON decoding options to use
 * @param delimiter the CSV field delimiter character
 * @param quoteChar the character used to quote fields containing delimiters
 * @param escapeChar the character used to escape special characters
 * @param includeHeader whether to write a header row with column names
 * @param columnOrder explicit column ordering (null for natural order)
 * @param autoDetectArray whether to automatically find the first array in TOON
 * @param arrayPath JSON pointer path to the array to extract (e.g., "/users")
 * @param nestedDataHandling how to handle nested objects and arrays
 * @param nullValue string to write for null values
 * @param quoteMode when to quote CSV fields
 * @param lineEnding the line ending to use (\n, \r\n, or system default)
 *
 * @author Ricken Bazolo
 * @since 0.3.0
 */
public record ToonToCsvOptions(
    ToonOptions toonOptions,
    char delimiter,
    char quoteChar,
    char escapeChar,
    boolean includeHeader,
    List<String> columnOrder,
    boolean autoDetectArray,
    String arrayPath,
    NestedDataHandling nestedDataHandling,
    String nullValue,
    QuoteMode quoteMode,
    String lineEnding
) {

    /**
     * Strategy for handling nested objects and arrays in CSV output.
     */
    public enum NestedDataHandling {
        /** Convert nested structures to JSON strings */
        JSON_STRING,
        /** Flatten nested objects using dot notation (e.g., user.name) */
        FLATTEN,
        /** Throw an error if nested data is encountered */
        ERROR
    }

    /**
     * Strategy for when to quote CSV fields.
     */
    public enum QuoteMode {
        /** Only quote fields when necessary (contains delimiter, quote, or newline) */
        MINIMAL,
        /** Quote all fields */
        ALL,
        /** Quote all non-numeric fields */
        NON_NUMERIC,
        /** Never quote fields (may produce invalid CSV) */
        NONE
    }

    /**
     * Default TOON-to-CSV conversion options.
     * <ul>
     *   <li>Delimiter: comma (,)</li>
     *   <li>Quote character: double quote (")</li>
     *   <li>Escape character: backslash (\)</li>
     *   <li>Include header: true</li>
     *   <li>Auto-detect array: true</li>
     *   <li>Nested data handling: JSON_STRING</li>
     *   <li>Null value: empty string</li>
     *   <li>Quote mode: MINIMAL</li>
     *   <li>Line ending: system default</li>
     * </ul>
     */
    public static final ToonToCsvOptions DEFAULT = new ToonToCsvOptions(
        ToonOptions.DEFAULT,
        ',',
        '"',
        '\\',
        true,
        null,
        true,
        null,
        NestedDataHandling.JSON_STRING,
        "",
        QuoteMode.MINIMAL,
        System.lineSeparator()
    );

    /**
     * Compact constructor that validates the options.
     *
     * @throws IllegalArgumentException if any option is invalid
     */
    public ToonToCsvOptions {
        if (toonOptions == null) {
            throw new IllegalArgumentException("ToonOptions cannot be null");
        }
        if (nestedDataHandling == null) {
            throw new IllegalArgumentException("NestedDataHandling cannot be null");
        }
        if (quoteMode == null) {
            throw new IllegalArgumentException("QuoteMode cannot be null");
        }
        if (nullValue == null) {
            throw new IllegalArgumentException("Null value string cannot be null (use empty string for default)");
        }
        if (lineEnding == null) {
            throw new IllegalArgumentException("Line ending cannot be null");
        }
        if (columnOrder != null && columnOrder.isEmpty()) {
            throw new IllegalArgumentException("Column order cannot be empty (use null to disable)");
        }
    }

    /**
     * Builder for creating ToonToCsvOptions instances.
     */
    public static class Builder {
        private ToonOptions toonOptions = DEFAULT.toonOptions;
        private char delimiter = DEFAULT.delimiter;
        private char quoteChar = DEFAULT.quoteChar;
        private char escapeChar = DEFAULT.escapeChar;
        private boolean includeHeader = DEFAULT.includeHeader;
        private List<String> columnOrder = DEFAULT.columnOrder;
        private boolean autoDetectArray = DEFAULT.autoDetectArray;
        private String arrayPath = DEFAULT.arrayPath;
        private NestedDataHandling nestedDataHandling = DEFAULT.nestedDataHandling;
        private String nullValue = DEFAULT.nullValue;
        private QuoteMode quoteMode = DEFAULT.quoteMode;
        private String lineEnding = DEFAULT.lineEnding;

        /**
         * Sets the TOON decoding options.
         *
         * @param opt the TOON options
         * @return this builder
         */
        public Builder toonOptions(ToonOptions opt) {
            this.toonOptions = opt;
            return this;
        }

        /**
         * Sets the CSV field delimiter character.
         *
         * @param delim the delimiter character (e.g., ',', ';', '\t', '|')
         * @return this builder
         */
        public Builder delimiter(char delim) {
            this.delimiter = delim;
            return this;
        }

        /**
         * Sets the quote character for CSV fields.
         *
         * @param quote the quote character
         * @return this builder
         */
        public Builder quoteChar(char quote) {
            this.quoteChar = quote;
            return this;
        }

        /**
         * Sets the escape character for special characters.
         *
         * @param escape the escape character
         * @return this builder
         */
        public Builder escapeChar(char escape) {
            this.escapeChar = escape;
            return this;
        }

        /**
         * Sets whether to include a header row with column names.
         *
         * @param include true to include header
         * @return this builder
         */
        public Builder includeHeader(boolean include) {
            this.includeHeader = include;
            return this;
        }

        /**
         * Sets explicit column ordering for the CSV output.
         *
         * @param order list of column names in desired order, or null for natural order
         * @return this builder
         */
        public Builder columnOrder(List<String> order) {
            this.columnOrder = order;
            return this;
        }

        /**
         * Sets whether to automatically detect and extract the first array found in TOON.
         *
         * @param autoDetect true to enable auto-detection
         * @return this builder
         */
        public Builder autoDetectArray(boolean autoDetect) {
            this.autoDetectArray = autoDetect;
            return this;
        }

        /**
         * Sets the JSON pointer path to the array to extract.
         *
         * @param path JSON pointer path (e.g., "/users", "/data/items"), or null to disable
         * @return this builder
         */
        public Builder arrayPath(String path) {
            this.arrayPath = path;
            return this;
        }

        /**
         * Sets the strategy for handling nested objects and arrays.
         *
         * @param handling the nested data handling strategy
         * @return this builder
         */
        public Builder nestedDataHandling(NestedDataHandling handling) {
            this.nestedDataHandling = handling;
            return this;
        }

        /**
         * Sets the string to write for null values.
         *
         * @param nullStr the string to represent null values
         * @return this builder
         */
        public Builder nullValue(String nullStr) {
            this.nullValue = nullStr;
            return this;
        }

        /**
         * Sets when to quote CSV fields.
         *
         * @param mode the quote mode
         * @return this builder
         */
        public Builder quoteMode(QuoteMode mode) {
            this.quoteMode = mode;
            return this;
        }

        /**
         * Sets the line ending to use in the CSV output.
         *
         * @param ending the line ending ("\n", "\r\n", or system default)
         * @return this builder
         */
        public Builder lineEnding(String ending) {
            this.lineEnding = ending;
            return this;
        }

        /**
         * Builds the ToonToCsvOptions instance.
         *
         * @return the configured options
         * @throws IllegalArgumentException if any option is invalid
         */
        public ToonToCsvOptions build() {
            return new ToonToCsvOptions(
                toonOptions,
                delimiter,
                quoteChar,
                escapeChar,
                includeHeader,
                columnOrder,
                autoDetectArray,
                arrayPath,
                nestedDataHandling,
                nullValue,
                quoteMode,
                lineEnding
            );
        }
    }

    /**
     * Creates a new builder for ToonToCsvOptions.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
