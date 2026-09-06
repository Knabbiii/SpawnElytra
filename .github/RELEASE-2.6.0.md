# SpawnElytra v2.6.0 Release Notes

- Date released: 2026-09-06
- Previous version: 2.5.2

---

## SpigotMC (BBCode):

```bbcode
[CENTER][SIZE=6][COLOR=#3498db][IMG]https://github.com/Knabbiii/SpawnElytra/blob/main/.github/assets/spawnelytra-banner.png?raw=true[/IMG]
Custom Areas & Flight Timer Update[/COLOR][/SIZE]

[IMG]https://img.shields.io/badge/SpawnElytra-v2.6.0-blue?style=for-the-badge[/IMG] [IMG]https://img.shields.io/badge/Minecraft-1.20.1+-green?style=for-the-badge[/IMG] [IMG]https://img.shields.io/badge/Bedrock-Geyser%20Compatible-orange?style=for-the-badge[/IMG][/CENTER]

[SIZE=5][COLOR=#e74c3c]📣 What's New in v2.6.0?[/COLOR][/SIZE]

A [B]set-up-entirely-in-game update[/B]: draw your own spawn area with a wand-free wizard, cap how long people can fly, and customize every single message the plugin shows.

[SIZE=5][COLOR=#27ae60]✨ v2.6.0 Updates[/COLOR][/SIZE]

[SIZE=4][COLOR=#3498db]🆕 New Features[/COLOR][/SIZE]
[LIST]
[*]✨ [B]/spawnelytra setup[/B] - Stand at two corners, run pos1/pos2/save, and you've got a fully custom rectangular flight area. Live particle preview while you set it up, gold flash when it's saved.
[*]✨ [B]/spawnelytra center[/B] - Move the flight area's center off the vanilla world spawn without touching /setworldspawn.
[*]✨ [B]Rectangular spawn areas[/B] - spawnAreaMode can now be "rectangle" instead of just a radius, with an automatic square fallback if you don't set up a custom box.
[*]✨ [B]/spawnelytra visualize[/B] - Outlines the current area with particles (circle, sphere, or box wireframe) so you can see exactly where it is.
[*]✨ [B]maxFlightDuration[/B] - Optionally force-land players after N seconds of flight, with a boss bar and title countdown. No fall damage on the forced landing. Closes the old "just keep gliding forever" exploit.
[*]✨ [B]Multiple boosts per flight[/B] - totalBoosts + boostToBoostCooldown let you allow more than one boost per flight, with an actionbar countdown between them.
[*]✨ [B]messages.yml[/B] - Every player-facing message (boost text, activation hints, flight-timer countdown) is now its own file with & color codes and placeholders, separate from config.yml.
[/LIST]

[SIZE=4][COLOR=#9b59b6]🎮 Enhancements[/COLOR][/SIZE]
[LIST]
[*]✅ [B]disableFireworksInSpawnElytra[/B] - Block firework-rocket boosting while using the virtual elytra (real elytras are unaffected).
[*]✅ [B]disableInAdventure[/B] - Optionally disable spawn elytra flight in Adventure mode. Creative is always excluded now (it has its own native flight anyway).
[*]✅ [B]/spawnelytra info[/B] now reports the actually active area mode and center, since area.yml can silently override config.yml.
[*]✅ config.yml reorganized into Quick Start / Boost Limits / Flight Duration / Advanced Settings so the common options aren't buried.
[/LIST]

[SIZE=4][COLOR=#e67e22]🐛 Bug Fixes[/COLOR][/SIZE]
[LIST]
[*]✅ [B]spawnelytra.use / .useboost permissions[/B] were declared but never actually checked anywhere - they do something now.
[*]✅ [B]/spawnelytra reload[/B] left the previous listener still registered, causing duplicate event handling and resetting Bedrock-player detection to empty for anyone already connected (they'd get treated as Java players until reconnecting).
[*]✅ Players who already had flight granted before a reload were never re-tracked, so leaving the spawn area afterward no longer revoked it - they could keep flying anywhere.
[*]✅ Switching to Creative or Spectator mid-flight (or while managed) could get flight disabled for those modes too, even though they always have their own native flight independent of this plugin.
[*]✅ Firework rockets could still boost players using the virtual elytra even with the option meant to block that.
[*]✅ Bedrock chestplate backups now live on the player's own data instead of a separate JSON file, more reliable across restarts.
[/LIST]

[SIZE=5][COLOR=#e67e22]📥 Installation[/COLOR][/SIZE]

[SIZE=4][COLOR=#3498db]⬆️ Upgrading from v2.5.2[/COLOR][/SIZE]
[LIST=1]
[*]Download [ICODE]SpawnElytra-2.6.0.jar[/ICODE] from the files section
[*]Stop your server
[*]Replace the old plugin file in your [ICODE]plugins/[/ICODE] folder
[*]Start your server - a new [ICODE]messages.yml[/ICODE] is created automatically
[*]✅ [B]Your existing config.yml keeps working as-is[/B] - new options just use their defaults
[/LIST]

[SIZE=4][COLOR=#e74c3c]⚠️ Important Notes[/COLOR][/SIZE]
The [ICODE]message[/ICODE] key (Java activation text) has moved out of [ICODE]config.yml[/ICODE] into [ICODE]messages.yml[/ICODE] as [ICODE]javaActivation[/ICODE]. If you'd customized it before, copy your text over - the old key in config.yml is no longer read. Everything else in config.yml still works untouched.

[SIZE=5][COLOR=#2ecc71]🌟 Why Update to v2.6.0?[/COLOR][/SIZE]

[LIST]
[*][B]🎯 No more hand-typed coordinates[/B] - set up a custom area entirely in-game
[*][B]🔧 Closes the infinite-flight exploit[/B] - optional, but there if you want it
[*][B]🎮 Say whatever you want[/B] - every message is now yours to rewrite
[*][B]✅ Easy Upgrade[/B] - drop-in replacement, your config.yml still works
[/LIST]

[CENTER][SIZE=4][COLOR=#27ae60][B]Draw your spawn area, cap flight time, and make it sound like your server.[/B][/COLOR][/SIZE][/CENTER]

[CENTER][URL='https://github.com/Knabbiii/SpawnElytra'][IMG]https://img.shields.io/badge/GitHub-Repository-black?style=for-the-badge&logo=github[/IMG][/URL][/CENTER]
```

---

## Modrinth (Markdown):

```markdown
![Spawn Elytra](https://cdn.modrinth.com/data/cached_images/6af846d6fcd8c9ec2549cca5bc1e28b113391caf.png)

<div align="center">

## Custom Areas & Flight Timer Update

[![Downloads](https://img.shields.io/modrinth/dt/spawnelytra?style=for-the-badge&logo=modrinth&color=d004f7)](https://modrinth.com/plugin/spawnelytra)
[![Version](https://img.shields.io/modrinth/v/spawnelytra?style=for-the-badge&logo=modrinth&color=d004f7)](https://modrinth.com/plugin/spawnelytra/versions)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%2B-green?style=for-the-badge&logo=minecraft)](https://www.minecraft.net/)
[![Bedrock](https://img.shields.io/badge/Bedrock-Geyser%20Compatible-orange?style=for-the-badge&logo=minecraft)](https://geysermc.org/)

</div>

## 🚀 SpawnElytra v2.6.0 - Custom Areas & Flight Timer Update

### 🎯 Major Changes
- **🗺️ In-game area setup** - Draw a custom rectangular spawn area or move its center without editing a single coordinate by hand
- **⏱️ Max flight duration** - Optional time limit with a boss bar/title countdown, closes the old infinite-flight exploit
- **💬 messages.yml** - Every player-facing message is now customizable in its own file

### ✨ What's New

#### New Features
- **`/spawnelytra setup`**: Stand at two corners, run `pos1`/`pos2`/`save`, done - a live particle preview shows you the box as you set it up, and a gold flash confirms it saved
- **`/spawnelytra center [reset]`**: Set a custom flight-area center independent of the vanilla world spawn
- **Rectangular spawn areas**: `spawnAreaMode: rectangle` as an alternative to the radius, with an automatic square fallback sized to `spawnRadius` if you don't set up a custom box
- **`/spawnelytra visualize [seconds]`**: Outlines the current area (circle, sphere, or box wireframe) with particles
- **`maxFlightDuration`**: Force-land players after N seconds, no fall damage, with a boss bar and last-3-seconds title countdown (sounds and each display element are individually toggleable)
- **Multiple boosts per flight**: `totalBoosts` + `boostToBoostCooldown`, with an actionbar countdown between boosts
- **`messages.yml`**: Boost/activation/flight-timer text, with `&` color codes and `%key%`/`%count%`/`%total%`/`%seconds%` placeholders

#### Enhancements
- `disableFireworksInSpawnElytra` blocks firework-rocket boosting while using the virtual elytra (real elytras unaffected)
- `disableInAdventure` optionally disables flight in Adventure mode; Creative is always excluded now (it has its own native flight)
- `/spawnelytra info` reports the actually active area mode and center, since `area.yml` can silently take priority over `config.yml`
- `config.yml` reorganized (Quick Start / Boost Limits / Flight Duration / Advanced Settings) so common options aren't buried under everything else

#### Bug Fixes
- **Permissions did nothing**: `spawnelytra.use`/`.useboost` were declared but never actually checked in code
- **Reload left stale state**: `/spawnelytra reload` never unregistered the previous listener, causing duplicate event handling and resetting Bedrock-player detection to empty for anyone already connected
- **Flight could get stuck on**: players already granted flight before a reload were never re-tracked, so leaving the spawn area afterward didn't revoke it
- **Creative/Spectator flight got disabled**: switching modes mid-flight could strip native flight from modes that manage their own, independent of this plugin
- **Firework boost bypass**: rockets could still boost players using the virtual elytra even with the block option on
- Bedrock chestplate backups now live on the player's own data instead of a separate JSON file

### 📥 Installation
Download `SpawnElytra-2.6.0.jar` and place it in your plugins folder.

### 🔄 Upgrading
Your existing `config.yml` keeps working - new options fall back to sensible defaults, and a fresh `messages.yml` is created automatically on first start.

### ⚠️ Important Notes
The `message` key (Java activation text) moved out of `config.yml` into `messages.yml` as `javaActivation`. If you'd customized it, copy your text over - the old key in `config.yml` is no longer read.

### 🌟 Why This Update?
- ✅ Set up a custom spawn area entirely in-game, no coordinate math
- ✅ Optional flight-time cap for servers that don't want players gliding forever
- ✅ Every message is now yours to rewrite
- ✅ Drop-in upgrade - your existing config keeps working
```

---

## CurseForge (Markdown):

```markdown
## 📦 Paper/Bukkit - v2.6.0: Custom Areas & Flight Timer Update

### 🎯 Major Changes
- **🗺️ In-game area setup**: `/spawnelytra setup` and `/spawnelytra center` replace hand-typed coordinates
- **⏱️ Max flight duration**: optional time limit with boss bar/title countdown
- **💬 messages.yml**: every player-facing message is now its own customizable file

### ✨ New Features

#### Custom Spawn Areas
- **`/spawnelytra setup`**: pos1/pos2/save wizard with a live particle preview and a gold confirmation flash
- **`/spawnelytra center [reset]`**: custom flight-area center independent of the vanilla world spawn
- **Rectangular mode**: `spawnAreaMode: rectangle`, with an automatic square fallback if no custom box is set
- **`/spawnelytra visualize [seconds]`**: particle outline of the current area (circle/sphere/box)

#### Flight Duration Limit
- **`maxFlightDuration`**: force-lands players after N seconds, no fall damage, boss bar + title countdown, individually toggleable display elements and a sound cue

#### Boost & Messages
- **Multiple boosts per flight**: `totalBoosts` + `boostToBoostCooldown` with an actionbar countdown
- **`messages.yml`**: boost/activation/flight-timer text with color codes and placeholders

### 🎮 Enhancements
- `disableFireworksInSpawnElytra` blocks firework-rocket boosting on the virtual elytra
- `disableInAdventure` for Adventure mode; Creative is always excluded (has its own native flight)
- `/spawnelytra info` shows the actually active area mode/center
- `config.yml` reorganized into clearer sections

### 🐛 Bug Fixes
- `spawnelytra.use`/`.useboost` permissions are now actually enforced
- `/spawnelytra reload` no longer leaves a stale listener registered (was causing duplicate events and resetting Bedrock detection to empty for connected players)
- Players granted flight before a reload are now correctly re-tracked, so leaving the area still revokes it afterward
- Creative/Spectator no longer lose their native flight from gamemode-restriction logic that isn't meant for them
- Firework rockets can no longer bypass `disableFireworksInSpawnElytra`
- Bedrock chestplate backups moved from a JSON file to the player's own persistent data

### 🔧 Compatibility
- **Minecraft:** 1.20.1 - 1.21.11
- **Java:** 21 LTS recommended
- **Platform:** Spigot/Paper
- **Tested With:** Geyser, Floodgate

### 📥 Installation
Download `SpawnElytra-2.6.0.jar` and place it in your `plugins` folder.

**Upgrading from v2.5.2**: your existing `config.yml` keeps working; a new `messages.yml` is generated on first start.

### ⚠️ Important Notice
The `message` key moved from `config.yml` to `messages.yml` (as `javaActivation`) - if you'd customized it, copy your text over.
```

---

## Discord Announcement:

```
🚀 **SpawnElytra v2.6.0 is now available!**

**Custom Areas & Flight Timer Update**

Set up your spawn area entirely in-game, cap how long people can fly, and rewrite every message the plugin shows.

## 🆕 What's New

**Custom Areas:**
• **/spawnelytra setup** - pos1/pos2/save wizard with a live particle preview
• **/spawnelytra center** - move the flight area's center off world spawn
• **/spawnelytra visualize** - see the current area outlined in particles

**Flight Timer:**
• **maxFlightDuration** - optional time cap, boss bar + title countdown, no fall damage on landing
• Closes the old "just keep gliding forever" exploit

**Messages:**
• **messages.yml** - every player-facing message is now yours to rewrite

## 🐛 Fixed
• Permissions (`spawnelytra.use`/`.useboost`) actually do something now
• `/spawnelytra reload` no longer leaves stale state (duplicate events, Bedrock detection reset)
• Creative/Spectator no longer lose their native flight from our gamemode restrictions
• Firework rockets can't bypass the firework-boost block anymore

## 📥 Download & Links

**SpigotMC:** <https://www.spigotmc.org/resources/spawnelytra.129704/>
**Modrinth:** <https://modrinth.com/plugin/spawnelytra>
**GitHub Release:** <https://github.com/Knabbiii/SpawnElytra/releases/tag/v2.6.0>

## ⚠️ Important

The `message` config key moved to `messages.yml` as `javaActivation` - copy your text over if you'd customized it. Everything else in `config.yml` keeps working as-is.

---

*Drop-in upgrade - existing configs still work.*
```
