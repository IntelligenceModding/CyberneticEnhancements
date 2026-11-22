package de.artemis.cyberneticenhancements.common.consumable;

import de.artemis.cyberneticenhancements.common.skills.SkillEffects;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareBalance;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffect;
import de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager;
import de.artemis.cyberneticenhancements.common.cyberware.TemporaryCyberwareEffectManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public enum CyberConsumableProfile {
    MAXDOC_MK1,
    MAXDOC_MK2,
    MAXDOC_MK3,
    BOUNCE_BACK_MK1,
    BOUNCE_BACK_MK2,
    BOUNCE_BACK_MK3,
    HEALTH_BOOSTER,
    STAMINA_BOOSTER,
    OXY_BOOSTER,
    CAPACITY_BOOSTER,
    RAM_JOLT,
    IMMUNOBLOCKERS,
    CHROME_SUPPRESSANT,
    BLACK_LACE,
    ASSKICK,
    JELLYTRICITY;

    public void apply(Player player) {
        switch (this) {
            case MAXDOC_MK1 -> player.heal(SkillEffects.healing(player, (float) value("maxdoc_mk1.heal")));
            case MAXDOC_MK2 -> player.heal(SkillEffects.healing(player, (float) value("maxdoc_mk2.heal")));
            case MAXDOC_MK3 -> player.heal(SkillEffects.healing(player, (float) value("maxdoc_mk3.heal")));
            case BOUNCE_BACK_MK1 -> {
                player.heal(SkillEffects.healing(player, (float) value("bounce_back_mk1.heal")));
                TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.healthRegen(value("bounce_back_mk1.regen")), ticks("bounce_back_mk1.duration_ticks"));
            }
            case BOUNCE_BACK_MK2 -> {
                player.heal(SkillEffects.healing(player, (float) value("bounce_back_mk2.heal")));
                TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.healthRegen(value("bounce_back_mk2.regen")), ticks("bounce_back_mk2.duration_ticks"));
            }
            case BOUNCE_BACK_MK3 -> {
                player.heal(SkillEffects.healing(player, (float) value("bounce_back_mk3.heal")));
                TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.healthRegen(value("bounce_back_mk3.regen")), ticks("bounce_back_mk3.duration_ticks"));
            }
            case HEALTH_BOOSTER -> TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.hearts(value("health_booster.hearts")), ticks("health_booster.duration_ticks"));
            case STAMINA_BOOSTER -> TemporaryCyberwareEffectManager.addEffects(player, ticks("stamina_booster.duration_ticks"),
                    CyberwareEffect.moveSpeed(value("stamina_booster.move_speed")),
                    CyberwareEffect.breakSpeed(value("stamina_booster.break_speed")));
            case OXY_BOOSTER -> player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, ticks("oxy_booster.duration_ticks"), 0, true, false, false));
            case CAPACITY_BOOSTER -> TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.chromeCapacity(value("capacity_booster.chrome_capacity")), ticks("capacity_booster.duration_ticks"));
            case RAM_JOLT -> CyberstrainManager.applyRamJolt(player, ticks("ram_jolt.duration_ticks"), value("ram_jolt.cooldown_factor"));
            case IMMUNOBLOCKERS -> CyberstrainManager.applySuppressionDose(player, intValue("immunoblockers.suppression_amount"), ticks("immunoblockers.duration_ticks"), true);
            case CHROME_SUPPRESSANT -> CyberstrainManager.applySuppressionDose(player, intValue("chrome_suppressant.suppression_amount"), ticks("chrome_suppressant.duration_ticks"), false);
            case BLACK_LACE -> {
                reduceHealthFraction(player, (float) value("black_lace.health_fraction_cost"));
                CyberstrainManager.addInstability(player, value("black_lace.instability"));
                TemporaryCyberwareEffectManager.addEffects(player, ticks("black_lace.duration_ticks"),
                        CyberwareEffect.moveSpeed(value("black_lace.move_speed")),
                        CyberwareEffect.damage(value("black_lace.damage")),
                        CyberwareEffect.damageReduction(value("black_lace.damage_reduction")),
                        CyberwareEffect.breakSpeed(value("black_lace.break_speed")));
            }
            case ASSKICK -> {
                CyberstrainManager.addInstability(player, value("asskick.instability"));
                TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.hearts(value("asskick.hearts")), ticks("asskick.duration_ticks"));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, ticks("asskick.duration_ticks"), 0, true, false, false));
            }
            case JELLYTRICITY -> {
                reduceHealthFraction(player, (float) value("jellytricity.health_fraction_cost"));
                CyberstrainManager.addInstability(player, value("jellytricity.instability"));
                TemporaryCyberwareEffectManager.addEffects(player, ticks("jellytricity.duration_ticks"),
                        CyberwareEffect.moveSpeed(value("jellytricity.move_speed")),
                        CyberwareEffect.breakSpeed(value("jellytricity.break_speed")));
            }
        }
    }

    private static int ticks(String key) {
        return CyberwareBalance.intValue("consumable." + key);
    }

    private static int intValue(String key) {
        return CyberwareBalance.intValue("consumable." + key);
    }

    private static double value(String key) {
        return CyberwareBalance.doubleValue("consumable." + key);
    }

    private static void reduceHealthFraction(Player player, float fraction) {
        float reduction = player.getMaxHealth() * fraction;
        player.setHealth(Math.max(1.0F, player.getHealth() - reduction));
    }
}
