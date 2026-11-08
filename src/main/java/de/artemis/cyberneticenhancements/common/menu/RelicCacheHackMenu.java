package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.block.RelicCacheBlock;
import de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity;
import de.artemis.cyberneticenhancements.common.registry.ModMenuTypes;
import de.artemis.cyberneticenhancements.common.reliccache.RelicCacheHackPuzzle;
import de.artemis.cyberneticenhancements.common.reliccache.RelicCacheHackRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.Level;

import java.util.Arrays;

public final class RelicCacheHackMenu extends AbstractBaseMenu implements NamedBlockMenu {
    private static final int STATUS_INDEX = 0;
    private static final int DIFFICULTY_INDEX = 1;
    private static final int GRID_SIZE_INDEX = 2;
    private static final int TIME_REMAINING_INDEX = 3;
    private static final int TIME_LIMIT_INDEX = 4;
    private static final int SELECTED_COUNT_INDEX = 5;
    private static final int TARGET_COUNT_INDEX = 6;
    private static final int COMPLETED_MASK_INDEX = 7;
    private static final int GRID_START_INDEX = 16;
    private static final int GRID_DATA_COUNT = RelicCacheHackPuzzle.MAX_GRID_CELLS;
    private static final int TARGET_LENGTH_START_INDEX = GRID_START_INDEX + GRID_DATA_COUNT;
    private static final int TARGET_LENGTH_COUNT = RelicCacheHackPuzzle.MAX_TARGET_COUNT;
    private static final int TARGET_TOKEN_START_INDEX = TARGET_LENGTH_START_INDEX + TARGET_LENGTH_COUNT;
    private static final int TARGET_TOKEN_COUNT = RelicCacheHackPuzzle.MAX_TARGET_COUNT * RelicCacheHackPuzzle.MAX_SEQUENCE_LENGTH;
    private static final int SELECTED_CELL_START_INDEX = TARGET_TOKEN_START_INDEX + TARGET_TOKEN_COUNT;
    private static final int SELECTED_CELL_COUNT = RelicCacheHackPuzzle.MAX_SELECTED_CELLS;
    private static final int TOTAL_DATA_SLOTS = SELECTED_CELL_START_INDEX + SELECTED_CELL_COUNT;

    private final Player player;
    private final Level level;
    private final BlockPos blockPos;
    private final String blockDisplayName;
    private final RelicCacheBlockEntity blockEntity;
    private final boolean clientSide;
    private final int[] syncedData = new int[TOTAL_DATA_SLOTS];

    public RelicCacheHackMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    public RelicCacheHackMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenuTypes.RELIC_CACHE_HACK.get(), containerId);
        this.player = playerInventory.player;
        this.level = player.level();
        this.clientSide = this.level.isClientSide();
        this.blockPos = blockPos.immutable();
        this.blockEntity = this.level.getBlockEntity(blockPos) instanceof RelicCacheBlockEntity cacheBlockEntity ? cacheBlockEntity : null;
        this.blockDisplayName = this.level.getBlockState(blockPos).getBlock().getName().getString();
        Arrays.fill(this.syncedData, -1);

        if (!clientSide && blockEntity != null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            blockEntity.ensureHackSession(serverPlayer);
        }

        addSyncedStateSlots();
    }

    private void addSyncedStateSlots() {
        for (int dataIndex = 0; dataIndex < TOTAL_DATA_SLOTS; dataIndex++) {
            final int syncIndex = dataIndex;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return !clientSide && blockEntity != null ? serverValue(syncIndex) : syncedData[syncIndex];
                }

                @Override
                public void set(int value) {
                    syncedData[syncIndex] = value;
                }
            });
        }
    }

    private int serverValue(int dataIndex) {
        if (blockEntity == null) {
            return -1;
        }
        if (dataIndex == STATUS_INDEX) {
            return blockEntity.hasActiveHackSession() ? 1 : blockEntity.isAnimating() ? 2 : 0;
        }
        if (dataIndex == DIFFICULTY_INDEX) {
            return blockEntity.getHackDifficulty();
        }
        if (dataIndex == GRID_SIZE_INDEX) {
            return blockEntity.getHackGridSize();
        }
        if (dataIndex == TIME_REMAINING_INDEX) {
            return blockEntity.getHackTimeRemainingTicks();
        }
        if (dataIndex == TIME_LIMIT_INDEX) {
            return blockEntity.getHackTimeLimitTicks();
        }
        if (dataIndex == SELECTED_COUNT_INDEX) {
            return blockEntity.getHackSelectedCount();
        }
        if (dataIndex == TARGET_COUNT_INDEX) {
            return blockEntity.getHackTargetCount();
        }
        if (dataIndex == COMPLETED_MASK_INDEX) {
            return blockEntity.getHackCompletedMask();
        }
        if (dataIndex >= GRID_START_INDEX && dataIndex < GRID_START_INDEX + GRID_DATA_COUNT) {
            return blockEntity.getHackGridToken(dataIndex - GRID_START_INDEX);
        }
        if (dataIndex >= TARGET_LENGTH_START_INDEX && dataIndex < TARGET_LENGTH_START_INDEX + TARGET_LENGTH_COUNT) {
            return blockEntity.getHackTargetLength(dataIndex - TARGET_LENGTH_START_INDEX);
        }
        if (dataIndex >= TARGET_TOKEN_START_INDEX && dataIndex < TARGET_TOKEN_START_INDEX + TARGET_TOKEN_COUNT) {
            return blockEntity.getHackTargetToken(dataIndex - TARGET_TOKEN_START_INDEX);
        }
        if (dataIndex >= SELECTED_CELL_START_INDEX && dataIndex < SELECTED_CELL_START_INDEX + SELECTED_CELL_COUNT) {
            return blockEntity.getHackSelectedCell(dataIndex - SELECTED_CELL_START_INDEX);
        }
        return -1;
    }

    public boolean isForBlock(BlockPos pos) {
        return blockPos.equals(pos);
    }

    public boolean hasActiveSession() {
        return syncedData[STATUS_INDEX] == 1;
    }

    public int difficulty() {
        return Math.max(1, syncedData[DIFFICULTY_INDEX]);
    }

    public int gridSize() {
        return Math.max(0, syncedData[GRID_SIZE_INDEX]);
    }

    public int timeRemainingTicks() {
        return Math.max(0, syncedData[TIME_REMAINING_INDEX]);
    }

    public int timeLimitTicks() {
        return Math.max(1, syncedData[TIME_LIMIT_INDEX]);
    }

    public int selectedCount() {
        return Math.max(0, syncedData[SELECTED_COUNT_INDEX]);
    }

    public int targetCount() {
        return Math.max(0, syncedData[TARGET_COUNT_INDEX]);
    }

    public int completedMask() {
        return Math.max(0, syncedData[COMPLETED_MASK_INDEX]);
    }

    public int tokenAtCell(int cellIndex) {
        if (cellIndex < 0 || cellIndex >= GRID_DATA_COUNT) {
            return -1;
        }
        return syncedData[GRID_START_INDEX + cellIndex];
    }

    public int targetLength(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= TARGET_LENGTH_COUNT) {
            return 0;
        }
        return Math.max(0, syncedData[TARGET_LENGTH_START_INDEX + targetIndex]);
    }

    public int targetToken(int targetIndex, int tokenIndex) {
        if (targetIndex < 0 || targetIndex >= TARGET_LENGTH_COUNT || tokenIndex < 0 || tokenIndex >= RelicCacheHackPuzzle.MAX_SEQUENCE_LENGTH) {
            return -1;
        }
        return syncedData[TARGET_TOKEN_START_INDEX + targetIndex * RelicCacheHackPuzzle.MAX_SEQUENCE_LENGTH + tokenIndex];
    }

    public int selectedCell(int selectionIndex) {
        if (selectionIndex < 0 || selectionIndex >= SELECTED_CELL_COUNT) {
            return -1;
        }
        return syncedData[SELECTED_CELL_START_INDEX + selectionIndex];
    }

    public boolean isTargetCompleted(int targetIndex) {
        return targetIndex >= 0
                && targetIndex < targetCount()
                && (completedMask() & (1 << targetIndex)) != 0;
    }

    public boolean isCellAlreadySelected(int cellIndex) {
        for (int index = 0; index < selectedCount(); index++) {
            if (selectedCell(index) == cellIndex) {
                return true;
            }
        }
        return false;
    }

    public boolean isCellSelectable(int cellIndex) {
        return hasActiveSession() && RelicCacheHackRules.isSelectable(gridSize(), selectedCount(), this::selectedCell, cellIndex);
    }

    public int targetPrefixLength(int targetIndex) {
        return RelicCacheHackRules.matchingPrefix(selectedCount(), i -> tokenAtCell(selectedCell(i)),
                targetLength(targetIndex), i -> targetToken(targetIndex, i));
    }

    public String tokenLabel(int tokenId) {
        if (tokenId < 0 || tokenId >= RelicCacheHackPuzzle.TOKEN_LABELS.length) {
            return "--";
        }
        return RelicCacheHackPuzzle.TOKEN_LABELS[tokenId];
    }

    public void trySelectCell(int cellIndex) {
        if (clientSide || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) || blockEntity == null || !stillValid(player)) {
            return;
        }
        blockEntity.trySelectHackCell(serverPlayer, cellIndex);
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player.level().getBlockState(blockPos).getBlock() instanceof RelicCacheBlock)) {
            return false;
        }
        return player.canInteractWithBlock(blockPos, 8.0D);
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    @Override
    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public String getBlockDisplayName() {
        return blockDisplayName;
    }
}
