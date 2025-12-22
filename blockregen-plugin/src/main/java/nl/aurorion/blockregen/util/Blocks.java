package nl.aurorion.blockregen.util;

import com.cryptomorin.xseries.XMaterial;
import lombok.experimental.UtilityClass;
import nl.aurorion.blockregen.BlockRegenPlugin;
import org.bukkit.block.Block;

@UtilityClass
public class Blocks {

    public String blockToString(Block block) {
        return "Block{" + Locations.locationToString(block.getLocation()) + ",type=" + block.getType() + "}";
    }

    public static boolean isMultiblockCrop(BlockRegenPlugin plugin, Block block) {
        XMaterial type = plugin.getVersionManager().getMethods().getType(block);
        return isMultiblockCrop(type);
    }

    public static boolean isKelp(XMaterial material) {
        return material == XMaterial.KELP || material == XMaterial.KELP_PLANT;
    }

    public static boolean isSeagrass(XMaterial material) {
        return material == XMaterial.SEAGRASS || material == XMaterial.TALL_SEAGRASS;
    }

    public static boolean shouldForceRegenerateWhole(BlockRegenPlugin plugin, Block block) {
        XMaterial type = plugin.getVersionManager().getMethods().getType(block);
        switch (type) {
            case TALL_GRASS:
            case LARGE_FERN:
                return true;
            default:
                return false;
        }
    }

    public static boolean isMultiblockCrop(XMaterial type) {
        switch (type) {
            case TALL_GRASS:
            case CACTUS:
            case BAMBOO:
            case KELP_PLANT:
            case KELP:
            case TALL_SEAGRASS:
            case LARGE_FERN:
            case SUGAR_CANE:
                return true;
            default:
                return false;
        }
    }

    public static boolean requiresFarmland(XMaterial material) {
        switch (material) {
            case CARROT:
            case CARROTS:
            case POTATO:
            case POTATOES:
            case PUMPKIN_SEEDS:
            case WHEAT_SEEDS:
            case WHEAT:
            case MELON_SEEDS:
            case BEETROOT_SEEDS:
            case BEETROOTS:
                return true;
            default:
                return false;
        }
    }

    public static boolean reliesOnBlockBelow(XMaterial material) {
        if (isMultiblockCrop(material)) {
            return true;
        }

        switch (material) {
            case MOSS_CARPET:
            case SHORT_GRASS:
                return true;
            default:
                return false;
        }
    }
}
