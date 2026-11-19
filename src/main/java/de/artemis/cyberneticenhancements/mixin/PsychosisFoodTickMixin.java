package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
abstract class PsychosisFoodTickMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void psychosis$pauseHunger(Player player,CallbackInfo ci){
        if(CyberstrainManager.ignoresHunger(player))ci.cancel();
    }
}
