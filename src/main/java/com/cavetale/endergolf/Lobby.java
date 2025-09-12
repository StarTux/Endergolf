package com.cavetale.endergolf;

import com.cavetale.mytems.Mytems;
import io.papermc.paper.datacomponent.DataComponentTypes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import static com.cavetale.core.font.Unicode.tiny;
import static io.papermc.paper.datacomponent.item.ItemLore.lore;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class Lobby {
    private final EndergolfPlugin plugin;
    private LobbyListener listener;

    public void enable() {
        listener = new LobbyListener(plugin, this);
        listener.enable();
        for (Player player : getPlayers()) {
            onJoin(player);
        }
    }

    public World getWorld() {
        return Bukkit.getWorlds().get(0);
    }

    public boolean isWorld(World world) {
        return world.equals(getWorld());
    }

    public boolean isInWorld(Entity entity) {
        return getWorld().equals(entity.getWorld());
    }

    public List<Player> getPlayers() {
        return getWorld().getPlayers();
    }

    public Location getSpawnLocation() {
        return getWorld().getSpawnLocation();
    }

    public void warp(Player player) {
        player.eject();
        player.leaveVehicle();
        player.teleport(getSpawnLocation());
        onJoin(player);
    }

    public void onJoin(Player player) {
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setVelocity(new Vector());
        player.setFallDistance(0f);
        player.getInventory().clear();
        player.getInventory().addItem(makeGolfItem());
        player.getInventory().addItem(makeVoteItem());
        player.getInventory().addItem(Mytems.BLIND_EYE.createItemStack());
    }

    public ItemStack makeGolfItem() {
        final ItemStack item = new ItemStack(Material.ARROW);
        item.setData(DataComponentTypes.CUSTOM_NAME, text("Play", GREEN).decoration(ITALIC, false));
        item.setData(DataComponentTypes.LORE,
                     lore(List.of(text(tiny("Player a game of"), GRAY).decoration(ITALIC, false),
                                  text(tiny("Endergolf alone."), GRAY).decoration(ITALIC, false),
                                  empty(),
                                  textOfChildren(Mytems.MOUSE_RIGHT, text(" Open Map Menu", GRAY)).decoration(ITALIC, false))));
        item.setData(DataComponentTypes.ITEM_MODEL, Mytems.GOLDEN_GOLF_CLUB.getNamespacedKey());
        return item;
    }

    public ItemStack makeVoteItem() {
        final ItemStack item = new ItemStack(Material.PAPER);
        item.setData(DataComponentTypes.CUSTOM_NAME, text("Vote", GREEN).decoration(ITALIC, false));
        item.setData(DataComponentTypes.LORE,
                     lore(List.of(text(tiny("Vote on the next"), GRAY).decoration(ITALIC, false),
                                  text(tiny("Endergolf map."), GRAY).decoration(ITALIC, false),
                                  empty(),
                                  textOfChildren(Mytems.MOUSE_RIGHT, text(" Open Vote Menu", GRAY)).decoration(ITALIC, false))));
        item.setData(DataComponentTypes.ITEM_MODEL, Mytems.IRON_GOLF_CLUB.getNamespacedKey());
        return item;
    }
}
