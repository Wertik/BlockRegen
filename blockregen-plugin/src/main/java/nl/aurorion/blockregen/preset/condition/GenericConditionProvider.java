package nl.aurorion.blockregen.preset.condition;

import com.linecorp.conditional.ComposedCondition;
import com.linecorp.conditional.Condition;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.aurorion.blockregen.ParseException;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class GenericConditionProvider implements ConditionProvider {

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ProviderEntry {
        private final ConditionProvider provider;
        private final Class<?>[] expectedClasses;
        private final ConditionRelation relation;

        @NotNull
        public static ProviderEntry of(@NotNull ConditionProvider provider, Class<?>... expectedClasses) {
            return new ProviderEntry(provider, expectedClasses, ConditionRelation.OR);
        }

        @NotNull
        public static ProviderEntry of(@NotNull ConditionProvider provider, @NotNull ConditionRelation relation, Class<?>... expectedClasses) {
            return new ProviderEntry(provider, expectedClasses, relation);
        }

        @NotNull
        public static ProviderEntry of(@NotNull ConditionProvider provider) {
            return of(provider, Object.class);
        }

        @NotNull
        public static ProviderEntry of(@NotNull ConditionProvider provider, @NotNull ConditionRelation relation) {
            return of(provider, relation, Object.class);
        }

        private boolean isApplicable(Class<?> nodeClazz) {
            for (Class<?> clazz : this.expectedClasses) {
                if (clazz.isAssignableFrom(nodeClazz)) {
                    return true;
                }
            }
            return false;
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
        return new GenericConditionProvider(Collections.singletonMap(key, ProviderEntry.of(provider)), extender);
    }

    @NotNull
    public static GenericConditionProvider singleNode(@NotNull String key, @NotNull ConditionProvider provider) {
        return new GenericConditionProvider(Collections.singletonMap(key, ProviderEntry.of(provider)), null);
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
        providers.put(key, ProviderEntry.of(provider));
        return this;
    }

    @NotNull
    public GenericConditionProvider extender(@Nullable ContextExtender extender) {
        this.extender = extender;
        return this;
    }

    @Override
    @NotNull
    public Condition load(@Nullable String key, @NotNull Object node) {
        ProviderEntry entry = providers.get(key);

        if (entry == null) {
            throw new ParseException("Invalid property '" + key + "'");
        }

        if (!(node instanceof ConfigurationSection) && !(node instanceof List) && !entry.isApplicable(node.getClass())) {
            throw new ParseException("Invalid property type '" + node.getClass().getSimpleName() + "' for '" + key + "'. Required: " + Arrays.stream(entry.getExpectedClasses())
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(",")));
        }

        Condition condition;
        try {
            condition = Conditions.fromNode(
                    node,
                    entry.getRelation(),
                    entry.getProvider());

            // Don't alias composed conditions to let them unwind go deeper on #toString.
            if (!(condition instanceof ComposedCondition) && condition.alias() == null) {
                condition = condition.alias(key);
            }
        } catch (ParseException e) {
            throw new ParseException("Failed to parse '" + key + "': " + e.getMessage(), e);
        }
        return this.extender == null ? condition : Conditions.wrap(condition, extender);
    }
}
