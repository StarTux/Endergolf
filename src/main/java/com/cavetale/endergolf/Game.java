package com.cavetale.endergolf;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.struct.Vec3i;
import com.winthier.creative.BuildWorld;
import com.winthier.creative.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
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
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
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
    private World world;
    private final Map<UUID, GamePlayer> players = new HashMap<>();
    private boolean finished;
    private State state = State.INIT;
    private Vec3i teeVector;
    private Vec3i holeVector;
    // Updated in tick()
    private Instant now;
    // Countdown
    private Instant countdownStart;
    private Instant countdownStop;
    private long countdownSeconds;
    // Play
    private boolean doDebugGravity = false;

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
                loadAreas();
                EndergolfPlugin.endergolfPlugin().getGames().addAndEnable(this);
            });
    }

    private void prepareWorld() {
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.FALL_DAMAGE, false);
        world.setGameRule(GameRule.DROWNING_DAMAGE, false);
        world.setGameRule(GameRule.LOCATOR_BAR, false);
    }

    private void loadAreas() {
        final AreasFile areasFile = AreasFile.load(world, "Endergolf");
        if (areasFile == null) {
            throw new IllegalStateException("[" + getWorldName() + "] No Endergolf areas file");
        }
        for (Area area : areasFile.find("tee")) {
            if (teeVector != null) {
                throw new IllegalStateException("[" + getWorldName() + "] Multiple tee areas");
            }
            this.teeVector = area.getMin();
        }
        for (Area area : areasFile.find("hole")) {
            if (holeVector != null) {
                throw new IllegalStateException("[" + getWorldName() + "] Multiple hole areas");
            }
            this.holeVector = area.getMin();
        }
        if (teeVector == null) {
            throw new IllegalStateException("[" + getWorldName() + "] No tee vector");
        }
        if (holeVector == null) {
            throw new IllegalStateException("[" + getWorldName() + "] No hole vector");
        }
    }

    public void enable() {
        if (world == null) {
            throw new IllegalStateException("World not loaded");
        }
        final int distance = (int) Math.round(teeVector.distance(holeVector));
        for (GamePlayer gp : List.copyOf(players.values())) {
            final Player player = gp.getPlayer();
            if (player == null) {
                players.remove(gp.getUuid());
            }
            gp.setBallVector(teeVector);
            gp.setDistance(distance);
            teleport(player, world.getSpawnLocation());
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
        }
        if (players.isEmpty()) {
            throw new IllegalStateException("[" + getWorldName() + "] No players");
        }
        setState(State.COUNTDOWN);
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
            int notFinished = 0;
            final List<GamePlayer> waitingPlayers = new ArrayList<>();
            final List<Strike> strikes = new ArrayList<>();
            // Check on all players and tick them
            for (GamePlayer gp : players.values()) {
                tickPlay(gp);
                if (gp.isPlaying() && !gp.isFinished()) {
                    notFinished += 1;
                }
                if (gp.getState() == GamePlayer.State.WAIT && gp.getWaitingSince() != null && gp.getPlayer() != null && now.isAfter(gp.getStrikeCooldown())) {
                    waitingPlayers.add(gp);
                }
                if (gp.getStrike() != null) {
                    strikes.add(gp.getStrike());
                }
            }
            if (notFinished == 0) {
                setState(State.END);
                return;
            }
            // Give all waiting players a chance to strike
            waitingPlayers.sort(Comparator.comparing(GamePlayer::getWaitingSince));
            for (GamePlayer gp : waitingPlayers) {
                boolean tooClose = false;
                for (Strike strike : strikes) {
                    // Distance must be at least 4
                    if (strike.getBallVector().distanceSquared(gp.getBallVector()) < 16.0) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;
                final Strike strike = new Strike(this, gp, gp.getBallVector(), now, now.plus(Duration.ofSeconds(45)));
                strike.enable();
                gp.setStrike(strike);
                gp.setState(GamePlayer.State.STRIKE);
                final Player player = gp.getPlayer();
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    teleport(player, gp.getBallVector().toCenterFloorLocation(world).add(0.0, 1.0, 0.0));
                    player.setGameMode(GameMode.ADVENTURE);
                    player.getInventory().clear();
                    for (GolfClub club : GolfClub.values()) {
                        player.getInventory().setItem(club.ordinal(), club.createItemStack());
                    }
                }
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
            // Cancel current strike if any
            if (gp.getState() == GamePlayer.State.STRIKE) {
                gp.getStrike().disable();
                gp.setStrike(null);
            }
            // Remove from game
            if (Duration.between(gp.getOfflineSince(), now).toSeconds() > 60L) {
                plugin.getLogger().info("[" + getWorldName() + "] Removing " + gp.getName() + " because they have been offline too long");
                gp.setPlaying(false);
                gp.setState(GamePlayer.State.SPECTATE);
            }
            return;
        }
        assert player != null;
        switch (gp.getState()) {
        case WAIT:
            // Switching from WAIT to STRIKE is handled outside this
            // function because we must order all waiting players by
            // their waitingSince timestamp.
            break;
        case STRIKE:
            tickStrike(gp, player, gp.getStrike());
            break;
        case FLIGHT:
            if (gp.getFlightBall().isDead()) {
                // The ball disappeared
                final Location lastLocation = gp.getFlightBall().getLocation();
                final Vec3i vector = Vec3i.of(lastLocation);
                if (vector.y <= world.getMinHeight()) {
                    // Void
                    player.sendMessage(text("Your ball fell out of the world", RED));
                    gp.setState(GamePlayer.State.WAIT);
                    gp.setWaitingSince(now);
                    gp.setStrikeCooldown(now.plus(Duration.ofSeconds(5)));
                    gp.setFlightBall(null);
                    gp.setBallVelocity(null);
                } else if (!vector.toBlock(world).isEmpty() && !Tag.REPLACEABLE.isTagged(vector.toBlock(world).getType())) {
                    // Non-replaceable block, such as carpet.
                    if (onBallLand(gp, gp.getFlightBall(), vector.toBlock(world))) {
                        vector.toBlock(world).setType(Material.DRAGON_EGG, false);
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
        default: break;
        }
    }

    /**
     * Here we give the player a preview of where their ball might fly
     * if they hit it.
     */
    private void tickStrike(GamePlayer gp, Player player, Strike strike) {
        final RayTraceResult rayTrace = player.rayTraceBlocks(4.0);
        if (rayTrace == null || rayTrace.getHitBlock() == null || !strike.getBallVector().equals(Vec3i.of(rayTrace.getHitBlock()))) {
            return;
        }
        final GolfClub club = GolfClub.ofMaterial(player.getInventory().getItemInMainHand().getType());
        if (club == null) return;
        Vector velocity = getStrikeDirection(rayTrace.getHitPosition(), strike.getBallVector())
            .multiply(club.getStrength());
        Location location = strike.getBallVector().toCenterFloorLocation(world);
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

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.hasBlock() && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
            return;
        }
        if (!event.hasBlock() || event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (!event.hasItem()) {
            return;
        }
        final Player player = event.getPlayer();
        final GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isPlaying() || gp.getState() != GamePlayer.State.STRIKE) {
            return;
        }
        final Vec3i clickVector = Vec3i.of(event.getClickedBlock());
        if (!clickVector.equals(gp.getStrike().getBallVector())) {
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
        gp.getStrike().disable();
        gp.setStrike(null);
        gp.setState(GamePlayer.State.FLIGHT);
        gp.setStrikeCount(gp.getStrikeCount() + 1);
        final Vector velocity = getStrikeDirection(rayTrace.getHitPosition(), clickVector)
            .multiply(club.getStrength());
        final Location ballLocation = clickVector.toCenterFloorLocation(world);
        gp.setFlightBall(spawnBall(ballLocation, velocity));
        gp.setBallVelocity(velocity);
        world.playSound(ballLocation, Sound.ITEM_MACE_SMASH_GROUND, SoundCategory.MASTER, 1.0f, 1.5f);
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
        final GroundType ground = GroundType.at(block);
        if (ground.isReset()) {
            gp.setFlightBall(null);
            gp.setState(GamePlayer.State.WAIT);
            gp.setWaitingSince(now);
            gp.setStrikeCooldown(now.plus(Duration.ofSeconds(5)));
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
        } else {
            gp.setBallVector(Vec3i.of(block));
            gp.setDistance((int) Math.round(gp.getBallVector().distance(holeVector)));
            gp.setFlightBall(null);
            gp.setState(GamePlayer.State.WAIT);
            gp.setWaitingSince(now);
            gp.setGroundType(ground);
            final Player player = gp.getPlayer();
            if (player != null) {
                player.sendMessage(textOfChildren(text("Your ball landed in the ", WHITE),
                                                  ground.getDisplayComponent()));
                player.showTitle(title(empty(),
                                       ground.getDisplayComponent(),
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
    public Vector getStrikeDirection(Vector hitPoint, Vec3i ballVector) {
        final Vector ballCenter = ballVector.toVector().add(new Vector(0.5, 1.125, 0.5));
        final Vector difference = ballCenter.subtract(hitPoint);
        return difference.normalize();
    }

    public FallingBlock spawnBall(Location location, Vector velocity) {
        return world.spawn(location, FallingBlock.class, e -> {
                e.setBlockData(Material.DRAGON_EGG.createBlockData());
                e.setVelocity(velocity);
            });
    }

    public void onPlayerHud(PlayerHudEvent event) {
        final List<Component> sidebar = new ArrayList<>();
        final GamePlayer gp = getGamePlayer(event.getPlayer());
        if (gp != null && gp.isPlaying()) {
            sidebar.add(textOfChildren(text(tiny("strikes "), GRAY), text(gp.getStrikeCount(), WHITE)));
            sidebar.add(textOfChildren(text(tiny("distance "), GRAY), text(gp.getDistance(), WHITE), text("yd", DARK_GRAY)));
            sidebar.add(textOfChildren(text(tiny("ground "), GRAY), gp.getGroundType().getDisplayComponent()));
            switch (gp.getState()) {
            case WAIT:
                event.bossbar(PlayerHudPriority.HIGH, text("Please Wait for Others", GRAY), BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, 0f);
                break;
            case STRIKE:
                event.bossbar(PlayerHudPriority.HIGH, text("You're Up", GREEN), BossBar.Color.GREEN, BossBar.Overlay.PROGRESS, 1f);
                break;
            case FLIGHT:
                event.bossbar(PlayerHudPriority.HIGH, text("Look at It Go!", GREEN), BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, 1f);
                break;
            default: break;
            }
        }
        event.sidebar(PlayerHudPriority.HIGH, sidebar);
    }
}
