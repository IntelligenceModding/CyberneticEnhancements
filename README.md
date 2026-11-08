# Cybernetic Enhancements

A Minecraft 1.21.1 NeoForge mod featuring cyberware, body upgrades, NPC services, contracts, and relic-cache hacking.

The current release is an **alpha**. Gameplay, balance, and saved-data formats may change during development. Back up existing worlds before updating.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.228 or newer within the Minecraft 1.21.1 line
- Java 21
- Install the mod on both the client and the server for multiplayer.
- JEI is optional; it adds recipe integration.

## Features

- Body-slot cyberware with passive effects, activated abilities, modules, and chipware
- Cyberstrain, cyberpsychosis, implant condition, repairs, upgrades, and recycling
- Fixers, ripperdocs, techies, netrunners, and mercs with shared dialogue trees per type
- Delivery, extraction, investigation, breach, elimination, and recovery contracts
- Relic caches with timed hacking puzzles and rewards
- Eurodollar banking, payments, contacts, quest tracking, and an in-game archive

Default keys: **J** opens the archive, **H** toggles the HUD, and **V / B / G / C** activate operating-system, auxiliary, arm, and face abilities. Bindings can be changed in Controls.

## Building

Use JDK 21 and the included Gradle wrapper:

```powershell
.\gradlew.bat runData
.\gradlew.bat clean build
```

Release JARs are written to `build/libs/` using the pattern `cyberneticenhancements-<version>-neoforge-mc1.21.1.jar`.

Include generated resources under `src/generated/resources/` when committing release sources. Do not include its `.cache/` directory.

## License

MIT; see [LICENSE](LICENSE).
