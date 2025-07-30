package com.cavetale.endergolf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

@RequiredArgsConstructor
public final class Games {
    private final EndergolfPlugin plugin;
    private BukkitTask task;
    private final Map<String, Game> worldGameMap = new HashMap<>();

    public void enable() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Game game : List.copyOf(worldGameMap.values())) {
            game.disable();
        }
        worldGameMap.clear();
    }

    public void addAndEnable(Game game) {
        game.enable();
        worldGameMap.put(game.getWorldName(), game);
    }

    public void removeAndDisable(Game game) {
        worldGameMap.remove(game.getWorldName());
        game.disable();
    }

    public Game getGameIn(World world) {
        return worldGameMap.get(world.getName());
    }

    private void tick() {
        for (Game game : List.copyOf(worldGameMap.values())) {
            if (game.isFinished()) {
                removeAndDisable(game);
            } else {
                try {
                    game.tick();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Ticking game " + game.getWorldName(), e);
                    removeAndDisable(game);
                }
            }
        }
    }
}
