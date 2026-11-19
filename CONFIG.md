# Cybernetic Enhancements Config

Cybernetic Enhancements has a client config for the beta welcome message, additional local presentation settings, and gameplay configuration files.

## In-game Config Screen

Open **Mods → Cybernetic Enhancements → Config → Client** to change the welcome message, update checker, **Show archive opening animation**, **Call window on left**, **Show compass**, and **Show dropoff delivery messages**. Save your changes when leaving the editor.

This uses NeoForge's config screen, including **Custom death screen**. Screen customizations normally default to enabled, with automatic compatibility defaults described below. Welcome-message options apply to future world and server logins; the archive animation option applies the next time you open the archive. No restart is needed. The other properties files listed below are edited manually.

For new, missing, or reset client settings, detected screen replacement mods change the defaults: **FancyMenu** defaults `customLoadingScreen`, `customTitleBackground`, `customTitleScreen`, and `showLaunchIntro` to `false`. **Drippy Loading Screen** alone defaults only `customLoadingScreen` to `false`. **PackMenu** or **Custom Main Menu** defaults `customTitleBackground`, `customTitleScreen`, and `showLaunchIntro` to `false`. The custom death screen defaults to enabled regardless of detected menu mods. Existing saved `true` and `false` values are preserved; modpack authors can explicitly enable any option. Other UI and gameplay settings are unaffected. The table below shows defaults without these replacement mods.

The Neural Archive also has a **Compass** tab. **Display** offers a live preview, individual visibility toggles, width, top spacing, opacity and the angle used to reveal names and distances. **Markers** lets you search and filter known landmarks, hide/show individual markers, give them local labels, cycle their colors and delete entries. Delete removes saved banner, cache and chromium waypoints from your player data. Deleted chromium discoveries stay dismissed when you look at the same ore again; banner and cache markers can be added again through their usual interactions. Live quest and NPC entries are permanently dismissed from the compass list for this world/server and player. Changes save immediately. **Reset display** restores the display defaults; **Restore markers** clears local hiding, labels and colors for the current world/server only; it does not restore deleted entries. The first-join Night City landmark is also listed here, including after it automatically disables on your first arrival. Its Show/Hide state and visit history are saved for your player on the server; labels and colors stay local. Restore markers returns it to enabled before visiting or disabled after visiting. Banner names in the world and quest progress are unchanged. The tab can be reordered like the other archive tabs.

## Client Config

The client config is written as:

```text
config/cyberneticenhancements-client.toml
```

| Key | Default | Meaning |
| --- | --- | --- |
| `customDeathScreen` | `true` | Uses the red FLATLINED death screen, local sound and first-person backward fall. The opening effects leave the fall visible. Camera movement checks block collision and preserves your camera preference after respawning. Disable for vanilla visuals and audio. Keeps Minecraft's death controls and hardcore behavior. Screen Effects at zero disables the fall; camera roll follows Damage Tilt strength. Sound follows Master Volume. No restart required. |
| `customLoadingScreen` | `true` | Styles Mojang's loading logo and continuous progress bar with the mod's dark background, red accents and scan lines. Leaves NeoForge's early window and the title screen unchanged. Screen Effects at zero disables moving scans. Disable to restore the original overlay. |
| `customTitleBackground` | `true` | Replaces Minecraft's shared panorama wherever it is used, including the title screen, menu backgrounds and accessibility onboarding, with nine images shuffled without replacement. One continuous sequence across screens, with no immediate or A-B-A repeats, including between cycles. Changes every 10 seconds with a 1.5-second crossfade. Center-crops to fill any aspect ratio without stretching. Screen Effects at zero uses hard cuts. Existing menu controls, blur and overlays are preserved. Independently constructed panoramas from other mods are unaffected. Disable for the vanilla panorama. |
| `customTitleScreen` | `true` | Applies the death screen's red palette to the centered Minecraft logo, footer text and vanilla menus plus the NeoForge Mods screen, including world selection, Create World, multiplayer, options and pause menus. Styles standard and option-cycle buttons, tabs, slider tracks and handles, checkboxes, icon buttons, the difficulty lock, search fields, list cards, scrollbars, separators, menu panels and preparing/saving message panels. Neutral menu text uses the Journal's readable pink tones, including in-world menu headings; warning, error and custom colors are preserved. Inventory/container screens, recipe-book controls and gameplay HUD colors retain native rendering. Keeps native layouts, hitboxes, input, tooltips, update badges and copyright links. Background shading, scan lines and the moving sweep continue across screens using Minecraft's shared panorama. Menu scan effects are drawn after normal background blur, before controls. Independent of the slideshow and death screen settings. Disabling it restores native widget rendering. Screen Effects at zero disables moving scans. Also plays the mod's looping title music on the Music volume throughout title menus, including world selection and options. It fades in over two seconds and out over one when entering a world, and returns with the next title visit. If the music cannot be heard (Music or Master volume is 0), the slideshow's quiet per-image ambience (Ambient volume) plays instead, resuming at the next image after the music is lowered to 0, and the music restarts with its fade-in when raised again. Disabling this option keeps the title music off and leaves only that ambience. |
| `showLaunchIntro` | `true` | After actual loading finishes, plays a once-per-launch sequence: Mojang, Intelligence Modding/Doomed Artemis, NeoForge, Minecraft, then a two-column page with copyright information on the left and client screen settings on the right. At 15.100 seconds, a thank-you page mentions Doomed Artemis, Intelligence Modding and their community platforms. At 18.000 seconds, a beta notice encourages issue reports. These pages use subtle fades, then the beta notice automatically glitches into the live title screen using the same horizontal tearing, brief black separator and scanline burst as the logo transitions, without Pigstep. If the tutorial is still open, the first title screen of each launch shows a **Tutorial Recommended** panel in place of the menu, with **Play Tutorial** and **No Thanks**. No Thanks marks the tutorial as skipped and fades in the normal title menu, which keeps a plain **Play Tutorial** button sharing the Realms row. The state is stored per instance in `config/cyberneticenhancements/tutorial.properties` as `open`, `done` or `skipped`. The panel does not return after a world or the tutorial in the same launch. The real title menu initializes normally underneath the presentation; no screen is swapped or rebuilt at the handoff. Escape glitches the current page directly into the menu and stops the startup clip. Menu input is blocked until the transition finishes. Screen Effects at zero uses a quiet dissolve instead of tearing. The full supplied 20.636-second startup sound plays once; the beta notice holds through its tail, with logo transitions at 3.26, 6.18, 9.18 and 12.22 seconds driven by its actual playback position. Audio and presentation pause together when unfocused. Silent/unavailable audio uses the same visual cue times. Fades advance every rendered frame; respects Screen Effects and Master volume. Stops its audio when leaving. Slideshow and intro textures join native title resource preloading. Does not change the real loading overlay or replay on returning from a world. |
| `showCompass` | `true` | Shows the slim N/E/S/W compass, quest navigation, the Night City guide and named banner landmarks at the top center, following Minecraft's GUI scale like the hotbar. Gold markers guide you to objectives; cyan markers guide ready quests back to their issuer; flags use banner colors. Applies immediately and respects Toggle HUD and F1. Moves below boss bars and cyberpsychosis alerts when needed. |
| `compassQuests`, `compassTurnIns`, `compassBanners`, `compassCities`, `compassMaxTac` | `true` | Enables each marker category independently. MaxTec markers follow pursuit squads; resident city guards do not appear on the compass. |
| `compassCardinals`, `compassTicks`, `compassCenter` | `true` | Shows cardinal labels, bearing ticks and the center pointer. |
| `compassNames`, `compassDistances` | `true` | Shows names and distances while looking toward a marker. |
| `compassHeightArrows`, `compassEdgeArrows` | `true` | Shows height indicators and arrows for offscreen landmarks. |
| `compassWidth` | `192` | Compass width in GUI pixels, from 128 to 320. |
| `compassTopMargin` | `6` | Gap below the top edge or reserved HUD space, from 0 to 60 GUI pixels. |
| `compassFocusAngle` | `10` | Angle in degrees used to reveal marker text, from 3 to 30. |
| `compassOpacity` | `1.0` | Compass opacity from 0.4 to 1.0. |
| `compassMarkerOverrides` | Empty | Automatically saved local hiding, dismissed live entries, labels and colors. Scoped to each world/server and player; use the Compass tab to edit them. |
| `customCompassMarkers` | Empty | Personal markers created with Alt+M (rebindable). Saved per world/server and player; manage names, colours, visibility and deletion in Neural Archive > Compass. |
| `showDropoffDeliveryMessages` | `true` | Shows a local chat message when quest items arrive at your Dropoff Point. Applies immediately; the holographic marker and arrival sound stay enabled. |
| `callWindowOnLeft` | `true` | Places NPC and player call windows in the top-left corner when enabled, or the top-right when disabled. Applies immediately, including during a call; no restart required. Existing saved preferences are preserved. |
| `enablePlayerCalls` | `true` | Allows incoming and outgoing player voice calls. Disabling ends your player call and makes your line unavailable. NPC calls are unaffected. Requires Simple Voice Chat on both clients and the server. |
| `archiveTabOrder` | Default archive order | Drag Neural Archive tabs to reorder them. The client saves the order automatically across worlds, servers and restarts. Unknown or duplicate IDs are ignored and new tabs are appended. |
| `showArchiveBootAnimation` | `true` | Plays the Neural Archive opening sequence. Disable to show the wiki and other archive tabs immediately, with no input delay. Applies on the next opening; no restart required. |
| `showJoinAnimation` | `true` | Carries the title slideshow through world loading and saving when the custom title background is enabled. Two bars open from the center on join and close toward the center on leave, then the destination menu fades in over the slideshow. Screen Effects at zero uses gentle fades. The tutorial uses its own presentation. |
| `showBetaWelcomeMessage` | `true` | Shows the formatted beta notice, installed versions, issue-report link, Discord link, and notification sound when joining a world or server. |
| `checkForUpdates` | `true` | Checks published CurseForge release/beta files for a newer compatible version when the welcome message appears. |

To disable the welcome message and its sound:

Click **Disable this message** in the join or update notice to save this preference directly, or edit the client config below. This disables both notices and the join sound; pending update results are suppressed too. You can enable it again in the in-game client config screen.

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

## Server-Enforced Client Settings

Server owners can make every player use specific client settings while connected, for example to keep the custom death screen on for everyone. Enforced settings are locked: the player's config screen shows the enforced value with a padlock and a "N enforced by this server" notice, and the setting cannot be edited, reset or saved there. The player's own files are never changed; their values return as soon as they leave.

```text
config/cyberneticenhancements-server.toml
<world>/serverconfig/cyberneticenhancements-server.toml   (optional per-world override)
```

```toml
enforcedClientSettings = ["customDeathScreen=true", "showJoinAnimation=false", "compassWidth=224", "hud.enabled=true"]
```

- **Keys:** any key from `cyberneticenhancements-client.toml` above, plus the properties-file settings `sandevistan.afterimages`, `sandevistan.edge_effects`, `sandevistan.restrained_fov`, `sandevistan.flash_intensity` (0 to 0.4), `sandevistan.sound_volume` (0 to 1) and `hud.enabled`. With `hud.enabled` enforced, the Toggle HUD key shows a short notice instead of toggling.
- **Values:** `true`/`false` for switches and numbers within the setting's allowed range. List settings (pinned designs, markers, tab order) are personal and cannot be enforced.
- **Mistakes are reported, not fatal:** an invalid line is skipped and named in the server log, and the other lines still apply. The client log lists what a server enforces and which of the player's own values it replaces.
- **When it applies:** NeoForge sends this file to each player while they connect, so changes reach players when they next join. Enforcement ends with the connection; nothing carries over to the title screen or other worlds.
- **Singleplayer and modpacks:** in singleplayer, the instance's own `config/cyberneticenhancements-server.toml` applies, so a modpack can ship enforced settings in `defaultconfigs/`. The list is empty by default.

## Gameplay Config Files

For multiplayer, edit these files on the server. Restart the server after changes; for singleplayer, restart the game.

| File | What it controls |
| --- | --- |
| `config/cyberneticenhancements-balance.properties` | Base cyberware effects, capacity and chip slots, module and chipware effects, operating-system timings, triggered cooldowns, combat statuses, cyberstrain, consumables, and implant ability values. |
| `config/cyberneticenhancements-psychosis-targeting.properties` | Which entities cyberpsychosis targeting ignores. |
| `config/cyberneticenhancements-npc-identities.properties` | NPC name pools and identity categories used for newly generated identities. |

The mod writes supported keys and their current values into these files. Use the generated balance file as the reference for the complete list of per-implant and ability settings.

### Damaged Cyberware and Water

Installed implants at 15% integrity or lower can short out in water or exposed rain. Brief exposure is safe; sustained exposure causes half a heart of damage and one second of Shock, at most once every 30–45 seconds across all implants. Repairing the implants removes the hazard. Creative and spectator players are exempt.

These settings are in `cyberneticenhancements-balance.properties` and are controlled by the server:

| Key | Default | Meaning |
| --- | --- | --- |
| `condition.short_circuit.integrity_threshold` | `0.15` | Maximum integrity fraction that can short out. |
| `condition.short_circuit.water_exposure_ticks` | `60` | Continuous water exposure before a short circuit. |
| `condition.short_circuit.rain_exposure_ticks` | `100` | Continuous rain exposure before a short circuit. |
| `condition.short_circuit.cooldown_ticks` | `600` | Minimum time between short circuits. |
| `condition.short_circuit.cooldown_variation_ticks` | `300` | Random extra cooldown, up to this many ticks. |
| `condition.short_circuit.shock_ticks` | `20` | Duration of the single-stack Shock pulse. Existing Shock is not strengthened or extended. |
| `condition.short_circuit.damage` | `1.0` | Damage per short circuit; set to `0` to disable damage. |

20 ticks equal one second. Exposure is checked once per second; getting dry resets exposure but keeps the cooldown.

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
