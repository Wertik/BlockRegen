package nl.aurorion.blockregen;

import lombok.Getter;

// An exception that happened while parsing a configuration value from the configuration.
public class ParseException extends RuntimeException {

    @Getter
    private boolean shouldRetry = false;

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, boolean shouldRetry) {
        super(message);
        this.shouldRetry = shouldRetry;
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseException(String message, Throwable cause, boolean shouldRetry) {
        super(message, cause);
        this.shouldRetry = shouldRetry;
    }

    public ParseException(Throwable cause) {
        super(cause);
    }

    public ParseException(Throwable cause, boolean shouldRetry) {
        super(cause);
        this.shouldRetry = shouldRetry;
    }
}
