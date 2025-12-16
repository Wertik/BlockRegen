package nl.aurorion.blockregen.storage;

// As in -- the storage failed and cannot do anything about it anymore.
// The caller has to figure it out.
public class StorageException extends Exception {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(Throwable cause) {
        super(cause);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
