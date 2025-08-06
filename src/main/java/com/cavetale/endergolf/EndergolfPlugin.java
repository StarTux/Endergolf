package com.cavetale.endergolf;

import com.cavetale.core.util.Json;
import com.cavetale.fam.trophy.Highscore;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import java.io.File;
import java.util.List;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter
public final class EndergolfPlugin extends JavaPlugin {
    private static EndergolfPlugin instance;
    private final EndergolfCommand endergolfCommand = new EndergolfCommand(this);
    private final EndergolfAdminCommand endergolfAdminCommand = new EndergolfAdminCommand(this);
    private final GameListener gameListener = new GameListener(this);
    private final Games games = new Games(this);
    private SaveTag saveTag;
    private List<Component> highscoreLines = List.of();
    private final Component title = textOfChildren(text("Ender", GREEN),
                                                   text("golf", WHITE));

    public EndergolfPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        endergolfCommand.enable();
        endergolfAdminCommand.enable();
        gameListener.enable();
        games.enable();
        loadSaveTag();
        computeHighscore();
    }

    @Override
    public void onDisable() {
        games.disable();
        saveSaveTag();
    }

    public static EndergolfPlugin endergolfPlugin() {
        return instance;
    }

    public void warpToLobby(Player player) {
        player.eject();
        player.leaveVehicle();
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.getInventory().clear();
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    public void loadSaveTag() {
        saveTag = Json.load(new File(getDataFolder(), "tag.json"), SaveTag.class, SaveTag::new);
    }

    public void saveSaveTag() {
        if (saveTag == null) return;
        getDataFolder().mkdirs();
        Json.save(new File(getDataFolder(), "tag.json"), saveTag);
    }

    public void computeHighscore() {
        highscoreLines = Highscore.sidebar(Highscore.of(saveTag.getScores()));
    }

    public int rewardHighscore() {
        return Highscore.reward(saveTag.getScores(),
                                "endergolf",
                                TrophyCategory.CUP,
                                title,
                                hi -> "You scored " + hi.score + " point" + (hi.score > 1 ? "s" : ""));
    }
}
