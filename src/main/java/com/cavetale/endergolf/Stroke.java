package com.cavetale.endergolf;

import com.cavetale.core.struct.Vec3i;
import java.time.Instant;
import lombok.Data;
import org.bukkit.Material;

@Data
public final class Stroke {
    private final Game game;
    private final GamePlayer gamePlayer;
    private final Vec3i ballVector;
    private final Instant startTime;
    private final Instant endTime;

    public void enable() {
        ballVector.toBlock(game.getWorld()).setType(Material.DRAGON_EGG, false);
    }

    public void disable() {
        ballVector.toBlock(game.getWorld()).setType(Material.AIR, false);
    }
}
