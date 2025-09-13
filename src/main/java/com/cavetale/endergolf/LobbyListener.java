package com.cavetale.endergolf;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public final class LobbyListener implements Listener {
    private final EndergolfPlugin plugin;
    private final Lobby lobby;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        if (!lobby.isInWorld(event.getPlayer())) return;
        if (!plugin.getSaveTag().isEvent()) return;
        event.sidebar(PlayerHudPriority.HIGH, plugin.getHighscoreLines());
    }

    @EventHandler
    private void onEntityDamage(EntityDamageEvent event) {
        if (!lobby.isInWorld(event.getEntity())) return;
        event.setCancelled(true);
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID && event.getEntity() instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> lobby.warp(player));
        }
    }

    @EventHandler
    private void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!lobby.isInWorld(event.getPlayer())) return;
        if (event.getPlayer().isOp()) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onEntityExhaustion(EntityExhaustionEvent event) {
        if (!lobby.isInWorld(event.getEntity())) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (!lobby.isInWorld(event.getPlayer())) return;
        lobby.onJoin(event.getPlayer());
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (!lobby.isInWorld(event.getPlayer())) return;
        lobby.onJoin(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (!lobby.isInWorld(event.getPlayer())) return;
        if (event.hasItem() && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) && event.getItem().getType() == Material.ARROW) {
            event.setCancelled(true);
            event.getPlayer().performCommand("golf");
        } else if (event.hasItem() && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) && event.getItem().getType() == Material.PAPER) {
            event.setCancelled(true);
            event.getPlayer().performCommand("mapvote");
        }
    }
}
