package nl.aurorion.blockregen.storage;

import lombok.extern.java.Log;

import java.util.function.Supplier;

@Log
public class RetryService {

    public static final int DEFAULT_RETRIES = 3;

    private RetryService() {
    }

    public static <T> T run(Supplier<T> supplier) {
        return run(supplier, DEFAULT_RETRIES);
    }

    // Run with retries.
    // If a recoverable exception is thrown, retry until max tries has been reached.
    // Then throw an unrecoverable exception.
    public static <T> T run(Supplier<T> supplier, int retries) {
        int tries = 1;

        Exception lastException = null;

        while (tries <= retries) {
            try {
                return supplier.get();
            } catch (RuntimeException e) {
                log.warning("Try #" + tries + " / " + retries + ": " + e.getMessage());

                tries += 1;
                lastException = e;
            }
        }

        log.fine("Failed to run " + retries + " times. Throwing unrecoverable.");

        throw new RecoverableStorageException(lastException);
    }
}
