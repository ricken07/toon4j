package com.rickenbazolo.toon.exception;

/**
 * Exception thrown when CSV parsing fails.
 *
 * <p>This exception provides additional context about where in the CSV
 * document the error occurred, including line and column numbers.</p>
 *
 * @author Ricken Bazolo
 * @since 0.3.0
 */
public class CsvParseException extends CsvException {

    private final long lineNumber;
    private final long columnNumber;

    /**
     * Constructs a new CSV parse exception with line and column information.
     *
     * @param message the detail message explaining the error
     * @param lineNumber the line number where the error occurred (1-based)
     * @param columnNumber the column number where the error occurred (1-based)
     */
    public CsvParseException(String message, long lineNumber, long columnNumber) {
        super(String.format("%s at line %d, column %d", message, lineNumber, columnNumber));
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Constructs a new CSV parse exception with line and column information and a cause.
     *
     * @param message the detail message explaining the error
     * @param lineNumber the line number where the error occurred (1-based)
     * @param columnNumber the column number where the error occurred (1-based)
     * @param cause the cause of this exception
     */
    public CsvParseException(String message, long lineNumber, long columnNumber, Throwable cause) {
        super(String.format("%s at line %d, column %d", message, lineNumber, columnNumber), cause);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Gets the line number where the parsing error occurred.
     *
     * @return the line number (1-based), or -1 if not available
     */
    public long getLineNumber() {
        return lineNumber;
    }

    /**
     * Gets the column number where the parsing error occurred.
     *
     * @return the column number (1-based), or -1 if not available
     */
    public long getColumnNumber() {
        return columnNumber;
    }
}
