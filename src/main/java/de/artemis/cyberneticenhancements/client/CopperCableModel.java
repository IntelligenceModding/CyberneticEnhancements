package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.block.CopperCableBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Puts the copper cable together out of its pieces: a middle for every side that holds a wire, and an arm wherever that wire
 * reaches another. A block state cannot hold all those arms, so they are worked out from the surrounding wires when the model
 * is built for the world, and the finished quads are kept for each combination.
 */
public final class CopperCableModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<CopperCableBlock.Links> CONNECTIONS = new ModelProperty<>();
    private static final ResourceLocation BLOCK = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "copper_cable");
    private final Map<Direction, BakedModel> cores = new EnumMap<>(Direction.class);
    private final Map<Direction, Map<Direction, BakedModel>> arms = new EnumMap<>(Direction.class);
    private final Map<Direction, Map<Direction, BakedModel>> cornerArms = new EnumMap<>(Direction.class);
    private final Map<Key, List<BakedQuad>> assembled = new ConcurrentHashMap<>();

    private CopperCableModel(BakedModel original, Map<ModelResourceLocation, BakedModel> models) {
        super(original);
        for (var side : Direction.values()) {
            cores.put(side, models.get(piece("core", side, null)));
            var armsOfSide = new EnumMap<Direction, BakedModel>(Direction.class);
            var cornerArmsOfSide = new EnumMap<Direction, BakedModel>(Direction.class);
            for (var toward : Direction.values()) {
                if (toward.getAxis() == side.getAxis()) continue;
                armsOfSide.put(toward, models.get(piece("arm", side, toward)));
                cornerArmsOfSide.put(toward, models.get(piece("corner_arm", side, toward)));
            }
            arms.put(side, armsOfSide);
            cornerArms.put(side, cornerArmsOfSide);
        }
    }

    /** What a drawn model depends on: how the wires reach, which of them run on round a corner, and which sides hold one. */
    private record Key(CopperCableBlock.Links links, int sides) {}

    private static ModelResourceLocation piece(String piece, Direction side, @Nullable Direction toward) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID,
                "block/" + CopperCableBlock.modelName(piece, side, toward)));
    }

    /** Asks for the pieces to be loaded alongside the other models. */
    public static void register(ModelEvent.RegisterAdditional event) {
        for (var side : Direction.values()) {
            event.register(piece("core", side, null));
            for (var toward : Direction.values()) {
                if (toward.getAxis() == side.getAxis()) continue;
                event.register(piece("arm", side, toward));
                event.register(piece("corner_arm", side, toward));
            }
        }
    }

    /** Swaps the cable's baked models, one per block state, for this one. */
    public static void replace(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        for (var entry : new ArrayList<>(models.entrySet()))
            if (entry.getKey().id().equals(BLOCK) && !entry.getKey().getVariant().equals(ModelResourceLocation.STANDALONE_VARIANT))
                models.put(entry.getKey(), new CopperCableModel(entry.getValue(), models));
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        return modelData.derive().with(CONNECTIONS, CopperCableBlock.connections(level, pos, state)).build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource random) {
        return getQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource random, @NotNull ModelData data, @Nullable RenderType renderType) {
        if (state == null || side != null) return List.of(); // None of the pieces has a face to cull.
        var links = data.get(CONNECTIONS);
        if (links == null) links = new CopperCableBlock.Links(0, 0);
        long reaches = links.reaches(), corners = links.corners();
        int sides = 0;
        for (var wire : Direction.values()) if (MultifaceBlock.hasFace(state, wire)) sides |= 1 << wire.ordinal();
        return assembled.computeIfAbsent(new Key(links, sides), ignored -> {
            var quads = new ArrayList<BakedQuad>();
            for (var wire : Direction.values()) {
                if (!MultifaceBlock.hasFace(state, wire)) continue;
                quads.addAll(cores.get(wire).getQuads(state, null, random, ModelData.EMPTY, null));
                for (var toward : arms.get(wire).keySet())
                    if ((reaches & CopperCableBlock.bit(wire, toward)) != 0) {
                        var arm = ((corners & CopperCableBlock.bit(wire, toward)) != 0 ? cornerArms : arms).get(wire).get(toward);
                        quads.addAll(arm.getQuads(state, null, random, ModelData.EMPTY, null));
                    }
            }
            return List.copyOf(quads);
        });
    }
}
