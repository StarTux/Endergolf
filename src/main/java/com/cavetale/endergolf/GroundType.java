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
    TEE(1.0, 0, 0f, 0f, NamedTextColor.GOLD),
    // Easy
    GREEN(1.0, 0.5, 0.125f, 0.5f, NamedTextColor.GREEN), // perfect, aka Fairway
    WOOD(1.0, 0.5, 0.25f, 0.5f, color(0xa47449)),
    SMOOTH(1.0, 0.75, 0.125f, 0.5f, NamedTextColor.BLUE),
    // Medium
    ROUGH(0.8, 0, 0.5f, 1.5f, NamedTextColor.DARK_GREEN), // tall grass
    HARDPAN(0.8, 0.25, 0.25f, 0.5f, color(0xf4a460)), // dirt
    ROCKS(1.0, 0.25, 0.5f, 1f, NamedTextColor.DARK_GRAY), // stone
    // Hard
    SAND(0.65, 0, 0.25f, 1f, NamedTextColor.YELLOW),
    MUD(0.5, 0, 0.5f, 1.5f, color(0xa52a2a)),
    BOUNCY(1.0, 1.25, 1.0f, 2f, color(0x00ff00)),
    // Reset
    WATER(0, 0, 0f, 0f, NamedTextColor.DARK_BLUE),
    LAVA(0, 0, 0f, 0f, NamedTextColor.DARK_RED),
    ;

    private final double strengthFactor;
    private final double bounciness;
    private final float wiggle;
    private final float wiggleSpeed;
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
        final Material ball = block.getType();
        if (!ball.isAir()) {
            if (ball == Material.WATER) {
                return WATER;
            }
            if (ball == Material.LAVA) {
                return LAVA;
            }
            if (ball == Material.DIRT_PATH || ball == Material.FARMLAND) {
                return MUD;
            }
            if (Tag.REPLACEABLE.isTagged(ball)) {
                return ROUGH;
            }
        }
        final Material floor = block.getRelative(0, -1, 0).getType();
        if (floor == Material.GRASS_BLOCK || floor == Material.MOSS_BLOCK || Tag.CONCRETE_POWDER.isTagged(floor) || Tag.WOOL.isTagged(floor)) {
            return GREEN;
        }
        if (Tag.DIRT.isTagged(floor)) {
            return HARDPAN;
        }
        if (Tag.SAND.isTagged(floor) || floor == Material.SOUL_SAND || floor == Material.SOUL_SOIL) {
            return SAND;
        }
        if (floor == Material.MUD || floor == Material.DIRT_PATH || floor == Material.FARMLAND) {
            return MUD;
        }
        if (floor == Material.SLIME_BLOCK || floor == Material.HONEY_BLOCK) {
            return BOUNCY;
        }
        if (floor == Material.STONE
            || floor == Material.SMOOTH_STONE
            || MaterialTags.SANDSTONES.isTagged(floor)
            || Tag.TERRACOTTA.isTagged(floor)
            || MaterialTags.CONCRETES.isTagged(floor)
            || MaterialTags.GLAZED_TERRACOTTA.isTagged(floor)
            || floor.name().contains("SMOOTH")
            || floor.name().contains("POLISHED")) {
            return SMOOTH;
        }
        if (Tag.LOGS.isTagged(floor) || Tag.PLANKS.isTagged(floor)) {
            return WOOD;
        }
        if (Tag.BASE_STONE_OVERWORLD.isTagged(floor)
            || Tag.BASE_STONE_NETHER.isTagged(floor)
            || MaterialTags.COBBLESTONES.isTagged(floor)
            || floor == Material.GRAVEL
            || floor == Material.NETHERRACK
            || Tag.STAIRS.isTagged(floor)) {
            return ROCKS;
        }
        return ROCKS; // good default?
    }
}
