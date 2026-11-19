package de.artemis.cyberneticenhancements.common.entity;

import net.minecraft.network.chat.Component;

public enum NpcType {
    FIXER("fixer", true, true, true, false, true, true),
    RIPPERDOC("ripperdoc", false, true, true, false, false, true),
    TECHIE("techie", false, false, true, true, true, false),
    NETRUNNER("netrunner", true, false, true, true, true, true),
    MERC("merc", false, false, true, true, true, false), MED_TECH("med_tech", false, true, true, false, false, true),
    MAXTEC("maxtec", false, false, false, false, false, false),
    /** The wandering clinic: speaks and looks like a ripperdoc (shared id) but sells another stock and is never kept as a contact. */
    STREET_RIPPERDOC("ripperdoc", false, true, true, false, false, true),
    /** The MedTec at a world encounter: only talks and hands out its quest, with no shop or services; shares the med tech id and look. */
    WORLD_MED_TECH("med_tech", false, false, false, true, false, false);

    private final String id;
    private final boolean supportsIntel;
    private final boolean supportsMedicalServices;
    private final boolean supportsShop;
    private final boolean supportsContracts;
    private final boolean supportsTrustDiscount;
    private final boolean usesPatientFocus;

    NpcType(
            String id,
            boolean supportsIntel,
            boolean supportsMedicalServices,
            boolean supportsShop,
            boolean supportsContracts,
            boolean supportsTrustDiscount,
            boolean usesPatientFocus
    ) {
        this.id = id;
        this.supportsIntel = supportsIntel;
        this.supportsMedicalServices = supportsMedicalServices;
        this.supportsShop = supportsShop;
        this.supportsContracts = supportsContracts;
        this.supportsTrustDiscount = supportsTrustDiscount;
        this.usesPatientFocus = usesPatientFocus;
    }

    public String id() {
        return id;
    }

    public boolean supportsIntel() {
        return supportsIntel;
    }

    public boolean supportsMedicalServices() {
        return supportsMedicalServices;
    }

    public boolean supportsShop() {
        return supportsShop;
    }

    public boolean supportsContracts() {
        return supportsContracts;
    }

    public boolean supportsTrustDiscount() {
        return supportsTrustDiscount;
    }

    public boolean usesPatientFocus() {
        return usesPatientFocus;
    }

    public Component displayName() {
        return Component.translatable("screen.cyberneticenhancements.npc.type." + (this == STREET_RIPPERDOC ? "street_ripperdoc" : id));
    }

    public static NpcType fromId(String id) {
        for (NpcType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return FIXER;
    }
}
