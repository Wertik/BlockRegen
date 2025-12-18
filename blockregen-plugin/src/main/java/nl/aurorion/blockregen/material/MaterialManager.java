package nl.aurorion.blockregen.material;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Strings;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.Pair;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.material.parser.MaterialParser;
import nl.aurorion.blockregen.preset.material.PlacementMaterial;
import nl.aurorion.blockregen.preset.material.TargetMaterial;
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

    private final Map<String, MaterialParser> registeredParsers = new HashMap<>();

    private final BlockRegenPlugin plugin;

    public MaterialManager(BlockRegenPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Register a material parser under a prefix. If there is a parser already registered, overwrite (aka Map#put).
     * <p>
     * A null prefix parser is used for inputs with no prefix.
     * <p>
     * A prefix cannot match a material name, otherwise the parsing screws up. We could use a different separator, but
     * screw it, a colon looks cool.
     */
    public void registerParser(@Nullable String prefix, @NotNull MaterialParser parser) {
        prefix = (prefix == null ? null : prefix.toLowerCase());

        MaterialParser registeredParser = registeredParsers.get(prefix);
        if (registeredParser != null && registeredParser.getClass() == parser.getClass()) {
            return;
        }

        registeredParsers.put(prefix, parser);
        log.fine(String.format("Registered material parser with prefix %s", prefix));
    }

    @Nullable
    public MaterialParser getParser(@Nullable String prefix) {
        return this.registeredParsers.get((prefix == null ? null : prefix.toLowerCase()));
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @NotNull
    private Pair<BlockRegenMaterial, Double> parseMaterialAndChance(@NotNull MaterialParser parser, @NotNull String input) {
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
            if (parser.containsColon() && count == 1) {
                rawMaterialInput = input;
                withChance = false;
            }

            BlockRegenMaterial material;
            try {
                material = parser.parseMaterial(rawMaterialInput);
            } catch (Exception e) {
                throw new ParseException(String.format("Failed to parse material '%s' with parser '%s'", rawMaterialInput, parser.getClass().getSimpleName()), e, true);
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
                BlockRegenMaterial material = parser.parseMaterial(input);
                return new Pair<>(material, null);
            } catch (Exception e) {
                throw new ParseException(String.format("Failed to parse material '%s' with parser '%s'", input, parser.getClass().getSimpleName()), e, true);
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
            if (trimmed.startsWith("*") && trimmed.indexOf('*', 1) == -1 && !trimmed.substring(1).isEmpty() && !trimmed.contains(":")) {
                String suffix = trimmed.substring(1).toUpperCase(Locale.ROOT);
                List<BlockRegenMaterial> expanded = new ArrayList<>();

                for (XMaterial xMaterial : XMaterial.values()) {
                    if (xMaterial.name().endsWith(suffix)) {
                        // Delegate to the normal parsing logic to keep behaviour consistent.
                        BlockRegenMaterial material = parseMaterial(xMaterial.name());
                        expanded.add(material);
                    }
                }

                if (expanded.isEmpty()) {
                    throw new ParseException("Target material wildcard " + trimmed + " did not match any materials.");
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
            MaterialParser parser = getParser(prefix);

            if (parser == null) {
                parser = getParser(null);

                if (parser == null) {
                    throw new ParseException(String.format("Material '%s' is invalid. No valid material parser found.", input));
                }

                log.fine(() -> "No prefix");

                // Not a prefix.
                Pair<BlockRegenMaterial, Double> result = parseMaterialAndChance(parser, materialInput);

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
                Pair<BlockRegenMaterial, Double> result = parseMaterialAndChance(parser, rest);

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

        MaterialParser parser = getParser(prefix);

        if (parser == null) {
            // Prefix not registered
            throw new ParseException(String.format("No valid parser found for prefix '%s'", prefix));
        }

        return parser.parseMaterial(sanitized);
    }
}
