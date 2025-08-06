package com.cavetale.endergolf;

import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

public final class Fireworks {
    static Random random = new Random();

    private Fireworks() { }

    public static Color randomColor() {
        return Color.fromBGR(random.nextInt(256),
                             random.nextInt(256),
                             random.nextInt(256));
    }

    public static FireworkEffect.Type randomFireworkEffectType() {
        switch (random.nextInt(10)) {
        case 0: return FireworkEffect.Type.CREEPER;
        case 1: return FireworkEffect.Type.STAR;
        case 2: case 3: return FireworkEffect.Type.BALL_LARGE;
        case 4: case 5: case 6: return FireworkEffect.Type.BURST;
        case 7: case 8: case 9: return FireworkEffect.Type.BALL;
        default: return FireworkEffect.Type.BALL;
        }
    }

    public static FireworkMeta randomFireworkMeta() {
        return randomFireworkMeta(randomFireworkEffectType());
    }

    public static FireworkMeta randomFireworkMeta(FireworkEffect.Type type) {
        FireworkMeta meta = (FireworkMeta) Bukkit.getItemFactory()
            .getItemMeta(Material.FIREWORK_ROCKET);
        FireworkEffect.Builder builder = FireworkEffect.builder().with(type);
        int amount = type == FireworkEffect.Type.CREEPER || type == FireworkEffect.Type.STAR
            ? 1 : 3 + random.nextInt(5);
        for (int i = 0; i < amount; i += 1) {
            builder.withColor(randomColor());
            meta.addEffect(builder.build());
        }
        meta.setPower(1 + random.nextInt(2));
        return meta;
    }

    public static void spawnFirework(Location location) {
        location.getWorld().spawn(location, Firework.class, fw -> {
                fw.setFireworkMeta(randomFireworkMeta());
            });
    }
}
