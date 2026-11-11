package de.artemis.cyberneticenhancements.common.blockentity;

import de.artemis.cyberneticenhancements.common.block.RelicCacheBlock;
import de.artemis.cyberneticenhancements.common.menu.RelicCacheHackMenu;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.registry.ModBlockEntities;
import de.artemis.cyberneticenhancements.common.reliccache.RelicCacheHackPuzzle;
import de.artemis.cyberneticenhancements.common.reliccache.RelicCacheHackRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class RelicCacheBlockEntity extends BlockEntity {
    private static final String ANIMATION_TICKS_TAG = "AnimationTicks";
    private static final String PREVIEW_INDEX_TAG = "PreviewIndex";
    private static final String OPENING_PLAYER_TAG = "OpeningPlayer";
    private static final String PREVIEW_ITEMS_TAG = "PreviewItems";
    private static final String REWARD_ITEMS_TAG = "RewardItems";
    private static final String PUZZLE_DIFFICULTY_TAG = "PuzzleDifficulty";
    private static final String PUZZLE_GRID_SIZE_TAG = "PuzzleGridSize";
    private static final String PUZZLE_TIME_LIMIT_TAG = "PuzzleTimeLimit";
    private static final String PUZZLE_TARGET_COUNT_TAG = "PuzzleTargetCount";
    private static final String PUZZLE_GRID_TOKENS_TAG = "PuzzleGridTokens";
    private static final String PUZZLE_TARGET_LENGTHS_TAG = "PuzzleTargetLengths";
    private static final String PUZZLE_TARGET_TOKENS_TAG = "PuzzleTargetTokens";
    private static final String HACKING_PLAYER_TAG = "HackingPlayer";
    private static final String HACK_DIFFICULTY_TAG = "HackDifficulty";
    private static final String HACK_GRID_SIZE_TAG = "HackGridSize";
    private static final String HACK_TIME_LIMIT_TAG = "HackTimeLimit";
    private static final String HACK_TIME_REMAINING_TAG = "HackTimeRemaining";
    private static final String HACK_TARGET_COUNT_TAG = "HackTargetCount";
    private static final String HACK_SELECTED_COUNT_TAG = "HackSelectedCount";
    private static final String HACK_COMPLETED_MASK_TAG = "HackCompletedMask";
    private static final String HACK_GRID_TOKENS_TAG = "HackGridTokens";
    private static final String HACK_TARGET_LENGTHS_TAG = "HackTargetLengths";
    private static final String HACK_TARGET_TOKENS_TAG = "HackTargetTokens";
    private static final String HACK_SELECTED_CELLS_TAG = "HackSelectedCells";

    public static final int ANIMATION_TICKS = 56;
    public static final int PREVIEW_STEP_TICKS = 4;

    private int animationTicksRemaining;
    private int previewIndex;
    private UUID openingPlayer;
    private final List<ItemStack> previewItems = new ArrayList<>();
    private final List<ItemStack> rewardItems = new ArrayList<>();

    private int puzzleVersion;
    private int puzzleDifficulty;
    private int puzzleGridSize;
    private int puzzleTimeLimitTicks;
    private int puzzleTargetCount;
    private final int[] puzzleGridTokens = new int[RelicCacheHackPuzzle.MAX_GRID_CELLS];
    private final int[] puzzleTargetLengths = new int[RelicCacheHackPuzzle.MAX_TARGET_COUNT];
    private final int[] puzzleTargetTokens = new int[RelicCacheHackPuzzle.MAX_TARGET_COUNT * RelicCacheHackPuzzle.MAX_SEQUENCE_LENGTH];

    private UUID hackingPlayer;
    private int hackDifficulty;
    private int hackGridSize;
    private int hackTimeLimitTicks;
    private int hackTimeRemainingTicks;
    private int hackTargetCount;
    private int hackSelectedCount;
    private int hackCompletedMask;
    private final int[] hackGridTokens = new int[RelicCacheHackPuzzle.MAX_GRID_CELLS];
    private final int[] hackTargetLengths = new int[RelicCacheHackPuzzle.MAX_TARGET_COUNT];
    private final int[] hackTargetTokens = new int[RelicCacheHackPuzzle.MAX_TARGET_COUNT * RelicCacheHackPuzzle.MAX_SEQUENCE_LENGTH];
    private final int[] hackSelectedCells = new int[RelicCacheHackPuzzle.MAX_SELECTED_CELLS];

    public RelicCacheBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RELIC_CACHE.get(), pos, blockState);
        clearStoredPuzzleState();
        clearHackSessionState();
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, RelicCacheBlockEntity blockEntity) {
        if (blockEntity.isAnimating()) {
            blockEntity.tickAnimation(level, pos, state);
            return;
        }
        blockEntity.tickHack(level, pos, state);
    }

    private void tickAnimation(ServerLevel level, BlockPos pos, BlockState state) {
        if (shouldAdvancePreview()) {
            previewIndex = (previewIndex + 1) % Math.max(1, previewItems.size());
            showPreviewFeedback(level, pos);
            syncToClient(level, pos, state);
            setChanged();
        }

        emitAnimationParticles(level, pos);
        animationTicksRemaining--;

        if (animationTicksRemaining <= 0) {
            finishAnimation(level, pos);
        }
    }

    private void tickHack(ServerLevel level, BlockPos pos, BlockState state) {
        if (!hasActiveHackSession()) {
            return;
        }

        if (hackSelectedCount == 0) {
            var owner = level.getServer().getPlayerList().getPlayer(hackingPlayer);
            if (owner == null || !(owner.containerMenu instanceof RelicCacheHackMenu menu) || !menu.isForBlock(pos)) {
                clearHackSessionState();
                setChanged();
                syncToClient(level, pos, state);
            }
            return;
        }
        emitHackSessionParticles(level, pos);
        hackTimeRemainingTicks--;
        if (hackTimeRemainingTicks <= 0) {
            ServerPlayer player = hackingPlayer == null ? null : level.getServer().getPlayerList().getPlayer(hackingPlayer);
            failHack(player, Component.translatable("message.cyberneticenhancements.relic_cache.breach_failed"));
            return;
        }

        ServerPlayer player = hackingPlayer == null ? null : level.getServer().getPlayerList().getPlayer(hackingPlayer);
        if (player != null && player.containerMenu instanceof RelicCacheHackMenu menu && menu.isForBlock(pos)) {
            menu.broadcastChanges();
        }
        if (hackTimeRemainingTicks % 20 == 0) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.10F, 0.55F + 0.02F * Math.max(0, 7 - hackTimeRemainingTicks / 20));
            syncToClient(level, pos, state);
            setChanged();
        }
    }

    public boolean ensureHackSession(ServerPlayer player) {
        if (isAnimating()) {
            return false;
        }
        if (hasActiveHackSession()) {
            return player.getUUID().equals(hackingPlayer);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        ensureStoredPuzzle(serverLevel, player);
        beginHackSession(player);

        setChanged();
        syncToClient(serverLevel, worldPosition, getBlockState());
        return true;
    }

    public void trySelectHackCell(ServerPlayer player, int cellIndex) {
        if (!hasActiveHackSession() || hackingPlayer == null || !hackingPlayer.equals(player.getUUID()) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!isSelectableCell(cellIndex)) {
            serverLevel.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.25F, 0.65F);
            return;
        }

        hackSelectedCells[hackSelectedCount++] = cellIndex;
        updateCompletedMask();
        setChanged();
        syncToClient(serverLevel, worldPosition, getBlockState());
        serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.28F, 1.15F + 0.06F * hackSelectedCount);

        if (hackCompletedMask == (1 << hackTargetCount) - 1) {
            completeHack(player, serverLevel);
            return;
        }

        if (hackSelectedCount >= getHackBufferSize()) {
            failHack(player, Component.translatable("message.cyberneticenhancements.relic_cache.buffer_full"));
            return;
        }
        boolean hasMove = false;
        for (int next = 0; next < hackGridSize * hackGridSize; next++) {
            if (isSelectableCell(next)) {
                hasMove = true;
                break;
            }
        }
        if (!hasMove) {
            failHack(player, Component.translatable("message.cyberneticenhancements.relic_cache.no_moves"));
            return;
        }
        if (player.containerMenu instanceof RelicCacheHackMenu menu && menu.isForBlock(worldPosition)) {
            menu.broadcastChanges();
        }
    }

    private void completeHack(ServerPlayer player, ServerLevel level) {
        RelicCacheBlock cacheBlock = currentCacheBlock();
        if (cacheBlock == null) {
            failHack(player, Component.translatable("message.cyberneticenhancements.relic_cache.unavailable"));
            return;
        }

        List<ItemStack> rewards = RelicCacheBlock.rollLoot(level, worldPosition, player, cacheBlock.lootTableKey(), 1L);
        List<ItemStack> previews = new ArrayList<>();
        for (int index = 0; index < 14; index++) {
            previews.addAll(RelicCacheBlock.rollLoot(level, worldPosition, player, cacheBlock.displayLootTableKey(), 71L + index));
        }
        previews.removeIf(ItemStack::isEmpty);

        if (rewards.isEmpty() || previews.isEmpty()) {
            failHack(player, Component.translatable("message.cyberneticenhancements.relic_cache.unavailable"));
            return;
        }

        clearHackSessionState();
        RelicCacheBlock.markPlayerOpened(player, level, worldPosition);
        PlayerQuestManager.onRelicCacheBreached(player, worldPosition);
        startAnimation(player.getUUID(), previews, rewards);
        syncToClient(level, worldPosition, getBlockState());
        setChanged();
        player.displayClientMessage(Component.translatable("message.cyberneticenhancements.relic_cache.breach_success").withStyle(ChatFormatting.AQUA), true);
        level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.65F, 1.25F);
        level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.55F, 0.8F);
        player.closeContainer();
    }

    private void failHack(@Nullable ServerPlayer player, Component message) {
        clearHackSessionState();
        if (level instanceof ServerLevel serverLevel) {
            syncToClient(serverLevel, worldPosition, getBlockState());
            serverLevel.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.45F, 0.75F);
            serverLevel.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.BLOCKS, 0.25F, 0.6F);
        }
        setChanged();
        if (player != null) {
            player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), true);
            if (player.level() == level && player.containerMenu instanceof RelicCacheHackMenu menu && menu.isForBlock(worldPosition)) {
                player.closeContainer();
            }
        }
    }

    private void updateCompletedMask() {
        for (int targetIndex = 0; targetIndex < hackTargetCount; targetIndex++) {
            if ((hackCompletedMask & (1 << targetIndex)) != 0) {
                continue;
            }
            int targetLength = hackTargetLengths[targetIndex];
            if (targetLength <= 0 || targetLength > hackSelectedCount) {
                continue;
            }
            if (pathContainsSequence(targetIndex, targetLength)) {
                hackCompletedMask |= 1 << targetIndex;
            }
        }
    }

    private boolean pathContainsSequence(int targetIndex, int targetLength) {
        return RelicCacheHackRules.containsSequence(hackSelectedCount,
                i -> getHackGridToken(getHackSelectedCell(i)), targetLength,
                i -> hackTargetTokens[targetIndex * RelicCacheHackPuzzle.MAX_SEQUENCE_LENGTH + i]);
    }

    private boolean isSelectableCell(int cellIndex) {
        return hasActiveHackSession() && hackSelectedCount < getHackBufferSize() && RelicCacheHackRules.isSelectable(hackGridSize, hackSelectedCount,
                i -> hackSelectedCells[i], cellIndex);
    }

    private void ensureStoredPuzzle(ServerLevel level, ServerPlayer player) {
        if (hasStoredPuzzle() && puzzleVersion >= 2) {
            return;
        }

        RandomSource random = RandomSource.create(
                level.getGameTime()
                        ^ worldPosition.asLong()
                        ^ player.getUUID().getMostSignificantBits()
                        ^ player.getUUID().getLeastSignificantBits()
        );
        storePuzzleDefinition(RelicCacheHackPuzzle.generate(random, currentCacheBlock().tier().ordinal() + 2));
    }

    private void beginHackSession(ServerPlayer player) {
        hackingPlayer = player.getUUID();
        hackDifficulty = puzzleDifficulty;
        hackGridSize = puzzleGridSize;
        hackTimeLimitTicks = puzzleTimeLimitTicks;
        hackTimeRemainingTicks = puzzleTimeLimitTicks;
        hackTargetCount = puzzleTargetCount;
        hackSelectedCount = 0;
        hackCompletedMask = 0;

        Arrays.fill(hackGridTokens, -1);
        Arrays.fill(hackTargetLengths, 0);
        Arrays.fill(hackTargetTokens, -1);
        Arrays.fill(hackSelectedCells, -1);

        System.arraycopy(puzzleGridTokens, 0, hackGridTokens, 0, hackGridTokens.length);
        System.arraycopy(puzzleTargetLengths, 0, hackTargetLengths, 0, hackTargetLengths.length);
        System.arraycopy(puzzleTargetTokens, 0, hackTargetTokens, 0, hackTargetTokens.length);
    }

    private void storePuzzleDefinition(RelicCacheHackPuzzle puzzle) {
        puzzleVersion = 2;
        puzzleDifficulty = puzzle.difficulty();
        puzzleGridSize = puzzle.gridSize();
        puzzleTimeLimitTicks = puzzle.timeLimitTicks();
        puzzleTargetCount = puzzle.targetCount();
        Arrays.fill(puzzleGridTokens, -1);
        Arrays.fill(puzzleTargetLengths, 0);
        Arrays.fill(puzzleTargetTokens, -1);
        System.arraycopy(puzzle.gridTokens(), 0, puzzleGridTokens, 0, puzzleGridTokens.length);
        System.arraycopy(puzzle.targetLengths(), 0, puzzleTargetLengths, 0, puzzleTargetLengths.length);
        System.arraycopy(puzzle.targetTokens(), 0, puzzleTargetTokens, 0, puzzleTargetTokens.length);
    }

    private boolean hasStoredPuzzle() {
        return puzzleGridSize > 0 && puzzleTargetCount > 0;
    }

    private void clearStoredPuzzleState() {
        puzzleDifficulty = 0;
        puzzleGridSize = 0;
        puzzleTimeLimitTicks = 0;
        puzzleTargetCount = 0;
        Arrays.fill(puzzleGridTokens, -1);
        Arrays.fill(puzzleTargetLengths, 0);
        Arrays.fill(puzzleTargetTokens, -1);
    }

    private void clearHackSessionState() {
        hackingPlayer = null;
        hackDifficulty = 0;
        hackGridSize = 0;
        hackTimeLimitTicks = 0;
        hackTimeRemainingTicks = 0;
        hackTargetCount = 0;
        hackSelectedCount = 0;
        hackCompletedMask = 0;
        Arrays.fill(hackGridTokens, -1);
        Arrays.fill(hackTargetLengths, 0);
        Arrays.fill(hackTargetTokens, -1);
        Arrays.fill(hackSelectedCells, -1);
    }

    public boolean hasActiveHackSession() {
        return hackingPlayer != null && hackGridSize > 0 && hackTimeRemainingTicks > 0;
    }

    public boolean isHackSessionOwnedBy(ServerPlayer player) {
        return hasActiveHackSession() && hackingPlayer != null && hackingPlayer.equals(player.getUUID());
    }

    public int getHackDifficulty() {
        return hackDifficulty;
    }

    public int getHackBufferSize() {
        return RelicCacheHackPuzzle.bufferSize(hackDifficulty);
    }

    public int getHackGridSize() {
        return hackGridSize;
    }

    public int getHackTimeLimitTicks() {
        return hackTimeLimitTicks;
    }

    public int getHackTimeRemainingTicks() {
        return hackTimeRemainingTicks;
    }

    public int getHackTargetCount() {
        return hackTargetCount;
    }

    public int getHackSelectedCount() {
        return hackSelectedCount;
    }

    public int getHackCompletedMask() {
        return hackCompletedMask;
    }

    public int getHackGridToken(int index) {
        return index >= 0 && index < hackGridTokens.length ? hackGridTokens[index] : -1;
    }

    public int getHackTargetLength(int index) {
        return index >= 0 && index < hackTargetLengths.length ? hackTargetLengths[index] : 0;
    }

    public int getHackTargetToken(int index) {
        return index >= 0 && index < hackTargetTokens.length ? hackTargetTokens[index] : -1;
    }

    public int getHackSelectedCell(int index) {
        return index >= 0 && index < hackSelectedCells.length ? hackSelectedCells[index] : -1;
    }

    public boolean isAnimating() {
        return animationTicksRemaining > 0 && !previewItems.isEmpty() && !rewardItems.isEmpty() && openingPlayer != null;
    }

    public boolean startAnimation(UUID playerId, List<ItemStack> previews, List<ItemStack> rewards) {
        if (isAnimating() || previews.isEmpty() || rewards.isEmpty()) {
            return false;
        }
        openingPlayer = playerId;
        previewItems.clear();
        rewardItems.clear();
        previews.forEach(stack -> previewItems.add(stack.copy()));
        rewards.forEach(stack -> rewardItems.add(stack.copy()));
        previewIndex = 0;
        animationTicksRemaining = ANIMATION_TICKS;
        setChanged();
        return true;
    }

    public ItemStack currentPreviewItem() {
        if (previewItems.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int index = Math.max(0, Math.min(previewIndex, previewItems.size() - 1));
        return previewItems.get(index);
    }

    public ItemStack previewItem(int offset) {
        if (previewItems.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int size = previewItems.size();
        int index = Math.floorMod(previewIndex + offset, size);
        return previewItems.get(index);
    }

    public ItemStack rewardItem(int index) {
        if (rewardItems.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return rewardItems.get(Math.floorMod(index, rewardItems.size()));
    }

    public float animationProgress(float partialTick) {
        if (!isAnimating()) {
            return 0.0F;
        }
        float remaining = Math.max(0.0F, animationTicksRemaining - partialTick);
        return Mth.clamp(1.0F - remaining / ANIMATION_TICKS, 0.0F, 1.0F);
    }

    public int previewItemCount() {
        return previewItems.size();
    }

    private void emitHackSessionParticles(ServerLevel level, BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 1.02D;
        double centerZ = pos.getZ() + 0.5D;
        float progress = 1.0F - (float) hackTimeRemainingTicks / (float) Math.max(1, hackTimeLimitTicks);
        float radius = 0.18F + 0.04F * Mth.sin((level.getGameTime() + progress * 20.0F) * 0.18F);

        if ((level.getGameTime() & 1L) == 0L) {
            for (int index = 0; index < 4; index++) {
                float angle = (level.getGameTime() * 0.18F) + index * ((float) Math.PI / 2.0F);
                double x = centerX + Mth.cos(angle) * radius;
                double z = centerZ + Mth.sin(angle) * radius;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, centerY, z, 1, 0.005D, 0.01D, 0.005D, 0.0D);
            }
        }
        if (hackTimeRemainingTicks % 10 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, centerX, centerY + 0.04D, centerZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private void showPreviewFeedback(ServerLevel level, BlockPos pos) {
        ServerPlayer player = openingPlayer == null ? null : level.getServer().getPlayerList().getPlayer(openingPlayer);
        ItemStack preview = currentPreviewItem();
        if (player != null && !preview.isEmpty()) {
            player.displayClientMessage(Component.literal(preview.getHoverName().getString()).withStyle(ChatFormatting.AQUA), true);
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4F, 1.4F + level.random.nextFloat() * 0.25F);
    }

    private void emitAnimationParticles(ServerLevel level, BlockPos pos) {
        RandomSource random = level.random;
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.42D;
        double centerZ = pos.getZ() + 0.5D;
        float progress = animationProgress(0.0F);
        float ringRadius = 0.36F - progress * 0.12F;
        float columnHeight = 0.26F + progress * 0.52F;
        float spin = (level.getGameTime() + progress * 10.0F) * 0.35F;

        for (int index = 0; index < 4; index++) {
            float angle = spin + index * ((float) Math.PI / 2.0F);
            double x = centerX + Mth.cos(angle) * ringRadius;
            double z = centerZ + Mth.sin(angle) * ringRadius;
            double y = centerY + 0.18D + (index % 2 == 0 ? 0.06D : 0.14D) + columnHeight;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, x, y + 0.03D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        level.sendParticles(ParticleTypes.END_ROD, centerX, centerY + columnHeight, centerZ, 1, 0.0D, 0.02D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centerX, centerY + 0.12D, centerZ, 1, 0.06D, 0.04D, 0.06D, 0.0D);

        if (animationTicksRemaining % 6 == 0) {
            for (int index = 0; index < 6; index++) {
                float angle = spin + index * ((float) Math.PI * 2.0F / 6.0F);
                double x = centerX + Mth.cos(angle) * (ringRadius + 0.04F);
                double z = centerZ + Mth.sin(angle) * (ringRadius + 0.04F);
                double y = centerY + 0.20D + progress * 0.38D;
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        ItemStack preview = currentPreviewItem();
        if (!preview.isEmpty()) {
            level.sendParticles(
                    new ItemParticleOption(ParticleTypes.ITEM, preview),
                    centerX,
                    centerY + 0.22D + progress * 0.26D,
                    centerZ,
                    2,
                    0.12D,
                    0.04D,
                    0.12D,
                    0.01D
            );
        }

        if (random.nextFloat() < 0.16F + progress * 0.18F) {
            level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.16F, 1.35F + progress * 0.25F + random.nextFloat() * 0.08F);
        }
    }

    private void finishAnimation(ServerLevel level, BlockPos pos) {
        ServerPlayer player = openingPlayer == null ? null : level.getServer().getPlayerList().getPlayer(openingPlayer);
        if (player != null) {
            for (ItemStack reward : rewardItems) {
                if (!player.addItem(reward.copy())) {
                    player.drop(reward.copy(), false);
                }
            }
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.relic_cache.unpacked").withStyle(ChatFormatting.AQUA), true);
        } else {
            rewardItems.forEach(stack -> Block.popResource(level, pos.above(), stack.copy()));
        }

        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D, 18, 0.25D, 0.25D, 0.25D, 0.12D);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 14, 0.32D, 0.28D, 0.32D, 0.02D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5D, pos.getY() + 0.86D, pos.getZ() + 0.5D, 12, 0.22D, 0.22D, 0.22D, 0.03D);
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, SoundSource.BLOCKS, 0.8F, 0.95F);
        level.playSound(null, pos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 0.95F, 1.05F);

        animationTicksRemaining = 0;
        previewIndex = 0;
        openingPlayer = null;
        previewItems.clear();
        rewardItems.clear();
        syncToClient(level, pos, getBlockState());
        setChanged();
    }

    public void syncToClient(ServerLevel level, BlockPos pos, BlockState state) {
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    private boolean shouldAdvancePreview() {
        return animationTicksRemaining < ANIMATION_TICKS
                && animationTicksRemaining > 1
                && animationTicksRemaining % currentPreviewStepInterval() == 0;
    }

    private int currentPreviewStepInterval() {
        if (animationTicksRemaining > 40) {
            return 8;
        }
        if (animationTicksRemaining > 24) {
            return 5;
        }
        if (animationTicksRemaining > 10) {
            return 3;
        }
        return 2;
    }

    private @Nullable RelicCacheBlock currentCacheBlock() {
        return getBlockState().getBlock() instanceof RelicCacheBlock cacheBlock ? cacheBlock : null;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        animationTicksRemaining = tag.getInt(ANIMATION_TICKS_TAG);
        previewIndex = tag.getInt(PREVIEW_INDEX_TAG);
        openingPlayer = tag.hasUUID(OPENING_PLAYER_TAG) ? tag.getUUID(OPENING_PLAYER_TAG) : null;
        readStacks(tag.getList(PREVIEW_ITEMS_TAG, Tag.TAG_COMPOUND), previewItems, registries);
        readStacks(tag.getList(REWARD_ITEMS_TAG, Tag.TAG_COMPOUND), rewardItems, registries);
        puzzleVersion = tag.getInt("PuzzleVersion");
        puzzleDifficulty = tag.getInt(PUZZLE_DIFFICULTY_TAG);
        puzzleGridSize = tag.getInt(PUZZLE_GRID_SIZE_TAG);
        puzzleTimeLimitTicks = tag.getInt(PUZZLE_TIME_LIMIT_TAG);
        puzzleTargetCount = tag.getInt(PUZZLE_TARGET_COUNT_TAG);
        readIntArray(tag, PUZZLE_GRID_TOKENS_TAG, puzzleGridTokens, -1);
        readIntArray(tag, PUZZLE_TARGET_LENGTHS_TAG, puzzleTargetLengths, 0);
        readIntArray(tag, PUZZLE_TARGET_TOKENS_TAG, puzzleTargetTokens, -1);
        hackingPlayer = tag.hasUUID(HACKING_PLAYER_TAG) ? tag.getUUID(HACKING_PLAYER_TAG) : null;
        hackDifficulty = tag.getInt(HACK_DIFFICULTY_TAG);
        hackGridSize = tag.getInt(HACK_GRID_SIZE_TAG);
        hackTimeLimitTicks = tag.getInt(HACK_TIME_LIMIT_TAG);
        hackTimeRemainingTicks = tag.getInt(HACK_TIME_REMAINING_TAG);
        hackTargetCount = tag.getInt(HACK_TARGET_COUNT_TAG);
        hackSelectedCount = tag.getInt(HACK_SELECTED_COUNT_TAG);
        hackCompletedMask = tag.getInt(HACK_COMPLETED_MASK_TAG);
        readIntArray(tag, HACK_GRID_TOKENS_TAG, hackGridTokens, -1);
        readIntArray(tag, HACK_TARGET_LENGTHS_TAG, hackTargetLengths, 0);
        readIntArray(tag, HACK_TARGET_TOKENS_TAG, hackTargetTokens, -1);
        readIntArray(tag, HACK_SELECTED_CELLS_TAG, hackSelectedCells, -1);
        RelicCacheHackPuzzle.repairLegacyFallback(puzzleGridSize, puzzleGridTokens, puzzleTargetLengths, puzzleTargetTokens);
        if (RelicCacheHackPuzzle.repairLegacyFallback(hackGridSize, hackGridTokens, hackTargetLengths, hackTargetTokens)) {

            clearHackSessionState();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(ANIMATION_TICKS_TAG, animationTicksRemaining);
        tag.putInt(PREVIEW_INDEX_TAG, previewIndex);
        if (openingPlayer != null) {
            tag.putUUID(OPENING_PLAYER_TAG, openingPlayer);
        }
        tag.put(PREVIEW_ITEMS_TAG, writeStacks(previewItems, registries));
        tag.put(REWARD_ITEMS_TAG, writeStacks(rewardItems, registries));
        tag.putInt("PuzzleVersion", puzzleVersion);
        tag.putInt(PUZZLE_DIFFICULTY_TAG, puzzleDifficulty);
        tag.putInt(PUZZLE_GRID_SIZE_TAG, puzzleGridSize);
        tag.putInt(PUZZLE_TIME_LIMIT_TAG, puzzleTimeLimitTicks);
        tag.putInt(PUZZLE_TARGET_COUNT_TAG, puzzleTargetCount);
        tag.putIntArray(PUZZLE_GRID_TOKENS_TAG, puzzleGridTokens);
        tag.putIntArray(PUZZLE_TARGET_LENGTHS_TAG, puzzleTargetLengths);
        tag.putIntArray(PUZZLE_TARGET_TOKENS_TAG, puzzleTargetTokens);
        if (hackingPlayer != null) {
            tag.putUUID(HACKING_PLAYER_TAG, hackingPlayer);
        }
        tag.putInt(HACK_DIFFICULTY_TAG, hackDifficulty);
        tag.putInt(HACK_GRID_SIZE_TAG, hackGridSize);
        tag.putInt(HACK_TIME_LIMIT_TAG, hackTimeLimitTicks);
        tag.putInt(HACK_TIME_REMAINING_TAG, hackTimeRemainingTicks);
        tag.putInt(HACK_TARGET_COUNT_TAG, hackTargetCount);
        tag.putInt(HACK_SELECTED_COUNT_TAG, hackSelectedCount);
        tag.putInt(HACK_COMPLETED_MASK_TAG, hackCompletedMask);
        tag.putIntArray(HACK_GRID_TOKENS_TAG, hackGridTokens);
        tag.putIntArray(HACK_TARGET_LENGTHS_TAG, hackTargetLengths);
        tag.putIntArray(HACK_TARGET_TOKENS_TAG, hackTargetTokens);
        tag.putIntArray(HACK_SELECTED_CELLS_TAG, hackSelectedCells);
    }

    private static void readIntArray(CompoundTag tag, String key, int[] target, int fallbackValue) {
        Arrays.fill(target, fallbackValue);
        int[] source = tag.getIntArray(key);
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }

    private static void readStacks(ListTag source, List<ItemStack> target, HolderLookup.Provider registries) {
        target.clear();
        for (Tag tag : source) {
            if (tag instanceof CompoundTag stackTag) {
                ItemStack stack = ItemStack.parseOptional(registries, stackTag);
                if (!stack.isEmpty()) {
                    target.add(stack);
                }
            }
        }
    }

    private static ListTag writeStacks(List<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag listTag = new ListTag();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                listTag.add(stack.saveOptional(registries));
            }
        }
        return listTag;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
