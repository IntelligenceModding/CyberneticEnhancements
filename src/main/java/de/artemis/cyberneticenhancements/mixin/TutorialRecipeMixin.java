package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.common.tutorial.TutorialDirector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Recipe-book unlocks from pickups and crafting would pop up mid-tutorial; the tutorial explains recipes itself. */
@Mixin(ServerPlayer.class)
abstract class TutorialRecipeMixin {
    @Inject(method = "awardRecipes", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$noTutorialRecipeUnlocks(java.util.Collection<RecipeHolder<?>> recipes, CallbackInfoReturnable<Integer> ci) {
        if (TutorialDirector.isTutorialWorld(((ServerPlayer) (Object) this).server)) ci.setReturnValue(0);
    }
}
