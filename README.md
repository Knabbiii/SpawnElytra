# 🚀 SpawnElytra

> **You might know this feature from CraftAttack - this is exactly the same!**

A Minecraft plugin that enables elytra-like flight mechanics at spawn without requiring actual elytra wings. Double-jump to soar through the air and boost yourself with the F key!

## ✨ Features

- 🏃‍♂️ **Double-jump to fly** - Activate elytra gliding anywhere within the spawn radius
- 🚀 **Boost mechanics** - Press F (swap hands) to get a speed boost while flying
- 🌍 **World-specific** - Configure which world the feature works in
- 🛡️ **No fall damage** - Players won't take damage while using the elytra
- ⚡ **Lightweight** - Minimal performance impact with efficient event handling

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
```

### Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `spawnRadius` | Radius around spawn where players can start flying | `50` |
| `multiplyValue` | How much the velocity gets multiplied when boosting | `5` |
| `boostEnabled` | Whether the boost feature is enabled | `true` |
| `world` | The world where the feature works | `"world"` |
| `message` | Action bar message shown to players (`%key%` = F key) | English message |

## 🎮 How to Use

1. **Enter the spawn area** (within the configured radius)
2. **Double-jump** (press space twice quickly) to start flying
3. **Use elytra controls** to glide around
4. **Press F** (swap hands) to boost forward while flying
5. **Land** to stop flying

## 🔧 Requirements

- **Minecraft:** 1.21+
- **Server:** Spigot, Paper, or compatible
- **Java:** 21+

## 🙏 Credits

**Original Developer:** [CoolePizza](https://www.spigotmc.org/resources/authors/coolepizza.901913/)  
**Original Plugin:** [SpawnElytra on SpigotMC](https://www.spigotmc.org/resources/spawnelytra.97565/)

This is an updated and improved version of the original plugin with bug fixes and enhanced compatibility for modern Minecraft versions.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

**Original Developer:** [CoolePizza](https://www.spigotmc.org/resources/authors/coolepizza.901913/)  
**Original Plugin:** [SpawnElytra on SpigotMC](https://www.spigotmc.org/resources/spawnelytra.97565/)

This is an updated and improved version of the original plugin with bug fixes and enhanced compatibility for modern Minecraft versions.

---

*Made with ❤️ for the Minecraft community*