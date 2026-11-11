# Cybernetic Enhancements Config

Cybernetic Enhancements has a client config for the beta welcome message, additional local presentation settings, and gameplay configuration files.

## In-game Config Screen

Open **Mods → Cybernetic Enhancements → Config → Client** to change the welcome message, update checker, and **Show archive opening animation**. Save your changes when leaving the editor.

This uses NeoForge's config screen. Welcome-message options apply to future world and server logins; the archive animation option applies the next time you open the archive. No restart is needed. The other properties files listed below are edited manually.

## Client Config

The client config is written as:

```text
config/cyberneticenhancements-client.toml
```

| Key | Default | Meaning |
| --- | --- | --- |
| `showArchiveBootAnimation` | `true` | Plays the Neural Archive opening sequence. Disable to show the wiki and other archive tabs immediately, with no input delay. Applies on the next opening; no restart required. |
| `showBetaWelcomeMessage` | `true` | Shows the formatted beta notice, installed versions, issue-report link, Discord link, and notification sound when joining a world or server. |
| `checkForUpdates` | `true` | Checks published CurseForge release/beta files for a newer compatible version when the welcome message appears. |

To disable the welcome message and its sound:

```toml
showBetaWelcomeMessage = false
```

To skip the archive's loading sequence:

```toml
showArchiveBootAnimation = false
```

The message and sound are local to your client. This setting does not broadcast anything or change another player's preferences. The notice appears once per login, after loading; respawning and changing dimensions do not repeat it.

### Automatic Update Checks

The welcome message can be followed by a local update notice with the installed and available versions and a clickable CurseForge file link. Updates are recommended for beta bug fixes; multiplayer players should follow their server or modpack's chosen version.

Metadata comes from [CFWidget's public API](https://cfwidget.com/), which provides CurseForge file information for [Cybernetic Enhancements](https://www.curseforge.com/minecraft/mc-mods/cybernetic-enhancements). The checker requires matching Minecraft and NeoForge tags and considers release and beta files, excluding alpha files. It compares version numbers, so a development build newer than the published files is not told to downgrade.

Checks run in the background with a timeout and are cached for one hour per game session. Unavailable or invalid metadata produces no chat warning. Nothing is downloaded or installed automatically. Disabling `checkForUpdates` stops these requests; disabling `showBetaWelcomeMessage` also suppresses the check and update notice.

## Sandevistan Presentation

```text
config/cyberneticenhancements/sandevistan-client.properties
```

| Key | Default | Meaning |
| --- | --- | --- |
| `afterimages` | `true` | Displays Sandevistan afterimages. |
| `edge_effects` | `true` | Displays screen-edge effects. |
| `restrained_fov` | `true` | Uses the restrained Sandevistan field-of-view effect. |
| `flash_intensity` | `0.20` | Screen-effect intensity, from `0.0` to `0.40`. |
| `sound_volume` | `0.50` | Sandevistan sound volume, from `0.0` to `1.0`. |

These settings affect presentation only. Restart the client after editing this file. Server gameplay values still determine the ability's strength, duration, and cooldown.

## HUD Visibility

```text
config/cyberneticenhancements-client.properties
```

| Key | Default | Meaning |
| --- | --- | --- |
| `hud.enabled` | `true` | Shows the cyberware HUD. |

Use **Toggle HUD** in Minecraft's controls menu, bound to **H** by default, to change this immediately and save your preference. Restart the client after manually editing the file. This properties file is separate from the client TOML config.

## Gameplay Config Files

For multiplayer, edit these files on the server. Restart the server after changes; for singleplayer, restart the game.

| File | What it controls |
| --- | --- |
| `config/cyberneticenhancements-balance.properties` | Base cyberware effects, capacity and chip slots, module and chipware effects, operating-system timings, triggered cooldowns, combat statuses, cyberstrain, consumables, and implant ability values. |
| `config/cyberneticenhancements-psychosis-targeting.properties` | Which entities cyberpsychosis targeting ignores. |
| `config/cyberneticenhancements-npc-identities.properties` | NPC name pools and identity categories used for newly generated identities. |

The mod writes supported keys and their current values into these files. Use the generated balance file as the reference for the complete list of per-implant and ability settings.

### Cyberpsychosis Targeting

| Key | Default | Meaning |
| --- | --- | --- |
| `ignore_tamed_animals` | `true` | Excludes tamed animals from cyberpsychosis targeting. |
| `ignore_entity_types` | Bats, fish, dolphins, squid, silverfish, and endermites | Comma-separated entity type IDs to exclude. The generated file lists the exact default IDs. |

### NPC Identities

`male_names`, `female_names`, and `generic_names` contain comma-separated name pools. Categories also select the corresponding NPC skin folder. Existing NPCs retain their saved names and categories.

## Datapack Interactions

Cyberware upgrade tracks, fabrication recipes, recycling recipes, and schematic discovery loot are configured through server datapacks. See [DATAPACKS.md](DATAPACKS.md) for their file formats and reload behavior.

The client welcome-message setting and presentation files do not change server-owned upgrade costs, schematic knowledge, or gameplay rules.
