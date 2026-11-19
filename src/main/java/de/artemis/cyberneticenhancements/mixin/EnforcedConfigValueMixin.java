package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.EnforcedSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every client setting is read through ConfigValue.get(), so one hook here lets a server enforce any of them, including ones added
 * later, without a check at each use. The player's own config file is never touched.
 */
@Mixin(ModConfigSpec.ConfigValue.class)
abstract class EnforcedConfigValueMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$enforced(CallbackInfoReturnable ci) {
        Object forced = EnforcedSettings.forced((ModConfigSpec.ConfigValue<?>) (Object) this);
        if (forced != null) ci.setReturnValue(forced);
    }
}
