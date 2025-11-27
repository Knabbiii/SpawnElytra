package dev.kkazi.spawnelytra.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.minecraft.world.level.Level;


@Config(name = "spawnelytra")
public class ModConfig implements ConfigData {
    @Comment("Value the velocity of the player should get multiplied with when boosting (3 by default)")
    public int boostStrength = 3;

    @Comment("Enable boost (true by default)")
    public boolean boostEnabled = true;

    @Comment("Enable firework rockets in spawn elytra (false by default)")
    public boolean fireworksEnabled = false;

    @Comment("Enable fall/kinetic damage when flying with the spawn elytra (false by default)")
    public boolean damageEnabled = false;

    @Comment("Set spawn radius (6 by default)")
    public int spawnRadius = 6;

}



