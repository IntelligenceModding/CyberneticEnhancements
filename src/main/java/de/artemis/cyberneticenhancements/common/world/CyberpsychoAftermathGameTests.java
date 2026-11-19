package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity;
import de.artemis.cyberneticenhancements.common.entity.EncounterBodyEntity;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CyberpsychoAftermathGameTests {
    @GameTest(template = "sandevistan_test", batch = "psycho_aftermath_visuals", timeoutTicks = 30)
    public static void decorativeBodyPresentationsHaveRenderableIdentity(GameTestHelper h) {
        var owner = ModEntityTypes.CYBERPSYCHO.get().create(h.getLevel());
        for (int appearance = 0; appearance < 4; appearance++) {
            var anchor = ModEntityTypes.ENCOUNTER_BODY.get().create(h.getLevel());
            anchor.setPos(2, 5, 3);
            anchor.configure(owner, appearance, 72);
            var saved = new CompoundTag();
            anchor.saveWithoutId(saved);
            var restored = ModEntityTypes.ENCOUNTER_BODY.get().create(h.getLevel());
            restored.load(saved);
            for (var body : List.of(anchor, restored)) {
                var source = body.createVisual();
                h.assertTrue(source != null && source.getUUID().equals(body.getUUID()) && source.position().equals(body.position())
                        && source.isNoAi() && !source.isCustomNameVisible(), "Body proxy lost its presentation state");
                if (source instanceof de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity npc)
                    h.assertTrue(npc.hasResolvedIdentity(), "Ordinary encounter body is rejected by its renderer: " + source.getType());
                else h.assertTrue(appearance == 0 && source instanceof de.artemis.cyberneticenhancements.common.entity.MaxTacEntity,
                        "Encounter appearance used the wrong renderer");
                h.assertTrue(h.getLevel().getEntity(source.getUUID()) == null, "Render proxy was added as a real NPC");
                source.discard();
            }
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "psycho_aftermath_hold", timeoutTicks = 550)
    public static void mixedBodiesStayBeyondNormalCorpseLifetimeAndLeaveAfterOwnerDies(GameTestHelper h) {
        var psycho = spawn(h);
        var initial = new AtomicReference<List<EncounterBodyEntity>>();
        h.runAfterDelay(5, () -> {
            var bodies = bodies(psycho);
            h.assertTrue(bodies.size() >= 2 && bodies.size() <= 5, "Encounter did not create 2-5 bodies");
            h.assertTrue(bodies.stream().anyMatch(b -> b.appearance() == 0) && bodies.stream().anyMatch(b -> b.appearance() != 0),
                    "Encounter did not contain both MaxTec and ordinary NPC bodies");
            h.assertTrue(bodies.stream().allMatch(b -> !b.isPickable() && !b.isPushable() && b.distanceToSqr(psycho) <= 26),
                    "Bodies were interactive or outside the immediate encounter area");
            initial.set(bodies);
        });
        h.runAfterDelay(290, () -> {
            h.assertTrue(initial.get().stream().allMatch(b -> !b.isRemoved() && b.departure() < 0), "Encounter bodies expired while their cyberpsycho was alive nearby");
            h.assertTrue(bodies(psycho).size() == initial.get().size(), "Encounter duplicated its body group");
            psycho.setHealth(0); psycho.die(psycho.damageSources().generic());
            long deathTick = h.getLevel().getGameTime();
            h.assertTrue(initial.get().stream().allMatch(body -> body.ownerDeath() == deathTick
                    && body.departure() == deathTick + de.artemis.cyberneticenhancements.common.util.DeathBodyLifetime.HOLD_TICKS),
                    "Decorative bodies did not share the cyberpsycho corpse's death timestamp and fade deadline");
            var saved = new CompoundTag(); initial.get().getFirst().saveWithoutId(saved);
            var restored = ModEntityTypes.ENCOUNTER_BODY.get().create(h.getLevel()); restored.load(saved);
            h.assertTrue(restored.ownerDeath() == deathTick && restored.departure() == initial.get().getFirst().departure(),
                    "Reload lost the synchronized corpse disappearance schedule");
        });
        h.runAfterDelay(335, () -> {
            h.assertTrue(initial.get().stream().allMatch(body -> !body.isRemoved()), "Decorative bodies vanished before the cyberpsycho corpse");
        });
        h.runAfterDelay(509, () -> {
            h.assertTrue(initial.get().stream().allMatch(body -> !body.isRemoved()), "Bodies vanished before the shared hologram fade completed");
        });
        h.runAfterDelay(511, () -> {
            h.assertTrue(initial.get().stream().allMatch(Entity::isRemoved), "Bodies remained after their owner's death and hologram fade");
            psycho.discard(); h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "psycho_aftermath_distance", timeoutTicks = 80)
    public static void distantOwnerReleasesBodiesWithoutCreatingAReplacementGroup(GameTestHelper h) {
        var psycho = spawn(h);
        var initial = new AtomicReference<List<EncounterBodyEntity>>();
        h.runAfterDelay(5, () -> {
            initial.set(bodies(psycho));
            h.assertTrue(initial.get().size() >= 2, "Encounter group was missing");
            psycho.setPos(psycho.position().add(40, 0, 0));
        });
        h.runAfterDelay(50, () -> {
            h.assertTrue(psycho.isAlive(), "Distance cleanup killed the owner");
            h.assertTrue(initial.get().stream().allMatch(Entity::isRemoved), "Bodies did not dissolve after the owner moved beyond 32 blocks");
            h.assertTrue(bodies(psycho).isEmpty(), "Moving the owner generated another group of bodies");
            psycho.discard(); h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "psycho_aftermath_reload", timeoutTicks = 100)
    public static void ownerReloadKeepsExistingBodiesAndTheirPersistentLinkWithoutDuplicates(GameTestHelper h) {
        var owner = new AtomicReference<>(spawn(h));
        var initial = new AtomicReference<List<EncounterBodyEntity>>();
        var savedOwner = new CompoundTag();
        h.runAfterDelay(5, () -> {
            var psycho = owner.get(); initial.set(bodies(psycho));
            h.assertTrue(initial.get().size() >= 2, "Encounter group was missing");
            psycho.saveWithoutId(savedOwner);
            h.assertTrue(savedOwner.getBoolean("AftermathCreated"), "Owner did not save its scenery creation state");
            var original = initial.get().getFirst();
            var savedBody = new CompoundTag(); original.saveWithoutId(savedBody);
            var restoredBody = ModEntityTypes.ENCOUNTER_BODY.get().create(h.getLevel()); restoredBody.load(savedBody);
            h.assertTrue(restoredBody.owner().equals(psycho.getUUID()) && restoredBody.appearance() == original.appearance()
                    && restoredBody.fallYaw() == original.fallYaw() && restoredBody.departure() < 0, "Body reload lost its owner, appearance or held state");
            psycho.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        });
        h.runAfterDelay(8, () -> {
            var restored = ModEntityTypes.CYBERPSYCHO.get().create(h.getLevel()); restored.load(savedOwner);
            h.getLevel().addFreshEntity(restored); owner.set(restored);
        });
        h.runAfterDelay(65, () -> {
            h.assertTrue(initial.get().stream().allMatch(b -> !b.isRemoved() && b.departure() < 0), "Owner reload released its existing bodies");
            h.assertTrue(bodies(owner.get()).size() == initial.get().size(), "Owner reload generated a duplicate body group");
            initial.get().forEach(Entity::discard); owner.get().discard(); h.succeed();
        });
    }

    private static CyberpsychoEntity spawn(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 5, 2));
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
            h.getLevel().setBlock(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
            for (int y = 0; y <= 3; y++) h.getLevel().setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        var psycho = ModEntityTypes.CYBERPSYCHO.get().create(h.getLevel());
        psycho.setPos(Vec3.atBottomCenterOf(pos)); psycho.setNoAi(true); psycho.setNoGravity(true);
        h.getLevel().addFreshEntity(psycho);
        return psycho;
    }

    private static List<EncounterBodyEntity> bodies(CyberpsychoEntity psycho) {
        return psycho.level().getEntitiesOfClass(EncounterBodyEntity.class, psycho.getBoundingBox().inflate(60),
                body -> psycho.getUUID().equals(body.owner()));
    }
    private CyberpsychoAftermathGameTests() { }
}
