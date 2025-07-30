package com.cavetale.endergolf;

import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Text;
import io.papermc.paper.datacomponent.DataComponentTypes;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.core.font.Unicode.tiny;
import static io.papermc.paper.datacomponent.item.ItemLore.lore;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@Getter
@RequiredArgsConstructor
public enum GolfClub {
    PUTTER(0.35, Material.IRON_SHOVEL,
           "The weakest club in terms of power, used for rolling the ball on the green. It is essential for accuracy and control on the putting surface."),
    DINKER(0.5, Material.IRON_HOE,
           "A very short-range club for delicate shots around the green. It imparts minimal power, allowing for precise, soft landings."),
    BUMPER(0.75, Material.IRON_AXE,
           "A shorter-range club designed for approach shots to the green. It provides more control and accuracy, making it easier to land the ball near the target."),
    THUMPER(1, Material.IRON_PICKAXE,
            "A mid-range club that offers a balance between distance and control. Suitable for a variety of shots where precision is important but some power is still needed."),
    BOOMER(2.5, Material.MACE,
           "The strongest club in your set, designed for maximum distance. Ideal for tee shots and long fairway shots where power is the primary concern."),
    ;

    private final double strength;
    private final Material material;
    private final String description;

    public ItemStack createItemStack() {
        final ItemStack result = new ItemStack(material);
        result.unsetData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        result.setData(DataComponentTypes.ITEM_NAME, text(name().substring(0, 1) + name().substring(1).toLowerCase(), GOLD));
        final List<Component> lore = new ArrayList<>();
        lore.addAll(Text.wrapLore2(description, txt -> text(tiny(txt), GRAY).decoration(ITALIC, false)));
        lore.add(empty());
        lore.add(textOfChildren(text(tiny("strength "), DARK_GRAY), text((int) Math.round(strength * 20.0), WHITE)).decoration(ITALIC, false));
        lore.add(textOfChildren(Mytems.MOUSE_LEFT, text(" Strike", GREEN)).decoration(ITALIC, false));
        result.setData(DataComponentTypes.LORE, lore(lore));
        return result;
    }

    public static GolfClub ofMaterial(Material material) {
        for (var it : values()) {
            if (it.material == material) return it;
        }
        return null;
    }
}
