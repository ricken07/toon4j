package com.rickenbazolo.toon.converter.csv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rickenbazolo.toon.core.ToonDecoder;
import com.rickenbazolo.toon.exception.CsvException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Converts TOON documents to CSV format.
 *
 * <p>This converter decodes TOON documents and transforms them into CSV format,
 * particularly useful for exporting TOON data to spreadsheet applications,
 * databases, and data analysis tools.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic array detection and extraction</li>
 *   <li>JSON pointer support for explicit array paths</li>
 *   <li>Nested data handling (flatten, JSON string, or error)</li>
 *   <li>Configurable CSV formatting (delimiters, quotes, headers)</li>
 *   <li>Custom column ordering</li>
 *   <li>Multiple output targets (String, Writer, File)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Basic conversion with default options
 * ToonToCsvConverter converter = new ToonToCsvConverter();
 * String csv = converter.convert(toonString);
 *
 * // Custom conversion options
 * ToonToCsvOptions options = ToonToCsvOptions.builder()
 *     .delimiter('\t')
 *     .includeHeader(true)
 *     .nestedDataHandling(NestedDataHandling.FLATTEN)
 *     .build();
 * ToonToCsvConverter customConverter = new ToonToCsvConverter(options);
 * customConverter.convert(toonString, outputFile);
 * }</pre>
 *
 * @author Ricken Bazolo
 * @since 0.3.0
 * @see ToonToCsvOptions
 * @see CsvToToonConverter
 */
public class ToonToCsvConverter {

    private final ToonToCsvOptions options;
    private final ToonDecoder decoder;

    /**
     * Creates a new converter with default options.
     */
    public ToonToCsvConverter() {
        this(ToonToCsvOptions.DEFAULT);
    }

    /**
     * Creates a new converter with custom options.
     *
     * @param options the conversion options to use
     * @throws IllegalArgumentException if options is null
     */
    public ToonToCsvConverter(ToonToCsvOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("ToonToCsvOptions cannot be null");
        }
        this.options = options;
        this.decoder = new ToonDecoder(options.toonOptions());
    }

    /**
     * Converts a TOON string to CSV format.
     *
     * @param toonString the TOON string to convert (must not be null)
     * @return the CSV representation of the TOON data
     * @throws IOException if conversion fails
     * @throws IllegalArgumentException if toonString is null
     */
    public String convert(String toonString) throws IOException {
        if (toonString == null) {
            throw new IllegalArgumentException("TOON string cannot be null");
        }

        var writer = new StringWriter();
        convert(toonString, writer);
        return writer.toString();
    }

    /**
     * Converts a TOON string to CSV format and writes to a file.
     *
     * @param toonString the TOON string to convert (must not be null)
     * @param outputFile the file to write CSV data to
     * @throws IOException if conversion or file writing fails
     * @throws IllegalArgumentException if any parameter is null
     */
    public void convert(String toonString, File outputFile) throws IOException {
        if (toonString == null) {
            throw new IllegalArgumentException("TOON string cannot be null");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }

        try (Writer writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
            convert(toonString, writer);
        }
    }

    /**
     * Converts a TOON string to CSV format and writes to a Writer.
     *
     * @param toonString the TOON string to convert (must not be null)
     * @param writer the writer to output CSV data to
     * @throws IOException if conversion or writing fails
     * @throws IllegalArgumentException if any parameter is null
     */
    public void convert(String toonString, Writer writer) throws IOException {
        if (toonString == null) {
            throw new IllegalArgumentException("TOON string cannot be null");
        }
        if (writer == null) {
            throw new IllegalArgumentException("Writer cannot be null");
        }

        try {

            var jsonNode = decoder.decode(toonString);

            var rows = extractTabularData(jsonNode);

            if (rows.isEmpty()) {
                return; // No data to write
            }

            var headers = determineHeaders(rows);

            var csvFormat = buildCsvFormat();
            try (var printer = new CSVPrinter(writer, csvFormat)) {

                if (options.includeHeader()) {
                    printer.printRecord(headers);
                }

                for (var row : rows) {
                    var values = new ArrayList<String>();
                    for (var header : headers) {
                        var value = row.get(header);
                        values.add(formatValue(value));
                    }
                    printer.printRecord(values);
                }
            }

        } catch (CsvException e) {
            throw e;
        } catch (Exception e) {
            throw new CsvException("Failed to convert TOON to CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts tabular data from a JsonNode.
     *
     * @param node the JsonNode to extract from
     * @return list of rows as maps
     * @throws CsvException if no array can be extracted
     */
    private List<Map<String, Object>> extractTabularData(JsonNode node) {

        if (node.isArray()) {
            return extractRowsFromArray((ArrayNode) node);
        }

        if (options.arrayPath() != null) {
            var arrayNode = node.at(options.arrayPath());
            if (arrayNode.isMissingNode()) {
                throw new CsvException("Array not found at path: " + options.arrayPath());
            }
            if (!arrayNode.isArray()) {
                throw new CsvException("Node at path is not an array: " + options.arrayPath());
            }
            return extractRowsFromArray((ArrayNode) arrayNode);
        }

        if (options.autoDetectArray() && node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                if (entry.getValue().isArray()) {
                    return extractRowsFromArray((ArrayNode) entry.getValue());
                }
            }
        }

        throw new CsvException("Cannot extract tabular data from TOON: no array found");
    }

    /**
     * Extracts rows from an ArrayNode.
     *
     * @param arrayNode the array to extract from
     * @return list of rows as maps
     */
    private List<Map<String, Object>> extractRowsFromArray(ArrayNode arrayNode) {
        var rows = new ArrayList<Map<String, Object>>();

        for (var element : arrayNode) {
            if (element.isObject()) {
                rows.add(extractRowFromObject((ObjectNode) element));
            } else {
                var row = new LinkedHashMap<String, Object>();
                row.put("value", jsonNodeToObject(element));
                rows.add(row);
            }
        }

        return rows;
    }

    /**
     * Extracts a row from an ObjectNode, handling nested data.
     *
     * @param objectNode the object to extract from
     * @return a map representing the row
     */
    private Map<String, Object> extractRowFromObject(ObjectNode objectNode) {
        var row = new LinkedHashMap<String, Object>();

        var fields = objectNode.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            var key = entry.getKey();
            var value = entry.getValue();

            if (value.isObject() || value.isArray()) {
                switch (options.nestedDataHandling()) {
                    case JSON_STRING -> row.put(key, value.toString());
                    case FLATTEN -> flattenObject(key, value, row);
                    case ERROR -> throw new CsvException(
                        "Nested data encountered at field '" + key + "' (set nestedDataHandling to JSON_STRING or FLATTEN)"
                    );
                }
            } else {
                row.put(key, jsonNodeToObject(value));
            }
        }

        return row;
    }

    /**
     * Flattens a nested object using dot notation.
     *
     * @param prefix the field name prefix
     * @param node the node to flatten
     * @param target the target map to add flattened fields to
     */
    private void flattenObject(String prefix, JsonNode node, Map<String, Object> target) {
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                var key = prefix + "." + entry.getKey();
                var value = entry.getValue();

                if (value.isObject() || value.isArray()) {
                    flattenObject(key, value, target);
                } else {
                    target.put(key, jsonNodeToObject(value));
                }
            }
        } else if (node.isArray()) {
            target.put(prefix, node.toString());
        } else {
            target.put(prefix, jsonNodeToObject(node));
        }
    }

    /**
     * Converts a JsonNode to a Java object.
     *
     * @param node the JsonNode to convert
     * @return the corresponding Java object
     */
    private Object jsonNodeToObject(JsonNode node) {
        return switch (node.getNodeType()) {
            case NULL -> null;
            case BOOLEAN -> node.asBoolean();
            case NUMBER -> convertNumericNode(node);
            case STRING -> node.asText();
            default -> node.toString();
        };
    }

    /**
     * Converts a numeric JsonNode to the appropriate Java number type.
     *
     * @param node the numeric JsonNode to convert
     * @return Long, Integer, or Double depending on the node's numeric type
     */
    private Number convertNumericNode(JsonNode node) {
        if (node.isLong()) {
            return node.asLong();
        }
        if (node.isInt()) {
            return node.asInt();
        }
        return node.asDouble(); // handles double and float
    }

    /**
     * Determines the header names for the CSV output.
     *
     * @param rows the data rows
     * @return list of header names
     */
    private List<String> determineHeaders(List<Map<String, Object>> rows) {

        if (options.columnOrder() != null) {
            return new ArrayList<>(options.columnOrder());
        }

        var headers = new LinkedHashSet<String>();
        for (var row : rows) {
            headers.addAll(row.keySet());
        }

        return new ArrayList<>(headers);
    }

    /**
     * Formats a value for CSV output.
     *
     * @param value the value to format
     * @return the formatted string
     */
    private String formatValue(Object value) {
        if (value == null) {
            return options.nullValue();
        }
        return value.toString();
    }

    /**
     * Builds the CSVFormat configuration based on options.
     *
     * @return the configured CSVFormat
     */
    private CSVFormat buildCsvFormat() {
        var apacheQuoteMode = switch (options.quoteMode()) {
            case ALL -> QuoteMode.ALL;
            case NON_NUMERIC -> QuoteMode.NON_NUMERIC;
            case MINIMAL -> QuoteMode.MINIMAL;
            case NONE -> QuoteMode.NONE;
        };

        return CSVFormat.DEFAULT.builder()
            .setDelimiter(options.delimiter())
            .setQuote(options.quoteChar())
            .setEscape(options.escapeChar())
            .setQuoteMode(apacheQuoteMode)
            .setRecordSeparator(options.lineEnding())
            .build();
    }

    /**
     * Returns the options used by this converter.
     *
     * @return the conversion options
     */
    public ToonToCsvOptions getOptions() {
        return options;
    }
}
