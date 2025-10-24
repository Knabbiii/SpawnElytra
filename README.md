# 🚀 SpawnElytra - Enhanced Edition

> **You might know this feature from CraftAttack - this is exactly the same, but better!**

A Minecraft plugin that enables elytra-like flight mechanics at spawn without requiring actual elytra wings. Double-jump to soar through the air and boost yourself with the F key!

**✨ Enhanced with features inspired by [blax-k's SpawnElytra implementation](https://github.com/blax-k/SpawnElytra)**

## ✨ Features

- 🏃‍♂️ **Double-jump to fly** - Activate elytra gliding anywhere within the spawn radius
- 🚀 **Enhanced boost mechanics** - Press F (swap hands) to get a speed boost while flying
- 🌍 **World-specific** - Configure which world the feature works in
- 🛡️ **No fall damage** - Players won't take damage while using the elytra
- ⚡ **Lightweight** - Minimal performance impact with efficient event handling
- 🎵 **Sound effects** - Configurable boost sounds for better feedback
- 🔧 **Admin commands** - `/spawnelytra reload` and `/spawnelytra info`
- 🎯 **Permission system** - Fine-grained control over who can use what features
- 💬 **Better messages** - Enhanced action bar messages with keybind support

## 📥 Installation

1. Download the latest `.jar` file from the [releases page](https://github.com/Knabbiii/craftattack-spawn-elytra/releases)
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/SpawnElytra/config.yml`

## ⚙️ Configuration

```yaml
spawnRadius: 50           # Radius around spawn where players can start flying
multiplyValue: 5          # Velocity multiplier for the boost (F key)
boostEnabled: true        # Whether the boost feature is enabled
world: "world"           # The world where the feature works
message: "Press %key% to boost yourself."  # Action bar message (%key% = F key)

# Enhanced features
boostSound: ENTITY_BAT_TAKEOFF    # Sound played when boosting
boostDirection: forward           # Direction of boost: 'forward' or 'upward'
showBoostMessage: true           # Show boost activation message
showActivationMessage: true      # Show flight activation message
```

### Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `spawnRadius` | Radius around spawn where players can start flying | `50` |
| `multiplyValue` | How much the velocity gets multiplied when boosting | `5` |
| `boostEnabled` | Whether the boost feature is enabled | `true` |
| `world` | The world where the feature works | `"world"` |
| `message` | Action bar message shown to players (`%key%` = F key) | English message |
| `boostSound` | Sound played when using boost | `ENTITY_BAT_TAKEOFF` |
| `boostDirection` | Direction of boost: `forward` or `upward` | `forward` |
| `showBoostMessage` | Show "Boost activated!" message | `true` |
| `showActivationMessage` | Show activation message with F key hint | `true` |

## 🎮 How to Use

1. **Enter the spawn area** (within the configured radius)
2. **Double-jump** (press space twice quickly) to start flying
3. **Use elytra controls** to glide around
4. **Press F** (swap hands) to boost forward while flying
5. **Land** to stop flying

## 🔧 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/spawnelytra info` | None | Show plugin information and current config |
| `/spawnelytra reload` | `spawnelytra.admin` | Reload plugin configuration |

**Aliases:** `/se`, `/selytra`

## 🔒 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `spawnelytra.use` | Allows using elytra flight at spawn | `true` |
| `spawnelytra.useboost` | Allows using boost functionality | `true` |  
| `spawnelytra.admin` | Allows access to admin commands | `op` |
| `spawnelytra.*` | Grants all permissions | `op` |

## 🔧 Requirements

- **Minecraft:** 1.21+
- **Server:** Spigot, Paper, or compatible
- **Java:** 21+

## 🙏 Credits

**Original Concept:** [CoolePizza](https://www.spigotmc.org/resources/authors/coolepizza.901913/) - [Original SpawnElytra](https://www.spigotmc.org/resources/spawnelytra.97565/)

**Enhanced Features Inspired By:** [blax-k](https://github.com/blax-k) - [SpawnElytra Implementation](https://github.com/blax-k/SpawnElytra)
- Command system and admin features
- Sound effects and enhanced boost mechanics  
- Better configuration options
- Permission system improvements

**This Version:** Updated, enhanced, and optimized implementation with bug fixes for modern Minecraft versions, combining the best ideas from both original works.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

**Original Developer:** [CoolePizza](https://www.spigotmc.org/resources/authors/coolepizza.901913/)  
**Original Plugin:** [SpawnElytra on SpigotMC](https://www.spigotmc.org/resources/spawnelytra.97565/)

This is an updated and improved version of the original plugin with bug fixes and enhanced compatibility for modern Minecraft versions.

---

*Made with ❤️ for the Minecraft community*