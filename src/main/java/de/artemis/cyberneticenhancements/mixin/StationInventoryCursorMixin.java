package de.artemis.cyberneticenhancements.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.artemis.cyberneticenhancements.client.ui.ResponsiveContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Keep vanilla's carried-item and touchscreen return rendering at native inventory scale. */
@Mixin(AbstractContainerScreen.class)
abstract class StationInventoryCursorMixin {
    @Shadow private ItemStack snapbackItem;
    @Shadow private ItemStack draggingItem;
    @Shadow private Slot snapbackEnd;
    @Shadow private int snapbackStartX;
    @Shadow private int snapbackStartY;
    @Shadow private long snapbackTime;

    @WrapMethod(method = "renderFloatingItem")
    private void cyberneticenhancements$nativeInventoryCursor(GuiGraphics g, ItemStack stack, int x, int y,
            String text, Operation<Void> original) {
        if (!((Object) this instanceof ResponsiveContainerScreen<?> screen) || !screen.usesMinecraftStationScale()) {
            original.call(g, stack, x, y, text); return;
        }
        int nativeX = screen.nativeFloatingX(x), nativeY = screen.nativeFloatingY(y);
        if (stack == snapbackItem && snapbackEnd != null) {
            float progress = Math.clamp((net.minecraft.Util.getMillis() - snapbackTime) / 100F, 0, 1);
            int endX = screen.isPlayerInventorySlot(snapbackEnd) ? screen.nativeInventory().slotX(snapbackEnd.x) : screen.nativeFloatingX(snapbackEnd.x);
            int endY = screen.isPlayerInventorySlot(snapbackEnd) ? screen.nativeInventory().slotY(snapbackEnd.y) : screen.nativeFloatingY(snapbackEnd.y);
            nativeX = Math.round(net.minecraft.util.Mth.lerp(progress, screen.nativeFloatingX(snapbackStartX), endX));
            nativeY = Math.round(net.minecraft.util.Mth.lerp(progress, screen.nativeFloatingY(snapbackStartY), endY));
        } else if (!draggingItem.isEmpty()) nativeY -= Math.round(8 * (1 - screen.viewport().renderScale()));
        g.flush();
        GuiGraphics nativeGui = new GuiGraphics(Minecraft.getInstance(), g.bufferSource());
        original.call(nativeGui, stack, nativeX, nativeY, text);
        nativeGui.flush();
    }
}
