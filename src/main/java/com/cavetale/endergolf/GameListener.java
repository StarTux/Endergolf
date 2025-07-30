package com.cavetale.endergolf;

import com.cavetale.core.event.hud.PlayerHudEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;

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
        if (falling.getBlockData().getMaterial() != Material.DRAGON_EGG) return;
        if (event.getTo() == Material.DRAGON_EGG) {
            if (!game.onBallLand(falling, event.getBlock())) {
                event.setCancelled(true);
                falling.remove();
            }
        } else {
            event.setCancelled(true);
            falling.remove();
        }
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        final Game game = Game.in(event.getPlayer().getWorld());
        if (game == null) return;
        game.onPlayerHud(event);
    }
}
