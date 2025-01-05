package nl.aurorion.blockregen.material;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Pair;
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

    private final BlockRegen plugin;

    private final Map<String, MaterialParser> registeredParsers = new HashMap<>();

    public MaterialManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    /**
     * Register a material parser under a prefix.
     * If there is a parser already registered, overwrite (aka Map#put).
     * <p>
     * A null prefix parser is used for inputs with no prefix.
     * <p>
     * A prefix cannot match a material name, otherwise the parsing screws up.
     * We could use a different separator, but screw it, a colon looks cool.
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

    @NotNull
    private Pair<BlockRegenMaterial, Double> parseMaterialAndChance(MaterialParser parser, String input) throws IllegalArgumentException {
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

            BlockRegenMaterial material = parser.parseMaterial(rawMaterialInput);

            if (withChance) {
                String rawChanceInput = input.substring(lastColon);
                log.fine(() -> "Raw chance input: '" + rawChanceInput + "'");
                try {
                    double chance = Double.parseDouble(rawChanceInput);
                    return new Pair<>(material, chance / 100);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid chance '" + rawChanceInput + "', has to be a number.");
                }
            } else {
                return new Pair<>(material, null);
            }
        } else {
            log.fine(() -> "Single material input for parseMaterialAndChance: '" + input + "'");
            BlockRegenMaterial material = parser.parseMaterial(input);
            return new Pair<>(material, null);
        }
    }

    // <prefix:?><material>;<prefix:?><material>;...
    @NotNull
    public TargetMaterial parseTargetMaterial(String input) throws IllegalArgumentException {
        List<String> materials = Arrays.asList(input.split(";"));

        if (materials.isEmpty()) {
            throw new IllegalArgumentException("Target material " + input + " doesn't have the correct syntax.");
        }

        List<BlockRegenMaterial> targetMaterials = new ArrayList<>();

        for (String materialInput : materials) {
            BlockRegenMaterial material = parseMaterial(materialInput);
            targetMaterials.add(material);
        }
        return TargetMaterial.of(targetMaterials);
    }

    @NotNull
    public PlacementMaterial parsePlacementMaterial(String input) throws IllegalArgumentException {
        List<String> materials = Arrays.asList(input.split(";"));

        // Materials without a chance.
        List<BlockRegenMaterial> restMaterials = new ArrayList<>();
        Map<BlockRegenMaterial, Double> valuedMaterials = new HashMap<>();

        if (materials.isEmpty()) {
            throw new IllegalArgumentException("Placement material " + input + " doesn't have the correct syntax.");
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
                    throw new IllegalArgumentException(String.format("Material '%s' is invalid. No valid material parser found.", input));
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

        double rest = 1.0 - valuedMaterials.values().stream().mapToDouble(e -> e).sum();

        if (restMaterials.size() == 1) {
            valuedMaterials.put(restMaterials.get(0), rest);
            log.fine(() -> String.format("Added material %s at chance %.2f%%", restMaterials.get(0), rest));
        } else {
            // Split the rest of the chance between the materials.
            double chance = rest / restMaterials.size();
            restMaterials.forEach(mat -> valuedMaterials.put(mat, chance));
        }

        return PlacementMaterial.from(valuedMaterials);
    }

    /**
     * Parse a material using registered parsers.
     *
     * @param input Input string, format (prefix:?)(material-name[nodedata,...])
     * @return Parsed material or null when no parser was found.
     * @throws IllegalArgumentException When the parser is unable to parse the material.
     */
    @NotNull
    public BlockRegenMaterial parseMaterial(@NotNull String input) throws IllegalArgumentException {

        // Separate parts
        String[] parts = new String[]{input};

        // Split around : for prefixes if possible
        int index = input.indexOf(':');
        if (index != -1) {
            parts = new String[]{input.substring(0, index), input.substring(index + 1)};
        }

        log.fine(String.format("Material parts: %s", String.join(", ", parts)));

        // First either prefix or material

        MaterialParser parser = getParser(parts[0].toLowerCase());

        if (parser == null) {
            parser = getParser(null);

            if (parser == null) {
                throw new IllegalArgumentException(String.format("Material '%s' invalid. No valid parser found", input));
            }
        } else {
            // remove parts[0] aka the parser prefix
            parts = Arrays.copyOfRange(parts, 1, parts.length);
        }

        return parser.parseMaterial(parts[0]);
    }
}
