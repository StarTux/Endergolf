package com.cavetale.endergolf;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.winthier.creative.BuildWorld;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class EndergolfAdminCommand extends AbstractCommand<EndergolfPlugin> {
    public EndergolfAdminCommand(final EndergolfPlugin plugin) {
        super(plugin, "endergolfadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("start").arguments("<world>")
            .description("Start a game")
            .completableList(ctx -> listWorldPaths())
            .senderCaller(this::start);
        rootNode.addChild("skip").denyTabCompletion()
            .description("Try to skip something")
            .playerCaller(this::skip);
        rootNode.addChild("stop").denyTabCompletion()
            .description("Stop current game")
            .playerCaller(this::stop);
    }

    private List<String> listWorldPaths() {
        List<String> result = new ArrayList<>();
        for (BuildWorld buildWorld : BuildWorld.findMinigameWorlds(MinigameMatchType.ENDERGOLF, false)) {
            result.add(buildWorld.getPath());
        }
        return result;
    }

    private boolean start(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final BuildWorld buildWorld = BuildWorld.findWithPath(args[0]);
        if (buildWorld == null || buildWorld.getRow().parseMinigame() != MinigameMatchType.ENDERGOLF) {
            throw new CommandWarn("Not an Endergolf world: " + args[0]);
        }
        final Game game = new Game(plugin, buildWorld);
        for (Player player : Bukkit.getWorlds().get(0).getPlayers()) {
            game.addPlayer(player);
        }
        if (game.getPlayers().isEmpty()) {
            throw new CommandWarn("No players in lobby!");
        }
        game.loadWorldAndEnableLater();
        sender.sendMessage(text("Starting game: " + buildWorld.getName(), YELLOW));
        return true;
    }

    private void skip(Player player) {
        final Game game = Game.in(player.getWorld());
        if (game == null) {
            throw new CommandWarn("There is no game here");
        }
        if (!game.skip()) {
            throw new CommandWarn("Could not skip");
        }
        player.sendMessage(text("Skipped", YELLOW));
    }

    private void stop(Player player) {
        final Game game = Game.in(player.getWorld());
        if (game == null) {
            throw new CommandWarn("There is no game here");
        }
        game.setFinished(true);
        player.sendMessage(text("Trying to stop game...", YELLOW));
    }
}
