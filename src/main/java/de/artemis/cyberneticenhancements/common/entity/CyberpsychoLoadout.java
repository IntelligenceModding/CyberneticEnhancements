package de.artemis.cyberneticenhancements.common.entity;

import net.minecraft.network.chat.Component;

public enum CyberpsychoLoadout {
    MANTIS("mantis_blades"), THERMAL("thermal_mantis_blades"), SHOCK("electrifying_mantis_blades"),
    LAUNCHER("projectile_launch_system"), GORILLA("gorilla_arms");

    private final String cyberware;
    CyberpsychoLoadout(String cyberware) { this.cyberware = cyberware; }
    public String cyberwareId() { return cyberware; }
    public Component label() { return Component.translatable("entity.cyberneticenhancements.cyberpsycho.loadout." + name().toLowerCase(java.util.Locale.ROOT)); }
    public static CyberpsychoLoadout fromIndex(int index) { return values()[Math.floorMod(index, values().length)]; }
}
