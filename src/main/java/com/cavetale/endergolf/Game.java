package com.cavetale.endergolf;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.event.minigame.MinigameMatchCompleteEvent;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.magicmap.event.MagicMapCursorEvent;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Entities;
import com.winthier.creative.BuildWorld;
import com.winthier.creative.file.Files;
import com.winthier.creative.review.MapReview;
import io.papermc.paper.datacomponent.DataComponentTypes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Data;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCursor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import static com.cavetale.core.font.Unicode.tiny;
import static io.papermc.paper.datacomponent.item.ItemLore.lore;
import static io.papermc.paper.datacomponent.item.LodestoneTracker.lodestoneTracker;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.kyori.adventure.title.Title.Times.times;
import static net.kyori.adventure.title.Title.title;

/**
 * To create a valid game:
 *
 * - Call the constructor.
 * - Add any number of players.
 * - Call loadWorldAndEnableLater.
 *
 * The game will then be ticked.
 */
@Data
public final class Game {
    private final EndergolfPlugin plugin;
    private final BuildWorld buildWorld;
    private MapReview mapReview;
    private World world;
    private final List<Cuboid> forceLoadedAreas = new ArrayList<>();
    private final Map<UUID, GamePlayer> players = new HashMap<>();
    private final Set<Vec2i> scannedChunks = new HashSet<>();
    private boolean finished;
    private State state = State.INIT;
    private Vec3i teeVector;
    private Cuboid holeArea;
    private Location holeLocation;
    private int par;
    private int totalPlaying;
    private int totalNotFinished;
    private UUID logTarget;
    // Updated in tick()
    private Instant now;
    // Countdown
    private Instant countdownStart;
    private Instant countdownStop;
    private long countdownSeconds;
    // Play
    private boolean doDebugGravity = false;
    // End
    private Instant endStart;
    private Instant endStop;
    private long endSeconds;
    private float endProgress;

    public enum State {
        INIT,
        COUNTDOWN,
        PLAY,
        END,
        ;
    }

    public static Game in(World world) {
        return EndergolfPlugin.endergolfPlugin().getGames().getGameIn(world);
    }

    public GamePlayer addPlayer(Player player) {
        final GamePlayer gamePlayer = new GamePlayer(this, player);
        gamePlayer.setPlaying(true);
        players.put(gamePlayer.getUuid(), gamePlayer);
        return gamePlayer;
    }

    public GamePlayer getGamePlayer(UUID uuid) {
        return players.get(uuid);
    }

    public GamePlayer getGamePlayer(Player player) {
        return getGamePlayer(player.getUniqueId());
    }

    public List<Player> getPresentPlayers() {
        if (world == null) return List.of();
        return world.getPlayers();
    }

    public void loadWorldAndEnableLater() {
        if (world != null) {
            throw new IllegalStateException("[" + getWorldName() + "] World already loaded");
        }
        buildWorld.makeLocalCopyAsync(loadedWorld -> {
                world = loadedWorld;
                prepareWorld();
                EndergolfPlugin.endergolfPlugin().getGames().addAndEnable(this);
            });
    }

    private void prepareWorld() {
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.FALL_DAMAGE, false);
        world.setGameRule(GameRule.DROWNING_DAMAGE, false);
        world.setGameRule(GameRule.LOCATOR_BAR, true);
    }

    private void loadAreas() {
        final AreasFile areasFile = AreasFile.load(world, "Endergolf");
        if (areasFile == null) {
            throw new IllegalStateException("No Endergolf areas file");
        }
        for (Area area : areasFile.find("tee")) {
            if (teeVector != null) {
                throw new IllegalStateException("Multiple tee areas");
            }
            this.teeVector = area.getMin();
        }
        for (Area area : areasFile.find("hole")) {
            if (holeArea != null) {
                throw new IllegalStateException("Multiple hole areas");
            }
            this.holeArea = area.toCuboid();
        }
        if (teeVector == null) {
            throw new IllegalStateException("No tee vector");
        }
        if (holeArea == null) {
            throw new IllegalStateException("No hole area");
        }
        for (Area area : areasFile.find("load")) {
            forceLoadedAreas.add(area.toCuboid());
        }
    }

    public boolean ifLogPlayer(Consumer<Player> callback) {
        if (logTarget == null) return false;
        final Player player = Bukkit.getPlayer(logTarget);
        if (player == null) return false;
        callback.accept(player);
        return true;
    }

    public void log(String msg) {
        plugin.getLogger().info("[" + getWorldName() + "] " + msg);
        ifLogPlayer(p -> p.sendMessage(text("[log] " + msg, YELLOW)));
    }

    public void warn(String msg) {
        plugin.getLogger().severe("[" + getWorldName() + "] " + msg);
        ifLogPlayer(p -> p.sendMessage(text("[log] " + msg, RED)));
    }

    public void enable() {
        if (world == null) {
            throw new IllegalStateException("World not loaded");
        }
        try {
            loadAreas();
        } catch (IllegalStateException ise) {
            ifLogPlayer(p -> p.sendMessage(text("[log] " + ise.getMessage(), DARK_RED)));
            throw new IllegalStateException(getWorldName(), ise);
        }
        final Vec3i holeCenter = holeArea.getCenter();
        holeLocation = holeCenter.toCenterFloorLocation(world);
        // Force load all chunks
        final Set<Vec2i> forceLoadedChunks = new HashSet<>();
        forceLoadedChunks.add(teeVector.blockToChunk());
        forceLoadedChunks.addAll(holeArea.blockToChunk().enumerateHorizontally());
        for (Cuboid forceLoadedArea : forceLoadedAreas) {
            forceLoadedChunks.addAll(forceLoadedArea.blockToChunk().enumerateHorizontally());
        }
        log("Force loading " + forceLoadedChunks.size() + " chunks");
        for (Vec2i c : forceLoadedChunks) {
            world.getChunkAtAsync(c.x, c.z, (Consumer<Chunk>) chunk -> {
                    chunk.addPluginChunkTicket(plugin);
                    scanChunk(chunk);
                });
        }
        final Vec2i holeChunk = holeCenter.blockToChunk();
        world.getChunkAtAsync(holeChunk.x, holeChunk.z, (Consumer<Chunk>) chunk -> {
                holeLocation.setYaw(180f); // MagicMap Icon
                world.spawn(holeLocation, ArmorStand.class, e -> {
                        e.setInvisible(true);
                        e.setGravity(false);
                        e.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE).setBaseValue(60.000);
                    });
                chunk.addPluginChunkTicket(plugin);
                scanChunk(chunk);
            });
        // Compute distance and par
        final int distance = (int) Math.round(teeVector.distance(holeArea.getCenter()));
        if (par == 0) {
            par = Math.max(3, (distance - 1) / 50 + 1);
        }
        for (GamePlayer gp : List.copyOf(players.values())) {
            final Player player = gp.getPlayer();
            if (player == null) {
                players.remove(gp.getUuid());
            }
            gp.setBallVector(teeVector);
            gp.setDistance(distance);
            teleport(player, directAtHole(world.getSpawnLocation()));
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
            player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE).setBaseValue(0.0);
        }
        if (players.isEmpty()) {
            throw new IllegalStateException("[" + getWorldName() + "] No players");
        }
        mapReview = MapReview.start(world, buildWorld);
        buildWorld.announceMap(world);
        setState(State.COUNTDOWN);
    }

    public void scanChunk(Chunk chunk) {
        final Vec2i vector = Vec2i.of(chunk);
        if (!scannedChunks.add(vector)) return;
        for (BlockState blockState : chunk.getTileEntities()) {
            if (blockState instanceof Sign sign) {
                final String firstLine = plainText().serialize(sign.getSide(Side.FRONT).line(0));
                final String secondLine = plainText().serialize(sign.getSide(Side.FRONT).line(1));
                if (!firstLine.equalsIgnoreCase("[par]")) continue;
                try {
                    par = Integer.parseInt(secondLine);
                } catch (IllegalArgumentException iae) {
                    warn("Bad par sign: " + secondLine + " at " + Vec3i.of(sign.getBlock()));
                }
                log("Setting par to " + par + " via sign at " + Vec3i.of(sign.getBlock()));
            }
        }
    }

    public boolean skip() {
        switch (state) {
        case COUNTDOWN:
            countdownStop = now;
            return true;
        default:
            return false;
        }
    }

    public void disable() {
        if (world != null) {
            MapReview.stop(world);
            world.removePluginChunkTickets(plugin);
            for (Player player : world.getPlayers()) {
                plugin.warpToLobby(player);
            }
            Files.deleteWorld(world);
            world = null;
        }
    }

    /**
     * Transition from the current state to a new state.
     */
    public void setState(final State newState) {
        final State oldState = this.state;
        log("State " + oldState + " => " + newState);
        // Exit
        switch (oldState) {
        default: break;
        }
        // Enter
        switch (newState) {
        case COUNTDOWN:
            countdownStart = Instant.now();
            countdownStop = countdownStart.plus(Duration.ofSeconds(12));
            countdownSeconds = 10;
            break;
        case PLAY:
            for (Player player : getPresentPlayers()) {
                player.showTitle(title(empty(),
                                       text("Game Start", GREEN, ITALIC),
                                       times(Duration.ZERO,
                                             Duration.ofSeconds(2),
                                             Duration.ZERO)));
                player.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.MASTER, 0.5f, 1.5f);
            }
            for (GamePlayer gp : players.values()) {
                if (!gp.isPlaying()) {
                    gp.setState(GamePlayer.State.SPECTATE);
                } else {
                    gp.setState(GamePlayer.State.WAIT);
                    gp.setWaitingSince(now);
                }
            }
            break;
        case END:
            determineWinners();
            endStart = now;
            endSeconds = 30L;
            endStop = now.plus(Duration.ofSeconds(endSeconds));
            endProgress = 1f;
            break;
        default: break;
        }
        // Set
        this.state = newState;
    }

    public void tick() {
        now = Instant.now();
        switch (state) {
        case COUNTDOWN: {
            if (now.isAfter(countdownStop)) {
                setState(State.PLAY);
                return;
            }
            final long secondsLeft = Duration.between(now, countdownStop).toSeconds();
            if (secondsLeft < countdownSeconds) {
                for (Player player : getPresentPlayers()) {
                    player.showTitle(title(text(countdownSeconds, GREEN),
                                           text("Get Ready", GREEN)));
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.MASTER, 0.5f, 2f - (float) countdownSeconds * 0.1f);
                }
                countdownSeconds = secondsLeft;
            }
            break;
        }
        case PLAY:
            int notObsolete = 0;
            final List<GamePlayer> waitingPlayers = new ArrayList<>();
            final List<Stroke> strokes = new ArrayList<>();
            // Check on all players and tick them
            totalPlaying = 0;
            totalNotFinished = 0;
            for (GamePlayer gp : players.values()) {
                tickPlay(gp);
                if (gp.isPlaying()) {
                    totalPlaying += 1;
                    if (!gp.isObsolete()) {
                        notObsolete += 1;
                    }
                    if (!gp.isFinished()) {
                        totalNotFinished += 1;
                    }
                }
                if (gp.getState() == GamePlayer.State.WAIT && gp.getWaitingSince() != null && gp.getPlayer() != null && now.isAfter(gp.getStrokeCooldown())) {
                    waitingPlayers.add(gp);
                }
                if (gp.getStroke() != null) {
                    strokes.add(gp.getStroke());
                }
            }
            if (notObsolete == 0) {
                setState(State.END);
                return;
            }
            // Give all waiting players a chance to stroke
            waitingPlayers.sort(Comparator.comparing(GamePlayer::getWaitingSince));
            final int blowUpCount = Math.max(par + 10, par * 2);
            for (GamePlayer gp : waitingPlayers) {
                if (gp.getStrokeCount() > blowUpCount) {
                    gp.setState(GamePlayer.State.DNF);
                    gp.setFinishedSince(now);
                    final Player player = gp.getPlayer();
                    if (player != null) {
                        player.showTitle(title(text("Blow Up", DARK_RED),
                                               text("Disqualified", DARK_RED),
                                               times(Duration.ofSeconds(1),
                                                     Duration.ofSeconds(3),
                                                     Duration.ofSeconds(1))));
                    }
                    final Component message = text(player.getName() + " had a bad round and was disqualified", DARK_RED);
                    for (Player p : getPresentPlayers()) {
                        p.sendMessage(message);
                    }
                    continue;
                }
                boolean tooClose = false;
                for (Stroke stroke : strokes) {
                    // Distance must be at least 4
                    if (stroke.getBallVector().distanceSquared(gp.getBallVector()) < 16.0) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;
                final Stroke stroke = new Stroke(this, gp, gp.getBallVector(), now, now.plus(Duration.ofSeconds(45)));
                strokes.add(stroke);
                stroke.enable();
                gp.setStroke(stroke);
                gp.setState(GamePlayer.State.STROKE);
                final Player player = gp.getPlayer();
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    teleport(player, directAtHole(gp.getBallVector().toCenterFloorLocation(world).add(0.0, 1.0, 0.0)));
                    player.setGameMode(GameMode.ADVENTURE);
                    player.getInventory().clear();
                    for (GolfClub club : GolfClub.values()) {
                        player.getInventory().setItem(club.ordinal(), club.createItemStack());
                    }
                    player.getInventory().addItem(makeBallCompass());
                    player.getInventory().setItemInOffHand(Mytems.MAGIC_MAP.createItemStack());
                }
                updateBallCompass(player, gp);
            }
            break;
        case END:
            if (now.isAfter(endStop)) {
                finished = true;
            } else {
                final Duration totalEndTime = Duration.between(endStart, endStop);
                final Duration remainingEndTime = Duration.between(now, endStop);
                endSeconds = remainingEndTime.toSeconds();
                endProgress = (float) remainingEndTime.toMillis() / (float) totalEndTime.toMillis();
            }
            break;
        default: break;
        }
    }

    public static double estimateGravity(final double currentY) {
        return Math.max(-3.92, (currentY - 0.04) * 0.98);
    }

    private void tickPlay(GamePlayer gp) {
        final Player player = gp.getPlayer();
        if (player == null) {
            // Player is offline
            // Record offline time
            if (gp.getOfflineSince() == null) {
                gp.setOfflineSince(Instant.now());
            }
            // Cancel current stroke if any
            if (gp.getState() == GamePlayer.State.STROKE) {
                gp.getStroke().disable();
                gp.setStroke(null);
            }
            // Remove from game
            if (Duration.between(gp.getOfflineSince(), now).toSeconds() > 60L) {
                log("Removing " + gp.getName() + " because they have been offline too long");
                gp.setPlaying(false);
                gp.setState(GamePlayer.State.SPECTATE);
                if (gp.getFlightBall() != null) {
                    gp.getFlightBall().remove();
                    gp.setFlightBall(null);
                }
            }
            return;
        }
        assert player != null;
        switch (gp.getState()) {
        case WAIT:
            // Switching from WAIT to STROKE is handled outside this
            // function because we must order all waiting players by
            // their waitingSince timestamp.
            break;
        case STROKE:
            tickStroke(gp, player, gp.getStroke());
            break;
        case FLIGHT:
            if (gp.getFlightBall().isDead()) {
                // The ball disappeared
                final Location lastLocation = gp.getFlightBall().getLocation();
                final Vec3i vector = Vec3i.of(lastLocation);
                if (vector.y <= world.getMinHeight()) {
                    // Void
                    player.showTitle(title(text("Void", DARK_PURPLE, BOLD),
                                           text("Your ball fell out of the world", RED),
                                           times(Duration.ZERO,
                                                 Duration.ofSeconds(3),
                                                 Duration.ofSeconds(1))));
                    player.sendMessage(text("Your ball fell out of the world", RED));
                    gp.setState(GamePlayer.State.WAIT);
                    gp.setWaitingSince(now);
                    gp.setStrokeCooldown(now.plus(Duration.ofSeconds(5)));
                    gp.setFlightBall(null);
                    gp.setBallVelocity(null);
                } else if (!vector.toBlock(world).isEmpty() && !Tag.REPLACEABLE.isTagged(vector.toBlock(world).getType())) {
                    // Non-replaceable block, such as carpet.
                    Block block = vector.toBlock(world);
                    switch (block.getType()) {
                    case SOUL_SAND:
                    case SOUL_SOIL:
                    case FARMLAND:
                    case DIRT_PATH:
                    case HONEY_BLOCK:
                    case SLIME_BLOCK:
                        block = block.getRelative(0, 1, 0);
                    default: break;
                    }
                    if (onBallLand(gp, gp.getFlightBall(), block)) {
                        block.setType(Material.DRAGON_EGG, false);
                    }
                } else {
                    final Location newLocation = vector.toCenterFloorLocation(world);
                    plugin.getLogger().info("[ " + getWorldName() + "] Respawning ball for " + gp.getName() + " at " + vector);
                    gp.setFlightBall(spawnBall(newLocation, new Vector()));
                    gp.setBallVelocity(gp.getFlightBall().getVelocity());
                }
            } else {
                final Vector oldVelocity = gp.getBallVelocity();
                final Vector newVelocity = gp.getFlightBall().getVelocity();
                gp.setBallVelocity(newVelocity);
                if (doDebugGravity && gp.getFlightBall().getTicksLived() > 0) {
                    final double estimateY = estimateGravity(oldVelocity.getY());
                    final double diffY = Math.abs(estimateY - newVelocity.getY());
                    if (diffY > 0.01) {
                        plugin.getLogger().warning(gp.getFlightBall().getTicksLived() + " Estimate wrong: "
                                                   + oldVelocity.getY() + " => " + newVelocity.getY()
                                                   + " diff " + (oldVelocity.getY() - newVelocity.getY())
                                                   + " instead of " + estimateY + " off by " + diffY);
                    } else {
                        plugin.getLogger().info("OK");
                    }
                }
            }
            break;
        case FINISH: case DNF: {
            final Duration finishTime = Duration.between(gp.getFinishedSince(), now);
            if (finishTime.toSeconds() >= 5) {
                mapReview.remindOnce(player);
            }
            if (finishTime.toSeconds() >= 10) {
                gp.setState(GamePlayer.State.OBSOLETE);
                gp.setObsolete(true);
            }
            break;
        }
        case OBSOLETE: break;
        default: break;
        }
    }

    /**
     * Here we give the player a preview of where their ball might fly
     * if they hit it.
     * We also watch over a timeout.
     */
    private void tickStroke(GamePlayer gp, Player player, Stroke stroke) {
        final Duration totalStrokeTime = Duration.between(stroke.getStartTime(), stroke.getEndTime());
        final Duration remainingStrokeTime = Duration.between(now, stroke.getEndTime());
        gp.setStrokeProgress((float) remainingStrokeTime.toMillis() / (float) totalStrokeTime.toMillis());
        if (now.isAfter(stroke.getEndTime())) {
            // Timeout
            stroke.disable();
            gp.setStroke(null);
            gp.setPlaying(false);
            gp.setObsolete(true);
            gp.setState(GamePlayer.State.OBSOLETE);
            player.showTitle(title(text("Timeout", DARK_RED),
                                   text("Disqualified", DARK_RED),
                                   times(Duration.ofSeconds(1),
                                         Duration.ofSeconds(3),
                                         Duration.ofSeconds(1))));
            final Component message = text(player.getName() + " timed out and was disqualified", DARK_RED);
            for (Player p : getPresentPlayers()) {
                p.sendMessage(message);
            }
            return;
        }
        final RayTraceResult rayTrace = player.rayTraceBlocks(4.0);
        if (rayTrace == null || rayTrace.getHitBlock() == null || !stroke.getBallVector().equals(Vec3i.of(rayTrace.getHitBlock()))) {
            return;
        }
        final GolfClub club = GolfClub.ofMaterial(player.getInventory().getItemInMainHand().getType());
        if (club == null) return;
        final Vector velocity = getBallVelocity(rayTrace.getHitPosition(), stroke.getBallVector(), club, gp.getGroundType());
        Location location = stroke.getBallVector().toCenterFloorLocation(world);
        for (int i = 0; i < 100; i += 1) {
            player.spawnParticle(Particle.CURRENT_DOWN, location, 1, 0.0, 0.0, 0.0, 0.0);
            if (velocity.length() >= 2.0) {
                Location location2 = location.clone().add(velocity.clone().multiply(0.2));
                player.spawnParticle(Particle.CURRENT_DOWN, location2, 1, 0.0, 0.0, 0.0, 0.0);
            }
            location = location.add(velocity);
            velocity.setY(estimateGravity(velocity.getY()));
            final Block block = location.getBlock();
            if (!block.isEmpty() && block.getType() != Material.DRAGON_EGG) {
                final Vector locationVector = location.toVector();
                boolean doesCollide = false;
                for (BoundingBox bb : block.getCollisionShape().getBoundingBoxes()) {
                    if (bb.contains(locationVector)) {
                        doesCollide = true;
                        break;
                    }
                }
                if (doesCollide) break;
            }
        }
    }

    public String getWorldName() {
        if (world == null) return "null";
        return world.getName();
    }

    public void teleport(Player player, Location location) {
        player.eject();
        player.leaveVehicle();
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.teleport(location);
        player.setFallDistance(0);
        player.setVelocity(new Vector());
    }

    public Location directAtHole(Location location) {
        final Vector direction = holeLocation.toVector().subtract(location.toVector());
        location.setDirection(direction);
        return location;
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.hasBlock() && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
            return;
        }
        if (event.hasItem() && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) && event.getItem().getType() == Material.COMPASS) {
            event.setCancelled(true);
            onPlayerUseCompass(event.getPlayer());
            return;
        }
        if (!event.hasBlock() || (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            // Adventure GameMode appears to always send
            // RIGHT_CLICK_BLOCK followed by LEFT_CLICK_BLOCK when the
            // player right clicks a block.
            return;
        }
        if (!event.hasItem()) {
            return;
        }
        final Player player = event.getPlayer();
        final GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isPlaying() || gp.getState() != GamePlayer.State.STROKE) {
            return;
        }
        final Vec3i clickVector = Vec3i.of(event.getClickedBlock());
        if (!clickVector.equals(gp.getStroke().getBallVector())) {
            return;
        }
        final RayTraceResult rayTrace = player.rayTraceBlocks(4.0);
        if (rayTrace == null || rayTrace.getHitBlock() == null || !clickVector.equals(Vec3i.of(rayTrace.getHitBlock()))) {
            return;
        }
        final GolfClub club = GolfClub.ofMaterial(event.getItem().getType());
        if (club == null) {
            return;
        }
        gp.getStroke().disable();
        gp.setStroke(null);
        gp.setState(GamePlayer.State.FLIGHT);
        gp.setStrokeCount(gp.getStrokeCount() + 1);
        final Vector velocity = getBallVelocity(rayTrace.getHitPosition(), clickVector, club, gp.getGroundType());
        final Location ballLocation = clickVector.toCenterFloorLocation(world);
        gp.setFlightBall(spawnBall(ballLocation, velocity));
        gp.setBallVelocity(velocity);
        world.playSound(ballLocation, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.MASTER, 1f, 0.5f);
        world.spawnParticle(Particle.BLOCK, ballLocation.clone().add(0, 0.5, 0), 32, 0.4, 0.125, 0.4, 0.0, Material.DRAGON_EGG.createBlockData());
    }

    /**
     * Called right as a dragon egg lands.  We try to find out who it
     * belongs to and update their state.
     *
     * @param falling the falling dragon egg entity
     * @param block where it is landing
     * @return true if the egg should stay, false if it should be
     *   removed.
     */
    public boolean onBallLand(FallingBlock falling, Block block) {
        for (GamePlayer gp : players.values()) {
            if (gp.getState() == GamePlayer.State.FLIGHT && falling.equals(gp.getFlightBall())) {
                return onBallLand(gp, falling, block);
            }
        }
        return false;
    }

    /**
     * Called right as the ball lands on a certain block, before said
     * block is changed into a dragon egg.
     *
     * Either by onEntityChangeBlock() in GameListener, or by
     * tickPlay() here.
     *
     * @param gp the GamePlayer, who must be in the FLIGHT state
     * @param falling the falling dragon egg entity
     * @param block the Block where it is landing
     * @return true if the egg should stay, false if it should be
     *   removed.
     *
     * As a side effect, the GamePlayer state may be updated.
     */
    public boolean onBallLand(GamePlayer gp, FallingBlock falling, Block block) {
        final Vec3i blockVector = Vec3i.of(block);
        if (holeArea.contains(blockVector)) {
            // Hole In (or Out), Finish
            gp.setFinished(true);
            gp.setBallVector(blockVector);
            gp.setBounceVector(null);
            gp.setState(GamePlayer.State.FINISH);
            gp.setFinishedSince(now);
            falling.remove();
            gp.setFlightBall(null);
            final int strokes = gp.getStrokeCount();
            log("" + gp.getName()
                                    + " finished with " + strokes + "/" + par + " strokes: "
                                    + gp.getPerformanceString());
            final Component term = GolfScoringTerm.getComponent(strokes, par);
            final Component message = textOfChildren(text(gp.getName(), WHITE),
                                                     text(" completed ", GRAY),
                                                     text(buildWorld.getName(), WHITE),
                                                     text(" in ", GRAY),
                                                     text(strokes, WHITE),
                                                     text(" strokes on a ", GRAY),
                                                     text("par-" + par, WHITE),
                                                     text(": "),
                                                     term);
            for (Player p : getPresentPlayers()) {
                p.sendMessage(empty());
                p.sendMessage(message);
                p.sendMessage(empty());
            }
            final Player player = gp.getPlayer();
            if (player != null) {
                player.getInventory().clear();
                player.showTitle(title(term,
                                       textOfChildren(text("Hole: ", GRAY),
                                                      text(strokes, WHITE),
                                                      text(" (Par ", GRAY),
                                                      text(par, WHITE),
                                                      text(") | ", GRAY),
                                                      text(gp.getPerformanceString(), WHITE)),
                                       times(Duration.ofSeconds(1),
                                             Duration.ofSeconds(3),
                                             Duration.ofSeconds(1))));
                player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1f, 2f);
            }
            return false;
        }
        final GroundType ground = GroundType.at(block);
        final double bounceY = gp.getBallVelocity().getY() * -1 * ground.getBounciness();
        if (ground.isReset()) {
            // Return to sender
            log("" + gp.getName() + " reset at " + blockVector);
            gp.setFlightBall(null);
            gp.setState(GamePlayer.State.WAIT);
            gp.setWaitingSince(now);
            gp.setStrokeCooldown(now.plus(Duration.ofSeconds(5)));
            final Player player = gp.getPlayer();
            if (player != null) {
                player.showTitle(title(ground.getDisplayComponent(),
                                       text("Try Again", RED),
                                       times(Duration.ZERO,
                                             Duration.ofSeconds(2),
                                             Duration.ofSeconds(2))));
                player.sendMessage(textOfChildren(text("Your ball landed in the ", RED),
                                                  ground.getDisplayComponent()));
                switch (ground) {
                case WATER:
                    player.playSound(player, Sound.AMBIENT_UNDERWATER_ENTER, SoundCategory.MASTER, 1f, 1f);
                    player.playSound(player, Sound.ENTITY_GENERIC_SPLASH, SoundCategory.MASTER, 1f, 1f);
                    break;
                case LAVA:
                    player.playSound(player, Sound.BLOCK_LAVA_POP, SoundCategory.MASTER, 1f, 1f);
                    player.playSound(player, Sound.ITEM_BUCKET_FILL_LAVA, SoundCategory.MASTER, 1f, 1f);
                    player.playSound(player, Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.MASTER, 1f, 1f);
                    break;
                default: break;
                }
            }
            return false;
        } else if (bounceY < 0.1 || blockVector.equals(gp.getBounceVector())) {
            // Land for real
            log("" + gp.getName() + " land at " + blockVector);
            final int distance = (int) Math.round(blockVector.distance(gp.getBallVector()));
            gp.setBallVector(blockVector);
            gp.setBounceVector(null);
            gp.setDistance((int) Math.round(gp.getBallVector().distance(holeArea.getCenter())));
            gp.setFlightBall(null);
            gp.setState(GamePlayer.State.WAIT);
            gp.setWaitingSince(now);
            gp.setGroundType(ground);
            final Player player = gp.getPlayer();
            if (player != null) {
                updateBallCompass(player, gp);
                player.sendMessage(textOfChildren(text("Your ball landed in the ", WHITE),
                                                  ground.getDisplayComponent(),
                                                  text(" " + distance + " yard" + (distance == 1 ? "" : "s") + " away", WHITE)));
                player.showTitle(title(ground.getDisplayComponent(),
                                       textOfChildren(text("distance ", GRAY), text(distance, ground.getColor()), text(" yard" + (distance == 1 ? "" : "s"), GRAY)),
                                       times(Duration.ZERO,
                                             Duration.ofSeconds(1),
                                             Duration.ofSeconds(2))));
                player.playSound(player, Sound.BLOCK_STONE_HIT, SoundCategory.MASTER, 1f, 0.5f);
                player.playSound(player, Sound.BLOCK_SAND_HIT, SoundCategory.MASTER, 1f, 1.5f);
            }
            final Material mat = !block.isEmpty()
                ? block.getType()
                : block.getRelative(0, -1, 0).getType();
            world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 32, 0.4, 0.125, 0.4, 0.0, mat.createBlockData());
            return true;
        } else {
            // Bounce
            log("" + gp.getName() + " bounce at " + blockVector + " by=" + bounceY);
            Vector newVelocity = gp.getBallVelocity().clone();
            newVelocity.setY(bounceY);
            gp.setBounceVector(blockVector);
            final FallingBlock newBall = spawnBall(falling.getLocation(), newVelocity);
            gp.setFlightBall(newBall);
            gp.setBallVelocity(newVelocity);
            final Player player = gp.getPlayer();
            if (player != null) {
                player.playSound(player, Sound.BLOCK_STONE_HIT, SoundCategory.MASTER, 1f, 1f);
                player.playSound(player, Sound.BLOCK_SAND_HIT, SoundCategory.MASTER, 1f, 1f);
                final Material mat = !block.isEmpty()
                    ? block.getType()
                    : block.getRelative(0, -1, 0).getType();
                world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 16, 0.4, 0.125, 0.4, 0.0, mat.createBlockData());
            }
            return false;
        }
    }

    /**
     * Get the direction that the ball will take after being struck at
     * the given point.
     *
     * @param hitPoint where the ball was hit
     * @param ballVector the block coordinates of the ball
     * @return the direction as a unit vector
     */
    public Vector getBallVelocity(Vector hitPoint, Vec3i ballVector, GolfClub club, GroundType ground) {
        final double millis = (double) (System.nanoTime() / 1_000_000L);
        final Vector ballCenter = ballVector.toVector().add(new Vector(0.5, 1.125, 0.5));
        Vector direction = ballCenter.subtract(hitPoint).normalize();
        if (ground.getWiggle() > 0) {
            Location location = new Location(world, 0, 0, 0);
            location.setDirection(direction);
            final double timef = 0.001;
            final float scalef = 0.5f * (float) (Math.sin(millis * 0.00125 + 11) + 1) * ground.getWiggleSpeed();
            location.setYaw(location.getYaw() + (float) Math.cos(millis * timef) * 22.5f * scalef * ground.getWiggle());
            location.setPitch(location.getPitch() + (float) Math.sin(millis * timef) * 11.25f * scalef * ground.getWiggle());
            direction = location.getDirection();
        }
        final double timeFactor = 0.5 * (Math.sin(millis * 0.002) + 1.0);
        final double strength = (club.getStrength() - (timeFactor * club.getStrengthFactor()))
            * ground.getStrengthFactor();
        return direction.multiply(strength);
    }

    public FallingBlock spawnBall(Location location, Vector velocity) {
        return world.spawn(location, FallingBlock.class, e -> {
                e.setBlockData(Material.DRAGON_EGG.createBlockData());
                e.setVelocity(velocity);
                Entities.setTransient(e);
            });
    }

    public void onPlayerHud(PlayerHudEvent event) {
        final List<Component> sidebar = new ArrayList<>();
        final GamePlayer gp = getGamePlayer(event.getPlayer());
        sidebar.add(textOfChildren(text(tiny("playing "), GRAY), text(totalNotFinished, WHITE), text("/", DARK_GRAY), text(totalPlaying, WHITE)));
        if (gp != null && gp.isPlaying()) {
            final int strokes = gp.getStrokeCount();
            sidebar.add(textOfChildren(text(tiny("hole "), GRAY),
                                       text(strokes, WHITE),
                                       text(tiny(" (par "), GRAY),
                                       text(par, WHITE),
                                       text(")", GRAY)));
            if (!gp.isFinished()) {
                sidebar.add(textOfChildren(text(tiny("distance "), GRAY), text(gp.getDistance(), WHITE), text("yd", DARK_GRAY)));
                sidebar.add(textOfChildren(text(tiny("ground "), GRAY), gp.getGroundType().getDisplayComponent()));
            }
            switch (gp.getState()) {
            case WAIT:
                event.bossbar(PlayerHudPriority.HIGH, text("Please Wait for Others", GRAY), BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, 0f);
                break;
            case STROKE:
                event.bossbar(PlayerHudPriority.HIGH, text("You're Up", GREEN), BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_20, gp.getStrokeProgress());
                break;
            case FLIGHT:
                event.bossbar(PlayerHudPriority.HIGH, text("Look at It Go!", GREEN), BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, 1f);
                break;
            default: break;
            }
        }
        if (state == State.END) {
            sidebar.add(textOfChildren(text(tiny("game over "), GRAY), text(endSeconds, WHITE)));
            event.bossbar(PlayerHudPriority.HIGH, text("Game Over", RED), BossBar.Color.RED, BossBar.Overlay.NOTCHED_20, endProgress);
        }
        event.sidebar(PlayerHudPriority.HIGH, sidebar);
    }

    public void onPlayerChangedWorld(Player player) {
        player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE).setBaseValue(0.0);
    }

    public void onPlayerJoin(Player player) {
        player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE).setBaseValue(0.0);
    }

    public void onMagicMapCursor(MagicMapCursorEvent event) {
        if (holeLocation != null) {
            event.addCursor(MapCursor.Type.BANNER_WHITE, holeLocation, text("hole"));
        }
        final GamePlayer gp = getGamePlayer(event.getPlayer());
        if (gp != null && gp.isPlaying()) {
            final Location ballLocation;
            if (gp.getState() == GamePlayer.State.FLIGHT && gp.getFlightBall() != null) {
                ballLocation = gp.getFlightBall().getLocation();
            } else {
                ballLocation = gp.getBallVector().toCenterFloorLocation(world);
            }
            event.addCursor(MapCursor.Type.PLAYER_OFF_LIMITS, ballLocation, text("ball"));
        }
    }

    public void onPlayerUseCompass(Player player) {
        final GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isPlaying() || gp.isFinished()) return;
        teleport(player, directAtHole(gp.getBallVector().toCenterFloorLocation(world).add(0.0, 1.0, 0.0)));
        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.MASTER, 1f, 1f);
    }

    public ItemStack makeBallCompass() {
        final ItemStack item = new ItemStack(Material.COMPASS);
        item.setData(DataComponentTypes.LODESTONE_TRACKER, lodestoneTracker().tracked(true).location(teeVector.toCenterLocation(world)));
        item.setData(DataComponentTypes.CUSTOM_NAME, text("Ball Compass", LIGHT_PURPLE));
        final List<Component> lore = List.of(text(tiny("Always points to your"), GRAY).decoration(ITALIC, false),
                                             text(tiny("ball as long as it is"), GRAY).decoration(ITALIC, false),
                                             text(tiny("on the ground."), GRAY).decoration(ITALIC, false),
                                             empty(),
                                             textOfChildren(Mytems.MOUSE_RIGHT, text(" Teleport", GRAY)).decoration(ITALIC, false));
        item.setData(DataComponentTypes.LORE, lore(lore));
        return item;
    }

    public void updateBallCompass(Player player, GamePlayer gp) {
        for (ItemStack item : player.getInventory()) {
            if (item == null || item.getType() != Material.COMPASS) continue;
            item.setData(DataComponentTypes.LODESTONE_TRACKER, lodestoneTracker().tracked(true).location(gp.getBallVector().toCenterLocation(world)));
        }
    }

    public boolean determineWinners() {
        final List<GamePlayer> finishers = new ArrayList<>();
        for (GamePlayer gp : players.values()) {
            if (gp.isFinished()) finishers.add(gp);
        }
        if (finishers.isEmpty()) return false;
        finishers.sort(Comparator.comparing(GamePlayer::getStrokeCount));
        final int minStrokeCount = finishers.get(0).getStrokeCount();
        final List<GamePlayer> winners = new ArrayList<>();
        for (GamePlayer gp : finishers) {
            if (gp.getStrokeCount() == minStrokeCount) {
                winners.add(gp);
            }
            log("Final Strokes: " + gp.getStrokeCount() + " " + gp.getName());
        }
        winners.sort(Comparator.comparing(GamePlayer::getName));
        // Announce
        final List<Component> winnerNames = new ArrayList<>(winners.size());
        for (GamePlayer gp : winners) winnerNames.add(text(gp.getName(), WHITE));
        final Component announcement;
        final Title title;
        if (winnerNames.size() == 1) {
            announcement = textOfChildren(winnerNames.get(0), text(" wins the game!", GREEN));
            title = title(winnerNames.get(0),
                          text("Wins the Game!", GREEN),
                          times(Duration.ofSeconds(1),
                                Duration.ofSeconds(4),
                                Duration.ofSeconds(1)));
        } else {
            announcement = join(JoinConfiguration.builder()
                                .separator(text(", ", GREEN))
                                .lastSeparator(text(" and ", GREEN))
                                .suffix(text(" win the game!", GREEN)),
                                winnerNames);
            title = title(text("Winners", GREEN),
                          join(separator(space()), winnerNames),
                          times(Duration.ofSeconds(1),
                                Duration.ofSeconds(3),
                                Duration.ofSeconds(1)));
        }
        for (Player player : getPresentPlayers()) {
            player.sendMessage(empty());
            player.sendMessage(announcement);
            player.sendMessage(empty());
            player.showTitle(title);
            player.playSound(player, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 2f);
        }
        // Call event
        final MinigameMatchCompleteEvent event = new MinigameMatchCompleteEvent(MinigameMatchType.ENDERGOLF);
        for (GamePlayer gp : finishers) event.addPlayerUuid(gp.getUuid());
        for (GamePlayer gp : winners) event.addWinnerUuid(gp.getUuid());
        event.callEvent();
        // Event
        int bonus = 3;
        if (plugin.getSaveTag().isEvent()) {
            int lastStrokes = finishers.get(0).getStrokeCount();
            final List<String> commandNames = new ArrayList<>();
            for (GamePlayer gp : finishers) {
                commandNames.add(gp.getName());
                plugin.getSaveTag().addScore(gp.getUuid(), 1);
                if (lastStrokes != gp.getStrokeCount()) {
                    bonus -= 1;
                    lastStrokes = gp.getStrokeCount();
                }
                if (bonus > 0) {
                    plugin.getLogger().info("[" + getWorldName() + " Bonus score: " + bonus + " " + gp.getName());
                    plugin.getSaveTag().addScore(gp.getUuid(), bonus);
                }
            }
            plugin.computeHighscore();
            final String command = "ml add " + String.join(" ", commandNames);
            plugin.getLogger().info("[" + getWorldName() + " Issuing console command: " + command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        // End
        return true;
    }
}
