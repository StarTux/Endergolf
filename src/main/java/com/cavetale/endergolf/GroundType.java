package com.cavetale.endergolf;

import com.destroystokyo.paper.MaterialTags;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextColor.color;

@Getter
@RequiredArgsConstructor
public enum GroundType {
    TEE(NamedTextColor.GOLD),
    GREEN(NamedTextColor.GREEN), // perfect, aka Fairway
    ROUGH(NamedTextColor.DARK_GREEN), // tall grass
    SAND(NamedTextColor.YELLOW),
    HARDPAN(color(0xf4a460)), // dirt
    ROCKS(NamedTextColor.GRAY), // stone
    MUD(color(0xa52a2a)),
    WATER(NamedTextColor.BLUE),
    LAVA(NamedTextColor.DARK_RED),
    ;

    private final TextColor color;

    public String getDisplayName() {
        return name().substring(0, 1) + name().substring(1).toLowerCase();
    }

    public Component getDisplayComponent() {
        return text(getDisplayName(), color);
    }

    public boolean isReset() {
        return this == WATER
            || this == LAVA;
    }

    /**
     * We determine the ground type first by the block that the dragon
     * egg will be replacing.  If said block is empty, we look at the
     * block below.
     */
    public static GroundType at(Block block) {
        final Material mat = block.getType();
        if (!mat.isAir()) {
            if (mat == Material.WATER) {
                return WATER;
            }
            if (mat == Material.LAVA) {
                return LAVA;
            }
            if (mat == Material.DIRT_PATH || mat == Material.FARMLAND) {
                return MUD;
            }
            if (Tag.REPLACEABLE.isTagged(mat)) {
                return ROUGH;
            }
        }
        final Block below = block.getRelative(0, -1, 0);
        final Material belowMat = below.getType();
        if (belowMat == Material.GRASS_BLOCK) {
            return GREEN;
        }
        if (Tag.DIRT.isTagged(belowMat)) {
            return HARDPAN;
        }
        if (Tag.SAND.isTagged(belowMat)) {
            return SAND;
        }
        if (belowMat == Material.MUD) {
            return MUD;
        }
        if (Tag.BASE_STONE_OVERWORLD.isTagged(mat)
            || Tag.BASE_STONE_NETHER.isTagged(mat)
            || MaterialTags.SANDSTONES.isTagged(mat)
            || MaterialTags.COBBLESTONES.isTagged(mat)) {
            return ROCKS;
        }
        return ROCKS; // good default?
    }
}
