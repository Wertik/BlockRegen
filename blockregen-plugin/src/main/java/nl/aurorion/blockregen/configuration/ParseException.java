package nl.aurorion.blockregen.configuration;

// An exception that happened while parsing a configuration value from the configuration.
public class ParseException extends RuntimeException {
    public ParseException(String message) {
        super(message);
    }
}
