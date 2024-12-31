package nl.aurorion.blockregen.raincloud;

public enum ParseResult {
    SUCCESS,
    TOO_MANY_ARGS,
    NOT_ENOUGH_ARGS,
    // Flag not supported by this command
    INVALID_FLAG,
    INVALID_ARGUMENT;

    public boolean isSuccessful() {
        return this == SUCCESS;
    }
}
