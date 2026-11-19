package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.render.FixerEntityRenderer;
import de.artemis.cyberneticenhancements.client.ui.QuickCallContacts;
import de.artemis.cyberneticenhancements.client.ui.NeuralLinkStyle;
import de.artemis.cyberneticenhancements.client.ui.GameplayHudStyle;
import de.artemis.cyberneticenhancements.client.ui.UiFadeLayer;
import de.artemis.cyberneticenhancements.client.ui.UiGraphics;
import de.artemis.cyberneticenhancements.client.ui.UiOpeningAnimation;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload.ContactEntry;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactsRequestPayload;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

public final class NpcContactsOverlay {
    private static final String PREFIX = "screen.cyberneticenhancements.quick_contacts.";
    private static final int WIDTH = NeuralLinkStyle.WIDTH;
    private static final int ROW_HEIGHT = 48;
    private static NpcContactsOverlay active;
    private static boolean openDown;
    private final long openedAt = System.nanoTime();
    private final UiOpeningAnimation opening = new UiOpeningAnimation(openedAt, 1, NeuralLinkStyle.OPEN_MILLIS);
    private final UiFadeLayer fade = new UiFadeLayer();
    private final boolean[] answerDown = new boolean[5];
    private UiOpeningAnimation closing;
    private long closingAt;
    private ArchiveContactsPayload snapshot;
    private List<ContactEntry> contacts = List.of();
    private int page;
    private long nextRefresh;

    private NpcContactsOverlay() {
        for (int index = 0; index < answerDown.length; index++) {
            var key = ModKeyMappings.CALL_ANSWERS.get(index);
            while (key.consumeClick()) {}
            answerDown[index] = key.isDown();
        }
        while (ModKeyMappings.END_CALL.consumeClick()) {}
        refresh();
        PacketDistributor.sendToServer(new ArchiveContactsRequestPayload());
    }

    public static boolean isActive() {
        return active != null && active.closing == null;
    }
    public static boolean isVisible() { return active != null; }

    public static void toggle() {
        var minecraft = Minecraft.getInstance();
        if (!GameplayHudVisibility.gameplay() || !GameplayHudVisibility.shows(GameplayHudVisibility.Element.CONTACTS, true)
                || NpcCallOverlay.hasLiveCall()) return;
        if (NpcCallOverlay.isActive()) { NpcCallOverlay.close(); NpcCallOverlay.reset(); }
        if (isActive()) close();
        else {
            reset();
            active = new NpcContactsOverlay();
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.OPEN_CONTACTS.get(), 1.0F, 1.0F));
        }
    }

    public static void tick() {
        boolean openClicked = ModKeyMappings.OPEN_CONTACTS.consumeClick();
        while (ModKeyMappings.OPEN_CONTACTS.consumeClick()) {}
        if (openClicked && !openDown && TutorialClient.shows("contacts")) toggle();
        openDown = ModKeyMappings.OPEN_CONTACTS.isDown();
        if (active == null) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !minecraft.player.isAlive() || NpcCallOverlay.isActive()) {
            reset();
            return;
        }
        if (active.closing != null) {
            if (active.closing.sample(System.nanoTime()).complete()) reset();
            return;
        }
        active.refresh();
        if (System.nanoTime() > active.nextRefresh) {
            PacketDistributor.sendToServer(new ArchiveContactsRequestPayload());
            active.nextRefresh = System.nanoTime() + 5_000_000_000L;
        }
        if (!GameplayHudVisibility.gameplay() || !GameplayHudVisibility.shows(GameplayHudVisibility.Element.CONTACTS, true)) return;
        int action = -1;
        for (int index = 0; index < active.answerDown.length; index++) {
            var key = ModKeyMappings.CALL_ANSWERS.get(index);
            boolean clicked = key.consumeClick();
            while (key.consumeClick()) {}
            if (clicked && !active.answerDown[index] && action == -1) action = index;
            active.answerDown[index] = key.isDown();
        }
        boolean cancel = false;
        while (ModKeyMappings.END_CALL.consumeClick()) cancel = true;
        if (cancel || action == 4) close();
        else if (action == 3 && QuickCallContacts.pageCount(active.contacts.size()) > 1) {
            active.page = (active.page + 1) % QuickCallContacts.pageCount(active.contacts.size());
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.1F, 0.12F));
        } else if (action >= 0 && action < QuickCallContacts.PAGE_SIZE) {
            var visible = QuickCallContacts.page(active.contacts, active.page);
            if (action < visible.size()) {
                ContactEntry contact = visible.get(action);
                reset();
                NpcCallOverlay.start(contact);
            }
        }
    }

    private void refresh() {
        if (snapshot == ArchiveContactsClientState.get()) return;
        snapshot = ArchiveContactsClientState.get();
        contacts = QuickCallContacts.available(snapshot.contacts());
        page = Math.min(page, QuickCallContacts.pageCount(contacts.size()) - 1);
    }

    public static void render(GuiGraphics original) {
        var minecraft = Minecraft.getInstance();
        if (active == null || minecraft.player == null || minecraft.level == null || minecraft.options.hideGui
                || NpcCallOverlay.isActive()) return;
        var picker = active;
        var viewport = NeuralLinkStyle.viewport();
        long now = System.nanoTime();
        var entry = picker.opening.sample(picker.closing == null ? now : picker.closingAt);
        float exit = picker.closing == null ? 0 : picker.closing.sample(now).wikiOpacity();
        float opacity = entry.wikiOpacity() * (1 - exit);
        if (opacity <= 0) return;
        int x = NeuralLinkStyle.x(viewport, ClientConfig.CALL_WINDOW_ON_LEFT.get());
        float scale = (float) viewport.renderScale();
        picker.fade.render(original, opacity, entry.wikiScale() * (1 - 0.04F * exit),
                (entry.wikiOffsetY() - 10 * exit) * scale, (x + WIDTH / 2.0F) * scale, NeuralLinkStyle.MARGIN * scale, () -> {
                    var graphics = new UiGraphics(original, viewport);
                    try { picker.draw(graphics, x, NeuralLinkStyle.MARGIN); }
                    finally { graphics.flush(); }
                });
    }

    private void draw(GuiGraphics graphics, int x, int y) {
        var font = Minecraft.getInstance().font;
        var visible = QuickCallContacts.page(contacts, page);
        int top = y + NeuralLinkStyle.CONTENT_TOP;
        int inset = NeuralLinkStyle.INSET;
        int gap = NeuralLinkStyle.GAP;
        int rowsHeight = visible.isEmpty() ? 64 : visible.size() * (ROW_HEIGHT + gap) - gap;
        int footerY = top + rowsHeight + gap;
        NeuralLinkStyle.panel(graphics, x, y, footerY - y + 40 + gap + inset, UiRenderer.ACCENT);
        Component count = Component.literal((page + 1) + " / " + QuickCallContacts.pageCount(contacts.size()));
        NeuralLinkStyle.header(graphics, x, y, Component.translatable(PREFIX + "title"), count, UiRenderer.ACCENT);
        if (visible.isEmpty()) {
            GameplayHudStyle.surface(graphics, x + inset, top, WIDTH - inset * 2, rowsHeight);
            Component text = Component.translatable(PREFIX + (ArchiveContactsClientState.isLoaded() ? "empty"
                    : System.nanoTime() - openedAt > 10_000_000_000L ? "unavailable" : "loading"));
            var lines = font.split(text, WIDTH - 24);
            int lineY = top + (rowsHeight - lines.size() * 11) / 2;
            for (var line : lines) {
                graphics.drawString(font, line, x + 12, lineY, UiRenderer.TEXT_SECONDARY, false);
                lineY += 11;
            }
        }
        for (int index = 0; index < visible.size(); index++) {
            ContactEntry contact = visible.get(index);
            int rowY = top + index * (ROW_HEIGHT + gap);
            GameplayHudStyle.option(graphics, x + inset, rowY, WIDTH - inset * 2, ROW_HEIGHT, true);
            GameplayHudStyle.surface(graphics, x + 10, rowY + 6, 36, 36);
            ResourceLocation texture = ResourceLocation.tryParse(contact.appearance());
            if (contact.isPlayer()) texture = PlayerCallClient.skin(UUID.fromString(contact.npcId()));
            else if (texture == null || Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty())
                texture = FixerEntityRenderer.resolveTexture(UUID.fromString(contact.npcId()), NpcCategory.fromId(contact.categoryId()));
            PlayerFaceRenderer.draw(graphics, texture, x + 12, rowY + 8, 32);
            graphics.fill(x + 12, rowY + 8, x + 44, rowY + 40, 0x0839C6DE);
            for (int line = rowY + 9; line < rowY + 40; line += 3)
                graphics.fill(x + 12, line, x + 44, line + 1, 0x100B1721);
            int sweep = rowY + 8 + (int) (((System.nanoTime() - openedAt) / 1_000_000L % 2400) * 31 / 2400);
            graphics.fill(x + 12, sweep, x + 44, sweep + 1, 0x328BDAF8);
            GameplayHudStyle.separator(graphics, x + 10, rowY + 41, 36);
            int textX = x + 52, textWidth = WIDTH - 64;
            graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(contact.displayName(), textWidth)), textX, rowY + 5, UiRenderer.TEXT, false);
            graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(Component.translatable(contact.isPlayer()
                            ? "screen.cyberneticenhancements.call.player." + contact.callStatus() : "entity.cyberneticenhancements." + contact.npcTypeId()), textWidth)),
                    textX, rowY + 17, UiRenderer.TEXT_SECONDARY, false);
            graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(Component.translatable(PREFIX + "call",
                    ModKeyMappings.CALL_ANSWERS.get(index).getTranslatedKeyMessage()), textWidth)), textX, rowY + 32, UiRenderer.ACCENT, false);
        }
        footer(graphics, x + inset, footerY, 3, "next", QuickCallContacts.pageCount(contacts.size()) > 1);
        footer(graphics, x + inset, footerY + 20 + gap, 4, "close", true);
    }

    private static void footer(GuiGraphics graphics, int x, int y, int index, String key, boolean enabled) {
        GameplayHudStyle.shortcut(graphics, Minecraft.getInstance().font, x, y, WIDTH - NeuralLinkStyle.INSET * 2,
                ModKeyMappings.CALL_ANSWERS.get(index).getTranslatedKeyMessage(), Component.translatable(PREFIX + key),
                Component.empty(), enabled, 1);
    }

    private static void close() {
        if (!isActive()) return;
        active.closingAt = System.nanoTime();
        active.closing = new UiOpeningAnimation(active.closingAt, 1, NeuralLinkStyle.CLOSE_MILLIS);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.CLOSE_CONTACTS.get(), 1.0F, 1.0F));
    }

    public static void reset() {
        if (active != null) {
            active.fade.close();
            active = null;
        }
    }

}
