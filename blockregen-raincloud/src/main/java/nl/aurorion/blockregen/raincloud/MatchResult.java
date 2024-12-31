package nl.aurorion.blockregen.raincloud;

import lombok.Getter;

@Getter
public class MatchResult {
    private final Command<?> command;
    private final int length;
    private final ParseResult result;

    public MatchResult(Command<?> command, int length, ParseResult result) {
        this.command = command;
        this.length = length;
        this.result = result;
    }
}
