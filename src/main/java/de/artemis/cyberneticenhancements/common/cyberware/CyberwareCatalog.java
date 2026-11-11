package de.artemis.cyberneticenhancements.common.cyberware;

import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CyberwareCatalog {
    private static final List<CyberwareDefinition> DEFINITIONS = List.of(
            def("camillo_ram_manager", "Camillo RAM Manager", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_2, "Emergency RAM management hardware.", Items.REPEATER, CyberwareEffect.chromeCapacity(4.0D)),
            def("ram_reallocator", "RAM Reallocator", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_4, "Advanced RAM redistribution core.", Items.RECOVERY_COMPASS, false, "camillo_ram_manager", CyberwareEffect.chromeCapacity(8.0D)),
            def("bioconductor", "Bioconductor", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_2, "Neural cooldown routing implant.", Items.CLOCK),
            def("cox_2_cybersomatic_optimizer", "COX-2 Cybersomatic Optimizer", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_4, "Aggressive neural overclock optimizer.", Items.BLAZE_POWDER, false, "bioconductor"),
            def("ex_disk", "Ex-Disk", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_1, "Expanded memory buffer disk.", Items.COMPARATOR, CyberwareEffect.chromeCapacity(6.0D)),
            def("kerenzikov_boost_system", "Kerenzikov Boost System", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_2, "Predictive pre-response neural shim.", Items.SUGAR),
            def("mechatronic_core", "Mechatronic Core", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_2, "Control routines for machine interaction.", Items.PISTON, CyberwareEffect.breakSpeed(0.15D)),
            def("memory_boost", "Memory Boost", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_1, "Baseline recall augmentation.", Items.REDSTONE),
            def("newton_module", "Newton Module", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_1, "Aggression-linked cooldown helper.", Items.FIREWORK_STAR),
            def("axolotl", "Axolotl", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_5, "Top-tier combat reset implant.", Items.NETHER_STAR, false, "newton_module"),
            def("quantum_tuner", "Quantum Tuner", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_5, "Quantumized cyberware reset controller.", Items.ECHO_SHARD),
            def("ram_upgrade", "RAM Upgrade", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_1, "General neural memory expansion.", Items.AMETHYST_SHARD, CyberwareEffect.chromeCapacity(4.0D)),
            def("self_ice", "Self-ICE", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_3, "Counterintrusion brainware.", Items.SHIELD),
            chipSocket("chipware_socket", "Chipware Socket", CyberwareTier.TIER_1, "Basic neural socket for swappable skill chips.", Items.COMPARATOR, null, 1),
            chipSocket("chipware_socket_mk2", "Chipware Socket Mk.2", CyberwareTier.TIER_3, "Expanded neural socket rack for multiple skillchips.", Items.REPEATER, "chipware_socket", 2),
            def("chipware_socket_mk3", "Chipware Socket Mk.3", CyberwareSlotType.FRONTAL_CORTEX, CyberwareTier.TIER_5, "High-density chipware bus with full quick-swap support.", Items.ENDER_CHEST, false, "chipware_socket_mk2", 0, 3, CyberwareEffect.chromeCapacity(2.0D)),

            def("biodyn_berserk", "BioDyne Berserk", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_2, "Adrenal combat operating system.", Items.NETHER_WART, 0, CyberwareEffect.damage(1.0D), CyberwareEffect.bonusAbsorption(4.0D), CyberwareEffect.damageReduction(0.10D)),
            def("militech_berserk", "Militech Berserk", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_3, "Heavy combat operating system.", Items.IRON_AXE, false, "biodyn_berserk", 0, CyberwareEffect.damage(2.0D), CyberwareEffect.bonusAbsorption(4.0D), CyberwareEffect.damageReduction(0.10D), CyberwareEffect.attackSpeed(0.10D)),
            def("moore_tech_berserk", "Moore Tech Berserk", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_3, "Offensive berserk suite.", Items.DIAMOND_AXE, false, "militech_berserk", 0, CyberwareEffect.damage(2.0D), CyberwareEffect.moveSpeed(0.20D), CyberwareEffect.damageReduction(0.05D)),
            def("zetatech_berserk", "Zetatech Berserk", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_3, "Refined berserk actuator suite.", Items.GOLDEN_AXE, false, "moore_tech_berserk", 0, CyberwareEffect.damage(2.0D), CyberwareEffect.damageReduction(0.20D), CyberwareEffect.fallReduction(-0.15D)),
            def("arasaka_shadow", "Arasaka Shadow", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_1, "Low-profile operating system shell.", Items.ENDER_PEARL, 2, CyberwareEffect.moveSpeed(0.05D)),
            def("biotech_sigma", "Biotech Sigma", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_2, "Combat support operating system.", Items.BREWING_STAND, false, "arasaka_shadow", 4, CyberwareEffect.healthRegen(0.4D)),
            def("militech_paraline", "Militech Paraline", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_2, "Utility combat operating system.", Items.LIGHTNING_ROD, false, "biotech_sigma", 5, CyberwareEffect.attackSpeed(0.10D)),
            def("netwatch_netdriver", "NetWatch Netdriver", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_3, "High-end neural control operating system.", Items.ENDER_EYE, false, "militech_paraline", 6, CyberwareEffect.damageReduction(0.05D)),
            def("raven_microcyber", "Raven Microcyber", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_4, "Wide-spectrum control operating system.", Items.CRYING_OBSIDIAN, false, "netwatch_netdriver", 8, CyberwareEffect.moveSpeed(0.06D), CyberwareEffect.attackSpeed(0.08D)),
            def("tetratronic_rippler", "Tetratronic Rippler", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_4, "Premium control operating system.", Items.ENCHANTED_BOOK, false, "raven_microcyber", 10, CyberwareEffect.attackSpeed(0.10D), CyberwareEffect.damageReduction(0.06D)),
            def("militech_canto", "Militech Canto", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_5, "Experimental neural control operating system.", Items.NETHER_STAR, false, "tetratronic_rippler", 12, CyberwareEffect.damage(1.0D), CyberwareEffect.damageReduction(0.08D)),
            def("dynalar_sandevistan", "Dynalar Sandevistan", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_2, "Entry-grade time dilation suite.", Items.CLOCK, 0, CyberwareEffect.moveSpeed(0.40D), CyberwareEffect.attackSpeed(0.20D)),
            def("militech_apogee", "Militech Apogee", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_5, "Elite military reflex suite.", Items.NETHER_STAR, false, "militech_falcon", 0, CyberwareEffect.moveSpeed(0.60D), CyberwareEffect.attackSpeed(0.50D)),
            def("militech_falcon", "Militech Falcon", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_4, "High-tier tactical reflex suite.", Items.FEATHER, false, "zetatech_sandevistan", 0, CyberwareEffect.moveSpeed(0.40D), CyberwareEffect.attackSpeed(0.35D)),
            def("qiant_warp_dancer", "QianT Warp Dancer", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_4, "Hyper-accelerated reflex suite.", Items.ENDER_PEARL, false, "dynalar_sandevistan", 0, CyberwareEffect.moveSpeed(0.60D), CyberwareEffect.attackSpeed(0.40D)),
            def("zetatech_sandevistan", "Zetatech Sandevistan", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_2, "Mass-market reflex acceleration OS.", Items.SUGAR, false, "dynalar_sandevistan", 0, CyberwareEffect.moveSpeed(0.40D), CyberwareEffect.attackSpeed(0.20D)),
            def("militech_experimental_sandevistan", "Militech Experimental Sandevistan", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_5, "Experimental military neural accelerator. Extraordinary speed at a devastating neurological cost.", Items.NETHER_STAR, 0),
            def("chrome_compressor", "Chrome Compressor", CyberwareSlotType.OPERATING_SYSTEM, CyberwareTier.TIER_2, "Expands cyberware capacity.", Items.PISTON, 20),

            def("gorilla_arms", "Gorilla Arms", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Heavy-duty industrial arm replacements.", Items.IRON_PICKAXE, true, null, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.breakSpeed(0.30D), CyberwareEffect.knockbackResistance(0.10D)),
            def("electrifying_gorilla_arms", "Electrifying Gorilla Arms", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Gorilla Arms with shock payloads.", Items.LIGHTNING_ROD, true, "gorilla_arms", 0, CyberwareEffect.damage(2.0D), CyberwareEffect.breakSpeed(0.30D), CyberwareEffect.attackSpeed(0.10D)),
            def("thermal_gorilla_arms", "Thermal Gorilla Arms", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Gorilla Arms with burn payloads.", Items.BLAZE_ROD, true, "gorilla_arms", 0, CyberwareEffect.damage(2.0D), CyberwareEffect.breakSpeed(0.30D), CyberwareEffect.damageReduction(0.05D)),
            def("chemical_gorilla_arms", "Chemical Gorilla Arms", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Gorilla Arms with toxin payloads.", Items.SPIDER_EYE, true, "gorilla_arms", 0, CyberwareEffect.damage(2.0D), CyberwareEffect.breakSpeed(0.30D), CyberwareEffect.entityReach(0.5D)),
            def("mantis_blades", "Mantis Blades", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Signature retractable forearm blades.", Items.IRON_SWORD, true, null, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.attackSpeed(0.15D)),
            def("electrifying_mantis_blades", "Electrifying Mantis Blades", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Shock-tuned mantis blades.", Items.LIGHTNING_ROD, true, "mantis_blades", 0, CyberwareEffect.damage(2.0D), CyberwareEffect.attackSpeed(0.15D), CyberwareEffect.moveSpeed(0.05D)),
            def("thermal_mantis_blades", "Thermal Mantis Blades", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Thermal mantis blades.", Items.BLAZE_POWDER, true, "mantis_blades", 0, CyberwareEffect.damage(2.0D), CyberwareEffect.attackSpeed(0.15D), CyberwareEffect.damageReduction(0.04D)),
            def("toxic_mantis_blades", "Toxic Mantis Blades", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Chemical mantis blades.", Items.POISONOUS_POTATO, true, "mantis_blades", 0, CyberwareEffect.damage(2.0D), CyberwareEffect.attackSpeed(0.15D), CyberwareEffect.entityReach(0.5D)),
            def("maxtac_mantis_blades", "MaxTac Mantis Blades", CyberwareSlotType.ARMS, CyberwareTier.TIER_4, "Top-end MaxTac mantis blades.", Items.NETHERITE_SWORD, true, "mantis_blades", 0, CyberwareEffect.damage(4.0D), CyberwareEffect.attackSpeed(0.25D), CyberwareEffect.moveSpeed(0.08D)),
            def("monowire", "Monowire", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Whiplike mono-filament weapon implant.", Items.LEAD, false, null, 0, CyberwareEffect.attackSpeed(0.10D), CyberwareEffect.entityReach(1.0D)),
            def("electrifying_monowire", "Electrifying Monowire", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Electrified monowire assembly.", Items.LIGHTNING_ROD, false, "monowire", 0, CyberwareEffect.attackSpeed(0.10D), CyberwareEffect.entityReach(1.0D), CyberwareEffect.moveSpeed(0.04D)),
            def("thermal_monowire", "Thermal Monowire", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Thermal monowire assembly.", Items.BLAZE_ROD, false, "monowire", 0, CyberwareEffect.attackSpeed(0.10D), CyberwareEffect.entityReach(1.0D), CyberwareEffect.damageReduction(0.03D)),
            def("toxic_monowire", "Toxic Monowire", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Chemical monowire assembly.", Items.SPIDER_EYE, false, "monowire", 0, CyberwareEffect.attackSpeed(0.10D), CyberwareEffect.entityReach(1.0D), CyberwareEffect.damage(1.0D)),
            def("projectile_launch_system", "Projectile Launch System", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Integrated micro-missile launcher.", Items.CROSSBOW, false, null, 0, CyberwareEffect.damage(2.0D)),
            def("electrifying_projectile_launch_system", "Electrifying Projectile Launch System", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Electrified projectile launcher.", Items.LIGHTNING_ROD, false, "projectile_launch_system", 0, CyberwareEffect.damage(2.0D), CyberwareEffect.attackSpeed(0.05D)),
            def("thermal_projectile_launch_system", "Thermal Projectile Launch System", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Thermal projectile launcher.", Items.FIRE_CHARGE, false, "projectile_launch_system", 0, CyberwareEffect.damage(2.0D), CyberwareEffect.damageReduction(0.04D)),
            def("toxic_projectile_launch_system", "Toxic Projectile Launch System", CyberwareSlotType.ARMS, CyberwareTier.TIER_2, "Chemical projectile launcher.", Items.DRAGON_BREATH, false, "projectile_launch_system", 0, CyberwareEffect.damage(2.0D), CyberwareEffect.entityReach(0.5D)),
            def("excavator_arms", "Excavator Arms", CyberwareSlotType.ARMS, CyberwareTier.TIER_4, "Industrial excavation arms tuned for controlled 3x3 extraction.", Items.NETHERITE_PICKAXE, true),

            def("basic_kiroshi_optics", "Basic Kiroshi Optics", CyberwareSlotType.FACE, CyberwareTier.TIER_1, "Foundational cyberoptic package.", Items.ENDER_EYE, CyberwareEffect.nightVision()),
            def("clairvoyant", "Clairvoyant", CyberwareSlotType.FACE, CyberwareTier.TIER_3, "Scan-heavy hunter optics.", Items.SPYGLASS, false, "basic_kiroshi_optics", 0, CyberwareEffect.nightVision(), CyberwareEffect.blockReach(0.5D)),
            def("doomsayer", "Doomsayer", CyberwareSlotType.FACE, CyberwareTier.TIER_1, "Trap-spotting combat optics.", Items.GUNPOWDER, false, "basic_kiroshi_optics", 0, CyberwareEffect.nightVision()),
            def("sentry", "Sentry", CyberwareSlotType.FACE, CyberwareTier.TIER_1, "Turret and camera spotting optics.", Items.DISPENSER, false, "basic_kiroshi_optics", 0, CyberwareEffect.nightVision()),
            def("stalker", "Stalker", CyberwareSlotType.FACE, CyberwareTier.TIER_1, "Wall-sighting hunter optics.", Items.SPECTRAL_ARROW, false, "basic_kiroshi_optics", 0, CyberwareEffect.nightVision(), CyberwareEffect.entityReach(1.0D)),
            def("the_oracle", "The Oracle", CyberwareSlotType.FACE, CyberwareTier.TIER_4, "Combined premium Kiroshi package.", Items.ENDER_EYE, false, "clairvoyant", 0, CyberwareEffect.nightVision(), CyberwareEffect.blockReach(1.0D), CyberwareEffect.entityReach(1.0D)),
            def("cockatrice", "Cockatrice", CyberwareSlotType.FACE, CyberwareTier.TIER_4, "Predatory iconic optics suite.", Items.FERMENTED_SPIDER_EYE, false, "the_oracle", 0, CyberwareEffect.nightVision(), CyberwareEffect.damage(1.0D)),
            def("behavioral_imprint_synced_faceplate", "Behavioral Imprint-synced Faceplate", CyberwareSlotType.FACE, CyberwareTier.TIER_5, "Identity-obscuring adaptive faceplate.", Items.PLAYER_HEAD),
            def("forager_lens", "Forager Lens", CyberwareSlotType.FACE, CyberwareTier.TIER_1, "Agricultural optic package that tags ripe crops and ready hives.", Items.HONEYCOMB),
            def("vein_reader_optics", "Vein Reader Optics", CyberwareSlotType.FACE, CyberwareTier.TIER_2, "Geology-focused optics that pulse out nearby ore signatures.", Items.RAW_IRON),
            def("relic_scanner", "Relic Scanner", CyberwareSlotType.FACE, CyberwareTier.TIER_3, "Archive-grade scanner for caches, suspicious blocks, and dungeon infrastructure.", Items.BRUSH),

            def("bionic_joints", "Bionic Joints", CyberwareSlotType.SKELETON, CyberwareTier.TIER_1, "Simple but effective reinforcement.", Items.IRON_INGOT, CyberwareEffect.armor(2.0D)),
            def("dense_marrow", "Dense Marrow", CyberwareSlotType.SKELETON, CyberwareTier.TIER_2, "Weighty marrow reinforcement.", Items.BONE_BLOCK, CyberwareEffect.damage(1.0D)),
            def("epimorphic_skeleton", "Epimorphic Skeleton", CyberwareSlotType.SKELETON, CyberwareTier.TIER_4, "Health-focused chassis reinforcement.", Items.NETHERITE_INGOT, CyberwareEffect.hearts(4.0D)),
            def("feen_x", "Feen-X", CyberwareSlotType.SKELETON, CyberwareTier.TIER_1, "RAM emergency support lattice.", Items.GLOWSTONE_DUST),
            def("kinetic_frame", "Kinetic Frame", CyberwareSlotType.SKELETON, CyberwareTier.TIER_1, "Mitigation-tuned frame package.", Items.SLIME_BALL, CyberwareEffect.knockbackResistance(0.10D)),
            def("para_bellum", "Para Bellum", CyberwareSlotType.SKELETON, CyberwareTier.TIER_3, "Militarized armor skeleton.", Items.IRON_BARS, CyberwareEffect.armor(4.0D)),
            def("rara_avis", "Rara Avis", CyberwareSlotType.SKELETON, CyberwareTier.TIER_4, "Iconic armor-focused skeleton.", Items.NETHERITE_SCRAP, false, "para_bellum", 0, CyberwareEffect.armor(6.0D)),
            def("ram_recoup", "RAM Recoup", CyberwareSlotType.SKELETON, CyberwareTier.TIER_1, "Damage-fed netrunner skeleton module.", Items.REDSTONE_TORCH),
            def("scar_coalescer", "Scar Coalescer", CyberwareSlotType.SKELETON, CyberwareTier.TIER_1, "Emergency tissue-hardening reinforcement.", Items.LEATHER, CyberwareEffect.armor(2.0D), CyberwareEffect.hearts(2.0D)),
            def("scarab", "Scarab", CyberwareSlotType.SKELETON, CyberwareTier.TIER_1, "Low-profile crouch combat plating.", Items.RABBIT_HIDE, CyberwareEffect.armor(2.0D), CyberwareEffect.safeFall(1.0D)),
            def("spring_joints", "Spring Joints", CyberwareSlotType.SKELETON, CyberwareTier.TIER_2, "Impact-dampening skeletal joints.", Items.SLIME_BLOCK, CyberwareEffect.fallReduction(-0.15D), CyberwareEffect.safeFall(3.0D)),
            def("titanium_bones", "Titanium Bones", CyberwareSlotType.SKELETON, CyberwareTier.TIER_1, "Heavy titanium frame reinforcement.", Items.IRON_BLOCK, CyberwareEffect.knockbackResistance(0.15D), CyberwareEffect.hearts(2.0D)),
            def("universal_booster", "Universal Booster", CyberwareSlotType.SKELETON, CyberwareTier.TIER_3, "General sustain booster.", Items.GOLDEN_CARROT, CyberwareEffect.healthRegen(0.6D)),
            def("cargo_spine", "Cargo Spine", CyberwareSlotType.SKELETON, CyberwareTier.TIER_1, "Load-bearing spine routing that helps vacuum up loose materials.", Items.HOPPER),

            def("ballistic_coprocessor", "Ballistic Coprocessor", CyberwareSlotType.HANDS, CyberwareTier.TIER_2, "Ballistic targeting assist.", Items.ARROW, CyberwareEffect.damage(1.0D)),
            def("handle_wrap", "Handle Wrap", CyberwareSlotType.HANDS, CyberwareTier.TIER_1, "Improved weapon handling overlays.", Items.LEATHER, CyberwareEffect.attackSpeed(0.20D)),
            def("microgenerator", "Microgenerator", CyberwareSlotType.HANDS, CyberwareTier.TIER_2, "Charge-on-hit palm hardware.", Items.REDSTONE),
            def("shock_absorber", "Shock Absorber", CyberwareSlotType.HANDS, CyberwareTier.TIER_2, "Recoil and impact dampener.", Items.SLIME_BALL, CyberwareEffect.knockbackResistance(0.10D)),
            def("immovable_force", "Immovable Force", CyberwareSlotType.HANDS, CyberwareTier.TIER_4, "Iconic recoil-nullifying bracer.", Items.OBSIDIAN, false, "shock_absorber", 0, CyberwareEffect.knockbackResistance(0.20D)),
            def("smart_link", "Smart Link", CyberwareSlotType.HANDS, CyberwareTier.TIER_2, "Smart weapon handshake interface.", Items.TRIPWIRE_HOOK, CyberwareEffect.damage(1.0D), CyberwareEffect.entityReach(0.5D)),
            def("precision_miner", "Precision Miner", CyberwareSlotType.HANDS, CyberwareTier.TIER_1, "Correct-tool guidance that speeds up clean mining lines.", Items.IRON_PICKAXE),
            def("masons_grip", "Mason's Grip", CyberwareSlotType.HANDS, CyberwareTier.TIER_1, "Placement-biased grip routing for cleaner building reach.", Items.BRICKS, CyberwareEffect.blockReach(0.5D)),
            def("harvester_hands", "Harvester Hands", CyberwareSlotType.HANDS, CyberwareTier.TIER_2, "Agrarian handware that streamlines harvest-and-replant loops.", Items.IRON_HOE),

            def("adrenaline_converter", "Adrenaline Converter", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_2, "Stress-to-speed neural converter.", Items.BLAZE_POWDER, CyberwareEffect.moveSpeed(0.20D)),
            def("adreno_trigger", "Adreno-trigger", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_4, "Advanced speed-trigger implant.", Items.REDSTONE_BLOCK, false, "adrenaline_converter", 0, CyberwareEffect.moveSpeed(0.40D), CyberwareEffect.attackSpeed(0.20D)),
            def("atomic_sensors", "Atomic Sensors", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_2, "Hypersensitive battlefield sensor array.", Items.SPYGLASS, CyberwareEffect.nightVision()),
            def("kerenzikov", "Kerenzikov", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_2, "Classic dodge-response accelerator.", Items.SUGAR, CyberwareEffect.moveSpeed(0.40D), CyberwareEffect.attackSpeed(0.20D)),
            def("neofiber", "NeoFiber", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_2, "Stability-focused nerve weave.", Items.STRING, CyberwareEffect.knockbackResistance(0.15D)),
            def("reflex_tuner", "Reflex Tuner", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_4, "Emergency reflex overdrive.", Items.CLOCK, CyberwareEffect.moveSpeed(0.40D), CyberwareEffect.attackSpeed(0.30D)),
            def("revulsor", "Revulsor", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_4, "Shockwave-capable reflex defense.", Items.WIND_CHARGE, false, "reflex_tuner", 0, CyberwareEffect.damageReduction(0.10D)),
            def("stabber", "Stabber", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_2, "Melee-leaning neural support.", Items.IRON_SWORD, CyberwareEffect.damage(1.0D), CyberwareEffect.attackSpeed(0.10D)),
            def("synaptic_accelerator", "Synaptic Accelerator", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_3, "Fast-trigger neural acceleration.", Items.GLOWSTONE_DUST, CyberwareEffect.moveSpeed(0.40D), CyberwareEffect.attackSpeed(0.30D)),
            def("tyrosine_injector", "Tyrosine Injector", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_3, "Combat stimulant support line.", Items.GHAST_TEAR, CyberwareEffect.damage(2.0D)),
            def("visual_cortex_support", "Visual Cortex Support", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_2, "Perception support hardware.", Items.ENDER_PEARL, CyberwareEffect.entityReach(0.5D)),
            def("deep_field_visual_interface", "Deep-field Visual Interface", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_4, "Long-range perception interface.", Items.ENDER_EYE, false, "visual_cortex_support", 0, CyberwareEffect.entityReach(1.0D), CyberwareEffect.blockReach(1.0D)),
            def("kiroshi_retrieval_suite", "Kiroshi Retrieval Suite", CyberwareSlotType.NERVOUS_SYSTEM, CyberwareTier.TIER_2, "Pickup-routing neuralware that pulls in nearby salvage and experience.", Items.HOPPER),

            def("adrenaline_booster", "Adrenaline Booster", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_1, "Circulatory combat booster.", Items.BEETROOT_SOUP, CyberwareEffect.healthRegen(0.6D)),
            def("biomonitor", "Biomonitor", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_3, "Automatic health monitor.", Items.RECOVERY_COMPASS, CyberwareEffect.healthRegen(1.0D)),
            def("black_mamba", "Black Mamba", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_3, "Lethal venom support package.", Items.FERMENTED_SPIDER_EYE),
            def("blood_pump", "Blood Pump", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_3, "Emergency circulatory overpressurizer.", Items.GLASS_BOTTLE, CyberwareEffect.hearts(2.0D), CyberwareEffect.bonusAbsorption(8.0D)),
            def("clutch_padding", "Clutch Padding", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_1, "Stability padding for violent recoil.", Items.RABBIT_HIDE, CyberwareEffect.knockbackResistance(0.10D)),
            def("isometric_stabilizer", "Isometric Stabilizer", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_4, "Iconic recoil control implant.", Items.OBSIDIAN, false, "clutch_padding", 0, CyberwareEffect.knockbackResistance(0.20D), CyberwareEffect.hearts(2.0D)),
            def("feedback_circuit", "Feedback Circuit", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_2, "Shot-fed capacitor return loop.", Items.REDSTONE_TORCH),
            def("electromag_recycler", "Electromag Recycler", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_4, "Iconic ammo energy recycler.", Items.LIGHTNING_ROD, false, "feedback_circuit"),
            def("heal_on_kill", "Heal-On-Kill", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_2, "Aggression-linked recovery implant.", Items.GOLDEN_APPLE),
            def("microrotors", "Microrotors", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_2, "Attack-speed circulatory assist.", Items.WIND_CHARGE, CyberwareEffect.attackSpeed(0.30D)),
            def("second_heart", "Second Heart", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_5, "Emergency backup heart.", Items.TOTEM_OF_UNDYING, CyberwareEffect.hearts(6.0D), CyberwareEffect.bonusAbsorption(8.0D)),
            def("threatevac", "ThreatEvac", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_3, "Panic-response evacuation chemistry.", Items.RABBIT_FOOT, CyberwareEffect.moveSpeed(0.10D)),
            def("hydrolung", "Hydrolung", CyberwareSlotType.CIRCULATORY_SYSTEM, CyberwareTier.TIER_2, "Flood-safe lungware that keeps underwater work stable.", Items.NAUTILUS_SHELL, CyberwareEffect.waterBreathing(), CyberwareEffect.dolphinsGrace()),

            def("carapace", "Carapace", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_2, "Heavy armor skin package.", Items.TURTLE_SCUTE, CyberwareEffect.armor(4.0D)),
            def("cellular_adapter", "Cellular Adapter", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_2, "Adaptive self-repair skinware.", Items.HONEY_BOTTLE, CyberwareEffect.healthRegen(0.6D)),
            def("cogito_lattice", "Cogito Lattice", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_4, "Mind-linked protective lattice.", Items.AMETHYST_CLUSTER, CyberwareEffect.damageReduction(0.10D)),
            def("countershell", "Countershell", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_1, "Reactive protection shell.", Items.SHIELD, CyberwareEffect.damageReduction(0.10D)),
            def("defenzikov", "Defenzikov", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_3, "Defensive speed-response skinware.", Items.ARMADILLO_SCUTE, CyberwareEffect.damageReduction(0.20D)),
            def("nano_plating", "Nano-plating", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_3, "Dense defensive nanoplating.", Items.IRON_CHESTPLATE, CyberwareEffect.armor(5.0D)),
            def("optical_camo", "Optical Camo", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_2, "Visual concealment skin system.", Items.PHANTOM_MEMBRANE),
            def("pain_editor", "Pain Editor", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_4, "Top-tier pain suppression implant.", Items.ENCHANTED_GOLDEN_APPLE, CyberwareEffect.damageReduction(0.20D)),
            def("painducer", "Painducer", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_4, "Damage-smoothing skinware.", Items.GHAST_TEAR, CyberwareEffect.hearts(2.0D), CyberwareEffect.damageReduction(0.10D)),
            def("proxishield", "ProxiShield", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_1, "Close-range threat plating.", Items.IRON_HELMET, CyberwareEffect.armor(2.0D)),
            def("peripheral_inverse", "Peripheral Inverse", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_4, "Iconic proximity damage reducer.", Items.OBSIDIAN, false, "proxishield", 0, CyberwareEffect.armor(4.0D)),
            def("rangeguard", "RangeGuard", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_1, "Spacing-sensitive armor package.", Items.LEATHER_CHESTPLATE, CyberwareEffect.armor(2.0D)),
            def("shock_n_awe", "Shock-n-Awe", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_1, "Electroshock retaliation plating.", Items.LIGHTNING_ROD),
            def("subdermal_armor", "Subdermal Armor", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_1, "The classic under-skin armor.", Items.IRON_CHESTPLATE, CyberwareEffect.armor(3.0D)),
            def("chitin", "Chitin", CyberwareSlotType.INTEGUMENTARY_SYSTEM, CyberwareTier.TIER_5, "Iconic hardened chitin armor.", Items.NETHERITE_CHESTPLATE, false, "subdermal_armor", 0, CyberwareEffect.armor(8.0D), CyberwareEffect.hearts(2.0D)),

            def("fortified_ankles", "Fortified Ankles", CyberwareSlotType.LEGS, CyberwareTier.TIER_1, "Charged-jump leg hardware.", Items.IRON_BOOTS, CyberwareEffect.jumpPower(0.35D), CyberwareEffect.safeFall(4.0D)),
            def("jenkins_tendons", "Jenkins' Tendons", CyberwareSlotType.LEGS, CyberwareTier.TIER_2, "Sprint-biased tendon upgrade.", Items.LEATHER_BOOTS, CyberwareEffect.moveSpeed(0.10D)),
            def("leeroy_ligament_system", "Leeroy Ligament System", CyberwareSlotType.LEGS, CyberwareTier.TIER_4, "Iconic movement-speed leg upgrade.", Items.GOLDEN_BOOTS, false, "jenkins_tendons", 0, CyberwareEffect.moveSpeed(0.15D)),
            def("lynx_paws", "Lynx Paws", CyberwareSlotType.LEGS, CyberwareTier.TIER_2, "Silent movement paws.", Items.RABBIT_HIDE, CyberwareEffect.moveSpeed(0.05D), CyberwareEffect.safeFall(4.0D), CyberwareEffect.stepHeight(0.5D)),
            def("reinforced_tendons", "Reinforced Tendons", CyberwareSlotType.LEGS, CyberwareTier.TIER_2, "Double-jump style tendon hardware.", Items.RABBIT_FOOT, CyberwareEffect.safeFall(6.0D)),
            def("brushstep_legs", "Brushstep Legs", CyberwareSlotType.LEGS, CyberwareTier.TIER_2, "Terrain-smoothing legware for farm rows, leaves, and other drag-heavy ground.", Items.SWEET_BERRIES)
    );

    private static final Map<String, CyberwareDefinition> DEFINITIONS_BY_ID = createDefinitionMap();

    private CyberwareCatalog() {
    }

    public static List<CyberwareDefinition> definitions() {
        return DEFINITIONS;
    }

    public static CyberwareDefinition get(String id) {
        CyberwareDefinition definition = DEFINITIONS_BY_ID.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown cyberware id: " + id);
        }
        return definition;
    }

    public static List<CyberwareDefinition> definitionsByAcquisition(CyberwareAcquisitionMethod acquisitionMethod) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.acquisitionMethod() == acquisitionMethod)
                .toList();
    }

    public static CyberwareDefinition findUpgradeStep(CyberwareDefinition definition) {
        for (CyberwareDefinition candidate : DEFINITIONS) {
            if (definition.id().equals(candidate.upgradeFromId())) {
                return candidate;
            }
        }
        return null;
    }

    private static Map<String, CyberwareDefinition> createDefinitionMap() {
        Map<String, CyberwareDefinition> definitions = new LinkedHashMap<>();
        for (CyberwareDefinition definition : DEFINITIONS) {
            definitions.put(definition.id(), definition);
        }
        return definitions;
    }

    private static CyberwareDefinition def(
            String id,
            String displayName,
            CyberwareSlotType slotType,
            CyberwareTier tier,
            String description,
            net.minecraft.world.level.ItemLike motifItem,
            CyberwareEffect... effects
    ) {
        return def(id, displayName, slotType, tier, description, motifItem, false, null, 0, 0, effects);
    }

    private static CyberwareDefinition def(
            String id,
            String displayName,
            CyberwareSlotType slotType,
            CyberwareTier tier,
            String description,
            net.minecraft.world.level.ItemLike motifItem,
            int capacityBonus,
            CyberwareEffect... effects
    ) {
        return def(id, displayName, slotType, tier, description, motifItem, false, null, capacityBonus, 0, effects);
    }

    private static CyberwareDefinition def(
            String id,
            String displayName,
            CyberwareSlotType slotType,
            CyberwareTier tier,
            String description,
            net.minecraft.world.level.ItemLike motifItem,
            boolean handheld,
            CyberwareEffect... effects
    ) {
        return def(id, displayName, slotType, tier, description, motifItem, handheld, null, 0, 0, effects);
    }

    private static CyberwareDefinition def(
            String id,
            String displayName,
            CyberwareSlotType slotType,
            CyberwareTier tier,
            String description,
            net.minecraft.world.level.ItemLike motifItem,
            boolean handheld,
            String upgradeFromId,
            CyberwareEffect... effects
    ) {
        return def(id, displayName, slotType, tier, description, motifItem, handheld, upgradeFromId, 0, 0, effects);
    }

    private static CyberwareDefinition def(
            String id,
            String displayName,
            CyberwareSlotType slotType,
            CyberwareTier tier,
            String description,
            net.minecraft.world.level.ItemLike motifItem,
            boolean handheld,
            String upgradeFromId,
            int capacityBonus,
            CyberwareEffect... effects
    ) {
        return def(id, displayName, slotType, tier, description, motifItem, handheld, upgradeFromId, capacityBonus, 0, effects);
    }

    private static CyberwareDefinition def(
            String id,
            String displayName,
            CyberwareSlotType slotType,
            CyberwareTier tier,
            String description,
            net.minecraft.world.level.ItemLike motifItem,
            boolean handheld,
            String upgradeFromId,
            int capacityBonus,
            int chipSlotCount,
            CyberwareEffect... effects
    ) {
        CyberwareBalance.CyberwareEntry balance = CyberwareBalance.resolveCyberware(id);
        return new CyberwareDefinition(
                id,
                displayName,
                slotType,
                tier,
                CyberwareAcquisitionRules.resolve(id, tier),
                description,
                motifItem,
                handheld,
                upgradeFromId,
                balance.capacityBonus(),
                balance.chipSlotCount(),
                balance.effects()
        );
    }

    private static CyberwareDefinition chipSocket(
            String id,
            String displayName,
            CyberwareTier tier,
            String description,
            net.minecraft.world.level.ItemLike motifItem,
            String upgradeFromId,
            int chipSlotCount
    ) {
        return def(id, displayName, CyberwareSlotType.FRONTAL_CORTEX, tier, description, motifItem, false, upgradeFromId, 0, chipSlotCount);
    }
}
