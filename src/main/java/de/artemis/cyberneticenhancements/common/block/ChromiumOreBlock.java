package de.artemis.cyberneticenhancements.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Saved on each ore block so discovery identifies real deposit ore, not nearby rock. */
public final class ChromiumOreBlock extends Block {
    public static final MapCodec<ChromiumOreBlock> CODEC = simpleCodec(ChromiumOreBlock::new);
    public static final BooleanProperty LARGE_DEPOSIT = BooleanProperty.create("large_deposit");

    public ChromiumOreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LARGE_DEPOSIT, false));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LARGE_DEPOSIT);
    }
}
