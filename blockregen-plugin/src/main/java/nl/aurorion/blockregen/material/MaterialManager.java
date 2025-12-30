package nl.aurorion.blockregen.material;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Strings;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.Pair;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.material.builtin.MinecraftMaterial;
import nl.aurorion.blockregen.preset.material.PlacementMaterial;
import nl.aurorion.blockregen.preset.material.TargetMaterial;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log
public class MaterialManager {

    private static final Pattern COLON_PATTERN = Pattern.compile("(?<!http(?s)):(?!//)");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^(\\w+):(.*)");

    private final BlockRegenPlugin plugin;

    private final LinkedHashMap<String, MaterialProvider> registeredProviders = new LinkedHashMap<>();

    private final Map<String, BlockRegenMaterial> cachedMaterialInputs = new HashMap<>();

    public MaterialManager(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    private <T extends BlockRegenMaterial> T cacheEntry(String input, T value) {
        if (input != null) {
            cachedMaterialInputs.put(input, value);
        }
        log.fine(() -> "Cached " + value);
        return value;
    }

    /**
     * Register a material parser under a prefix. If there is a parser already registered, overwrite (aka Map#put).
     * <p>
     * A null prefix parser is used for inputs with no prefix.
     * <p>
     * A prefix cannot match a material name, otherwise the parsing screws up. We could use a different separator, but
     * screw it, a colon looks cool.
     */
    public void register(@Nullable String prefix, @NotNull MaterialProvider provider) {
        prefix = (prefix == null ? null : prefix.toLowerCase());

        MaterialProvider registeredProvider = registeredProviders.get(prefix);
        if (registeredProvider != null && registeredProvider.getClass() == provider.getClass()) {
            return;
        }

        registeredProviders.put(prefix, provider);
        log.fine(String.format("Registered material provider with prefix %s", prefix));
    }

    @Nullable
    public MaterialProvider getProvider(@Nullable String prefix) {
        return this.registeredProviders.get((prefix == null ? null : prefix.toLowerCase()));
    }

    @SuppressWarnings("unchecked")
    private Iterator<Map.Entry<String, MaterialProvider>> reversedEntryIterator() {
        Map.Entry<String, MaterialProvider>[] array = (Map.Entry<String, MaterialProvider>[]) this.registeredProviders.entrySet().toArray(new Map.Entry[0]);

        class ReversedEntryIterator implements Iterator<Map.Entry<String, MaterialProvider>> {

            private int currentIndex = array.length - 1;

            @Override
            public boolean hasNext() {
                return currentIndex >= 0;
            }

            @Override
            public Map.Entry<String, MaterialProvider> next() {
                if (currentIndex < 0) {
                    throw new NoSuchElementException();
                }

                return array[this.currentIndex--];
            }
        }

        return new ReversedEntryIterator();
    }

    @Nullable
    public BlockRegenMaterial getMaterial(@NotNull Block block) {
        Iterator<Map.Entry<String, MaterialProvider>> it = reversedEntryIterator();
        while (it.hasNext()) {
            Map.Entry<String, MaterialProvider> entry = it.next();

            BlockRegenMaterial material = entry.getValue().load(block);

            if (material == null) {
                continue;
            }

            log.fine(() -> "Loaded material '" + material + "' with loader '" + entry.getKey() + "'");
            return material;
        }

        return null;
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @NotNull
    private Pair<BlockRegenMaterial, Double> parseMaterialAndChance(@NotNull MaterialProvider provider, @NotNull String input) {
        // The part until the last colon that's not part of 'https://'
        Matcher matcher = COLON_PATTERN.matcher(input);

        log.fine(() -> "Input for parseMaterialAndChance: '" + input + "'");

        // <namespace>:<id>
        // <namespace>:<id>:<chance>
        // <material>
        // <material>:<chance>

        List<MatchResult> results = new ArrayList<>();

        while (matcher.find()) {
            results.add(matcher.toMatchResult());
        }

        // Count of colons in the input.
        long count = results.size();

        log.fine(() -> String.join(",", results.stream().map(MatchResult::group).collect(Collectors.joining(","))) + " " + count);

        if (count != 0) {
            int lastColon = results.get(results.size() - 1).end();

            boolean withChance = true;

            String rawMaterialInput = input.substring(0, lastColon - 1);
            if (provider.containsColon() && count == 1) {
                rawMaterialInput = input;
                withChance = false;
            }

            BlockRegenMaterial material;
            try {
                material = parseMaterial(provider, rawMaterialInput);
            } catch (Exception e) {
                throw new ParseException(String.format("Failed to parse material '%s' with parser '%s'", rawMaterialInput, provider.getClass().getSimpleName()), e, true);
            }

            if (withChance) {
                String rawChanceInput = input.substring(lastColon);
                log.fine(() -> "Raw chance input: '" + rawChanceInput + "'");
                try {
                    double chance = Double.parseDouble(rawChanceInput);
                    return new Pair<>(material, chance / 100);
                } catch (NumberFormatException e) {
                    throw new ParseException("Invalid chance '" + rawChanceInput + "', has to be a number.");
                }
            } else {
                return new Pair<>(material, null);
            }
        } else {
            log.fine(() -> "Single material input for parseMaterialAndChance: '" + input + "'");
            try {
                BlockRegenMaterial material = parseMaterial(provider, input);
                return new Pair<>(material, null);
            } catch (Exception e) {
                throw new ParseException(String.format("Failed to parse material '%s' with parser '%s'", input, provider.getClass().getSimpleName()), e, true);
            }
        }
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    // <prefix:?><material>;<prefix:?><material>;...
    @NotNull
    public TargetMaterial parseTargetMaterial(String input) {
        List<String> materials = Arrays.asList(input.split(";"));

        if (materials.isEmpty()) {
            throw new ParseException("Target material " + input + " doesn't have the correct syntax.");
        }

        List<BlockRegenMaterial> targetMaterials = new ArrayList<>();

        for (String materialInput : materials) {
            if (materialInput == null) {
                continue;
            }

            String trimmed = materialInput.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            // Wildcard support for vanilla materials, e.g. '*_ORE' or '*_TERRACOTTA'
            // Only apply when there is no explicit prefix.
            if (trimmed.startsWith("*") && trimmed.indexOf('*', 1) == -1 && !trimmed.contains(":")) {
                String suffix = trimmed.substring(1).toUpperCase(Locale.ROOT);
                List<BlockRegenMaterial> expanded = new ArrayList<>();

                for (XMaterial xMaterial : XMaterial.values()) {
                    Material nativeMaterial = xMaterial.get();
                    if (nativeMaterial != null && xMaterial.name().endsWith(suffix) && nativeMaterial.isBlock()) {
                        // Delegate to the normal parsing logic to keep behaviour consistent.
                        BlockRegenMaterial material = parseMaterial(xMaterial.name());
                        expanded.add(material);
                    }
                }

                if (expanded.isEmpty()) {
                    throw new ParseException("Target material wildcard '" + trimmed + "' did not match any materials.");
                }

                targetMaterials.addAll(expanded);
                continue;
            }

            BlockRegenMaterial material = parseMaterial(trimmed);
            targetMaterials.add(material);
        }
        return TargetMaterial.of(targetMaterials);
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @NotNull
    public PlacementMaterial parsePlacementMaterial(@NotNull String input) {
        List<String> materials = Arrays.asList(input.split(";"));

        // Materials without a chance.
        List<BlockRegenMaterial> restMaterials = new ArrayList<>();
        Map<BlockRegenMaterial, Double> valuedMaterials = new HashMap<>();

        if (materials.isEmpty()) {
            throw new ParseException("Placement material " + input + " doesn't have the correct syntax.");
        }

        for (String materialInput : materials) {
            // Parse individual materials
            /* Formats:
             * <prefix>:<material>:<chance> - 2x colon (don't count url), last part only double
             * <prefix>:<material>
             *
             * <material>:<chance>
             * <material>

             * For ItemsAdder:
             * <material> -> <namespace>:<id>
             * */

            int firstColon = materialInput.indexOf(":");

            // First split the chance away from the end? MMOItems use numerical IDs... now way to match that
            // Try first part as prefix...

            String prefix = firstColon == -1 ? null : materialInput.substring(0, firstColon);
            MaterialProvider provider = getProvider(prefix);

            if (provider == null) {
                provider = getProvider(null);

                if (provider == null) {
                    throw new ParseException(String.format("Material '%s' is invalid. No valid material parser found.", input));
                }

                log.fine(() -> "No prefix");

                // Not a prefix.
                Pair<BlockRegenMaterial, Double> result = parseMaterialAndChance(provider, materialInput);

                if (result.getSecond() == null) {
                    restMaterials.add(result.getFirst());
                } else {
                    valuedMaterials.put(result.getFirst(), result.getSecond());
                }
            } else {
                // Prefixed
                String rest = materialInput.substring(firstColon + 1);
                log.fine(() -> "Prefix: '" + prefix + "'");
                log.fine(() -> "Rest: '" + rest + "'");
                Pair<BlockRegenMaterial, Double> result = parseMaterialAndChance(provider, rest);

                if (result.getSecond() == null) {
                    restMaterials.add(result.getFirst());
                } else {
                    valuedMaterials.put(result.getFirst(), result.getSecond());
                    log.fine(() -> String.format("Added material %s at chance %.2f%%", result.getFirst(), result.getSecond()));
                }
            }
        }

        double rest = 1.0 - valuedMaterials.values().stream()
                .mapToDouble(e -> e)
                .sum();

        // Anything up to 100% left to distribute?
        if (rest > 0.0) {
            // Fill the rest with AIR if no material was provided.
            if (restMaterials.isEmpty()) {
                valuedMaterials.put(new MinecraftMaterial(plugin, XMaterial.AIR, null), rest);
                log.fine(() -> "Only a single material with chance provided. Filling the rest with AIR.");
            } else if (restMaterials.size() == 1) {
                valuedMaterials.put(restMaterials.get(0), rest);
                log.fine(() -> String.format("Added material %s at chance %.2f%%", restMaterials.get(0), rest));
            } else {
                // Split the rest of the chance between the materials.
                double chance = rest / restMaterials.size();
                restMaterials.forEach(mat -> valuedMaterials.put(mat, chance));
            }
        }

        try {
            return PlacementMaterial.from(valuedMaterials);
        } catch (IllegalArgumentException e) {
            throw new ParseException(e);
        }
    }

    /**
     * Parse a material using registered parsers.
     *
     * @param input Input string, format (prefix:?)(material-name[nodedata,...])
     * @return Parsed material or null when no parser was found.
     * @throws ParseException When the parser is unable to parse the material or the input is empty.
     */
    @NotNull
    public BlockRegenMaterial parseMaterial(@NotNull String input) {
        if (Strings.isNullOrEmpty(input)) {
            throw new ParseException("Empty input.");
        }

        // Check for a prefix first.
        Matcher matcher = PREFIX_PATTERN.matcher(input);

        String prefix = null;
        String sanitized = input;

        if (matcher.matches()) {
            prefix = matcher.group(1);
            sanitized = matcher.group(2);
        }

        MaterialProvider provider = getProvider(prefix);

        if (provider == null) {
            // Prefix not registered
            throw new ParseException(String.format("No valid provider found for prefix '%s'", prefix));
        }

        return parseMaterial(provider, sanitized);
    }

    @NotNull
    private BlockRegenMaterial parseMaterial(@NotNull MaterialProvider provider, @NotNull String input) {
        BlockRegenMaterial cached = this.cachedMaterialInputs.get(input);

        if (cached != null) {
            log.fine(() -> "Material input cache hit " + input + " -> " + cached);
            return cached;
        }

        return cacheEntry(input, provider.parseMaterial(input));
    }

    @NotNull
    public Map<String, MaterialProvider> getProviders() {
        return Collections.unmodifiableMap(this.registeredProviders);
    }
}
