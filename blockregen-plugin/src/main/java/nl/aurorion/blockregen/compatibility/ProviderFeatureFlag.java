package nl.aurorion.blockregen.compatibility;

import lombok.Getter;

public enum ProviderFeatureFlag {
    DROPS("drops"),
    MATERIALS("materials"),
    REWARDS("rewards"),
    CONDITIONS("conditions");

    @Getter
    private final String name;
    ProviderFeatureFlag(String name) {
        this.name = name;
    }
}
