package dev.kkazi.spawnelytra.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;


@Config(name = "spawnelytra")
public class ModConfig implements ConfigData {
    
    // Core Settings
    @Comment("Radius around spawn where players can activate elytra flight (50 by default)")
    public int spawnRadius = 50;

    // Boost Settings
    @Comment("Enable boost feature (true by default)")
    public boolean boostEnabled = true;

    @Comment("Boost strength multiplier - higher values = stronger boost (5 by default, recommended range: 2-8)")
    public int boostStrength = 5;

    @Comment("Direction of boost: 'forward' or 'upward' (forward by default)")
    public String boostDirection = "forward";

    // Sound & Feedback
    @Comment("Sound played when using boost - valid Minecraft sound ID (entity.bat.takeoff by default)")
    public String boostSound = "entity.bat.takeoff";

    // Messages
    @Comment("Message shown when activating flight - %key% will be replaced with boost key hint")
    public String message = "Press %key% to boost yourself.";

    @Comment("Show 'Boost activated!' message when player uses boost (true by default)")
    public boolean showBoostMessage = true;

    @Comment("Show flight activation message with boost key hint (true by default)")
    public boolean showActivationMessage = true;

    // Update Checker
    @Comment("Check for plugin updates on server start (true by default)")
    public boolean checkForUpdates = true;

}



