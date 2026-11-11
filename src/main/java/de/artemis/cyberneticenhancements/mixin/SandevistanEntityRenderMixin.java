package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanTimeDilation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LevelRenderer.class)
abstract class SandevistanEntityRenderMixin {
    @ModifyArgs(method = "renderEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void cyberneticenhancements$continuousMotion(Args args) {
        Entity entity = args.get(0);
        float original = args.get(5);
        float partialTick = SandevistanTimeDilation.renderPartial(entity, original);
        if (partialTick == original) return;
        args.set(1, (double) args.get(1) + (partialTick - original) * (entity.getX() - entity.xOld));
        args.set(2, (double) args.get(2) + (partialTick - original) * (entity.getY() - entity.yOld));
        args.set(3, (double) args.get(3) + (partialTick - original) * (entity.getZ() - entity.zOld));
        args.set(4, Mth.lerp(partialTick, entity.yRotO, entity.getYRot()));
        args.set(5, partialTick);
    }
}
