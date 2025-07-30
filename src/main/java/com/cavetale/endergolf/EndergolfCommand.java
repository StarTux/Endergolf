package com.cavetale.endergolf;

import com.cavetale.core.command.AbstractCommand;
import org.bukkit.command.CommandSender;

public final class EndergolfCommand extends AbstractCommand<EndergolfPlugin> {
    protected EndergolfCommand(final EndergolfPlugin plugin) {
        super(plugin, "endergolf");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").denyTabCompletion()
            .description("Info Command")
            .senderCaller(this::info);
    }

    private boolean info(CommandSender sender, String[] args) {
        return false;
    }
}
