package nl.aurorion.blockregen.system.preset;

import java.util.Random;

public class Command {

    private static final Random random = new Random();

    private final String command;

    private final double chance;

    public Command(String command, double chance) {
        this.command = command;
        this.chance = chance;
    }

    protected boolean shouldExecute() {
        return random.nextDouble() * 100.0 <= chance;
    }

    public String getCommand() {
        return command;
    }

    public double getChance() {
        return chance;
    }

    public boolean isEmpty() {
        return this.command.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "Command{" +
                "command='" + command + '\'' +
                ", chance=" + chance +
                '}';
    }
}
