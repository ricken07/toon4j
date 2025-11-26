package com.rickenbazolo.toon.converter.csv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rickenbazolo.toon.core.ToonEncoder;
import com.rickenbazolo.toon.exception.CsvException;
import com.rickenbazolo.toon.exception.CsvParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

/**
 * Converts CSV documents to TOON format.
 *
 * <p>This converter parses CSV documents and transforms them into TOON format,
 * providing a more compact and LLM-friendly representation of tabular data.
 * The converter supports various CSV formats, custom delimiters, type inference,
 * and flexible header handling.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Configurable CSV parsing (delimiters, quotes, escape characters)</li>
 *   <li>Automatic type inference (numbers, booleans, dates)</li>
 *   <li>Flexible header handling (auto-detect, custom headers, no headers)</li>
 *   <li>Empty value and null handling strategies</li>
 *   <li>Support for various input sources (String, File, InputStream, Reader)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Basic conversion with default options
 * CsvToToonConverter converter = new CsvToToonConverter();
 * String toon = converter.convert("id,name,age\n1,Alice,30\n2,Bob,25");
 *
 * // Custom conversion options
 * CsvToToonOptions options = CsvToToonOptions.builder()
 *     .delimiter(';')
 *     .typeInference(true)
 *     .build();
 * CsvToToonConverter customConverter = new CsvToToonConverter(options);
 * String customToon = customConverter.convert(csvFile);
 * }</pre>
 *
 * @author Ricken Bazolo
 * @since 0.3.0
 * @see CsvToToonOptions
 * @see ToonToCsvConverter
 */
public class CsvToToonConverter {

    private final CsvToToonOptions options;
    private final ObjectMapper objectMapper;
    private final ToonEncoder encoder;

    /**
     * Creates a new converter with default options.
     */
    public CsvToToonConverter() {
        this(CsvToToonOptions.DEFAULT);
    }

    /**
     * Creates a new converter with custom options.
     *
     * @param options the conversion options to use
     * @throws IllegalArgumentException if options is null
     */
    public CsvToToonConverter(CsvToToonOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("CsvToToonOptions cannot be null");
        }
        this.options = options;
        this.objectMapper = new ObjectMapper();
        this.encoder = new ToonEncoder(options.toonOptions());
    }

    /**
     * Converts a CSV string to TOON format.
     *
     * @param csvString the CSV document as a string (must be valid CSV)
     * @return the TOON representation of the CSV data
     * @throws IOException if CSV parsing fails
     * @throws IllegalArgumentException if csvString is null
     */
    public String convert(String csvString) throws IOException {
        if (csvString == null) {
            throw new IllegalArgumentException("CSV string cannot be null");
        }
        return convert(new StringReader(csvString));
    }

    /**
     * Converts a CSV file to TOON format.
     *
     * @param csvFile the CSV file to parse (must exist and be readable)
     * @return the TOON representation of the CSV data
     * @throws IOException if file reading or CSV parsing fails
     * @throws IllegalArgumentException if csvFile is null
     */
    public String convert(File csvFile) throws IOException {
        if (csvFile == null) {
            throw new IllegalArgumentException("CSV file cannot be null");
        }
        return convert(Files.newBufferedReader(csvFile.toPath(), StandardCharsets.UTF_8));
    }

    /**
     * Converts a CSV input stream to TOON format.
     *
     * @param csvStream the CSV input stream (must not be null)
     * @return the TOON representation of the CSV data
     * @throws IOException if stream reading or CSV parsing fails
     * @throws IllegalArgumentException if csvStream is null
     */
    public String convert(InputStream csvStream) throws IOException {
        if (csvStream == null) {
            throw new IllegalArgumentException("CSV stream cannot be null");
        }
        return convert(new String(csvStream.readAllBytes(), StandardCharsets.UTF_8));
    }

    /**
     * Converts CSV data from a Reader to TOON format.
     *
     * @param csvReader the reader providing CSV data (must not be null)
     * @return the TOON representation of the CSV data
     * @throws IOException if reading or CSV parsing fails
     * @throws IllegalArgumentException if csvReader is null
     */
    public String convert(Reader csvReader) throws IOException {
        if (csvReader == null) {
            throw new IllegalArgumentException("CSV reader cannot be null");
        }

        try {
            var jsonNode = parseCsvToJsonNode(csvReader);

            // TOON format requires arrays to be fields within objects, not standalone
            // Wrap the array in an object with a "data" key
            var wrapper = objectMapper.createObjectNode();
            wrapper.set("data", jsonNode);

            return encoder.encode(wrapper);
        } catch (CsvException e) {
            throw e;
        } catch (Exception e) {
            throw new CsvException("Failed to convert CSV to TOON: " + e.getMessage(), e);
        }
    }

    /**
     * Parses CSV data into a JsonNode representation.
     *
     * @param reader the reader providing CSV data
     * @return a JsonNode (ArrayNode) containing the CSV data
     * @throws IOException if parsing fails
     */
    private JsonNode parseCsvToJsonNode(Reader reader) throws IOException {
        var csvFormat = buildCsvFormat();

        try (var parser = csvFormat.parse(reader)) {
            var headers = extractHeaders(parser);
            var arrayNode = objectMapper.createArrayNode();

            for (var record : parser) {
                if (record.size() == 0 || (record.size() == 1 && record.get(0).isEmpty())) {
                    if (options.skipEmptyLines()) {
                        continue;
                    }
                }

                var rowNode = objectMapper.createObjectNode();

                for (int i = 0; i < headers.size(); i++) {
                    var header = headers.get(i);
                    var value = i < record.size() ? record.get(i) : "";

                    if (options.trimWhitespace() && value != null) {
                        value = value.trim();
                    }

                    if (options.nullValue() != null && options.nullValue().equals(value)) {
                        rowNode.putNull(header);
                        continue;
                    }

                    if (value == null || value.isEmpty()) {
                        switch (options.emptyValueHandling()) {
                            case NULL -> rowNode.putNull(header);
                            case EMPTY_STRING -> rowNode.put(header, "");
                            case SKIP -> {} // Skip this field
                        }
                        continue;
                    }

                    var typedValue = inferType(value);
                    addValueToNode(rowNode, header, typedValue);
                }

                arrayNode.add(rowNode);
            }

            return arrayNode;

        } catch (IllegalArgumentException e) {
            throw new CsvParseException("Invalid CSV format: " + e.getMessage(), -1, -1, e);
        }
    }

    /**
     * Builds the CSVFormat configuration based on options.
     *
     * @return the configured CSVFormat
     */
    private CSVFormat buildCsvFormat() {
        var builder = CSVFormat.DEFAULT.builder()
            .setDelimiter(options.delimiter())
            .setQuote(options.quoteChar())
            .setEscape(options.escapeChar())
            .setIgnoreEmptyLines(options.skipEmptyLines())
            .setTrim(options.trimWhitespace());

        if (options.hasHeader() && options.customHeaders() == null) {
            builder.setHeader().setSkipHeaderRecord(true);
        }

        return builder.build();
    }

    /**
     * Extracts header names from the CSV parser or uses custom headers.
     *
     * @param parser the CSV parser
     * @return list of header names
     */
    private List<String> extractHeaders(CSVParser parser) {

        if (options.customHeaders() != null) {
            return new ArrayList<>(options.customHeaders());
        }

        if (options.hasHeader()) {
            var headerMap = parser.getHeaderMap();
            if (headerMap != null && !headerMap.isEmpty()) {
                var headers = new ArrayList<String>(headerMap.size());
                headerMap.entrySet().stream()
                    .sorted(Comparator.comparingInt(Entry::getValue))
                    .forEach(entry -> headers.add(entry.getKey()));
                return headers;
            }
        }

        var firstRecord = parser.iterator().hasNext() ? parser.iterator().next() : null;
        if (firstRecord == null) {
            return List.of();
        }

        var headers = new ArrayList<String>();
        for (int i = 0; i < firstRecord.size(); i++) {
            headers.add("col" + i);
        }
        return headers;
    }

    /**
     * Infers the type of a string value and returns the appropriate typed object.
     *
     * @param value the string value to type
     * @return the typed value (String, Long, Double, Boolean, or Instant)
     */
    private Object inferType(String value) {
        if (value == null || value.isEmpty() || !options.typeInference()) {
            return value;
        }

        if (isBooleanValue(value)) {
            return Boolean.parseBoolean(value);
        }

        var numericResult = tryParseNumeric(value);
        if (numericResult != null) {
            return numericResult;
        }

        if (options.parseDates()) {
            var dateResult = tryParseDate(value);
            if (dateResult != null) {
                return dateResult;
            }
        }

        // Default to string
        return value;
    }

    /**
     * Checks if a value represents a boolean.
     *
     * @param value the string value to check
     * @return true if the value is "true" or "false" (case-insensitive)
     */
    private boolean isBooleanValue(String value) {
        return value.length() <= 5 &&
               ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value));
    }

    /**
     * Attempts to parse a numeric value (Long or Double).
     *
     * @param value the string value to parse
     * @return Long, Double, or null if parsing fails
     */
    private Number tryParseNumeric(String value) {
        var firstChar = value.charAt(0);
        if (!Character.isDigit(firstChar) && firstChar != '-' && firstChar != '+') {
            return null;
        }

        if (isIntegerCandidate(value)) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                // Fall through to try double
            }
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Checks if a value looks like an integer (no decimal point or exponent).
     *
     * @param value the string value to check
     * @return true if the value could be an integer
     */
    private boolean isIntegerCandidate(String value) {
        return value.indexOf('.') == -1 &&
               value.indexOf('e') == -1 &&
               value.indexOf('E') == -1;
    }

    /**
     * Attempts to parse an ISO-8601 date string.
     *
     * @param value the string value to parse
     * @return Instant or null if parsing fails
     */
    private Instant tryParseDate(String value) {
        if (value.indexOf('-') == -1 && value.indexOf('T') == -1) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /**
     * Adds a typed value to an ObjectNode with the appropriate JsonNode type.
     *
     * @param node the ObjectNode to add to
     * @param key the field name
     * @param value the value to add
     */
    private void addValueToNode(ObjectNode node, String key, Object value) {
        if (value == null) {
            node.putNull(key);
        } else if (value instanceof String) {
            node.put(key, (String) value);
        } else if (value instanceof Long) {
            node.put(key, (Long) value);
        } else if (value instanceof Double) {
            node.put(key, (Double) value);
        } else if (value instanceof Boolean) {
            node.put(key, (Boolean) value);
        } else if (value instanceof Instant) {
            node.put(key, value.toString());
        } else {
            node.put(key, value.toString());
        }
    }

    /**
     * Returns the options used by this converter.
     *
     * @return the conversion options
     */
    public CsvToToonOptions getOptions() {
        return options;
    }
}
