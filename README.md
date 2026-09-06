<div align="center">

![SpawnElytra Banner](.github/assets/spawnelytra-banner.png)

[![Spigot Downloads](https://img.shields.io/spiget/downloads/129704?style=for-the-badge&logo=spigotmc&color=d004f7)](https://www.spigotmc.org/resources/spawnelytra-elytra-flight-at-spawn.129704/)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/spawnelytra?logo=modrinth&style=for-the-badge&label=Downloads&color=d004f7)](https://modrinth.com/plugin/spawnelytra)
[![CodeFactor](https://img.shields.io/codefactor/grade/github/knabbiii/spawnelytra?style=for-the-badge&logo=codefactor&color=d004f7&label=Code%20Quality)](https://www.codefactor.io/repository/github/knabbiii/spawnelytra)
[![License: MIT](https://img.shields.io/github/license/Knabbiii/SpawnElytra?color=d004f7&label=License&style=for-the-badge&logo=github)](https://opensource.org/licenses/MIT)
[![GitHub release](https://img.shields.io/github/v/release/knabbiii/spawnelytra?style=for-the-badge&label=Release&color=d004f7&logo=github)](https://github.com/Knabbiii/spawnelytra/releases)

</div>

> **You might know this feature from CraftAttack - this is exactly the same, but better!**

A Minecraft plugin that enables elytra-like flight mechanics at spawn without requiring actual elytra wings. Double-jump to soar through the air and boost yourself with the F key!

**Enhanced with features inspired by [blax-k's SpawnElytra implementation](https://github.com/blax-k/SpawnElytra)**

## Features

- **Double-jump to fly** - Activate elytra gliding anywhere within the spawn area
- **Circle or rectangle spawn area** - Use the classic radius, a fixed box, or draw one in-game with `/spawnelytra setup`
- **Custom center point** - Move the flight area's center off the vanilla world spawn with `/spawnelytra center`
- **Enhanced boost mechanics** - Press F (Java) or Sneak (Bedrock) to get a speed boost while flying, with optional multiple boosts per flight and a cooldown between them
- **Max flight duration** - Optionally cap how long a single flight can last, with a boss bar and title countdown, so nobody can fly forever
- **Bedrock/Geyser compatible** - Full support for Bedrock Edition players via GeyserMC or Floodgate, virtual elytra cannot be displaced by inventory actions
- **Fully customizable messages** - Every player-facing message lives in `messages.yml` with color codes and placeholders
- **Particle visualization** - `/spawnelytra visualize` outlines the current spawn area so admins can see exactly where it is
- **Game mode restrictions** - Optionally disable flight in Adventure mode (Creative always keeps its own native flight instead)
- **World-specific** - Configure which world the feature works in
- **No fall damage** - Players won't take damage while using the elytra, or when a max-duration flight ends
- **Lightweight** - Minimal performance impact with efficient event handling
- **Sound effects** - Configurable boost and flight-timer sounds for better feedback
- **Update checker** - Notifies ops on first join after restart if a new version is available
- **Admin commands** - `/spawnelytra reload`, `info`, `visualize`, `setup`, `center`
- **Permission system** - Fine-grained control over who can use what features
- **Anonymous metrics** - Optional bStats integration (can be disabled in config)

## Installation

1. Download the latest `.jar` file from the [releases page](https://github.com/Knabbiii/SpawnElytra/releases)
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/SpawnElytra/config.yml` (behavior) and `messages.yml` (player-facing text)

## Configuration

By default, the plugin uses the **world spawn point** as the center for flight activation - the exact location where players spawn when they first join the server or use `/spawn`. Set it with `/setworldspawn`, or override it independently with `/spawnelytra center`.

```yaml
world: world                      # The world where the feature works
spawnRadius: 50                   # Radius around the center point where players can start flying
boostEnabled: true                # Whether the boost feature is enabled
multiplyValue: 5                  # Velocity multiplier for the boost
boostDirection: forward           # Direction of boost: 'forward' or 'upward'
totalBoosts: 1                    # How many boosts allowed per flight
boostToBoostCooldown: 0           # Cooldown in seconds between boosts (if totalBoosts > 1)
disableFireworksInSpawnElytra: true  # Block firework-rocket boosting while using spawn elytra
maxFlightDuration: 0              # Force-land after this many seconds (0 = unlimited)
flightTimerSound: true            # Play a sound with the flight-duration countdown
boostSound: ENTITY_BAT_TAKEOFF    # Sound played when boosting
showBoostMessage: true            # Show boost activation message
showActivationMessage: true       # Show flight activation message
showFlightTimerBossBar: true      # Show the boss bar while maxFlightDuration is active
showFlightTimerCountdown: true    # Show the big title countdown in the last 3 seconds
showFlightTimerTimeoutMessage: true  # Show the "time's up" message on force-land
ignoreYInSpawnRadius: false       # When true: only X/Z distance checked (ignore height)
spawnAreaMode: circle             # 'circle' (spawnRadius) or 'rectangle' (a box)
disableInAdventure: false         # Disable flight in Adventure mode (Creative is always disabled)
checkForUpdates: true             # Check Modrinth for updates on startup
debugMode: false                  # When true: verbose save/load logging enabled
enableMetrics: true               # Send anonymous usage statistics to bStats
```

A custom rectangular area and/or center point set via `/spawnelytra setup`/`center` are stored in a separate, plugin-managed `area.yml` and take priority over the settings above - that way your own comments in `config.yml` are never touched. Run `/spawnelytra info` to see which area mode and center are actually active.

### Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `spawnRadius` | Radius around the center point where players can start flying | `50` |
| `multiplyValue` | How much the velocity gets multiplied when boosting | `5` |
| `boostEnabled` | Whether the boost feature is enabled | `true` |
| `world` | The world where the feature works | `"world"` |
| `boostSound` | Sound played when using boost | `ENTITY_BAT_TAKEOFF` |
| `boostDirection` | Direction of boost: `forward` or `upward` | `forward` |
| `totalBoosts` | Boosts allowed per flight | `1` |
| `boostToBoostCooldown` | Cooldown in seconds between boosts (if `totalBoosts` > 1) | `0` |
| `disableFireworksInSpawnElytra` | Block firework-rocket boosting while using spawn elytra | `true` |
| `maxFlightDuration` | Force-land players after this many seconds of flight (`0` disables it) | `0` |
| `flightTimerSound` | Play a sound with the flight-duration countdown/timeout | `true` |
| `showBoostMessage` | Show "Boost activated!" message | `true` |
| `showActivationMessage` | Show activation message with boost key hint | `true` |
| `showFlightTimerBossBar` | Show the boss bar while `maxFlightDuration` is active | `true` |
| `showFlightTimerCountdown` | Show the big title countdown in the last 3 seconds | `true` |
| `showFlightTimerTimeoutMessage` | Show the "time's up" title/actionbar on force-land | `true` |
| `ignoreYInSpawnRadius` | When `true`: only horizontal distance (X/Z) is checked, height is ignored | `false` |
| `spawnAreaMode` | `circle` (radius) or `rectangle` (a box, custom or auto-sized) | `circle` |
| `disableInAdventure` | Disable spawn elytra flight in Adventure mode | `false` |
| `checkForUpdates` | Check Modrinth for updates on startup, notify first op to join | `true` |
| `debugMode` | Enable verbose save/load logging for troubleshooting | `false` |
| `enableMetrics` | Send anonymous usage statistics to bStats | `true` |

All player-facing text (boost messages, activation hints, flight-timer countdown/title) lives in **`messages.yml`**, with `&` color codes and placeholders like `%key%`, `%count%`, `%total%` and `%seconds%`.

## How to Use

1. **Enter the spawn area** (within the configured radius or box)
2. **Double-jump** (press space twice quickly) to start flying
3. **Use elytra controls** to glide around
4. **Press F** (swap hands) to boost forward while flying
5. **Land** to stop flying

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/spawnelytra info` | `spawnelytra.admin` | Show plugin info, including the currently active area mode/center |
| `/spawnelytra reload` | `spawnelytra.admin` | Reload plugin configuration |
| `/spawnelytra visualize [seconds]` | `spawnelytra.admin` | Outline the current spawn area with particles |
| `/spawnelytra setup` | `spawnelytra.admin` | Wizard to define a custom rectangular spawn area (`pos1`/`pos2`/`save`/`cancel`/`reset`) |
| `/spawnelytra center [reset]` | `spawnelytra.admin` | Set (or reset) a custom flight-area center point |

**Aliases:** `/se`, `/selytra`

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `spawnelytra.use` | Allows using elytra flight at spawn | `true` |
| `spawnelytra.useboost` | Allows using boost functionality | `true` |  
| `spawnelytra.admin` | Allows access to admin commands | `op` |
| `spawnelytra.*` | Grants all permissions | `op` |

## Requirements

- **Minecraft:** 1.20.1+ (compatible with all versions up to 26.2.x and beyond)
- **Server:** Spigot, Paper, or compatible
- **Java:** 21+

## Credits

**Original Concept:** [CoolePizza](https://www.spigotmc.org/resources/authors/coolepizza.901913/) - [Original SpawnElytra](https://www.spigotmc.org/resources/spawnelytra.97565/)   
**Fabric Port:** [@SchlangeGoto](https://github.com/SchlangeGoto) - Initial Fabric implementation

**Enhanced Features Inspired By:** [blax-k](https://github.com/blax-k) - [SpawnElytra Implementation](https://github.com/blax-k/SpawnElytra)
- Command system and admin features
- Sound effects and enhanced boost mechanics  
- Better configuration options
- Permission system improvements

**This Version:** Updated, enhanced, and optimized implementation with bug fixes for modern Minecraft versions, combining the best ideas from both original works.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

**Original Developer:** [CoolePizza](https://www.spigotmc.org/resources/authors/coolepizza.901913/)  
**Original Plugin:** [SpawnElytra on SpigotMC](https://www.spigotmc.org/resources/spawnelytra.97565/)

This is an updated and improved version of the original plugin with bug fixes and enhanced compatibility for modern Minecraft versions.

---

*Made with care for the Minecraft community*
