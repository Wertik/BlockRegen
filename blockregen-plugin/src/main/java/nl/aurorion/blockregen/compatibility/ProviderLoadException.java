package nl.aurorion.blockregen.compatibility;

// Compatibility provider failed to load.
public class ProviderLoadException extends Exception {
    public ProviderLoadException(String message) {
        super(message);
    }

    public ProviderLoadException() {
        super();
    }

    public ProviderLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderLoadException(Throwable cause) {
        super(cause);
    }

    protected ProviderLoadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
