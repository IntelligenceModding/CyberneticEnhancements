package de.artemis.cyberneticenhancements.common.block;

import de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity;
import de.artemis.cyberneticenhancements.common.menu.RelicCacheHackMenu;
import de.artemis.cyberneticenhancements.common.world.RelicCacheStructurePlacementSavedData;
import de.artemis.cyberneticenhancements.common.world.RelicCacheIntel;
import de.artemis.cyberneticenhancements.common.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class RelicCacheBlock extends BaseEntityBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final String OPENED_CACHES_KEY = "cyberneticenhancements.relic_caches";

    private final RelicCacheTier tier;

    protected RelicCacheBlock(BlockBehaviour.Properties properties, RelicCacheTier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public RelicCacheTier tier() {
        return tier;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (level instanceof ServerLevel serverLevel && !oldState.is(state.getBlock())) {
            RelicCacheStructurePlacementSavedData.get(serverLevel).register(pos, tier);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (level instanceof ServerLevel serverLevel && !state.is(newState.getBlock())) {
            RelicCacheStructurePlacementSavedData.get(serverLevel).remove(pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    public ResourceKey<LootTable> lootTableKey() {
        return tier.lootTableKey();
    }

    public ResourceKey<LootTable> displayLootTableKey() {
        return tier.displayLootTableKey();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RelicCacheBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, ModBlockEntities.RELIC_CACHE.get(), (tickerLevel, pos, tickerState, blockEntity) ->
                RelicCacheBlockEntity.tick((ServerLevel) tickerLevel, pos, tickerState, blockEntity));
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return useWithoutItem(state, level, pos, player, hitResult) == InteractionResult.SUCCESS
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            RelicCacheIntel.removeCompassForCache(serverPlayer, pos);
            openHackInterface(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    private void openHackInterface(ServerPlayer player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (hasPlayerOpened(player, serverLevel, pos)) {
            notifyPlayer(player, "message.cyberneticenhancements.relic_cache.already_opened", ChatFormatting.DARK_AQUA);
            serverLevel.playSound(null, pos, SoundEvents.VAULT_REJECT_REWARDED_PLAYER, SoundSource.BLOCKS, 0.75F, 0.95F);
            return;
        }

        if (!(serverLevel.getBlockEntity(pos) instanceof RelicCacheBlockEntity blockEntity)) {
            notifyPlayer(player, "message.cyberneticenhancements.relic_cache.unavailable", ChatFormatting.RED);
            return;
        }

        if (blockEntity.isAnimating()) {
            notifyPlayer(player, "message.cyberneticenhancements.relic_cache.sync_in_progress", ChatFormatting.GRAY);
            return;
        }

        boolean resuming = blockEntity.isHackSessionOwnedBy(player);
        if (!blockEntity.ensureHackSession(player)) {
            notifyPlayer(player, "message.cyberneticenhancements.relic_cache.remote_user", ChatFormatting.GRAY);
            serverLevel.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.35F, 0.9F);
            return;
        }

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new RelicCacheHackMenu(containerId, inventory, pos),
                Component.translatable("screen.cyberneticenhancements.relic_cache_hack.title")
        );
        player.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
        if (!resuming) {
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.relic_cache.breach_started").withStyle(ChatFormatting.AQUA), true);
            serverLevel.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 0.5F, 1.3F);
            serverLevel.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.35F, 1.25F);
        }
    }

    public static List<ItemStack> rollLoot(ServerLevel level, BlockPos pos, Player player, ResourceKey<LootTable> lootTableKey, long salt) {
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableKey);
        LootParams lootParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.CHEST);
        return lootTable.getRandomItems(lootParams, RandomSource.create(level.getGameTime() ^ pos.asLong() ^ player.getUUID().getLeastSignificantBits() ^ salt));
    }

    public static boolean hasPlayerOpened(Player player, ServerLevel level, BlockPos pos) {
        return openedCaches(player).contains(cacheKey(level, pos));
    }

    public static void markPlayerOpened(Player player, ServerLevel level, BlockPos pos) {
        Set<String> openedCaches = openedCaches(player);
        openedCaches.add(cacheKey(level, pos));
        storeOpenedCaches(player, openedCaches);
    }

    private static Set<String> openedCaches(Player player) {
        Set<String> openedCaches = new HashSet<>();
        ListTag stored = player.getPersistentData().getList(OPENED_CACHES_KEY, Tag.TAG_STRING);
        for (Tag tag : stored) {
            openedCaches.add(tag.getAsString());
        }
        return openedCaches;
    }

    private static void storeOpenedCaches(Player player, Set<String> openedCaches) {
        CompoundTag persistentData = player.getPersistentData();
        if (openedCaches.isEmpty()) {
            persistentData.remove(OPENED_CACHES_KEY);
            return;
        }
        ListTag stored = new ListTag();
        for (String key : openedCaches) {
            stored.add(StringTag.valueOf(key));
        }
        persistentData.put(OPENED_CACHES_KEY, stored);
    }

    private static String cacheKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    private static void notifyPlayer(Player player, String translationKey, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(translationKey).withStyle(color), true);
    }
}
