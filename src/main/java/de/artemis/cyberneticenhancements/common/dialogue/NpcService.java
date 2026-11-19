package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum NpcService {
    MEDICAL_STIMS(NpcType.MED_TECH, 3, 8, 24, 180, false, false), TRAUMA_COVERAGE(NpcType.MED_TECH, 2, 0, 0, 0, false, false), STRAIN_BUFFER(NpcType.FIXER, 2, 0, 18, 120, false, false),
    NEURAL_RESET(NpcType.FIXER, 3, 0, 0, 0, true, false),
    CLINIC_CARE(NpcType.RIPPERDOC, 2, 8, 24, 180, false, false),
    DETOX(NpcType.RIPPERDOC, 3, 4, 0, 0, true, true),
    FIELD_TRIAGE(NpcType.STREET_RIPPERDOC, 2, 20, 40, 300, false, true,
            buff(() -> MobEffects.REGENERATION, 20, 1), buff(() -> MobEffects.ABSORPTION, 180, 1)),
    NEURAL_FLUSH(NpcType.STREET_RIPPERDOC, 3, 8, 0, 0, true, true,
            buff(() -> MobEffects.DAMAGE_RESISTANCE, 120, 0)),
    MINING_CALIBRATION(NpcType.TECHIE, 2, 0, 0, 0, false, false,
            buff(() -> MobEffects.DIG_SPEED, 300, 1)),
    HAZARD_PREPARATION(NpcType.TECHIE, 3, 0, 0, 0, false, false,
            buff(() -> MobEffects.FIRE_RESISTANCE, 300, 0), buff(() -> MobEffects.DAMAGE_RESISTANCE, 300, 0)),
    RECON_SUPPORT(NpcType.NETRUNNER, 2, 0, 0, 0, false, false,
            buff(() -> MobEffects.NIGHT_VISION, 480, 0), buff(() -> MobEffects.MOVEMENT_SPEED, 480, 0)),
    STEALTH_PROTOCOL(NpcType.NETRUNNER, 3, 0, 0, 0, false, false,
            buff(() -> MobEffects.INVISIBILITY, 180, 0), buff(() -> MobEffects.MOVEMENT_SPEED, 180, 0)),
    FIELD_RECOVERY(NpcType.MERC, 2, 0, 0, 0, false, false,
            buff(() -> MobEffects.REGENERATION, 15, 0), buff(() -> MobEffects.ABSORPTION, 180, 0)),
    COMBAT_PREPARATION(NpcType.MERC, 3, 0, 0, 0, false, false,
            buff(() -> MobEffects.DAMAGE_BOOST, 180, 0), buff(() -> MobEffects.DAMAGE_RESISTANCE, 180, 0));

    private final NpcType type;
    private final int optionId, healing, suppression, suppressionSeconds;
    private final boolean reset, detox;
    private final List<Buff> buffs;

    NpcService(NpcType type, int optionId, int healing, int suppression, int suppressionSeconds,
               boolean reset, boolean detox, Buff... buffs) {
        this.type = type;
        this.optionId = optionId;
        this.healing = healing;
        this.suppression = suppression;
        this.suppressionSeconds = suppressionSeconds;
        this.reset = reset;
        this.detox = detox;
        this.buffs = List.of(buffs);
    }

    public static List<NpcService> forNpc(NpcType type) {
        return switch (type) {
            case FIXER -> List.of(STRAIN_BUFFER, NEURAL_RESET);
            case RIPPERDOC -> List.of(CLINIC_CARE, DETOX);
            case STREET_RIPPERDOC -> List.of(FIELD_TRIAGE, NEURAL_FLUSH);
            case TECHIE -> List.of(MINING_CALIBRATION, HAZARD_PREPARATION);
            case NETRUNNER -> List.of(RECON_SUPPORT, STEALTH_PROTOCOL);
            case MERC -> List.of(FIELD_RECOVERY, COMBAT_PREPARATION);
            case MED_TECH -> List.of(TRAUMA_COVERAGE, MEDICAL_STIMS);
            case MAXTEC, WORLD_MED_TECH -> List.of();
        };
    }

    public static NpcService forOption(NpcType type, int optionId) {
        return forNpc(type).stream().filter(service -> service.optionId == optionId).findFirst().orElseThrow();
    }

    public NpcType type() {
        return type;
    }

    public int optionId() {
        return optionId;
    }

    public int basePrice() {
        return de.artemis.cyberneticenhancements.common.economy.EconomyBalance.value("service." + id());
    }

    public int requiredTrust() {
        return de.artemis.cyberneticenhancements.common.economy.EconomyBalance.value("access.service." + id());
    }

    public boolean daily() {
        return reset;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String nameKey() {
        return "screen.cyberneticenhancements.npc.service." + id() + ".name";
    }

    public String descriptionKey() {
        return "screen.cyberneticenhancements.npc.service." + id() + ".description";
    }

    public List<Component> details() {
        List<Component> lines = new ArrayList<>();
        if (healing > 0)
            lines.add(Component.translatable("screen.cyberneticenhancements.npc.service.healing", healing));
        if (suppression > 0) {
            lines.add(Component.translatable("screen.cyberneticenhancements.npc.service.suppression", suppression, duration(suppressionSeconds)));
            lines.add(effectLine(MobEffects.MOVEMENT_SLOWDOWN, 4, 0));
        }
        if (reset) lines.add(Component.translatable("screen.cyberneticenhancements.npc.service.reset"));
        if (detox) lines.add(Component.translatable("screen.cyberneticenhancements.npc.service.detox"));
        for (Buff buff : buffs) lines.add(effectLine(buff.effect.get(), buff.seconds, buff.amplifier));
        return List.copyOf(lines);
    }

    public void apply(ServerPlayer player) {
        if (this == TRAUMA_COVERAGE)
            player.getPersistentData().putBoolean("cyberneticenhancements.trauma_coverage", true);
        if (reset) CyberstrainManager.purgeNegativeStatuses(player, true);
        if (detox) {
            for (var effect : new ArrayList<>(player.getActiveEffects())) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                    player.removeEffect(effect.getEffect());
            }
        }
        if (suppression > 0)
            CyberstrainManager.applySuppressionDose(player, suppression, suppressionSeconds * 20L, false);
        if (healing > 0) player.heal(healing);
        for (Buff buff : buffs)
            player.addEffect(new MobEffectInstance(buff.effect.get(), buff.seconds * 20, buff.amplifier));
    }

    private static Component effectLine(Holder<MobEffect> effect, int seconds, int amplifier) {
        Component name = Component.translatable(effect.value().getDescriptionId());
        if (amplifier > 0)
            name = Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + amplifier));
        return Component.translatable("screen.cyberneticenhancements.npc.service.effect", name, duration(seconds));
    }

    private static String duration(int seconds) {
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private static Buff buff(java.util.function.Supplier<Holder<MobEffect>> effect, int seconds, int amplifier) {
        return new Buff(effect, seconds, amplifier);
    }

    private record Buff(java.util.function.Supplier<Holder<MobEffect>> effect, int seconds, int amplifier) {
    }
}
