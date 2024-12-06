package nl.aurorion.blockregen.raincloud;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public class ParseException extends RuntimeException {

    private final String key;
    private final String value;

    public ParseException(@Nullable String key, String value, String message) {
        super(message);
        this.key = key;
        this.value = value;
    }

    public ParseException(Throwable underlying, @Nullable String key, String value,  String message) {
        super(message, underlying);
        this.key = key;
        this.value = value;
    }
}
