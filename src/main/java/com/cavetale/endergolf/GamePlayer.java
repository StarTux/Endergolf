package com.cavetale.endergolf;

import com.cavetale.core.struct.Vec3i;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@Data
public final class GamePlayer {
    private final Game game;
    private final UUID uuid;
    private String name;
    private boolean playing;
    private boolean finished;
    private State state = State.INIT;
    private Vec3i ballVector;
    private int distance;
    // Wait
    private Instant waitingSince;
    private Instant offlineSince;
    private Instant strikeCooldown = Instant.EPOCH;
    // Strike
    private Strike strike;
    private int strikeCount;
    private GroundType groundType = GroundType.TEE;
    // Flight
    private FallingBlock flightBall;
    private Vector ballVelocity; // debugging

    public enum State {
        INIT,
        SPECTATE,
        WAIT,
        STRIKE,
        FLIGHT,
        ;
    }

    public GamePlayer(final Game game, final Player player) {
        this.game = game;
        this.uuid = player.getUniqueId();
        this.name = player.getName();
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
}
