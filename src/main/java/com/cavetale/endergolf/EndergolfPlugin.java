package com.cavetale.endergolf;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

@Getter
public final class EndergolfPlugin extends JavaPlugin {
    private static EndergolfPlugin instance;
    private final EndergolfCommand endergolfCommand = new EndergolfCommand(this);
    private final EndergolfAdminCommand endergolfAdminCommand = new EndergolfAdminCommand(this);
    private final GameListener gameListener = new GameListener(this);
    private final Games games = new Games(this);

    public EndergolfPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        endergolfCommand.enable();
        endergolfAdminCommand.enable();
        gameListener.enable();
        games.enable();
    }

    @Override
    public void onDisable() {
        games.disable();
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
}
