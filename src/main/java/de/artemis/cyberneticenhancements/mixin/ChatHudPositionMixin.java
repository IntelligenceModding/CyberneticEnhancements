package de.artemis.cyberneticenhancements.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.artemis.cyberneticenhancements.client.CyberwareHudOverlay;
import de.artemis.cyberneticenhancements.client.GameplayHudVisibility;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatComponent.class)
abstract class ChatHudPositionMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$hideFocusedChat(GuiGraphics graphics, int tickCount, int mouseX, int mouseY,
                                                       boolean focused, CallbackInfo ci) {
        if (focused && !GameplayHudVisibility.shows(GameplayHudVisibility.Element.CHAT, true)) ci.cancel();
    }

    @Inject(method = {"getClickedComponentStyleAt", "getMessageTagAt"}, at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$hideChatLinks(double x, double y, CallbackInfoReturnable<Object> callback) {
        if (!GameplayHudVisibility.shows(GameplayHudVisibility.Element.CHAT, true)) callback.setReturnValue(null);
    }

    @Inject(method = "handleChatQueueClicked", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$hideChatQueue(double x, double y, CallbackInfoReturnable<Boolean> callback) {
        if (!GameplayHudVisibility.shows(GameplayHudVisibility.Element.CHAT, true)) callback.setReturnValue(false);
    }
    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;guiHeight()I"))
    private int cyberneticenhancements$raiseChat(int height) {
        return height - CyberwareHudOverlay.chatOffset();
    }

    @ModifyExpressionValue(method = {"screenToChatY", "handleChatQueueClicked"},
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
    private int cyberneticenhancements$raiseChatHitTargets(int height) {
        return height - CyberwareHudOverlay.chatOffset();
    }
}
