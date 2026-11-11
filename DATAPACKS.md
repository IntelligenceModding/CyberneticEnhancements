# Cybernetic Enhancements datapacks

Cyberware upgrade tracks and repair prices are server data. The mod ships one file per upgradeable implant in:

`data/cyberneticenhancements/cyberware_upgrades/<cyberware_id>.json`

Copy a bundled file into the same path in a higher-priority datapack to override it. For example, David's implant uses `militech_experimental_sandevistan.json`. This replaces that implant's entire service definition; other implants keep their own definitions. An empty `levels` array disables its upgrades. Omitted `repair_materials` disables repairs for that implant, so retain that field when overriding only an upgrade track.

Install the datapack in `<world>/datapacks/` on the server (or your singleplayer world), then run `/reload` as an operator. Minecraft 1.21.1 datapacks use a `pack.mcmeta` such as:

```json
{
  "pack": {
    "pack_format": 48,
    "description": "Custom cyberware upgrades"
  }
}
```

A one-level track could look like this:

```json
{
  "levels": [
    {
      "effects": {"movement_speed": 0.03, "attack_speed": 0.1},
      "capacity_bonus": 0,
      "chip_slot_bonus": 0,
      "module_slot_bonus": 0,
      "duration_multiplier": 1.15,
      "cooldown_multiplier": 0.9,
      "instability_multiplier": 0.9,
      "materials": [
        {"item": "cyberneticenhancements:legendary_item_components", "count": 1},
        {"item": "cyberneticenhancements:neural_processor", "count": 1}
      ]
    }
  ]
}
```

- Array entry 1 is upgrade level 1, entry 2 is level 2, and so on (maximum 32).
- Bonuses are **cumulative totals at that level**, not additions to the preceding level. Base implant effects are added separately. Each level can have different bonuses and prices.
- `effects` uses effect IDs from `CyberwareEffectType`, such as `armor`, `max_health`, `attack_damage`, `attack_speed`, `movement_speed`, `health_regen`, or `chrome_capacity`. Amounts keep the same units as the base effect: attack speed adds attribute points, while movement speed is a proportional multiplier bonus. Values must be finite and between -10000 and 10000.
- `capacity_bonus` adds capacity points (0..10000); `chip_slot_bonus` and `module_slot_bonus` add sockets/bays where the implant supports them (0..16).
- The three multiplier fields tune **Sandevistan activation**. Duration multiplies base duration before condition scaling; cooldown multiplies the cooldown after other cyberware reductions; instability multiplies activation and running instability before repeated-use/condition penalties. They do not change overload thresholds, wear, movement bonuses, or slowdown strength. Duration accepts 0.1..10, cooldown 0.05..10, and instability 0..10. Existing gameplay duration bounds still apply.
- Omitted bonuses default to zero and omitted multipliers default to one. `materials` is required and contains one or two distinct registered item IDs. Counts must be whole numbers from 1 through the item's maximum stack size, capped at 64. Costs are for reaching that particular level.
- Unknown effect names, fields, items, or cyberware IDs and invalid values are rejected. Error messages identify the upgrade file. A structurally invalid reload does not publish a partially updated upgrade table.

The server syncs the effective definitions at login and after successful reloads. Tooltips, wiki upgrade tracks, and Upgrade Station previews use this synchronized view. Upgrade output, material consumption, passive bonuses, and activation tuning are calculated on the server. Changing a client resource pack, local JSON, or the client's display copy cannot change a multiplayer server's rules; there is no client-to-server upload packet for these definitions.

Existing items retain their stored upgrade level. If a track is shortened or disabled, its effective level is capped to the new track length without rewriting the stored level. Reloads refresh passive bonuses and open Upgrade Station previews. Activation tuning is captured when a Sandevistan session starts, so an already active session retains its original tuning until it ends.

## Fabrication recipes and discoveries

Fabricator recipes use the normal, server-controlled recipe datapack system. Defaults are in:

`data/cyberneticenhancements/recipe/fabrication/<item_id>.json`

A minimal counted, fixed-position recipe:

```json
{
  "type": "cyberneticenhancements:fabrication",
  "parts": [
    {"slot": 12, "ingredient": {"item": "minecraft:iron_ingot"}, "count": 4},
    {"slot": 7, "ingredient": {"tag": "minecraft:planks"}, "count": 2}
  ],
  "result": {"id": "cyberneticenhancements:alloy_plate", "count": 4},
  "tier": 1,
  "ticks": 100,
  "schematic": "",
  "category": "Components"
}
```

Slots are numbered 0 through 24, left-to-right and top-to-bottom in a 5x5 grid. Unlisted positions must be empty. Every listed position must contain its required ingredient and count (default 1). Tags and alternative ingredients use Minecraft's ingredient format. Layouts do not rotate, mirror, or shift. Duplicate positions and empty recipes are rejected.

`tier` is 1..5. Basic machines support tiers 1-2, Precision machines tiers 1-4, and Restricted machines tiers 1-5. `ticks` is 1..72000, at 20 ticks per second. No fuel or external energy is required. `category` organizes the built-in browser. Recipes with an empty `schematic` are known immediately; otherwise the value is a registered design ID, such as `dynalar_sandevistan` or `militech_apogee`. Players learn it by right-clicking the corresponding `<design>_schematic` item. Learning does not consume it. Knowledge is permanent and server-owned, and survives death; players do not need to carry learned schematics. Designs requiring schematics must be learned from their schematic items.

Schematic requirements apply to each fabrication recipe, including datapack-added recipes and multiple recipes sharing one design. Locked recipes are absent from JEI recipe/usage searches and the Fabricator browser; ingredients, previews and filling become available after the server confirms learning. The wiki also checks the current recipe requirement before showing a cached layout. Learning updates visibility without reconnecting. Recipes with an empty `schematic` remain available by default. Missing archive collection tiles remain silhouettes and cannot open recipe details. The server independently rejects unlearned selection, filling and fabrication, including recipe-based shift-click routing.

Default recipes replace crafting-table routes for finished cyberware, chipware and limb modules. Basic materials, consumables and station blocks retain their crafting-table recipes. Override any individual recipe with a server datapack and `/reload`. Existing implants remain installable and upgrade data carries forward through model-to-model fabrication.

Discovery loot tables are `data/cyberneticenhancements/loot_table/fabrication/discoveries_2.json` through `discoveries_5.json`. Uncommon through Legendary relic caches reference the corresponding tables. They provide schematics and parts, with proprietary cores at high tiers. Schematics reserved for quest progression are excluded from cache pools; specialist contracts can award missing designs. Existing intact implant loot remains available. Techies sell Neural Processors as parts stock.

Recycling recipes in `data/cyberneticenhancements/recipe/recycling/` define the entire recovery batch. There are no implicit extra outputs. Specialized fabrication components have partial salvage recipes; restricted cores and schematic items cannot be returned by recycling.

### Repair prices and upgrade balance

Each implant's `cyberware_upgrades/<id>.json` can contain `repair_materials` alongside `levels`:

```json
{
  "repair_materials": [
    {"item": "cyberneticenhancements:rare_item_components", "count": 2},
    {"item": "cyberneticenhancements:basic_circuit_plate", "count": 2}
  ],
  "levels": []
}
```

The repair counts are prices for restoring a completely broken implant. Actual costs are `ceil(count × missing integrity fraction)`, with at least one of each material for any damage. Repair always restores full integrity in one action. Use one or two distinct material types, with the same item/count validation as upgrade materials. Normal repairs never consume proprietary cores in the bundled definitions.

Bundled upgrade caps remain T1: 2, T2: 3, T3: 4, T4/T5: 5. Prices grow gently: T1 uses 2–4 Common bundles and one functional part; T2/T3 use 1–3 bundles and 1–2 parts; T4 uses 1–2 bundles and 1–2 parts; T5 uses 1–2 bundles and 1–3 parts. Datapacks may override these values. Upgrading advances exactly one level, retains wear, and preserves the original item's data.

`function_multiplier` (default 1, allowed 1..3) scales an implant's implemented special behavior. It is cumulative, like the other bonuses. The preview names the affected behavior and shows its before/after percentage. Bundled examples include optics detection range, Blood Pump/Biomonitor/Second Heart healing, neural cooldown refunds, pickup range, specialized mining bonuses, and Microgenerator shock damage. It is not a generic multiplier for all stats. The server rejects a non-default value on models without a function-multiplier implementation; use the fields already present in that model's bundled file. Sandevistans continue using their duration/cooldown/instability fields, and ordinary passive improvements continue using `effects` or `capacity_bonus`. Pure capacity implants gain capacity rather than unrelated combat stats.

### Complete salvage recipes

```json
{
  "type": "cyberneticenhancements:recycling",
  "ingredient": {"item": "cyberneticenhancements:dynalar_sandevistan"},
  "outputs": [
    {"id": "cyberneticenhancements:neural_processor", "count": 1},
    {"id": "cyberneticenhancements:basic_circuit_plate", "count": 1}
  ],
  "worn_outputs": [
    {"id": "cyberneticenhancements:neural_processor", "count": 1}
  ],
  "damaged_outputs": [
    {"id": "cyberneticenhancements:basic_circuit_plate", "count": 1}
  ]
}
```

All three batches are required and contain one to three material stacks. Integrity at least 75% uses `outputs`; 45% to below 75% uses `worn_outputs`; below 45% uses `damaged_outputs`. Non-implant items always use `outputs`; give them identical batches. Identical output types combine into one stack, which must fit the item's stack limit. Full cyberware, schematic items and proprietary cores are rejected as recovery outputs.

The Recycler's preview is exactly the chosen batch. A server-validated **Recycle 1** action consumes one input and creates all outputs together. Upgrades do not increase recovery or refund their materials. Stored chips/modules are destroyed with their host, as the GUI warns. Keep custom salvage strictly cheaper than manufacturing its input, including the cost and output counts of nested subassemblies. The bundled recipes are checked against their expanded manufacturing costs in server tests. The previous `result`/`damage_sensitive` recipe format is replaced by these explicit batches; existing custom recycling datapacks must migrate.
### Schematic collection and discovery history

The archive's Schematics tab uses a full-width icon grid with search, Owned/Missing filters and category filtering. Missing designs use black item silhouettes and obfuscated labels; learning reveals the name and icon. Selecting a tile opens a detail page with a Back button. Learned designs show every server recipe referencing that schematic, including the counted 5x5 layout, total materials, output, processing time and machine tier.

The server records the first unlock's real-world date, world time, dimension, coordinates and learning method (studying a schematic item). This record survives saving, death and respawn; learning the same design again does not replace its original history. Existing saves keep their previously learned designs, with old discovery details shown as unavailable. Client display data cannot grant server unlocks or rewrite their recorded history.
### Dedicated workshop machines

Repair Stations (the existing `tech_station` block) restore integrity; the separate `upgrade_station` applies the server-defined upgrade track. Upgrading preserves current integrity. Both retain existing upgrade progress and custom item data. Fill materials transfers the listed costs from your inventory, and an explicit Repair/Upgrade button commits the operation. Inputs and completed output return to your inventory when closing the menu.

The Recycler previews recovery before an explicit Recycle 1 action. Each action consumes exactly one input and produces all recovery together. Pending recovered parts must be collected before another operation; closing returns them to the player. Recovery does not refund implant upgrades.
