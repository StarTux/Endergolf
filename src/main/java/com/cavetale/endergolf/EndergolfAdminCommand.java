package com.cavetale.endergolf;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.trophy.Highscore;
import com.winthier.creative.BuildWorld;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
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
        rootNode.addChild("event").arguments("true|false")
            .completers(CommandArgCompleter.BOOLEAN)
            .description("Set event mode")
            .senderCaller(this::event);
        rootNode.addChild("pause").arguments("true|false")
            .completers(CommandArgCompleter.BOOLEAN)
            .description("Set pause mode")
            .senderCaller(this::pause);
        // Score
        CommandNode score = rootNode.addChild("score")
            .description("Score subcommands");
        score.addChild("clear").denyTabCompletion()
            .description("Clear scores")
            .senderCaller(this::scoreClear);
        score.addChild("add").arguments("<player> <amount>")
            .completers(CommandArgCompleter.PLAYER_CACHE,
                        CommandArgCompleter.integer(i -> true))
            .description("Add score highscore")
            .senderCaller(this::scoreAdd);
        score.addChild("reward").denyTabCompletion()
            .description("Give out trophy rewards")
            .senderCaller(this::scoreReward);
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
        if (sender instanceof Player player) {
            game.setLogTarget(player.getUniqueId());
        }
        for (Player player : plugin.getLobby().getPlayers()) {
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

    private boolean event(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (args.length == 1) {
            plugin.getSaveTag().setEvent(CommandArgCompleter.requireBoolean(args[0]));
            plugin.saveSaveTag();
        }
        sender.sendMessage(textOfChildren(text("Event mode: ", YELLOW),
                                          (plugin.getSaveTag().isEvent()
                                           ? text("Yes", GREEN)
                                           : text("No", RED))));
        return true;
    }

    private boolean pause(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (args.length == 1) {
            plugin.getSaveTag().setPause(CommandArgCompleter.requireBoolean(args[0]));
            plugin.saveSaveTag();
        }
        sender.sendMessage(textOfChildren(text("Pause mode: ", YELLOW),
                                          (plugin.getSaveTag().isPause()
                                           ? text("Yes", GREEN)
                                           : text("No", RED))));
        return true;
    }

    private void scoreClear(CommandSender sender) {
        plugin.getSaveTag().getScores().clear();
        plugin.saveSaveTag();
        plugin.computeHighscore();
        sender.sendMessage(text("Scores cleared", YELLOW));
    }

    private boolean scoreAdd(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final PlayerCache target = CommandArgCompleter.requirePlayerCache(args[0]);
        final int amount = CommandArgCompleter.requireInt(args[1], i -> true);
        plugin.getSaveTag().addScore(target.uuid, amount);
        plugin.saveSaveTag();
        plugin.computeHighscore();
        sender.sendMessage(text("Score changed: " + target.name + ", " + amount, AQUA));
        return true;
    }

    private void scoreReward(CommandSender sender) {
        plugin.computeHighscore();
        final int count = plugin.rewardHighscore();
        sender.sendMessage(text(count + " scores rewarded", YELLOW));
        Highscore.rewardMoneyWithFeedback(sender, plugin, plugin.getSaveTag().getScores(), "Endergolf");
    }
}
