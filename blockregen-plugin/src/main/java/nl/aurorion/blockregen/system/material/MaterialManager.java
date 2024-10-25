package nl.aurorion.blockregen.system.material;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.material.parser.MaterialParser;
import nl.aurorion.blockregen.system.preset.struct.material.DynamicMaterial;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
public class MaterialManager {

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
        registeredParsers.put((prefix == null ? null : prefix.toLowerCase()), parser);
        log.fine(String.format("Registered material parser with prefix %s", prefix));
    }

    public MaterialParser getParser(@Nullable String prefix) {
        return this.registeredParsers.get((prefix == null ? null : prefix.toLowerCase()));
    }

    record Pair<F, S>(F first, S second) {
    }

    private Pair<TargetMaterial, Double> parseMaterialAndChance(MaterialParser parser, String input) {
        // The part until the last colon that's not part of 'https://'
        Matcher matcher = Pattern.compile("(?<!http(?s)):(?!//)").matcher(input);

        log.fine("Input for parseMaterialAndChance: '" + input + "'");

        // <namespace>:<id>
        // <namespace>:<id>:<chance>
        // <material>
        // <material>:<chance>

        List<MatchResult> results = matcher.results().toList();
        long count = results.size();

        if (count != 0) {
            int lastColon = results.getLast().end();

            boolean withChance = true;

            String rawMaterialInput = input.substring(0, lastColon - 1);
            if (parser.containsColon() && count == 1) {
                rawMaterialInput = input;
                withChance = false;
            }
            log.fine("Raw material input: '" + rawMaterialInput + "'");

            TargetMaterial material = parser.parseMaterial(rawMaterialInput);

            if (withChance) {
                String rawChanceInput = input.substring(lastColon);
                log.fine("Raw chance input: '" + rawChanceInput + "'");
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
            log.fine("Single material input for parseMaterialAndChance: '" + input + "'");
            TargetMaterial material = parser.parseMaterial(input);
            return new Pair<>(material, null);
        }
    }

    public DynamicMaterial parseDynamicMaterial(String input) {
        List<String> materials = Arrays.asList(input.split(";"));

        // Materials without a chance.
        List<TargetMaterial> restMaterials = new ArrayList<>();
        Map<TargetMaterial, Double> valuedMaterials = new HashMap<>();

        if (materials.isEmpty()) {
            throw new IllegalArgumentException("Dynamic material " + input + " doesn't have the correct syntax");
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
                    log.warning(String.format("No valid parser found for material input %s", input));
                    return null;
                }

                log.fine("No prefix");

                // Not a prefix.
                Pair<TargetMaterial, Double> result = parseMaterialAndChance(parser, materialInput);

                if (result.second() == null) {
                    restMaterials.add(result.first());
                } else {
                    valuedMaterials.put(result.first(), result.second());
                }
            } else {
                // Prefixed
                String rest = materialInput.substring(firstColon + 1);
                log.fine("Prefix: '" + prefix + "'");
                log.fine("Rest: '" + rest + "'");
                Pair<TargetMaterial, Double> result = parseMaterialAndChance(parser, rest);

                if (result.second() == null) {
                    restMaterials.add(result.first());
                } else {
                    valuedMaterials.put(result.first(), result.second());
                    log.fine(String.format("Added material %s at chance %.2f%%", result.first(), result.second()));
                }
            }
        }

        double rest = 1.0 - valuedMaterials.values().stream().mapToDouble(e -> e).sum();

        if (restMaterials.size() == 1) {
            valuedMaterials.put(restMaterials.getFirst(), rest);
            log.fine(String.format("Added material %s at chance %.2f%%", restMaterials.getFirst(), rest));
        } else {
            // Split the rest of the chance between the materials.
            double chance = rest / restMaterials.size();
            restMaterials.forEach(mat -> valuedMaterials.put(mat, chance));
        }

        return DynamicMaterial.from(valuedMaterials);
    }

    /**
     * Parse a material using registered parsers.
     *
     * @param input Input string, format (prefix:?)(material-name[nodedata,...])
     * @return Parsed material or null when no parser was found.
     * @throws IllegalArgumentException When the parser is unable to parse the material.
     */
    public @Nullable TargetMaterial parseMaterial(@NotNull String input) throws IllegalArgumentException {

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
                log.fine(String.format("No valid parser found for material input %s", input));
                return null;
            }
        } else {
            // remove parts[0] aka the parser prefix
            parts = Arrays.copyOfRange(parts, 1, parts.length);
        }

        return parser.parseMaterial(parts[0]);
    }
}
