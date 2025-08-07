package com.cavetale.endergolf;

import com.cavetale.afk.AFKPlugin;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.winthier.creative.vote.MapVote;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
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
        int activeGameCount = 0;
        for (Game game : List.copyOf(worldGameMap.values())) {
            activeGameCount += 1;
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
        final World lobbyWorld = Bukkit.getWorlds().get(0);
        int notAfk = 0;
        for (Player player : lobbyWorld.getPlayers()) {
            if (!AFKPlugin.isAfk(player)) notAfk += 1;
        }
        if (MapVote.isActive(MinigameMatchType.ENDERGOLF)) {
            if (activeGameCount > 0 || plugin.getSaveTag().isPause() || notAfk <= 1) {
                MapVote.stop(MinigameMatchType.ENDERGOLF);
            }
        } else {
            if (activeGameCount == 0 && !plugin.getSaveTag().isPause() && notAfk > 1) {
                MapVote.start(MinigameMatchType.ENDERGOLF, mapVote -> {
                        mapVote.setTitle(plugin.getTitle());
                        mapVote.setCallback(mapVoteResult -> {
                                final Game game = new Game(plugin, mapVoteResult.getBuildWorldWinner());
                                for (Player player : lobbyWorld.getPlayers()) {
                                    game.addPlayer(player);
                                }
                                game.setWorldAndEnable(mapVoteResult.getLocalWorldCopy());
                            });
                        mapVote.setLobbyWorld(lobbyWorld);
                        mapVote.setAvoidRepetition(0);
                    });
            }
        }
    }
}
