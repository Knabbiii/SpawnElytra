# Copilot Instructions for SpawnElytra

This is a Minecraft Spigot plugin that enables elytra flight mechanics at spawn without requiring actual elytra wings.

## Project Architecture

### Core Components
- **Main Plugin Class**: `SpawnElytra` - minimal bootstrap that registers the listener
- **Event Listener**: `SpawnBoostListener` - handles all game mechanics via Bukkit events
- **Configuration**: Uses Bukkit's config.yml system with automatic validation and reloading

### Package Structure
```
de.knabbiii.spawnelytra/
├── SpawnElytra.java          # Main plugin class
└── listener/
    └── SpawnBoostListener.java # Event handling and flight mechanics
```

### Key Design Pattern: Static Factory with Validation
The plugin uses a static factory pattern in `SpawnBoostListener.create()` that:
```java
// Validates all required config keys exist, regenerates config.yml if missing
if (!config.contains("multiplyValue") || !config.contains("spawnRadius") || /* ... */) {
    plugin.saveResource("config.yml", true);
    plugin.reloadConfig();
}
```

### Event-Driven Architecture
The plugin is entirely event-driven with these key handlers:
- `PlayerToggleFlightEvent` - triggers elytra gliding on double-jump
- `PlayerSwapHandItemsEvent` - provides velocity boost (F key by default)
- `EntityDamageEvent` - prevents fall/collision damage during flight
- `EntityToggleGlideEvent` - prevents manual gliding toggle
- `PlayerChangedWorldEvent` - immediately stops flight when changing worlds

## Critical Implementation Details

### World Safety Pattern
```java
private boolean isInSpawnRadius(Player player) {
    // CRITICAL: Must check exact world equality to prevent nether portal coordinate bugs
    if (!player.getWorld().equals(world)) return false;
    return player.getWorld().getSpawnLocation().distance(player.getLocation()) <= spawnRadius;
}
```

### Cross-World Flight Prevention
The scheduler checks ALL online players (not just those in the configured world) and includes a `PlayerChangedWorldEvent` handler that immediately stops flight when players change worlds. This prevents the nether portal bug where players could continue flying after world transitions.

### State Management with Lists
- `List<Player> flying` - tracks players currently in elytra mode
- `List<Player> boosted` - prevents multiple velocity boosts per flight
- State cleanup happens via scheduler delays and ground detection

### Cross-Version Compatibility
The plugin handles BungeeCord chat API gracefully:
```java
try {
    // Modern approach with KeybindComponent for localized keybinds
    BaseComponent[] components = new ComponentBuilder(messageParts[0])
            .append(new KeybindComponent("key.swapOffhand"))
            .create();
} catch (Exception e) {
    // Fallback for older versions
    event.getPlayer().sendMessage(message.replace("%key%", "[F]"));
}
```

## Build & Development

### Technology Stack
- **Java 21** (toolchain configured in build.gradle.kts)
- **Spigot API 1.21.10** (compileOnly dependency)
- **Gradle Kotlin DSL** for build configuration

### Build Commands
```bash
./gradlew build          # Builds plugin JAR
./gradlew clean build    # Clean rebuild
```

### Key Files
- `plugin.yml` - Spigot plugin manifest (version must match build.gradle.kts)
- `config.yml` - Runtime configuration with English default message
- JAR output: `build/libs/spawnelytra-{version}.jar`

## Configuration Schema
```yaml
spawnRadius: 50           # Blocks from spawn where flight is enabled
multiplyValue: 5          # Velocity multiplier for boost (F key)
boostEnabled: true        # Whether F key boost is available
world: "world"           # Target world name (must exist)
message: "..."           # Action bar message with %key% placeholder
```

## Common Patterns
- **GameMode filtering**: Always check `SURVIVAL` and `ADVENTURE` modes only
- **Task scheduling**: Use `BukkitRunnable` for continuous monitoring (every 3 ticks)
- **Event cancellation**: Cancel events to override default Minecraft behavior
- **Null safety**: Use `Objects.requireNonNull()` for critical dependencies like world lookup

## Testing Notes
- Test in both overworld and nether to verify world isolation
- Verify ground detection works on various block types
- Test gamemode switching behavior
- Validate config regeneration when keys are missing