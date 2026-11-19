package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.CompassHeading;
import de.artemis.cyberneticenhancements.client.ui.CompassProjection;
import de.artemis.cyberneticenhancements.client.ui.CompassLandmark;
import de.artemis.cyberneticenhancements.client.ui.ResponsiveUi;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.CompassMarkerFocus;
import de.artemis.cyberneticenhancements.client.ui.ResponsiveContainerScreen;
import de.artemis.cyberneticenhancements.client.ui.GameplayHudStyle;
import de.artemis.cyberneticenhancements.common.quest.QuestItemLink;
import de.artemis.cyberneticenhancements.common.quest.QuestDimension;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import java.util.Optional;

public final class CompassHudOverlay {
    private static final String[] CARDINALS = {"N", "E", "S", "W"};
    private static int bossBottom;

    private CompassHudOverlay() {
    }

    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (!event.isCanceled()) bossBottom = Math.max(bossBottom, event.getY() + 5);
    }

    static void reserveBossSpace(int bottom) {
        bossBottom = Math.max(bossBottom, bottom);
    }

    static void render(GuiGraphics original, float partialTick) {
        int occupied = bossBottom;
        bossBottom = 0;
        var minecraft = Minecraft.getInstance();
        if (!visible()) return;
        int statusBottom = (int) Math.ceil(CyberwareHudOverlay.topStatusBottom()
                * ResponsiveUi.referenceViewport().renderScale());
        int y = Math.max(occupied, statusBottom) + ClientConfig.COMPASS_TOP_MARGIN.get();
        GameplayHudVisibility.renderToggleable(GameplayHudVisibility.Element.COMPASS, ClientConfig.SHOW_COMPASS.get(),
                original, () -> renderAt(original, original.guiWidth() / 2, y, partialTick));
    }

    public static void renderPreview(GuiGraphics graphics, int center, int top) {
        if (Minecraft.getInstance().player != null) renderAt(graphics, center, top, 1);
    }

    public static void renderInventory(GuiGraphics graphics, AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        var minecraft = Minecraft.getInstance();
        if (!visible()) return;
        int center = graphics.guiWidth() / 2, top = ClientConfig.COMPASS_TOP_MARGIN.get();
        var position = minecraft.player.position();
        var markers = CompassProjection.project(CompassPreferences.visible(), minecraft.level.dimension().location().toString(),
                position.x, position.y, position.z, minecraft.player.getViewYRot(1));
        var hovered = CompassMarkerFocus.hovered(markers, center, top, width(graphics), mouseX, mouseY, ClientConfig.COMPASS_EDGE_ARROWS.get());
        GameplayHudVisibility.renderToggleable(GameplayHudVisibility.Element.COMPASS, ClientConfig.SHOW_COMPASS.get(), graphics, () -> {
            renderAt(graphics, center, top, 1);
            if (hovered == null) return;
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 350);
            try {
                for (var slot : screen.getMenu().slots) {
                    if (!slot.isActive() || !matchesQuestItem(slot.getItem(), hovered.target().id())) continue;
                    int x = screen.getGuiLeft() + slot.x, y = screen.getGuiTop() + slot.y, size = 16;
                    if (screen instanceof ResponsiveContainerScreen<?> responsive) {
                        if (responsive.usesMinecraftStationScale() && responsive.isPlayerInventorySlot(slot)) {
                            x = responsive.nativeInventory().slotX(slot.x);
                            y = responsive.nativeInventory().slotY(slot.y);
                        } else {
                            double scale = responsive.viewport().renderScale();
                            x = screen.getGuiLeft() + (int) Math.round(slot.x * scale);
                            y = screen.getGuiTop() + (int) Math.round(slot.y * scale);
                            size = Math.max(1, (int) Math.round(16 * scale));
                        }
                    }
                    graphics.fill(x, y, x + size, y + size, 0x188BDAF8);
                    UiRenderer.outline(graphics, x - 1, y - 1, size + 2, size + 2, UiRenderer.ACCENT);
                }
            } finally { graphics.pose().popPose(); }
            graphics.renderTooltip(minecraft.font, Component.literal(hovered.target().title()), (int) mouseX, (int) mouseY);
        });
    }

    private static boolean matchesQuestItem(ItemStack stack, String questId) {
        var target = QuestNavigationClientState.target(questId);
        return target != null && QuestItemLink.matches(stack, questId, target.itemId());
    }

    public static void renderHotbarFocus(GuiGraphics graphics, float partialTick) {
        var minecraft = Minecraft.getInstance();
        if (!visible() || !GameplayHudVisibility.gameplay() || minecraft.player.isSpectator()
                || minecraft.getCameraEntity() != minecraft.player) return;
        var position = minecraft.player.getPosition(partialTick);
        var markers = CompassProjection.project(CompassPreferences.visible(), minecraft.level.dimension().location().toString(),
                position.x, position.y, position.z, minecraft.player.getViewYRot(partialTick));
        int focusAngle = ClientConfig.COMPASS_FOCUS_ANGLE.get();
        var focused = CompassMarkerFocus.focused(markers, focusAngle);
        if (focused == null || !CompassMarkerFocus.isQuest(focused.target())) return;
        float opacity = CompassMarkerFocus.captionAlpha(focused, focusAngle) / 235F * ClientConfig.COMPASS_OPACITY.get().floatValue();
        GameplayHudVisibility.renderToggleable(GameplayHudVisibility.Element.COMPASS, ClientConfig.SHOW_COMPASS.get(), graphics, () -> {
            int center = graphics.guiWidth() / 2, y = graphics.guiHeight() - 19;
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 350);
            try {
                for (int slot = 0; slot < 9; slot++) {
                    if (matchesQuestItem(minecraft.player.getInventory().getItem(slot), focused.target().id()))
                        highlightHotbarItem(graphics, center - 88 + slot * 20, y, opacity);
                }
                if (matchesQuestItem(minecraft.player.getOffhandItem(), focused.target().id())) {
                    int x = minecraft.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? center - 117 : center + 101;
                    highlightHotbarItem(graphics, x, y, opacity);
                }
            } finally { graphics.pose().popPose(); }
        });
    }

    private static boolean visible() {
        return GameplayHudVisibility.shows(GameplayHudVisibility.Element.COMPASS, ClientConfig.SHOW_COMPASS.get());
    }

    private static void highlightHotbarItem(GuiGraphics graphics, int x, int y, float opacity) {
        graphics.fill(x, y, x + 16, y + 16, GameplayHudStyle.color(UiRenderer.ACCENT, .1F * opacity));
        UiRenderer.outline(graphics, x - 1, y - 1, 18, 18, GameplayHudStyle.color(UiRenderer.ACCENT, opacity));
    }

    private static void renderAt(GuiGraphics original, int center, int y, float partialTick) {
        var minecraft = Minecraft.getInstance();
        float yaw = minecraft.player.getViewYRot(partialTick);
        float[] tint = RenderSystem.getShaderColor().clone();
        original.flush();
        RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3] * ClientConfig.COMPASS_OPACITY.get().floatValue());
        try {
            draw(original, center, y, yaw);
            drawLandmarks(original, center, y, yaw, partialTick);
        } finally {
            original.flush();
            RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);
        }
    }

    private static int width(GuiGraphics graphics) {
        return Math.max(64, Math.min(ClientConfig.COMPASS_WIDTH.get(), graphics.guiWidth() - 24));
    }

    private static void drawLandmarks(GuiGraphics graphics, int center, int y, float yaw, float partialTick) {
        var minecraft = Minecraft.getInstance();
        var targets = CompassPreferences.visible();
        if (targets.isEmpty()) return;
        int width = width(graphics);
        var position = minecraft.player.getPosition(partialTick);
        String dimension = minecraft.level.dimension().location().toString();
        var markers = CompassProjection.project(targets, dimension,
                position.x, position.y, position.z, yaw);
        var font = minecraft.font;
        for (int index = markers.size() - 1; index >= 0; index--) {
            var marker = markers.get(index);
            int x = CompassMarkerFocus.x(marker, center, width);
            int color = marker.target().color();
            boolean held = CompassMarkerFocus.isQuest(marker.target()) && (matchesQuestItem(minecraft.player.getMainHandItem(), marker.target().id())
                    || matchesQuestItem(minecraft.player.getOffhandItem(), marker.target().id()));
            if (Math.abs(marker.angle()) > 85) {
                if (!ClientConfig.COMPASS_EDGE_ARROWS.get()) continue;
                int direction = marker.angle() < 0 ? 1 : -1;
                for (int step = 0; step < (held ? 4 : 3); step++)
                    graphics.fill(x + direction * step, y + 13 - step, x + direction * step + 1, y + 14 + step, color);
            } else if (marker.target().kind() == CompassLandmark.Kind.BANNER) {
                renderBannerIcon(graphics, x - 4, y + 10, color);
            } else if (marker.target().kind() == CompassLandmark.Kind.CITY) {
                renderCityIcon(graphics, x - 4, y + 10, color);
            } else if (marker.target().kind() == CompassLandmark.Kind.MAX_TAC) {
                renderMaxTacIcon(graphics, x - 4, y + 10, color);
            } else if (marker.target().kind() == CompassLandmark.Kind.MEDTEC) {
                renderMedtecIcon(graphics, x - 4, y + 10, color);
            } else {
                int radius = held ? 3 : 2;
                for (int row = -radius; row <= radius; row++) {
                    int halfWidth = radius - Math.abs(row);
                    graphics.fill(x - halfWidth, y + 13 + row, x + halfWidth + 1, y + 14 + row, color);
                }
            }
            if (ClientConfig.COMPASS_HEIGHT_ARROWS.get() && Math.abs(marker.angle()) <= 85 && marker.verticalDistance() != 0)
                drawHeightArrow(graphics, x, y, marker.verticalDistance() > 0);
        }
        int focus = ClientConfig.COMPASS_FOCUS_ANGLE.get();
        var focused = CompassMarkerFocus.focused(markers, focus);
        if (ClientConfig.COMPASS_NAMES.get()) {
            var linkedTargets = targets.stream()
                    .filter(CompassMarkerFocus::isQuest)
                    .map(target -> QuestNavigationClientState.target(target.id())).filter(java.util.Objects::nonNull).toList();
            var linked = QuestItemLink.uniqueTarget(minecraft.player.getMainHandItem(), linkedTargets);
            if (linked == null) linked = QuestItemLink.uniqueTarget(minecraft.player.getOffhandItem(), linkedTargets);
            if (linked != null && !linked.dimension().equals(dimension)) {
                String label = Component.translatable("hud.cyberneticenhancements.quest_dimensions", QuestDimension.name(linked.dimension())).getString();
                int labelY = focused == null ? y + 23 : y + (ClientConfig.COMPASS_DISTANCES.get() ? 44 : 34);
                graphics.drawCenteredString(font, font.plainSubstrByWidth(label, width), center, labelY, UiRenderer.TEXT_SECONDARY);
            }
        }
        if (focused == null) return;
        int captionAlpha = CompassMarkerFocus.captionAlpha(focused, focus);
        if (ClientConfig.COMPASS_DISTANCES.get()) {
            String distance = Component.translatable("hud.cyberneticenhancements.quest_distance", focused.distance()).getString();
            int markerX = center + (int) Math.round(focused.angle() * width / 180.0);
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(markerX, y + 23, 0);
                graphics.pose().scale(0.75F, 0.75F, 1);
                graphics.drawString(font, distance, -font.width(distance) / 2, 0, captionAlpha << 24 | UiRenderer.TEXT & 0xFFFFFF, true);
            } finally {
                graphics.pose().popPose();
            }
        }
        if (!ClientConfig.COMPASS_NAMES.get()) return;
        String title = focused.target().kind() == CompassLandmark.Kind.TURN_IN ? Component.translatable("hud.cyberneticenhancements.quest_turn_in", focused.target().title()).getString()
                : focused.target().title();
        int available = Math.min(180, (int) (width / 0.8F));
        if (font.width(title) > available) title = font.plainSubstrByWidth(title, Math.max(1, available - font.width("…"))) + "…";
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(center, y + (ClientConfig.COMPASS_DISTANCES.get() ? 33 : 23), 0);
            graphics.pose().scale(0.8F, 0.8F, 1);
            graphics.drawString(font, title, -font.width(title) / 2, 0, captionAlpha << 24 | UiRenderer.TEXT & 0xFFFFFF, true);
        } finally {
            graphics.pose().popPose();
        }
    }

    public static void renderMedtecIcon(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 3, y + 1, x + 6, y + 8, color);
        graphics.fill(x + 1, y + 3, x + 8, y + 6, color);
    }

    public static void renderBannerIcon(GuiGraphics graphics, int x, int y, int color) {
        DyeColor dye = null;
        for (DyeColor candidate : DyeColor.values()) {
            if ((candidate.getTextColor() & 0xFFFFFF) == (color & 0xFFFFFF)) {
                dye = candidate;
                break;
            }
        }
        boolean custom = dye == null;
        var type = new MapBanner(BlockPos.ZERO, custom ? DyeColor.WHITE : dye, Optional.empty()).getDecoration();
        var decoration = new MapDecoration(type, (byte) 0, (byte) 0, (byte) 0, Optional.empty());
        var sprite = Minecraft.getInstance().getMapDecorationTextures().get(decoration);
        graphics.flush();
        graphics.blit(x, y, 0, 8, 8, sprite,
                custom ? (color >> 16 & 255) / 255.0F : 1,
                custom ? (color >> 8 & 255) / 255.0F : 1,
                custom ? (color & 255) / 255.0F : 1, 1);
    }

    public static void renderCityIcon(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 3, y + 1, x + 5, y + 2, color);
        graphics.fill(x + 2, y + 2, x + 6, y + 3, color);
        graphics.fill(x + 1, y + 3, x + 7, y + 4, color);
        graphics.fill(x + 2, y + 4, x + 6, y + 5, color);
        graphics.fill(x + 2, y + 5, x + 3, y + 7, color);
        graphics.fill(x + 5, y + 5, x + 6, y + 7, color);
    }

    public static void renderMaxTacIcon(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 1, y + 1, x + 7, y + 4, color);
        graphics.fill(x + 2, y + 4, x + 6, y + 6, color);
        graphics.fill(x + 3, y + 6, x + 5, y + 7, color);
    }

    private static void drawHeightArrow(GuiGraphics graphics, int x, int y, boolean above) {
        int top = y + (above ? 7 : 18);
        for (int row = 0; row < 3; row++) {
            int halfWidth = above ? row : 2 - row;
            graphics.fill(x - halfWidth, top + row, x + halfWidth + 1, top + row + 1, UiRenderer.TEXT_SECONDARY);
        }
    }

    private static void draw(GuiGraphics graphics, int center, int y, float yaw) {
        var font = Minecraft.getInstance().font;
        int width = width(graphics);
        int left = center - width / 2;
        for (int pixel = 0; pixel < width; pixel++) {
            double fade = fade(pixel - width / 2.0, width);
            graphics.fill(left + pixel, y + 13, left + pixel + 1, y + 14, alpha(UiRenderer.PANEL_EDGE_ACTIVE, fade * 0.55));
        }
        for (int bearing = 0; bearing < 360; bearing += 15) {
            double offset = CompassHeading.offsetDegrees(yaw, bearing) * width / 180.0;
            double fade = fade(offset, width);
            if (fade <= 0) continue;
            int x = center + (int) Math.round(offset);
            boolean cardinal = bearing % 90 == 0;
            if (ClientConfig.COMPASS_TICKS.get()) graphics.fill(x, y + (cardinal ? 10 : 12), x + 1, y + 14,
                    alpha(cardinal ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY, fade * (cardinal ? 0.7 : 0.3)));
            if (cardinal && ClientConfig.COMPASS_CARDINALS.get()) {
                String label = CARDINALS[bearing / 90];
                int textAlpha = (int) Math.round(fade * 235);
                if (textAlpha < 8) continue;
                int textX = x - font.width(label) / 2;
                int shadow = alpha(0, fade * 0.35);
                if ((shadow >>> 24) >= 8) graphics.drawString(font, label, textX + 1, y + 1, shadow, false);
                graphics.drawString(font, label, textX, y,
                        textAlpha << 24 | UiRenderer.TEXT & 0xFFFFFF, false);
            }
        }
        if (ClientConfig.COMPASS_CENTER.get()) {
            graphics.fill(center - 2, y + 17, center + 3, y + 18, alpha(UiRenderer.ACCENT, 0.85));
            graphics.fill(center - 1, y + 16, center + 2, y + 17, alpha(UiRenderer.ACCENT, 0.85));
            graphics.fill(center, y + 15, center + 1, y + 16, alpha(UiRenderer.ACCENT, 0.85));
        }
    }

    private static double fade(double offset, int width) {
        return Math.clamp((width / 2.0 - Math.abs(offset)) / 24.0, 0, 1);
    }

    private static int alpha(int color, double opacity) {
        return (int) Math.round(opacity * 255) << 24 | color & 0xFFFFFF;
    }
}
