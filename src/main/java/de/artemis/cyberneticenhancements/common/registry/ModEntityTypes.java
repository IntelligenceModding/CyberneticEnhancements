package de.artemis.cyberneticenhancements.common.registry;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.FixerEntity;
import de.artemis.cyberneticenhancements.common.entity.MercEntity;
import de.artemis.cyberneticenhancements.common.entity.NetrunnerEntity;
import de.artemis.cyberneticenhancements.common.entity.RipperdocEntity;
import de.artemis.cyberneticenhancements.common.entity.TechieEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CyberneticEnhancements.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<FixerEntity>> FIXER =
            ENTITY_TYPES.register(
                    "fixer",
                    () -> EntityType.Builder.of(FixerEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .build("fixer")
            );
    public static final DeferredHolder<EntityType<?>, EntityType<RipperdocEntity>> RIPPERDOC =
            registerNpc("ripperdoc", RipperdocEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<TechieEntity>> TECHIE =
            registerNpc("techie", TechieEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<NetrunnerEntity>> NETRUNNER =
            registerNpc("netrunner", NetrunnerEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<MercEntity>> MERC =
            registerNpc("merc", MercEntity::new);

    public static final DeferredHolder<EntityType<?>, EntityType<de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity>> CYBERPSYCHO =
            ENTITY_TYPES.register("cyberpsycho", () -> EntityType.Builder.of(de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("cyberpsycho"));
    public static final DeferredHolder<EntityType<?>, EntityType<de.artemis.cyberneticenhancements.common.entity.EncounterBodyEntity>> ENCOUNTER_BODY =
            ENTITY_TYPES.register("encounter_body", () -> EntityType.Builder.<de.artemis.cyberneticenhancements.common.entity.EncounterBodyEntity>of(
                    de.artemis.cyberneticenhancements.common.entity.EncounterBodyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.4F).clientTrackingRange(8).updateInterval(20).build("encounter_body"));
    public static final DeferredHolder<EntityType<?>, EntityType<de.artemis.cyberneticenhancements.common.entity.CyberpsychoMissile>> CYBERPSYCHO_MISSILE =
            ENTITY_TYPES.register("cyberpsycho_missile", () -> EntityType.Builder.<de.artemis.cyberneticenhancements.common.entity.CyberpsychoMissile>of(
                            de.artemis.cyberneticenhancements.common.entity.CyberpsychoMissile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).build("cyberpsycho_missile"));
    public static final DeferredHolder<EntityType<?>, EntityType<de.artemis.cyberneticenhancements.common.entity.MaxTacEntity>> MAX_TAC =
            ENTITY_TYPES.register("max_tac", () -> EntityType.Builder.of(de.artemis.cyberneticenhancements.common.entity.MaxTacEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("max_tac"));

    public static final DeferredHolder<EntityType<?>, EntityType<de.artemis.cyberneticenhancements.common.entity.GangMemberEntity>> GANG_MEMBER =
            ENTITY_TYPES.register("gang_member", () -> EntityType.Builder.of(de.artemis.cyberneticenhancements.common.entity.GangMemberEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("gang_member"));

    public static final DeferredHolder<EntityType<?>, EntityType<de.artemis.cyberneticenhancements.common.entity.DevTestEntity>> DEV_TEST =
            registerNpc("dev_test", de.artemis.cyberneticenhancements.common.entity.DevTestEntity::new);

    public static final DeferredHolder<EntityType<?>, EntityType<de.artemis.cyberneticenhancements.common.entity.MedTechEntity>> MED_TECH = registerNpc("med_tech", de.artemis.cyberneticenhancements.common.entity.MedTechEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<de.artemis.cyberneticenhancements.common.entity.TraumaTeamEntity>> TRAUMA_TEAM = registerNpc("trauma_team", de.artemis.cyberneticenhancements.common.entity.TraumaTeamEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity>> WORLD_ENCOUNTER_NPC = registerNpc("world_encounter_npc", de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<de.artemis.cyberneticenhancements.common.entity.StreetRipperdocEntity>> STREET_RIPPERDOC = registerNpc("street_ripperdoc", de.artemis.cyberneticenhancements.common.entity.StreetRipperdocEntity::new);

    public static final DeferredHolder<EntityType<?>, EntityType<de.artemis.cyberneticenhancements.common.entity.MaxTecGuardEntity>> MAXTEC_GUARD =
            registerNpc("maxtec_guard", de.artemis.cyberneticenhancements.common.entity.MaxTecGuardEntity::new);

    public static final DeferredHolder<EntityType<?>, EntityType<de.artemis.cyberneticenhancements.common.tarot.TarotCardEntity>> TAROT_CARD =
            ENTITY_TYPES.register("tarot_card", () -> EntityType.Builder.<de.artemis.cyberneticenhancements.common.tarot.TarotCardEntity>of(
                    de.artemis.cyberneticenhancements.common.tarot.TarotCardEntity::new, MobCategory.MISC)
                    .sized(de.artemis.cyberneticenhancements.common.tarot.TarotCardEntity.WIDTH,
                            de.artemis.cyberneticenhancements.common.tarot.TarotCardEntity.HEIGHT)
                    .clientTrackingRange(8).updateInterval(20).build("tarot_card"));

    private ModEntityTypes() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MAXTEC_GUARD.get(), AbstractCityNpcEntity.createAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 70)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 12)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 8)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT, 1.5).build());
        event.put(MED_TECH.get(), AbstractCityNpcEntity.createAttributes().build());
        event.put(TRAUMA_TEAM.get(), AbstractCityNpcEntity.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT, 1.5).add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 50).add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.34).build());
        event.put(FIXER.get(), AbstractCityNpcEntity.createAttributes().build());
        event.put(RIPPERDOC.get(), AbstractCityNpcEntity.createAttributes().build());
        event.put(WORLD_ENCOUNTER_NPC.get(), AbstractCityNpcEntity.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT, 1.5).build());
        event.put(STREET_RIPPERDOC.get(), AbstractCityNpcEntity.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT, 1.5).build());
        event.put(TECHIE.get(), AbstractCityNpcEntity.createAttributes().build());
        event.put(NETRUNNER.get(), AbstractCityNpcEntity.createAttributes().build());
        event.put(DEV_TEST.get(), AbstractCityNpcEntity.createAttributes().build());
        event.put(MERC.get(), AbstractCityNpcEntity.createAttributes().build());
        event.put(CYBERPSYCHO.get(), de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity.createAttributes().build());
        event.put(GANG_MEMBER.get(), de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity.createAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT, 1.0).build());
        event.put(MAX_TAC.get(),de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity.createAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT, 1.5)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 70)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 12)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 6).build());
    }

    private static <T extends AbstractCityNpcEntity> DeferredHolder<EntityType<?>, EntityType<T>> registerNpc(
            String id,
            EntityType.EntityFactory<T> factory
    ) {
        return ENTITY_TYPES.register(
                id,
                () -> EntityType.Builder.of(factory, MobCategory.CREATURE)
                        .sized(0.6F, 1.95F)
                        .clientTrackingRange(8)
                        .build(id)
        );
    }
}
