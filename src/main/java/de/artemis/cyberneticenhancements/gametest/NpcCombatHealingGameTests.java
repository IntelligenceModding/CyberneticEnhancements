package de.artemis.cyberneticenhancements.gametest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.*;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NpcCombatHealingGameTests {
    @GameTest(template = "sandevistan_test", batch = "combat_medicine", timeoutTicks = 360)
    public static void attackGapsAllowInterruptibleHealingAndSuppliesPersist(GameTestHelper h) {
        var attacker = EntityType.ZOMBIE.create(h.getLevel());
        attacker.setNoAi(true);
        var npcs = new ArrayList<Mob>();
        var healthBeforeHealing = new java.util.HashMap<Mob, Float>();
        for (var type : List.of(ModEntityTypes.FIXER.get(), ModEntityTypes.CYBERPSYCHO.get(),
                ModEntityTypes.MAX_TAC.get(), ModEntityTypes.MAXTEC_GUARD.get())) {
            var npc = type.create(h.getLevel());
            npc.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))));
            npc.setNoAi(true);
            npc.setNoGravity(true);
            npc.setHealth(npc.getMaxHealth() * 0.3F);
            h.getLevel().addFreshEntity(npc);
            npc.hurt(npc.damageSources().mobAttack(attacker), 1);
            npcs.add(npc);
        }
        h.runAtTickTime(NpcCombatHealing.GAP - 2, () -> {
            for (var npc : npcs) h.assertTrue(!NpcCombatHealing.of(npc).usingItem(), "NPC healed before the attack gap");
        });
        h.runAtTickTime(NpcCombatHealing.GAP + 5, () -> {
            for (var npc : npcs) {
                var healing = NpcCombatHealing.of(npc);
                h.assertTrue(healing.usingItem() && healing.progress(0) < 1, "Attack gap did not start a visible use action: " + npc.getType());
                npc.invulnerableTime = 0;
                npc.hurt(npc.damageSources().mobAttack(attacker), 1);
                healthBeforeHealing.put(npc, npc.getHealth());
                h.assertTrue(!healing.usingItem() && healing.remainingDoses() == NpcCombatHealing.DOSES, "Hit failed to interrupt healing or consumed a dose");
            }
        });
        h.runAtTickTime(NpcCombatHealing.GAP * 2 + 10, () -> {
            for (var npc : npcs) h.assertTrue(NpcCombatHealing.of(npc).usingItem(), "NPC did not retry after another attack gap");
        });
        h.runAtTickTime(NpcCombatHealing.GAP * 2 + NpcCombatHealing.USE_TIME + 12, () -> {
            for (var npc : npcs) {
                var healing = NpcCombatHealing.of(npc);
                h.assertTrue(!healing.usingItem() && healing.highlighted() && healing.remainingDoses() == 2,
                        "Healing did not finish exactly once: " + npc.getType());
                h.assertTrue(Math.abs(npc.getHealth() - healthBeforeHealing.get(npc)
                        - npc.getMaxHealth() * NpcCombatHealing.HEAL_FRACTION) < 0.01F, "Completed item use restored the wrong health amount");
                if (npc instanceof CyberpsychoEntity psycho && !(npc instanceof MaxTacEntity))
                    h.assertTrue(Math.abs(psycho.bossEvent().getProgress() - npc.getHealth() / npc.getMaxHealth()) < 0.001F, "Boss bar missed the heal");
                healing.tick();
                h.assertTrue(!healing.usingItem(), "Cooldown allowed immediate repeat use");
                var saved = new CompoundTag();
                npc.saveWithoutId(saved);
                var copy = (Mob) npc.getType().create(h.getLevel());
                copy.load(saved);
                h.assertTrue(NpcCombatHealing.of(copy).remainingDoses() == 2, "Reload replenished consumed medicine");
                saved.getCompound("CombatMedicine").putInt("Doses", 0);
                saved.getCompound("CombatMedicine").putLong("ReadyAt", 0);
                copy.load(saved);
                NpcCombatHealing.of(copy).tick();
                h.assertTrue(!NpcCombatHealing.of(copy).usingItem(), "Exhausted NPC created free medicine");
                copy.discard();
                npc.discard();
            }
            attacker.discard();
            h.succeed();
        });
    }
}
