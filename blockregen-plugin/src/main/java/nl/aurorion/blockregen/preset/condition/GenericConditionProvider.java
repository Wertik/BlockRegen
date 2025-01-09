package nl.aurorion.blockregen.preset.condition;

import com.linecorp.conditional.Condition;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.aurorion.blockregen.configuration.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GenericConditionProvider implements ConditionProvider {

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ProviderEntry {
        private final ConditionProvider provider;
        private final Class<?> expectedClass;

        public static ProviderEntry of(ConditionProvider provider, Class<?> expectedClass) {
            return new ProviderEntry(provider, expectedClass);
        }

        public static ProviderEntry provider(ConditionProvider provider) {
            return new ProviderEntry(provider, Object.class);
        }
    }

    private final Map<String, ProviderEntry> providers;

    @Nullable
    private ContextExtender extender;

    GenericConditionProvider(Map<String, ProviderEntry> providers, @Nullable ContextExtender extender) {
        this.providers = new HashMap<>(providers);
        this.extender = extender;
    }

    public Map<String, ProviderEntry> getProviders() {
        return Collections.unmodifiableMap(providers);
    }

    @NotNull
    public static GenericConditionProvider singleNode(@NotNull String key, @NotNull ProviderEntry entry) {
        return new GenericConditionProvider(Collections.singletonMap(key, entry), null);
    }

    @NotNull
    public static GenericConditionProvider singleNode(@NotNull String key, @NotNull ProviderEntry entry, @Nullable ContextExtender extender) {
        return new GenericConditionProvider(Collections.singletonMap(key, entry), extender);
    }

    @NotNull
    public static GenericConditionProvider singleNode(@NotNull String key, @NotNull ConditionProvider provider, @Nullable ContextExtender extender) {
        return new GenericConditionProvider(Collections.singletonMap(key, ProviderEntry.provider(provider)), extender);
    }

    @NotNull
    public static GenericConditionProvider singleNode(@NotNull String key, @NotNull ConditionProvider provider) {
        return new GenericConditionProvider(Collections.singletonMap(key, ProviderEntry.provider(provider)), null);
    }

    @NotNull
    public static GenericConditionProvider empty() {
        return new GenericConditionProvider(Collections.emptyMap(), null);
    }

    @NotNull
    public GenericConditionProvider addProvider(@NotNull String key, @NotNull ProviderEntry entry) {
        providers.put(key, entry);
        return this;
    }

    @NotNull
    public GenericConditionProvider addProvider(@NotNull String key, @NotNull ConditionProvider provider) {
        providers.put(key, ProviderEntry.provider(provider));
        return this;
    }

    @NotNull
    public GenericConditionProvider extender(@Nullable ContextExtender extender) {
        this.extender = extender;
        return this;
    }

    @Override
    @NotNull
    public Condition load(@NotNull Object node, @Nullable String key) {
        Condition composed = Conditions.fromNodeMultiple(node, ConditionRelation.AND, (mNode, mKey) -> {
            ProviderEntry entry = providers.get(mKey);

            if (entry == null) {
                throw new ParseException("Invalid property '" + mKey + "'");
            }

            if (!entry.expectedClass.isAssignableFrom(mNode.getClass())) {
                throw new ParseException("Invalid property type '" + mNode.getClass().getSimpleName() + "' for '" + mKey + "'");
            }

            try {
                return Conditions.fromNode(
                                mNode,
                                ConditionRelation.OR,
                                entry.getProvider())
                        .alias(mKey);
            } catch (ParseException e) {
                throw new ParseException("Failed to parse '" + mKey + "': " + e.getMessage());
            }
        });
        return this.extender == null ? composed : Conditions.wrap(composed, extender);
    }
}
