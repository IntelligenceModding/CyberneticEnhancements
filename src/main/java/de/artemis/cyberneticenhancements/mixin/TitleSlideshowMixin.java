package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.TitleSlideshow;
import de.artemis.cyberneticenhancements.client.TitleStartPrompt;
import de.artemis.cyberneticenhancements.client.TutorialProgress;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
abstract class TitleSlideshowMixin extends Screen implements de.artemis.cyberneticenhancements.client.TitleFadeAccess {
    @Shadow private boolean fading;
    protected TitleSlideshowMixin(Component title) { super(title); }

    @Override public void cyberneticenhancements$skipFade() { fading = false; }

    @Inject(method = "preloadResources", at = @At("RETURN"), cancellable = true)
    private static void cyberneticenhancements$preloadPresentation(
            net.minecraft.client.renderer.texture.TextureManager textures, java.util.concurrent.Executor executor,
            CallbackInfoReturnable<java.util.concurrent.CompletableFuture<Void>> ci) {
        var original = ci.getReturnValue();
        var intro = de.artemis.cyberneticenhancements.client.ui.LaunchIntroPresentation.preload(textures, executor);
        // The slideshow has its own reload listener, including initial startup.
        ci.setReturnValue(java.util.concurrent.CompletableFuture.allOf(original, intro));
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void cyberneticenhancements$prepareSlideshow(CallbackInfo ci) {
        if (((Object) this).getClass() != TitleScreen.class) return;
        TitleSlideshow.prepare();
        int journalX = width / 2 - 100, journalY = height / 4 + 80, journalWidth = 200;
        // Share the existing Mods row, so adding an action does not push the
        // menu into the copyright footer at high GUI scales.
        for (var child : children()) {
            if (child instanceof net.neoforged.neoforge.client.gui.widget.ModsButton mods) {
                mods.setWidth(98);
                journalX = width / 2 + 2;
                journalY = mods.getY();
                journalWidth = 98;
                break;
            }
        }
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.translatable("screen.cyberneticenhancements.title.journal"),
                button -> minecraft.setScreen(new de.artemis.cyberneticenhancements.client.screen.TitleJournalScreen(this)))
                .bounds(journalX, journalY, journalWidth, 20).build());
        // Share the Realms row the same way. Realms keeps the right half, where its
        // notification icons are drawn against the row's right edge.
        net.minecraft.client.gui.components.Button realms = null;
        for (var child : children()) {
            if (child instanceof net.minecraft.client.gui.components.Button button
                    && button.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents key
                    && key.getKey().equals("menu.online")) {
                realms = button;
                break;
            }
        }
        if (realms != null) {
            realms.setWidth(98);
            realms.setX(width / 2 + 2);
            var tutorial = net.minecraft.client.gui.components.Button.builder(
                    Component.translatable("screen.cyberneticenhancements.title.tutorial.play"), button -> de.artemis.cyberneticenhancements.client.TutorialWorld.play(this))
                    .bounds(width / 2 - 100, realms.getY(), 98, 20).build();
            if (!TutorialProgress.AVAILABLE) {
                tutorial.active = false;
                tutorial.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.cyberneticenhancements.title.tutorial.soon")));
            }
            addRenderableWidget(tutorial);
        }
        Component credit = Component.translatableWithFallback("screen.cyberneticenhancements.title.artwork_credit",
                "Cyberpunk 2077 artwork \u00A9 CD PROJEKT S.A. All rights reserved.");
        int creditWidth = font.width(credit);
        addRenderableWidget(new PlainTextButton(
                width - creditWidth - 2, height - 22, creditWidth, 10, credit,
                button -> net.minecraft.Util.getPlatform().openUri("https://www.cyberpunk.net/us/en/"), font));
        Component notices = Component.translatable("screen.cyberneticenhancements.title.notices");
        int noticesWidth = font.width(notices);
        addRenderableWidget(new PlainTextButton(width - noticesWidth - 2, height - 34, noticesWidth, 10, notices,
                button -> minecraft.setScreen(new de.artemis.cyberneticenhancements.client.screen.IntroNoticesScreen(this)), font));
        boolean changelogDue = de.artemis.cyberneticenhancements.client.ChangelogPopup.due();
        if (!TitleStartPrompt.hidesMenu() && !changelogDue
                && de.artemis.cyberneticenhancements.client.screen.ChangelogScreen.available()) {
            // Bottom left, directly above the two version lines the loader prints there.
            Component whatsNew = Component.translatable("screen.cyberneticenhancements.title.changelog");
            addRenderableWidget(new PlainTextButton(2, height - 32, font.width(whatsNew), 10, whatsNew,
                    button -> minecraft.setScreen(new de.artemis.cyberneticenhancements.client.screen.ChangelogScreen(this, true)), font));
        }
        if (!TitleStartPrompt.hidesMenu() && !changelogDue) return;
        // The tutorial offer replaces every menu action until it is answered; footer links stay.
        for (var child : java.util.List.copyOf(children()))
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget && !(child instanceof PlainTextButton)) removeWidget(child);
        // The changelog opens over the title before any button is shown; closing it builds the menu again.
        if (changelogDue) return;
        var offer = TitleStartPrompt.offerLayout(width, height);
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.translatable("screen.cyberneticenhancements.title.tutorial.play"), button -> {
                    TitleStartPrompt.settle();
                    de.artemis.cyberneticenhancements.client.TutorialWorld.play(this);
                })
                .bounds(width / 2 - 100, offer.buttonsY(), 200, 20).build());
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("screen.cyberneticenhancements.title.tutorial.decline"), button -> {
            TutorialProgress.set(TutorialProgress.State.SKIPPED);
            TitleStartPrompt.leave();
        }).bounds(width / 2 - 100, offer.buttonsY() + 24, 200, 20).build());
    }

    @Inject(method = "realmsNotificationsEnabled", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$hideRealmsDuringStart(CallbackInfoReturnable<Boolean> ci) {
        if (((Object) this).getClass() == TitleScreen.class && TitleStartPrompt.hidesMenu()) ci.setReturnValue(false);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void cyberneticenhancements$renderStartScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (((Object) this).getClass() == TitleScreen.class) TitleStartPrompt.renderBehindMenu(graphics, this, fading);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$coverLoadingHandoff(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (de.artemis.cyberneticenhancements.client.LaunchIntro.coverLoadingHandoff(this, graphics)) {
            ci.cancel();
            return;
        }
    }

    @ModifyArgs(method = "*", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"))
    private void cyberneticenhancements$footerColor(Args args) {
        args.set(4, de.artemis.cyberneticenhancements.client.ui.TitleScreenPresentation.footerTextColor(args.get(4)));
    }
}
