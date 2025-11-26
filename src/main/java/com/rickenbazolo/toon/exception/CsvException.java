package com.rickenbazolo.toon.exception;

/**
 * Base exception for CSV-related errors.
 *
 * <p>This exception is thrown when CSV parsing, generation, or conversion
 * operations encounter errors that cannot be recovered from.</p>
 *
 * @author Ricken Bazolo
 * @since 0.3.0
 */
public class CsvException extends RuntimeException {

    /**
     * Constructs a new CSV exception with the specified detail message.
     *
     * @param message the detail message explaining the error
     */
    public CsvException(String message) {
        super(message);
    }

    /**
     * Constructs a new CSV exception with the specified detail message and cause.
     *
     * @param message the detail message explaining the error
     * @param cause the cause of this exception
     */
    public CsvException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new CSV exception with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public CsvException(Throwable cause) {
        super(cause);
    }
}
