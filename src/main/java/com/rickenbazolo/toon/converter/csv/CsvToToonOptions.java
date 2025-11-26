package com.rickenbazolo.toon.converter.csv;

import com.rickenbazolo.toon.config.ToonOptions;

import java.util.List;

/**
 * Configuration options for converting CSV to TOON format.
 *
 * <p>This record class encapsulates all configuration options needed to parse
 * CSV documents and convert them to TOON format. It includes options for:
 * </p>
 * <ul>
 *   <li>CSV parsing (delimiter, quote character, escape character)</li>
 *   <li>Header handling (auto-detect, custom headers)</li>
 *   <li>Type inference (automatic type detection from string values)</li>
 *   <li>Empty value and null handling</li>
 *   <li>TOON encoding options (via ToonOptions)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Use default options
 * CsvToToonOptions options = CsvToToonOptions.DEFAULT;
 *
 * // Custom configuration
 * CsvToToonOptions custom = CsvToToonOptions.builder()
 *     .delimiter(';')
 *     .typeInference(true)
 *     .parseDates(true)
 *     .emptyValueHandling(EmptyValueHandling.NULL)
 *     .build();
 * }</pre>
 *
 * @param toonOptions the TOON encoding options to use
 * @param delimiter the CSV field delimiter character
 * @param quoteChar the character used to quote fields containing delimiters
 * @param escapeChar the character used to escape special characters
 * @param hasHeader whether the first row should be treated as a header
 * @param customHeaders custom header names (overrides CSV headers), null to use CSV headers
 * @param typeInference whether to automatically infer types from string values
 * @param parseDates whether to attempt parsing ISO-8601 date strings
 * @param emptyValueHandling how to handle empty CSV cells
 * @param trimWhitespace whether to trim leading/trailing whitespace from values
 * @param skipEmptyLines whether to skip empty lines in the CSV
 * @param nullValue string value to interpret as null (e.g., "NULL", "N/A"), null to disable
 *
 * @author Ricken Bazolo
 * @since 0.3.0
 */
public record CsvToToonOptions(
    ToonOptions toonOptions,
    char delimiter,
    char quoteChar,
    char escapeChar,
    boolean hasHeader,
    List<String> customHeaders,
    boolean typeInference,
    boolean parseDates,
    EmptyValueHandling emptyValueHandling,
    boolean trimWhitespace,
    boolean skipEmptyLines,
    String nullValue
) {

    /**
     * Strategy for handling empty values in CSV cells.
     */
    public enum EmptyValueHandling {
        /** Empty cells are converted to empty strings ("") */
        EMPTY_STRING,
        /** Empty cells are converted to null values */
        NULL,
        /** Empty cells are skipped (field omitted from output) */
        SKIP
    }

    /**
     * Default CSV-to-TOON conversion options.
     * <ul>
     *   <li>Delimiter: comma (,)</li>
     *   <li>Quote character: double quote (")</li>
     *   <li>Escape character: backslash (\)</li>
     *   <li>Has header: true</li>
     *   <li>Type inference: enabled</li>
     *   <li>Date parsing: disabled</li>
     *   <li>Empty value handling: EMPTY_STRING</li>
     *   <li>Trim whitespace: true</li>
     *   <li>Skip empty lines: true</li>
     * </ul>
     */
    public static final CsvToToonOptions DEFAULT = new CsvToToonOptions(
        ToonOptions.DEFAULT,
        ',',
        '"',
        '\\',
        true,
        null,
        true,
        false,
        EmptyValueHandling.EMPTY_STRING,
        true,
        true,
        null
    );

    /**
     * Compact constructor that validates the options.
     *
     * @throws IllegalArgumentException if any option is invalid
     */
    public CsvToToonOptions {
        if (toonOptions == null) {
            throw new IllegalArgumentException("ToonOptions cannot be null");
        }
        if (emptyValueHandling == null) {
            throw new IllegalArgumentException("EmptyValueHandling cannot be null");
        }
        if (customHeaders != null && customHeaders.isEmpty()) {
            throw new IllegalArgumentException("Custom headers cannot be empty (use null to disable)");
        }
    }

    /**
     * Builder for creating CsvToToonOptions instances.
     */
    public static class Builder {
        private ToonOptions toonOptions = DEFAULT.toonOptions;
        private char delimiter = DEFAULT.delimiter;
        private char quoteChar = DEFAULT.quoteChar;
        private char escapeChar = DEFAULT.escapeChar;
        private boolean hasHeader = DEFAULT.hasHeader;
        private List<String> customHeaders = DEFAULT.customHeaders;
        private boolean typeInference = DEFAULT.typeInference;
        private boolean parseDates = DEFAULT.parseDates;
        private EmptyValueHandling emptyValueHandling = DEFAULT.emptyValueHandling;
        private boolean trimWhitespace = DEFAULT.trimWhitespace;
        private boolean skipEmptyLines = DEFAULT.skipEmptyLines;
        private String nullValue = DEFAULT.nullValue;

        /**
         * Sets the TOON encoding options.
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
         * Sets whether the first row should be treated as a header.
         *
         * @param hasHdr true to treat first row as header
         * @return this builder
         */
        public Builder hasHeader(boolean hasHdr) {
            this.hasHeader = hasHdr;
            return this;
        }

        /**
         * Sets custom header names, overriding any CSV headers.
         *
         * @param headers list of header names, or null to use CSV headers
         * @return this builder
         */
        public Builder customHeaders(List<String> headers) {
            this.customHeaders = headers;
            return this;
        }

        /**
         * Sets whether to automatically infer types from string values.
         *
         * @param infer true to enable type inference
         * @return this builder
         */
        public Builder typeInference(boolean infer) {
            this.typeInference = infer;
            return this;
        }

        /**
         * Sets whether to attempt parsing ISO-8601 date strings.
         *
         * @param parse true to enable date parsing
         * @return this builder
         */
        public Builder parseDates(boolean parse) {
            this.parseDates = parse;
            return this;
        }

        /**
         * Sets the strategy for handling empty CSV cells.
         *
         * @param handling the empty value handling strategy
         * @return this builder
         */
        public Builder emptyValueHandling(EmptyValueHandling handling) {
            this.emptyValueHandling = handling;
            return this;
        }

        /**
         * Sets whether to trim leading/trailing whitespace from values.
         *
         * @param trim true to trim whitespace
         * @return this builder
         */
        public Builder trimWhitespace(boolean trim) {
            this.trimWhitespace = trim;
            return this;
        }

        /**
         * Sets whether to skip empty lines in the CSV.
         *
         * @param skip true to skip empty lines
         * @return this builder
         */
        public Builder skipEmptyLines(boolean skip) {
            this.skipEmptyLines = skip;
            return this;
        }

        /**
         * Sets the string value that should be interpreted as null.
         *
         * @param nullStr the string to treat as null (e.g., "NULL", "N/A"), or null to disable
         * @return this builder
         */
        public Builder nullValue(String nullStr) {
            this.nullValue = nullStr;
            return this;
        }

        /**
         * Builds the CsvToToonOptions instance.
         *
         * @return the configured options
         * @throws IllegalArgumentException if any option is invalid
         */
        public CsvToToonOptions build() {
            return new CsvToToonOptions(
                toonOptions,
                delimiter,
                quoteChar,
                escapeChar,
                hasHeader,
                customHeaders,
                typeInference,
                parseDates,
                emptyValueHandling,
                trimWhitespace,
                skipEmptyLines,
                nullValue
            );
        }
    }

    /**
     * Creates a new builder for CsvToToonOptions.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
