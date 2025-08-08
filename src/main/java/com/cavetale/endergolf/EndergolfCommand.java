package com.cavetale.endergolf;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.winthier.creative.BuildWorld;
import com.winthier.creative.vote.MapVote;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class EndergolfCommand extends AbstractCommand<EndergolfPlugin> {
    private MapVote mapVote;

    protected EndergolfCommand(final EndergolfPlugin plugin) {
        super(plugin, "golf");
    }

    @Override
    protected void onEnable() {
        rootNode.denyTabCompletion()
            .description("Pick a map for singleplayer")
            .playerCaller(this::golf);
        rootNode.addChild("singleplayer").arguments("<map>")
            .hidden(true)
            .description("Start a singleplayer game")
            .playerCaller(this::singleplayer);
        rootNode.addChild("quit").denyTabCompletion()
            .description("Quit this game")
            .playerCaller(this::quit);
        mapVote = new MapVote(MinigameMatchType.ENDERGOLF);
        mapVote.setTitle(textOfChildren(plugin.getTitle(), text(" Practice", GRAY)));
        mapVote.setVoteBookCommandMaker(bw -> "/golf singleplayer " + bw.getPath());
        mapVote.setAvoidRepetition(0);
        mapVote.load();
    }

    private void golf(Player player) {
        if (Game.in(player.getWorld()) != null) {
            throw new CommandWarn("You're already in a game");
        }
        if (plugin.getSaveTag().isEvent()) {
            throw new CommandWarn("Cannot play alone during an event");
        }
        mapVote.openVoteBook(player);
    }

    private boolean singleplayer(Player player, String[] args) {
        if (args.length != 1) return true;
        if (Game.in(player.getWorld()) != null) {
            throw new CommandWarn("You're already in a game");
        }
        if (plugin.getSaveTag().isEvent()) {
            throw new CommandWarn("Cannot play alone during an event");
        }
        final BuildWorld buildWorld = BuildWorld.findWithPath(args[0]);
        if (buildWorld == null || buildWorld.getRow().parseMinigame() != MinigameMatchType.ENDERGOLF || !buildWorld.getRow().isPurposeConfirmed()) {
            throw new CommandWarn("Not an Endergolf world: " + args[0]);
        }
        final Game game = new Game(plugin, buildWorld);
        game.setSingleplayer(true);
        game.addPlayer(player);
        game.loadWorldAndEnableLater();
        player.sendMessage(textOfChildren(text("Starting game: ", WHITE),
                                          text(buildWorld.getName(), GREEN)));
        return true;
    }

    private void quit(Player player) {
        final Game game = Game.in(player.getWorld());
        if (game == null) {
            throw new CommandWarn("You're not in a game");
        }
        if (plugin.getSaveTag().isEvent()) {
            throw new CommandWarn("Cannot quit games during an event");
        }
        player.sendMessage(text("Quitting game...", YELLOW));
        game.quit(player);
    }
}
