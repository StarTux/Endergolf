package com.cavetale.endergolf;

import com.cavetale.core.struct.Vec3i;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

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
    // Wind
    private Location windLocation;
    private Vector windVector;
    private Component windComponent;

    public enum State {
        INIT,
        SPECTATE,
        WAIT,
        STROKE,
        FLIGHT,
        FINISH,
        DNF, // way over par
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

    public void updateWind() {
        windLocation = new Location(game.getWorld(), 0, 0, 0);
        windLocation.setPitch(0);
        final double strength = game.getRandom().nextDouble() * game.getRandom().nextDouble() * 0.55;
        final float yaw = Location.normalizeYaw(game.getRandom().nextFloat() * 360.0f);
        windLocation.setYaw(yaw);
        windVector = windLocation.getDirection().multiply(strength);
        // Component
        final int kmh = (int) Math.round(strength * 20.0 * 60.0 * 60.0 * 0.001);
        windComponent = textOfChildren(text(kmh, WHITE), text(tiny("km/h"), DARK_GRAY));
    }
}
