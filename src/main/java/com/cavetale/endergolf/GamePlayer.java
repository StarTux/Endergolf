package com.cavetale.endergolf;

import com.cavetale.core.struct.Vec3i;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
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
    private final List<ItemDisplay> previewEntities = new ArrayList<>();
    private int previewEntitySize;
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

    public void prepareNewPreview() {
        previewEntitySize = 0;
    }

    private static final Transformation TRANS = new Transformation(new Vector3f(0f, 0f, 0f),
                                                                   new AxisAngle4f(0f, 0f, 0f, 0f),
                                                                   new Vector3f(0.125f, 0.125f, 0.125f),
                                                                   new AxisAngle4f(0f, 0f, 0f, 0f));

    public void pushPreviewEntity(Location location, Player player, boolean glowing) {
        final int index = previewEntitySize++;
        if (index < previewEntities.size()) {
            final ItemDisplay display = previewEntities.get(index);
            display.setGlowing(glowing);
            display.teleport(location);
        } else {
            previewEntities.add(location.getWorld().spawn(location, ItemDisplay.class, e -> {
                        e.setItemStack(new ItemStack(Material.NETHER_STAR));
                        e.setTransformation(TRANS);
                        e.setBillboard(ItemDisplay.Billboard.CENTER);
                        e.setVisibleByDefault(false);
                        player.sendActionBar("SPAWN " + e.getViewRange());
                        e.setViewRange(512f);
                        e.setBrightness(new ItemDisplay.Brightness(15, 15));
                        e.setGlowing(glowing);
                        player.showEntity(game.getPlugin(), e);
                    }));
        }
    }

    public void trimPreviewEntities() {
        while (previewEntities.size() > previewEntitySize) {
            final ItemDisplay display = previewEntities.removeLast();
            display.remove();
        }
    }

    public void clearPreviewEntities() {
        for (ItemDisplay display : previewEntities) {
            display.remove();
        }
        previewEntities.clear();
        previewEntitySize = 0;
    }
}
