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
    private boolean obsolete;
    private State state = State.INIT;
    private Vec3i bounceVector;
    private Vec3i ballVector;
    private int distance;
    // Wait
    private Instant waitingSince;
    private Instant offlineSince;
    private Instant strokeCooldown = Instant.EPOCH;
    // Stroke
    private Stroke stroke;
    private int strokeCount;
    private GroundType groundType = GroundType.TEE;
    private float strokeProgress; // bossbar
    // Flight
    private FallingBlock flightBall;
    private Vector ballVelocity; // debugging
    // Finished
    private Instant finishedSince;
    private boolean winner;

    public enum State {
        INIT,
        SPECTATE,
        WAIT,
        STROKE,
        FLIGHT,
        FINISH,
        OBSOLETE,
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

    public String getPerformanceString() {
        final int par = game.getPar();
        if (strokeCount == par) {
            return "E";
        } else if (strokeCount < par) {
            return "-" + (par - strokeCount);
        } else {
            return "+" + (strokeCount - par);
        }
    }
}
