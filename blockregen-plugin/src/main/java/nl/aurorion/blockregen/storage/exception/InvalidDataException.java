package nl.aurorion.blockregen.storage.exception;

import org.jetbrains.annotations.NotNull;

// Stored data somehow ended up invalid.
public class InvalidDataException extends Exception {
    public InvalidDataException() {
        super();
    }

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidDataException(Throwable cause) {
        super(cause);
    }

    protected InvalidDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static <T> T throwIfNull(T value) throws InvalidDataException {
        if (value == null) {
            throw new InvalidDataException();
        }

        return value;
    }

    public static <T> T throwIfNull(T value, @NotNull String message) throws InvalidDataException {
        if (value == null) {
            throw new InvalidDataException(String.format(message, value));
        }

        return value;
    }
}
