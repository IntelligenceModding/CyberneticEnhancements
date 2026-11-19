package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.TutorialClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * In the tutorial world every action is blocked unless the tutorial has introduced it, at the mapping itself, so vanilla
 * (advancements, chat, perspective) and other mods (maps, menus) are covered whether they poll, consume clicks or match key events.
 */
@Mixin(KeyMapping.class)
abstract class TutorialKeyBlockMixin {
    @Shadow private int clickCount;

    @Unique private static final IKeyConflictContext cyberneticenhancements$BLOCKED = new IKeyConflictContext() {
        @Override public boolean isActive() { return false; }
        @Override public boolean conflicts(IKeyConflictContext other) { return false; }
    };

    @Unique
    private boolean cyberneticenhancements$blocked() { return TutorialClient.blocks((KeyMapping) (Object) this); }

    @Unique
    private static boolean cyberneticenhancements$inGame() {
        var minecraft = Minecraft.getInstance();
        // Menus keep their own key handling: closing screens and the controls list must keep working.
        return minecraft != null && minecraft.screen == null;
    }

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$blockHeld(CallbackInfoReturnable<Boolean> ci) {
        if (cyberneticenhancements$blocked()) ci.setReturnValue(false);
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$blockClick(CallbackInfoReturnable<Boolean> ci) {
        if (!cyberneticenhancements$blocked()) return;
        clickCount = 0;
        ci.setReturnValue(false);
    }

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$blockKeyMatch(int keysym, int scancode, CallbackInfoReturnable<Boolean> ci) {
        if (cyberneticenhancements$inGame() && cyberneticenhancements$blocked()) ci.setReturnValue(false);
    }

    @Inject(method = "matchesMouse", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$blockMouseMatch(int key, CallbackInfoReturnable<Boolean> ci) {
        if (cyberneticenhancements$inGame() && cyberneticenhancements$blocked()) ci.setReturnValue(false);
    }

    /** Covers {@code isActiveAndMatches}, which is how many mods check their own keys in event handlers. */
    @Inject(method = "getKeyConflictContext", at = @At("HEAD"), cancellable = true, require = 0)
    private void cyberneticenhancements$blockContext(CallbackInfoReturnable<IKeyConflictContext> ci) {
        if (cyberneticenhancements$inGame() && cyberneticenhancements$blocked()) ci.setReturnValue(cyberneticenhancements$BLOCKED);
    }
}
