package nl.aurorion.blockregen.storage.migration;

import nl.aurorion.blockregen.preset.BlockPreset;
import nl.aurorion.blockregen.region.CuboidRegion;
import nl.aurorion.blockregen.region.RawRegion;

public class MigrateRegionsFromYaml {
    // todo: Load Regions.yml regions and import into selected storage option.

    /*private boolean loadRegion(RawRegion rawRegion) {
        CuboidRegion region = rawRegion.build();

        if (region == null) {
            log.warning("Could not load region " + rawRegion.getName() + ", world " + rawRegion.getMax() + " still not loaded.");
            return false;
        }

        // Attach presets
        for (String presetName : rawRegion.getBlockPresets()) {
            BlockPreset preset = plugin.getPresetManager().getPreset(presetName);

            if (preset == null) {
                log.warning(String.format("Preset %s isn't loaded, but is included in region %s.", presetName, rawRegion.getName()));
            }

            region.addPreset(presetName);
        }

        this.loadedAreas.add(region);
        this.sort();
        log.fine(() -> "Loaded region " + region);
        return true;
    }*/
}
