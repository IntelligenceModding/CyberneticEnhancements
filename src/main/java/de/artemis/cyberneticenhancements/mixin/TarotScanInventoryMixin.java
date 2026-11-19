package de.artemis.cyberneticenhancements.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.artemis.cyberneticenhancements.client.TarotScanClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
abstract class TarotScanInventoryMixin {
    @WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"))
    private boolean cyberneticenhancements$scanInsteadOfInventory(KeyMapping mapping, Operation<Boolean> original) {
        boolean clicked = original.call(mapping);
        return clicked && !TarotScanClient.capturesVanillaAction(mapping);
    }
}
