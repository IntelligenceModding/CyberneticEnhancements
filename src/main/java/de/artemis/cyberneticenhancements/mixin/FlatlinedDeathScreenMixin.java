package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation;
import de.artemis.cyberneticenhancements.client.ui.FlatlinedLayout;
import de.artemis.cyberneticenhancements.client.FlatlinedDeathCounter;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
abstract class FlatlinedDeathScreenMixin extends Screen {
    @Shadow @Final private boolean hardcore;
    @Shadow @Final private Component causeOfDeath;
    @Shadow private Component deathScore;
    @Shadow @Final private java.util.List<Button> exitButtons;
    @Shadow private int delayTicker;
    @Unique private Button cyberneticenhancements$callMedtec;
    @Unique private long cyberneticenhancements$openedAt;
    @Unique private SimpleSoundInstance cyberneticenhancements$flatlineSound;

    protected FlatlinedDeathScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void cyberneticenhancements$begin(CallbackInfo ci) {
        if (FlatlinedPresentation.enabled(this)) {
            var layout = FlatlinedLayout.of(width, height);
            for (int index = 0; index < exitButtons.size(); index++) {
                Button button = exitButtons.get(index);
                button.setX(layout.left());
                button.setY(layout.buttonTop() + index * 24);
                button.setWidth(layout.columnWidth());
            }
        }
        if (!exitButtons.isEmpty()) {
            Button respawn = exitButtons.getFirst();
            int top = Math.min(respawn.getY(), height - 80);
            respawn.setY(top);
            for (int index = 1; index < exitButtons.size(); index++) {
                Button button = exitButtons.get(index);
                button.setY(top + (index + 1) * 24);
            }
            cyberneticenhancements$callMedtec = addRenderableWidget(Button.builder(
                    Component.translatable("screen.cyberneticenhancements.call_medtec"), button -> {
                        button.active = false;
                        de.artemis.cyberneticenhancements.client.TraumaRescueClient.callMedtec();
                    }).bounds(respawn.getX(), respawn.getY() + 24, respawn.getWidth(), 20).build());
            cyberneticenhancements$callMedtec.active = false;
        }
        if (cyberneticenhancements$openedAt != 0) return;
        cyberneticenhancements$openedAt = Util.getMillis();
        de.artemis.cyberneticenhancements.client.FlatlinedDeathCamera.begin(this);
        if (FlatlinedPresentation.enabled(this)) {
            FlatlinedDeathCounter.request();
            cyberneticenhancements$flatlineSound = SimpleSoundInstance.forUI(ModSounds.FLATLINED.get(), 1.0F, 0.65F);
            minecraft.getSoundManager().play(cyberneticenhancements$flatlineSound);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void cyberneticenhancements$updateMedtecButton(CallbackInfo ci) {
        if (cyberneticenhancements$callMedtec != null)
            cyberneticenhancements$callMedtec.active = delayTicker >= 20
                    && de.artemis.cyberneticenhancements.client.TraumaRescueClient.canCallMedtec();
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void cyberneticenhancements$background(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!FlatlinedPresentation.enabled(this)) return;
        FlatlinedPresentation.background(graphics, width, height, (Util.getMillis() - cyberneticenhancements$openedAt) / 1000.0, hardcore);
        ci.cancel();
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), require = 0)
    private void cyberneticenhancements$textLayout(Args args) {
        if (!FlatlinedPresentation.enabled(this)) {
            Component text = args.get(1);
            if (de.artemis.cyberneticenhancements.client.TraumaRescueClient.rescueActive()
                    && (text == causeOfDeath || text == deathScore)) args.set(1, Component.empty());
            return;
        }
        var layout = FlatlinedLayout.of(width, height);
        Component component = args.get(1);
        if (component == title) {
            Component flatlined = de.artemis.cyberneticenhancements.client.TraumaRescueClient.deathScreenTitle();
            args.set(1, flatlined);
            args.set(2, layout.center() / 2);
            args.set(3, layout.titleY() / 2);
            args.set(4, de.artemis.cyberneticenhancements.client.TraumaRescueClient.deathScreenTitleColor());
        } else if (component == causeOfDeath) {
            if (de.artemis.cyberneticenhancements.client.TraumaRescueClient.rescueActive()) args.set(1, Component.empty());
            args.set(2, layout.causeCenter(font.width(component), width));
            args.set(3, layout.causeY());
        } else if (component == deathScore) {
            if (de.artemis.cyberneticenhancements.client.TraumaRescueClient.rescueActive()) args.set(1, Component.empty());
            args.set(2, layout.center());
            args.set(3, layout.scoreY());
        }
    }

    @ModifyConstant(method = {"render", "mouseClicked"}, constant = @Constant(intValue = 85), require = 0)
    private int cyberneticenhancements$causeTop(int original) {
        return FlatlinedPresentation.enabled(this) ? FlatlinedLayout.of(width, height).causeY() : original;
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 94), require = 0)
    private int cyberneticenhancements$causeBottom(int original) {
        return FlatlinedPresentation.enabled(this) ? FlatlinedLayout.of(width, height).causeY() + 9 : original;
    }

    @ModifyConstant(method = "mouseClicked", constant = @Constant(doubleValue = 85), require = 0)
    private double cyberneticenhancements$clickTop(double original) {
        return FlatlinedPresentation.enabled(this) ? FlatlinedLayout.of(width, height).causeY() : original;
    }

    @ModifyConstant(method = "mouseClicked", constant = @Constant(doubleValue = 94), require = 0)
    private double cyberneticenhancements$clickBottom(double original) {
        return FlatlinedPresentation.enabled(this) ? FlatlinedLayout.of(width, height).causeY() + 9 : original;
    }

    @ModifyVariable(method = "getClickedComponentStyleAt", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int cyberneticenhancements$causeStyle(int mouseX) {
        return FlatlinedPresentation.enabled(this) && causeOfDeath != null
                ? mouseX + width / 2 - FlatlinedLayout.of(width, height).causeCenter(font.width(causeOfDeath), width) : mouseX;
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void cyberneticenhancements$glitch(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (FlatlinedPresentation.enabled(this)) {
            FlatlinedPresentation.titleGlitch(graphics, width, height, (Util.getMillis() - cyberneticenhancements$openedAt) / 1000.0);
        } else if (cyberneticenhancements$flatlineSound != null) {
            minecraft.getSoundManager().stop(cyberneticenhancements$flatlineSound);
            cyberneticenhancements$flatlineSound = null;
        }
    }

    @Override
    public void removed() {
        if (cyberneticenhancements$flatlineSound != null) {
            minecraft.getSoundManager().stop(cyberneticenhancements$flatlineSound);
            cyberneticenhancements$flatlineSound = null;
        }
        super.removed();
    }
}
