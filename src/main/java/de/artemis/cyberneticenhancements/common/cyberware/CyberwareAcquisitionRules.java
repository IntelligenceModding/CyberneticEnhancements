package de.artemis.cyberneticenhancements.common.cyberware;

import java.util.Set;

public final class CyberwareAcquisitionRules {
    private static final Set<String> STARTER_CRAFTABLE = Set.of(
            "ex_disk",
            "memory_boost",
            "newton_module",
            "ram_upgrade",
            "chipware_socket",
            "arasaka_shadow",
            "chrome_compressor",
            "basic_kiroshi_optics",
            "bionic_joints",
            "kinetic_frame",
            "scar_coalescer",
            "scarab",
            "spring_joints",
            "titanium_bones",
            "handle_wrap",
            "shock_absorber",
            "adrenaline_converter",
            "atomic_sensors",
            "neofiber",
            "visual_cortex_support",
            "adrenaline_booster",
            "clutch_padding",
            "heal_on_kill",
            "countershell",
            "proxishield",
            "rangeguard",
            "shock_n_awe",
            "subdermal_armor",
            "fortified_ankles",
            "jenkins_tendons"
    );
    private static final Set<String> LIVE_TIER_FIVE_LOOT = Set.of(
            "axolotl",
            "quantum_tuner",
            "chipware_socket_mk3",
            "militech_canto",
            "militech_apogee",
            "militech_experimental_sandevistan",
            "behavioral_imprint_synced_faceplate",
            "second_heart",
            "chitin"
    );

    private CyberwareAcquisitionRules() {
    }

    public static CyberwareAcquisitionMethod resolve(String id, CyberwareTier tier) {
        if (STARTER_CRAFTABLE.contains(id)) {
            return CyberwareAcquisitionMethod.CRAFTABLE;
        }
        if (tier == CyberwareTier.TIER_5 && !LIVE_TIER_FIVE_LOOT.contains(id)) {
            return CyberwareAcquisitionMethod.QUEST;
        }
        return CyberwareAcquisitionMethod.LOOT;
    }
}
