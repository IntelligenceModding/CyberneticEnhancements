package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.common.tutorial.TutorialDirector;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No advancement progress in the tutorial world, so no popups, announcements or recipe unlocks interrupt it. */
@Mixin(PlayerAdvancements.class)
abstract class TutorialAdvancementMixin {
    @Shadow private ServerPlayer player;

    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$noTutorialAdvancements(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> ci) {
        if (player != null && TutorialDirector.isTutorialWorld(player.server)) ci.setReturnValue(false);
    }
}
