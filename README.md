<div align="center">

![SpawnElytra Banner](.github/assets/spawnelytra-banner.png)

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/spawnelytra?logo=modrinth&style=for-the-badge&label=Downloads&color=d004f7)](https://modrinth.com/plugin/spawnelytra)
[![CodeFactor](https://img.shields.io/codefactor/grade/github/knabbiii/spawnelytra?style=for-the-badge&logo=codefactor&color=d004f7&label=Code%20Quality)](https://www.codefactor.io/repository/github/knabbiii/spawnelytra)
[![License: MIT](https://img.shields.io/github/license/Knabbiii/SpawnElytra?color=d004f7&label=License&style=for-the-badge&logo=github)](https://opensource.org/licenses/MIT)
[![GitHub release](https://img.shields.io/github/v/release/knabbiii/spawnelytra?style=for-the-badge&label=Release&color=d004f7&logo=github)](https://github.com/Knabbiii/spawnelytra/releases)

</div>

> **You might know this feature from CraftAttack - this is exactly the same, but better!**

A Minecraft mod/plugin that enables elytra-like flight mechanics at spawn without requiring actual elytra wings. Double-jump to soar through the air and boost yourself with the F key!

**Available for both Fabric and Paper/Bukkit (1.20.1+)**

## Features

- **Double-jump to fly** - Activate elytra gliding anywhere within the spawn radius
- **Enhanced boost mechanics** - Press F to get a speed boost while flying (once per flight)
- **World-specific** - Works in the configured world's spawn area
- **No fall damage** - Players won't take damage while using the spawn elytra
- **Lightweight** - Minimal performance impact with efficient event handling
- **Sound effects** - Configurable boost sounds for better feedback
- **Admin commands** - `/spawnelytra reload` and `/spawnelytra info`
- **Permission system** - Fine-grained control with LuckPerms support (optional)
- **Update checker** - Automatic notification for new versions via Modrinth API

## Installation

### Fabric
1. Make sure you have a Fabric server with [Fabric API](https://modrinth.com/mod/fabric-api) installed
2. Download [Cloth Config](https://modrinth.com/mod/cloth-config) and place it in your `mods` folder
3. Download SpawnElytra Fabric from [Releases](https://github.com/Knabbiii/SpawnElytra/releases) or [Modrinth](https://modrinth.com/plugin/spawnelytra)
4. Place the `.jar` in your `mods` folder
5. Start your server
6. Configure in `config/spawnelytra.toml`

## Configuration

### Fabric (config/spawnelytra.toml)
```toml
spawnRadius = 50                  # Radius around spawn where players can start flying
boostEnabled = true               # Whether the boost feature is enabled
boostStrength = 5                 # Velocity multiplier for the boost
boostDirection = "forward"        # Direction of boost: 'forward' or 'upward'
boostSound = "entity.bat.takeoff" # Sound played when boosting
message = "Press %key% to boost yourself."  # Action bar message
showBoostMessage = true           # Show boost activation message
showActivationMessage = true      # Show flight activation message
checkForUpdates = true            # Check for updates on startup
```

### Paper/Bukkit (plugins/SpawnElytra/config.yml)
```yaml
world: world                      # The world where the feature works
spawnRadius: 50                   # Radius around spawn
boostEnabled: true                # Whether boost is enabled
multiplyValue: 5                  # Velocity multiplier
boostDirection: forward           # 'forward' or 'upward'
boostSound: ENTITY_BAT_TAKEOFF    # Boost sound
message: "Press %key% to boost yourself."
showBoostMessage: true
showActivationMessage: true
checkForUpdates: true             # Update checker
```

## How to Use

1. **Enter the spawn area** (within the configured radius)
2. **Double-jump** (press space twice quickly) to start flying
3. **Use elytra controls** to glide around
4. **Press F** (swap hands) to boost yourself while flying
5. **Land** to stop flying

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/spawnelytra info` | None | Show plugin information and current config |
| `/spawnelytra reload` | `spawnelytra.admin` | Reload plugin configuration |

**Aliases:** `/se`, `/selytra`

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `spawnelytra.use` | Allows using elytra flight at spawn | `true` |
| `spawnelytra.useboost` | Allows using boost functionality | `true` |  
| `spawnelytra.admin` | Allows access to admin commands | `op` |
| `spawnelytra.*` | Grants all permissions | `op` |

**Note:** LuckPerms integration is optional for Fabric. Without LuckPerms, all players have all permissions by default.

## Requirements

### Fabric
- **Minecraft:** 1.20.1 - 1.21.10
- **Fabric Loader:** 0.15.0+
- **Fabric API:** Compatible version for your Minecraft version
- **Cloth Config:** 11.0.0+
- **Java:** 21+

## Credits

**Fabric Port:** [@SchlangeGoto](https://github.com/SchlangeGoto) - Initial Fabric implementation

**Original Concept:** [CoolePizza](https://www.spigotmc.org/resources/authors/coolepizza.901913/) - [Original SpawnElytra](https://www.spigotmc.org/resources/spawnelytra.97565/)

**Enhanced Features Inspired By:** [blax-k](https://github.com/blax-k) - [SpawnElytra Implementation](https://github.com/blax-k/SpawnElytra)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

*Made with care for the Minecraft community*