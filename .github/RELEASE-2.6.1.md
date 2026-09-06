# SpawnElytra v2.6.1 Release Notes

- Date released: 2026-09-06
- Previous version: 2.6.0

---

## SpigotMC (BBCode):

```bbcode
[CENTER][SIZE=6][COLOR=#3498db][IMG]https://github.com/Knabbiii/SpawnElytra/blob/main/.github/assets/spawnelytra-banner.png?raw=true[/IMG]
Critical Chestplate Fix[/COLOR][/SIZE]

[IMG]https://img.shields.io/badge/SpawnElytra-v2.6.1-blue?style=for-the-badge[/IMG] [IMG]https://img.shields.io/badge/Minecraft-1.20.1+-green?style=for-the-badge[/IMG] [IMG]https://img.shields.io/badge/Bedrock-Geyser%20Compatible-orange?style=for-the-badge[/IMG][/CENTER]

[SIZE=5][COLOR=#e74c3c]📣 What's New in v2.6.1?[/COLOR][/SIZE]

A small but important [B]hotfix[/B] for a bug introduced in 2.6.0 that could destroy a player's actual chestplate.

[SIZE=5][COLOR=#27ae60]✨ v2.6.1 Updates[/COLOR][/SIZE]

[SIZE=4][COLOR=#e74c3c]🔧 Breaking Fixes[/COLOR][/SIZE]
[LIST]
[*]🔧 [B]Java players could lose their real chestplate[/B] - landing (or a world/gamemode change, or a flight-timer force-land) always tried to "restore" a backed-up chestplate, even for Java players who never had one backed up in the first place (only Bedrock players get the virtual-elytra swap). That cleared their chestplate slot to nothing instead of leaving it alone.
[/LIST]

[SIZE=4][COLOR=#34495e]⚙️ Technical Improvements[/COLOR][/SIZE]
[LIST]
[*]✅ [B]Reduced complexity in onDoubleJump()[/B] - split into smaller helper methods, addressing CodeFactor's "Complex Method" and "one statement per line" findings. No behavior change.
[/LIST]

[SIZE=5][COLOR=#e67e22]📥 Installation[/COLOR][/SIZE]

[SIZE=4][COLOR=#3498db]⬆️ Upgrading from v2.6.0[/COLOR][/SIZE]
[LIST=1]
[*]Download [ICODE]SpawnElytra-2.6.1.jar[/ICODE] from the files section
[*]Stop your server
[*]Replace the old plugin file in your [ICODE]plugins/[/ICODE] folder
[*]Start your server
[*]✅ [B]No config changes[/B] - drop-in replacement
[/LIST]

[SIZE=4][COLOR=#e74c3c]⚠️ Important Notes[/COLOR][/SIZE]
If any of your Java players flew with a real chestplate on 2.6.0 and lost it, you'll need to give it back manually - this update only stops it from happening again, it can't recover items already lost.

[SIZE=5][COLOR=#2ecc71]🌟 Why Update to v2.6.1?[/COLOR][/SIZE]

[LIST]
[*][B]🎯 Stops real armor from disappearing[/B] - if any players fly with a chestplate on, update now
[*][B]✅ Easy Upgrade[/B] - drop-in replacement, no config changes
[/LIST]

[CENTER][SIZE=4][COLOR=#27ae60][B]If you're on 2.6.0, update now - this one can cost players real gear.[/B][/COLOR][/SIZE][/CENTER]

[CENTER][URL='https://github.com/Knabbiii/SpawnElytra'][IMG]https://img.shields.io/badge/GitHub-Repository-black?style=for-the-badge&logo=github[/IMG][/URL][/CENTER]
```

---

## Modrinth (Markdown):

```markdown
![Spawn Elytra](https://cdn.modrinth.com/data/cached_images/6af846d6fcd8c9ec2549cca5bc1e28b113391caf.png)

<div align="center">

## Critical Chestplate Fix

[![Downloads](https://img.shields.io/modrinth/dt/spawnelytra?style=for-the-badge&logo=modrinth&color=d004f7)](https://modrinth.com/plugin/spawnelytra)
[![Version](https://img.shields.io/modrinth/v/spawnelytra?style=for-the-badge&logo=modrinth&color=d004f7)](https://modrinth.com/plugin/spawnelytra/versions)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%2B-green?style=for-the-badge&logo=minecraft)](https://www.minecraft.net/)
[![Bedrock](https://img.shields.io/badge/Bedrock-Geyser%20Compatible-orange?style=for-the-badge&logo=minecraft)](https://geysermc.org/)

</div>

## 🚀 SpawnElytra v2.6.1 - Critical Chestplate Fix

### 🎯 Major Changes
- **🔧 Java players could lose their real chestplate** - a bug from 2.6.0, fixed now

### ✨ What's New

#### Breaking Fixes
- **Chestplate wipe on landing**: Landing (or a world/gamemode change, or a flight-timer force-land) always tried to restore a backed-up chestplate, even for Java players - but only Bedrock players ever get one backed up (that's part of the virtual-elytra swap). For Java players this cleared their chestplate slot to nothing instead of leaving it untouched, deleting whatever they were actually wearing.

#### Technical Improvements
- Reduced complexity in `onDoubleJump()` by splitting it into smaller helper methods (addresses CodeFactor's "Complex Method" and "one statement per line" findings) - no behavior change

### 📥 Installation
Download `SpawnElytra-2.6.1.jar` and place it in your plugins folder.

### 🔄 Upgrading
Drop-in replacement for v2.6.0 - no config changes.

### ⚠️ Important Notes
This only stops the bug going forward - if a player already lost their chestplate on 2.6.0, you'll need to give it back to them manually.

### 🌟 Why This Update?
- ✅ Stops real chestplates from being deleted while flying
- ✅ Drop-in upgrade, no config changes
```

---

## CurseForge (Markdown):

```markdown
## 📦 Paper/Bukkit - v2.6.1: Critical Chestplate Fix

### 🎯 Major Changes
- **🔧 Chestplate wipe bug**: Java players could lose their real chestplate on landing - fixed

### ✨ Breaking Fixes

#### Chestplate Handling
- **Chestplate wipe on landing**: `restoreChestplateIfPresent()` ran for every player on landing, world/gamemode change, and flight-timer force-land, unconditionally trying to restore a backed-up chestplate. Only Bedrock players ever get one backed up (part of the virtual-elytra swap) - for Java players this cleared their chestplate slot instead of leaving it alone, deleting whatever real chestplate they had equipped.

### 🔧 Technical Improvements
- **Reduced method complexity**: `onDoubleJump()` split into smaller helper methods, addressing CodeFactor's "Complex Method" and "one statement per line" findings - no behavior change

### 🔧 Compatibility
- **Minecraft:** 1.20.1 - 1.21.11
- **Java:** 21 LTS recommended
- **Platform:** Spigot/Paper

### 📥 Installation
Download `SpawnElytra-2.6.1.jar` and place it in your `plugins` folder.

**Upgrading from v2.6.0**: drop-in replacement, no config changes.

### ⚠️ Important Notice
This only prevents the bug going forward - chestplates already lost on 2.6.0 need to be given back manually.
```

---

## Discord Announcement:

```
🚀 **SpawnElytra v2.6.1 is now available!**

**Critical Chestplate Fix**

A hotfix for a bug introduced in 2.6.0 that could delete a Java player's real chestplate.

## 🔧 What's Fixed

**Breaking Fix:**
• Landing (or a world/gamemode change, or a flight-timer force-land) always tried to restore a backed-up chestplate - but only Bedrock players ever get one backed up. For Java players this wiped their chestplate slot to nothing instead of leaving it alone.

## 📥 Download & Links

**SpigotMC:** <https://www.spigotmc.org/resources/spawnelytra.129704/>
**Modrinth:** <https://modrinth.com/plugin/spawnelytra>
**GitHub Release:** <https://github.com/Knabbiii/SpawnElytra/releases/tag/v2.6.1>

## ⚠️ Important

If you're on 2.6.0 and players fly with real chestplates, **update now** - this can permanently delete their armor. This update only stops it going forward; already-lost chestplates need to be given back manually.

---

*Drop-in upgrade - no config changes.*
```
