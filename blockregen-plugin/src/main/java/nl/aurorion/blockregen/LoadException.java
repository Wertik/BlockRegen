package nl.aurorion.blockregen;

/**
 * Something failed to load.
 */
public class LoadException extends Exception {
    public LoadException(String message) {
        super(message);
    }

    public LoadException() {
        super();
    }

    public LoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadException(Throwable cause) {
        super(cause);
    }

    protected LoadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
