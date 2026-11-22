package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.entity.HangingWireEntity;
import de.artemis.cyberneticenhancements.common.entity.WireSigns;
import de.artemis.cyberneticenhancements.common.network.HangingSignTextPayload;
import de.artemis.cyberneticenhancements.common.network.OpenHangingSignPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Field;
import java.util.List;

/** A hanging sign on a hanging wire is not a block, so it is shown and written on through a stand-in sign block entity that is never in the world. */
public final class HangingSignClient {
    private HangingSignClient() {}

    /** The stand-in: it has no level, so it never counts as too far away from the player. */
    public static final class WireSign extends HangingSignBlockEntity {
        private WireSign(BlockPos pos, BlockState state) { super(pos, state); }

        @Override
        public boolean playerIsTooFarAwayToEdit(java.util.UUID uuid) { return false; }

        /** A sign block entity showing what is written on the item. */
        public static WireSign of(ItemStack stack, HolderLookup.Provider access, BlockPos pos) {
            BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState().setValue(CeilingHangingSignBlock.ATTACHED, true);
            WireSign sign = new WireSign(pos, state);
            sign.loadWithComponents(WireSigns.data(stack), access);
            return sign;
        }
    }

    private static final class Screen extends HangingSignEditScreen {
        private static final Field MESSAGES = messages();
        private final int wire;
        private final double t;
        private final boolean front;

        private Screen(WireSign sign, boolean front, int wire, double t) {
            super(sign, front, false);
            this.wire = wire;
            this.t = t;
            this.front = front;
        }

        private static Field messages() {
            try {
                Field field = AbstractSignEditScreen.class.getDeclaredField("messages");
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        /** Instead of the sign packet, which names a block, send the lines to the wire they are written on. */
        @Override
        public void removed() {
            try {
                PacketDistributor.sendToServer(new HangingSignTextPayload(wire, t, front, List.of((String[]) MESSAGES.get(this))));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static void open(OpenHangingSignPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.level.getEntity(payload.wire()) instanceof HangingWireEntity wire)) return;
        HangingWireEntity.Decoration decoration = wire.decorationAt(payload.t(), 0.6);
        if (decoration == null || !WireSigns.isSign(decoration.stack())) return;
        WireSign sign = WireSign.of(decoration.stack(), wire.registryAccess(), wire.blockPosition());
        // Typing sets the text on the sign, which tells its level; without one that fails.
        sign.setLevel(minecraft.level);
        minecraft.setScreen(new Screen(sign, payload.front(), payload.wire(), decoration.t()));
    }
}
