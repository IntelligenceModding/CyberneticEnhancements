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

    private ModEntityTypes() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(FIXER.get(), AbstractCityNpcEntity.createAttributes().build());
        event.put(RIPPERDOC.get(), AbstractCityNpcEntity.createAttributes().build());
        event.put(TECHIE.get(), AbstractCityNpcEntity.createAttributes().build());
        event.put(NETRUNNER.get(), AbstractCityNpcEntity.createAttributes().build());
        event.put(MERC.get(), AbstractCityNpcEntity.createAttributes().build());
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
