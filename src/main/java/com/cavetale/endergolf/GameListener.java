package com.cavetale.endergolf;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.magicmap.event.MagicMapCursorEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;

@RequiredArgsConstructor
public final class GameListener implements Listener {
    private final EndergolfPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onPlayerInteract(PlayerInteractEvent event) {
        final Game game = Game.in(event.getPlayer().getWorld());
        if (game != null) game.onPlayerInteract(event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityChangeBlock(EntityChangeBlockEvent event) {
        final Game game = Game.in(event.getEntity().getWorld());
        if (game == null) return;
        if (!(event.getEntity() instanceof FallingBlock falling)) return;
        if (falling.getBlockData().getMaterial() == Material.DRAGON_EGG) {
            if (event.getTo() == Material.DRAGON_EGG) {
                if (!game.onBallLand(falling, event.getBlock())) {
                    event.setCancelled(true);
                    falling.remove();
                }
            } else {
                event.setCancelled(true);
                falling.remove();
            }
        } else if (event.getTo().isAir()) {
            // Avoid falling blocks.
            event.setCancelled(true);
        }
    }

    /**
     * Avoid falling blocks.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onEntitySpawn(EntitySpawnEvent event) {
        final Game game = Game.in(event.getEntity().getWorld());
        if (game == null) return;
        if (game.isSpawning()) return;
        switch (event.getEntity().getType()) {
        case FALLING_BLOCK:
            event.setCancelled(true);
        default: break;
        }
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        if (plugin.getSaveTag().isEvent() && event.getPlayer().getWorld().equals(Bukkit.getWorlds().get(0))) {
            event.sidebar(PlayerHudPriority.HIGH, plugin.getHighscoreLines());
            return;
        }
        final Game game = Game.in(event.getPlayer().getWorld());
        if (game == null) return;
        game.onPlayerHud(event);
    }

    @EventHandler
    private void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        final Game game = Game.in(event.getPlayer().getWorld());
        if (game == null) return;
        game.onPlayerChangedWorld(event.getPlayer());
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        final Game game = Game.in(event.getPlayer().getWorld());
        if (game == null) return;
        game.onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    private void onMagicMapCursor(MagicMapCursorEvent event) {
        final Game game = Game.in(event.getPlayer().getWorld());
        if (game == null) return;
        game.onMagicMapCursor(event);
    }

    @EventHandler
    private void onChunkLoad(ChunkLoadEvent event) {
        final Game game = Game.in(event.getWorld());
        if (game == null) return;
        game.scanChunk(event.getChunk());
    }

    @EventHandler
    private void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        event.setCancelled(true);
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            final Game game = Game.in(event.getEntity().getWorld());
            if (game == null) return;
            if (game != null) {
                Bukkit.getScheduler().runTask(plugin, () -> game.teleport(player, game.getWorld().getSpawnLocation()));
            }
        }
    }

    @EventHandler
    private void onEntityExhaustion(EntityExhaustionEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }
}
