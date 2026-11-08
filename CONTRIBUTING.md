<p align="center">
  <img src="https://raw.githubusercontent.com/IntelligenceModding/Cybernetic-Enhancements/refs/heads/assets/Project%20Title.png" alt="Cybernetic Enhancements" width="900">
</p>

<h1 align="center">Contributing to Cybernetic Enhancements</h1>

Thank you for helping improve Cybernetic Enhancements. Bug fixes, gameplay improvements, compatibility fixes, assets, translations, and documentation are welcome.

**This is an alpha project. Features, balance, and saved-data formats can change, including changes that break existing worlds. Test with disposable worlds or backups, and explain compatibility impacts in your pull request.**

## Reporting Problems and Proposing Features

Use the [issue forms](https://github.com/IntelligenceModding/Cybernetic-Enhancements/issues/new/choose) for bugs, feature suggestions, and Minecraft or loader support requests. Search existing issues first. For substantial gameplay changes, discuss the intended behavior before implementing them.

For general help, see [SUPPORT.md](SUPPORT.md). Participants are expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Development Setup

This branch targets **Minecraft 1.21.1 with NeoForge**, and requires **JDK 21**. Use the included Gradle wrapper; a separate Gradle installation is unnecessary. Configure your IDE's Gradle JVM to use JDK 21.

Fork and clone the repository, start from the branch matching the intended Minecraft version and loader, and create a working branch. The current gameplay branch is `1.21.1-neoforge`; the `assets` branch holds repository graphics rather than gameplay source.

## Building and Running

On Linux or macOS:

```sh
chmod +x gradlew
./gradlew runData
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat runData
.\gradlew.bat build
```

Run data generation and the build as separate commands so the build packages the newly generated resources. Build includes the automated tests; release JARs are written to `build/libs/`.

Use `runClient` or `runServer` with the wrapper for development launches. For a dedicated-server test, use a disposable world, stop the server when finished, and follow Minecraft's EULA requirements. The mod is installed on both sides in multiplayer. JEI is optional for players and available in the development runtime.

## Project Structure

- `src/main/java/.../common/` contains gameplay, registries, menus, persistence, payloads, and data providers.
- `src/main/java/.../client/` contains screens, rendering, HUD, input, and JEI integration.
- `src/main/resources/` contains maintained textures, models, and data files.
- `src/generated/resources/` contains generated language, model, recipe, loot, and tag files.
- `src/test/java/` contains regression tests.

Update data providers when changing generated content, run `runData`, and include the resulting resources with the change. Generated JSON alone is insufficient if the next generator run would overwrite it. Do not commit the generator's `.cache/` directory.

## Testing Gameplay Changes

Use relevant tests and in-game checks. A passing build does not establish gameplay correctness.

- Cyberware: installation, removal, capacity, condition, ability cooldowns, and death/respawn behavior.
- NPCs and dialogue: type-specific branches, trust changes, one-time rewards, refusal, services, and identity persistence.
- Quests: acceptance, objective progress, turn-in, abandonment, guidance items, and reconnect/reload behavior.
- Relic caches: legal moves, solvability, timer expiry, retries, ownership, and reward delivery.
- Banking: transfers, requests, balance limits, offline targets, failed saves, and duplicate-payment prevention.
- Screens and rendering: different GUI scales, active skins, resource reloads, tooltips, and readability.
- Networking and persistence: server-side validation, multiple players, reconnects, and world reloads.

Add regression tests where they meaningfully cover a bug or behavior. Keep shared gameplay code independent of client-only classes. Preserve existing save data where practical; when a change is intentionally incompatible, document that clearly rather than silently discarding state.

## Pull Requests

Use the [PR template](.github/PULL_REQUEST_TEMPLATE.md). Explain the concrete problem, final behavior, validation, and save or balance impact. Keep unrelated refactors and formatting out of the change. Include screenshots for visual changes and disclose tests that were not performed.

Do not add Java code comments, unused helpers, temporary files, extracted Minecraft/NeoForge sources, logs, build output, or IDE state. Follow existing naming and formatting. Keep Gradle configuration portable; do not commit local JDK paths.

Only submit code and assets you have permission to redistribute, with attribution where required. The project uses the [MIT License](LICENSE); do not assume third-party material is covered by it.
