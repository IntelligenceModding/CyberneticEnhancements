package de.artemis.cyberneticenhancements.mixin;

import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The tutorial's throwaway save is opened directly, so it never appears in the world list. */
@Mixin(LevelStorageSource.class)
abstract class TutorialWorldListMixin {
    @Inject(method = "findLevelCandidates", at = @At("RETURN"), cancellable = true)
    private void cyberneticenhancements$hideTutorialWorld(CallbackInfoReturnable<LevelStorageSource.LevelCandidates> ci) {
        var levels = ci.getReturnValue().levels();
        if (levels.stream().noneMatch(level -> de.artemis.cyberneticenhancements.client.TutorialWorld.isTutorialFolder(level.directoryName()))) return;
        ci.setReturnValue(new LevelStorageSource.LevelCandidates(levels.stream()
                .filter(level -> !de.artemis.cyberneticenhancements.client.TutorialWorld.isTutorialFolder(level.directoryName())).toList()));
    }
}
