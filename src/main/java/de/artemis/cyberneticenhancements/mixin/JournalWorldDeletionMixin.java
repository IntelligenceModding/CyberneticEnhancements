package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.JournalHistorySync;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.nio.file.Path;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
abstract class JournalWorldDeletionMixin {
    @Shadow public abstract Path getLevelPath(LevelResource resource);

    @Inject(method = "deleteLevel", at = @At("RETURN"))
    private void cyberneticenhancements$deleteJournal(CallbackInfo ci) {
        JournalHistorySync.worldDeleted(getLevelPath(LevelResource.ROOT));
    }
}
