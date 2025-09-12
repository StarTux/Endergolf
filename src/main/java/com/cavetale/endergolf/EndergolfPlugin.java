package com.cavetale.endergolf;

import com.cavetale.core.util.Json;
import com.cavetale.endergolf.sql.SQLMapPlayerBest;
import com.cavetale.fam.trophy.Highscore;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import com.winthier.sql.SQLDatabase;
import java.io.File;
import java.util.List;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
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
    private final SQLDatabase database = new SQLDatabase(this);
    private final Lobby lobby = new Lobby(this);

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
        database.registerTables(List.of(SQLMapPlayerBest.class));
        database.createAllTables();
        lobby.enable();
    }

    @Override
    public void onDisable() {
        games.disable();
        saveSaveTag();
        database.waitForAsyncTask();
        database.close();
    }

    public static EndergolfPlugin endergolfPlugin() {
        return instance;
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
                                TrophyCategory.GOLF,
                                title,
                                hi -> "You scored " + hi.score + " point" + (hi.score > 1 ? "s" : ""));
    }
}
