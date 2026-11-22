package de.artemis.cyberneticenhancements.common.registry;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.world.ScorchedGroundPiece;
import de.artemis.cyberneticenhancements.common.world.MedTecAvStructure;
import de.artemis.cyberneticenhancements.common.world.ShippingContainerPiece;
import de.artemis.cyberneticenhancements.common.world.ShippingContainerStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, CyberneticEnhancements.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, CyberneticEnhancements.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<MedTecAvStructure>> MED_TEC_AV =
            STRUCTURE_TYPES.register("med_tec_av", () -> () -> MedTecAvStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<ShippingContainerStructure>> SHIPPING_CONTAINER =
            STRUCTURE_TYPES.register("shipping_container", () -> () -> ShippingContainerStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHIPPING_CONTAINER_PIECE =
            PIECE_TYPES.register("shipping_container", () -> (StructurePieceType.StructureTemplateType) ShippingContainerPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SCORCHED_GROUND_PIECE =
            PIECE_TYPES.register("scorched_ground", () -> (StructurePieceType.ContextlessType) ScorchedGroundPiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHIPPING_CONTAINER_GROUND_PIECE =
            PIECE_TYPES.register("shipping_container_ground", () -> (StructurePieceType.ContextlessType) de.artemis.cyberneticenhancements.common.world.ShippingContainerGroundPiece::new);

    private ModStructures() {}

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        PIECE_TYPES.register(modEventBus);
    }
}
