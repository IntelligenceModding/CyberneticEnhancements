package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableCategory;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableManager;
import de.artemis.cyberneticenhancements.common.cyberware.ArmCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.CombatStatusManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareAbilities;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffectType;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlotType;
import de.artemis.cyberneticenhancements.common.cyberware.FaceCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.FrontalCortexManager;
import de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory;
import de.artemis.cyberneticenhancements.common.cyberware.TemporaryCyberwareEffectManager;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record CyberwareHudPayload(
        int installedChrome,
        int chromeCapacity,
        int cyberstrain,
        int effectiveCyberstrain,
        int suppressionAmount,
        int suppressionSeconds,
        int psychosisSeconds,
        String psychosisStateKey,
        List<AbilityEntry> abilities,
        List<StatusEntry> statuses,
        List<CooldownEntry> cooldowns
) implements CustomPacketPayload {
    public static final Type<CyberwareHudPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "cyberware_hud"));
    private static final StreamCodec<RegistryFriendlyByteBuf, CapacityState> CAPACITY_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            CapacityState::installedChrome,
            ByteBufCodecs.INT,
            CapacityState::chromeCapacity,
            CapacityState::new
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, StrainState> STRAIN_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            StrainState::cyberstrain,
            ByteBufCodecs.INT,
            StrainState::effectiveCyberstrain,
            ByteBufCodecs.INT,
            StrainState::suppressionAmount,
            ByteBufCodecs.INT,
            StrainState::suppressionSeconds,
            ByteBufCodecs.INT,
            StrainState::psychosisSeconds,
            ByteBufCodecs.STRING_UTF8,
            StrainState::psychosisStateKey,
            StrainState::new
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, AbilityEntry> ABILITY_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            AbilityEntry::translationKey,
            ByteBufCodecs.STRING_UTF8,
            AbilityEntry::activationKey,
            ByteBufCodecs.INT,
            AbilityEntry::activeSeconds,
            ByteBufCodecs.INT,
            AbilityEntry::activeTotalSeconds,
            ByteBufCodecs.INT,
            AbilityEntry::cooldownSeconds,
            ByteBufCodecs.INT,
            AbilityEntry::cooldownTotalSeconds,
            AbilityEntry::new
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, StatusEntry> STATUS_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            StatusEntry::translationKey,
            ByteBufCodecs.DOUBLE,
            StatusEntry::amount,
            ByteBufCodecs.INT,
            StatusEntry::remainingSeconds,
            ByteBufCodecs.INT,
            StatusEntry::totalSeconds,
            ByteBufCodecs.BOOL,
            StatusEntry::playerApplied,
            StatusEntry::new
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, CooldownEntry> COOLDOWN_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            CooldownEntry::translationKey,
            ByteBufCodecs.INT,
            CooldownEntry::remainingSeconds,
            ByteBufCodecs.INT,
            CooldownEntry::totalSeconds,
            CooldownEntry::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CyberwareHudPayload> STREAM_CODEC = StreamCodec.composite(
            CAPACITY_STREAM_CODEC,
            payload -> new CapacityState(payload.installedChrome(), payload.chromeCapacity()),
            STRAIN_STREAM_CODEC,
            payload -> new StrainState(
                    payload.cyberstrain(),
                    payload.effectiveCyberstrain(),
                    payload.suppressionAmount(),
                    payload.suppressionSeconds(),
                    payload.psychosisSeconds(),
                    payload.psychosisStateKey()
            ),
            ByteBufCodecs.collection(ArrayList::new, ABILITY_STREAM_CODEC),
            CyberwareHudPayload::abilities,
            ByteBufCodecs.collection(ArrayList::new, STATUS_STREAM_CODEC),
            CyberwareHudPayload::statuses,
            ByteBufCodecs.collection(ArrayList::new, COOLDOWN_STREAM_CODEC),
            CyberwareHudPayload::cooldowns,
            CyberwareHudPayload::fromCodecParts
    );
    private record CapacityState(int installedChrome, int chromeCapacity) {
    }

    private record StrainState(
            int cyberstrain,
            int effectiveCyberstrain,
            int suppressionAmount,
            int suppressionSeconds,
            int psychosisSeconds,
            String psychosisStateKey
    ) {
    }

    public record AbilityEntry(
            String translationKey,
            String activationKey,
            int activeSeconds,
            int activeTotalSeconds,
            int cooldownSeconds,
            int cooldownTotalSeconds
    ) {
    }

    public record StatusEntry(String translationKey, double amount, int remainingSeconds, int totalSeconds, boolean playerApplied) {
    }

    public record CooldownEntry(String translationKey, int remainingSeconds, int totalSeconds) {
    }

    @Override
    public Type<CyberwareHudPayload> type() {
        return TYPE;
    }

    private static CyberwareHudPayload fromCodecParts(
            CapacityState capacity,
            StrainState strain,
            List<AbilityEntry> abilities,
            List<StatusEntry> statuses,
            List<CooldownEntry> cooldowns
    ) {
        return new CyberwareHudPayload(
                capacity.installedChrome(),
                capacity.chromeCapacity(),
                strain.cyberstrain(),
                strain.effectiveCyberstrain(),
                strain.suppressionAmount(),
                strain.suppressionSeconds(),
                strain.psychosisSeconds(),
                strain.psychosisStateKey(),
                abilities,
                statuses,
                cooldowns
        );
    }

    public static CyberwareHudPayload capture(ServerPlayer player) {
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        long gameTime = player.level().getGameTime();
        int suppressionSeconds = CyberstrainManager.getSuppressionSecondsRemaining(player);

        return new CyberwareHudPayload(
                inventory.getInstalledChromeCost(),
                inventory.getChromeCapacity(),
                inventory.getCyberstrain(),
                CyberstrainManager.getEffectiveCyberstrain(player, inventory, gameTime),
                CyberstrainManager.getSuppressionAmount(player),
                suppressionSeconds,
                (int) CyberstrainManager.getPsychosisSecondsRemaining(player),
                psychosisStateKey(player, inventory),
                collectAbilities(player, inventory),
                collectStatuses(player, gameTime, suppressionSeconds),
                collectCooldowns(player, inventory)
        );
    }

    private static List<AbilityEntry> collectAbilities(ServerPlayer player, PlayerCyberwareInventory inventory) {
        List<AbilityEntry> entries = new ArrayList<>();

        String operatingSystemAbilityKey = resolveOperatingSystemAbilityKey(player, inventory);
        if (!operatingSystemAbilityKey.isEmpty()) {
            int cooldownSeconds = CyberwareAbilities.getAbilityCooldownSecondsRemaining(player);
            entries.add(new AbilityEntry(
                    operatingSystemAbilityKey,
                    "activate_cyberware",
                    CyberwareAbilities.getActiveSecondsRemaining(player),
                    CyberwareAbilities.getActiveTotalSeconds(player),
                    cooldownSeconds,
                    resolveOperatingSystemAbilityCooldownTotalSeconds(player, inventory, cooldownSeconds)
            ));
        }

        String armsAbilityKey = ArmCyberwareManager.getInstalledArmsTranslationKey(inventory);
        if (!armsAbilityKey.isEmpty()) {
            entries.add(new AbilityEntry(
                    armsAbilityKey,
                    "activate_arm_cyberware",
                    ArmCyberwareManager.getActiveSecondsRemaining(player),
                    ArmCyberwareManager.getActiveTotalSeconds(player),
                    ArmCyberwareManager.getCooldownSecondsRemaining(player),
                    ArmCyberwareManager.getCooldownTotalSeconds(player)
            ));
        }

        String faceAbilityKey = FaceCyberwareManager.getInstalledFaceTranslationKey(inventory);
        if (!faceAbilityKey.isEmpty()) {
            entries.add(new AbilityEntry(
                    faceAbilityKey,
                    "activate_face_cyberware",
                    FaceCyberwareManager.getActiveSecondsRemaining(player),
                    FaceCyberwareManager.getActiveTotalSeconds(player),
                    FaceCyberwareManager.getCooldownSecondsRemaining(player),
                    FaceCyberwareManager.getCooldownTotalSeconds(player)
            ));
        }

        if (inventory.hasInstalledCyberware("self_ice")) {
            entries.add(new AbilityEntry(
                    "item.cyberneticenhancements.self_ice",
                    "activate_auxiliary_cyberware",
                    0,
                    0,
                    FrontalCortexManager.getSelfIceCooldownSecondsRemaining(player),
                    resolveAuxiliaryCooldownTotalSeconds(FrontalCortexManager.getSelfIceCooldownTotalSeconds(player), 70)
            ));
        }

        if (inventory.hasInstalledCyberware("quantum_tuner")) {
            entries.add(new AbilityEntry(
                    "item.cyberneticenhancements.quantum_tuner",
                    "activate_auxiliary_cyberware",
                    0,
                    0,
                    FrontalCortexManager.getQuantumTunerCooldownSecondsRemaining(player),
                    resolveAuxiliaryCooldownTotalSeconds(FrontalCortexManager.getQuantumTunerCooldownTotalSeconds(player), 90)
            ));
        }

        return entries;
    }

    private static List<StatusEntry> collectStatuses(ServerPlayer player, long gameTime, int suppressionSeconds) {
        List<StatusEntry> entries = new ArrayList<>();
        if (de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanManager.recentUses(player) > 0) {
            int remaining = (int) Math.max(1, (de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanManager.recoveryRemaining(player) + 19) / 20);
            entries.add(new StatusEntry("hud.cyberneticenhancements.sandevistan.instability", CyberstrainManager.getInstability(player),
                    remaining, Math.max(remaining, de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanManager.recoveryTotalSeconds(player)), true));
        }
        if (suppressionSeconds > 0) {
            entries.add(new StatusEntry(
                    "hud.cyberneticenhancements.suppression",
                    CyberstrainManager.getSuppressionAmount(player),
                    suppressionSeconds,
                    CyberstrainManager.getSuppressionTotalSeconds(player),
                    false
            ));
        }

        int ramJoltSeconds = CyberstrainManager.getRamJoltSecondsRemaining(player);
        if (ramJoltSeconds > 0) {
            entries.add(new StatusEntry(
                    "hud.cyberneticenhancements.ram_jolt",
                    CyberstrainManager.getRamJoltCooldownMultiplier(player),
                    ramJoltSeconds,
                    CyberstrainManager.getRamJoltTotalSeconds(player),
                    false
            ));
        }

        List<TemporaryCyberwareEffectManager.ActiveEffectStatus> activeEffects = new ArrayList<>(TemporaryCyberwareEffectManager.collectActiveStatuses(player, gameTime));
        activeEffects.removeIf(status -> !isHudTrackedEffect(status.type()));
        activeEffects.sort(Comparator
                .comparingInt((TemporaryCyberwareEffectManager.ActiveEffectStatus status) -> effectPriority(status.type()))
                .thenComparing(TemporaryCyberwareEffectManager.ActiveEffectStatus::remainingSeconds, Comparator.reverseOrder()));

        for (TemporaryCyberwareEffectManager.ActiveEffectStatus status : activeEffects) {
            entries.add(new StatusEntry(status.type().translationKey(), status.amount(), status.remainingSeconds(), status.totalSeconds(), false));
        }
        for (CombatStatusManager.ActiveStatus status : CombatStatusManager.collectActiveStatuses(player, gameTime)) {
            entries.add(new StatusEntry(status.type().translationKey(), status.stacks(), status.remainingSeconds(), status.totalSeconds(), status.playerApplied()));
        }
        return entries;
    }

    private static List<CooldownEntry> collectCooldowns(ServerPlayer player, PlayerCyberwareInventory inventory) {
        List<CooldownEntry> entries = new ArrayList<>();
        for (CyberConsumableCategory category : CyberConsumableCategory.values()) {
            int seconds = CyberConsumableManager.getRemainingCategoryCooldownSeconds(player, category);
            entries.add(new CooldownEntry(category.translationKey(), seconds, resolveConsumableCooldownTotalSeconds(player, category)));
        }

        if (inventory.hasInstalledCyberware("biomonitor")) {
            addTriggeredCooldown(entries, "item.cyberneticenhancements.biomonitor", CyberwareAbilities.getBiomonitorCooldownSecondsRemaining(player), CyberwareAbilities.getBiomonitorCooldownTotalSeconds(player), 45);
        }
        if (inventory.hasInstalledCyberware("blood_pump")) {
            addTriggeredCooldown(entries, "item.cyberneticenhancements.blood_pump", CyberwareAbilities.getBloodPumpCooldownSecondsRemaining(player), CyberwareAbilities.getBloodPumpCooldownTotalSeconds(player), 60);
        }
        if (inventory.hasInstalledCyberware("second_heart")) {
            addTriggeredCooldown(entries, "item.cyberneticenhancements.second_heart", CyberwareAbilities.getSecondHeartCooldownSecondsRemaining(player), CyberwareAbilities.getSecondHeartCooldownTotalSeconds(player), 180);
        }
        if (inventory.hasInstalledCyberware("reflex_tuner")) {
            addTriggeredCooldown(entries, "item.cyberneticenhancements.reflex_tuner", CyberwareAbilities.getReflexTunerCooldownSecondsRemaining(player), CyberwareAbilities.getReflexTunerCooldownTotalSeconds(player), 40);
        }
        return entries;
    }

    private static void addTriggeredCooldown(List<CooldownEntry> entries, String translationKey, int seconds, int trackedTotalSeconds, int fallbackTotalSeconds) {
        if (seconds > 0) {
            entries.add(new CooldownEntry(translationKey, seconds, trackedTotalSeconds > 0 ? trackedTotalSeconds : fallbackTotalSeconds));
        }
    }

    private static boolean isHudTrackedEffect(CyberwareEffectType type) {
        return switch (type) {
            case MAX_HEALTH,
                 ATTACK_DAMAGE,
                 ATTACK_SPEED,
                 MOVEMENT_SPEED,
                 BLOCK_BREAK_SPEED,
                 HEALTH_REGEN,
                 DAMAGE_REDUCTION,
                 BONUS_ABSORPTION,
                 WATER_BREATHING -> true;
            default -> false;
        };
    }

    private static int effectPriority(CyberwareEffectType type) {
        return switch (type) {
            case HEALTH_REGEN -> 0;
            case BONUS_ABSORPTION -> 1;
            case MAX_HEALTH -> 2;
            case DAMAGE_REDUCTION -> 3;
            case MOVEMENT_SPEED -> 4;
            case ATTACK_DAMAGE -> 5;
            case ATTACK_SPEED -> 6;
            case BLOCK_BREAK_SPEED -> 7;
            case WATER_BREATHING -> 8;
            default -> 20;
        };
    }

    private static String resolveOperatingSystemAbilityKey(ServerPlayer player, PlayerCyberwareInventory inventory) {
        return CyberwareAbilities.getAbilityTranslationKey(player, inventory);
    }

    private static int resolveOperatingSystemAbilityCooldownTotalSeconds(ServerPlayer player, PlayerCyberwareInventory inventory, int abilityCooldownSeconds) {
        int trackedCooldown = CyberwareAbilities.getAbilityCooldownTotalSeconds(player);
        if (trackedCooldown > 0) {
            return trackedCooldown;
        }

        CyberwareItem operatingSystem = inventory.getInstalledCyberwareBySlotType(CyberwareSlotType.OPERATING_SYSTEM);
        if (operatingSystem == null) {
            return 0;
        }

        int baseSeconds = CyberwareAbilities.getBaseAbilityCooldownSeconds(operatingSystem);
        if (baseSeconds == 0) {
            return Math.max(abilityCooldownSeconds, 0);
        }
        return (int) Math.max(1L, Math.round(baseSeconds * CyberstrainManager.getAbilityCooldownMultiplier(player, inventory)));
    }

    private static int resolveAuxiliaryCooldownTotalSeconds(int trackedCooldownSeconds, int fallbackCooldownSeconds) {
        return trackedCooldownSeconds > 0 ? trackedCooldownSeconds : fallbackCooldownSeconds;
    }

    private static int resolveConsumableCooldownTotalSeconds(ServerPlayer player, CyberConsumableCategory category) {
        int tracked = CyberConsumableManager.getCategoryCooldownTotalSeconds(player, category);
        if (tracked > 0) {
            return tracked;
        }

        return switch (category) {
            case MEDICAL -> 100;
            case BOOSTER -> 200;
            case NEURAL -> 140;
            case SUPPRESSANT -> 240;
            case STREET -> 220;
        };
    }

    private static String psychosisStateKey(ServerPlayer player, PlayerCyberwareInventory inventory) {
        return "hud.cyberneticenhancements.state." + CyberstrainManager.getPsychosisStateLabel(player, inventory).toLowerCase(java.util.Locale.ROOT);
    }
}
