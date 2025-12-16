package nl.aurorion.blockregen.storage;

// Internal storage exception. Recovery attempts can still be made.
public class RecoverableStorageException extends RuntimeException {
    public RecoverableStorageException() {
        super();
    }

    public RecoverableStorageException(String message) {
        super(message);
    }

    public RecoverableStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecoverableStorageException(Throwable cause) {
        super(cause);
    }

    protected RecoverableStorageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
