package de.artemis.cyberneticenhancements.client;

import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.cyberneticenhancements.client.ui.UiViewport;
import de.artemis.cyberneticenhancements.client.ui.HudLayout;
import de.artemis.cyberneticenhancements.client.ui.UiGraphics;
import de.artemis.cyberneticenhancements.client.ui.ResponsiveUi;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.screen.StationScreenStyle;
import de.artemis.cyberneticenhancements.client.tooltip.ModTooltipStyle;
import de.artemis.cyberneticenhancements.common.cyberware.CombatStatusManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffect;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffectType;
import de.artemis.cyberneticenhancements.common.network.CyberwareHudPayload;
import de.artemis.cyberneticenhancements.common.network.PsychosisOverlayPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

final class CyberwareHudOverlay {
    private static final int TOP_MARGIN = 6;
    private static final int CARD_GAP = 4;
    private static final int CARD_PADDING = 4;
    private static final int CARD_HEADER_HEIGHT = 15;
    private static final int ROW_HEIGHT = 13;
    private static final int ROW_GAP = 2;
    private static final int BAR_HEIGHT = 3;
    private static final int BAR_TEXT_GAP = 1;

    private static final int TOP_CARD_MIN_WIDTH = 180;

    private static final int TEXT_PRIMARY = StationScreenStyle.TEXT_PRIMARY;
    private static final int TEXT_SECONDARY = StationScreenStyle.TEXT_SECONDARY;

    private static final int BAR_BACKGROUND = StationScreenStyle.PANEL_DEEP;
    private static final int BAR_IDLE = StationScreenStyle.SECTION_HIGHLIGHT;
    private static final int BAR_CHROME = 0xFF1EE2B5;
    private static final int BAR_CHROME_IDLE = StationScreenStyle.SECTION_SHADOW;
    private static final int BAR_STRAIN_LOW = 0xFF4E9FFF;
    private static final int BAR_STRAIN_MEDIUM = 0xFFFFAE42;
    private static final int BAR_STRAIN_HIGH = 0xFFFF5C5C;
    private static final int BAR_ABILITY_ACTIVE = 0xFF54E7FF;
    private static final int BAR_ABILITY_READY = 0xFF38D9B2;
    private static final int BAR_ABILITY_COOLDOWN = 0xFF3E718C;
    private static final int BAR_STATUS_MEDICAL = 0xFF5AE58A;
    private static final int BAR_STATUS_NEURAL = 0xFF49C8FF;
    private static final int BAR_STATUS_SUPPRESSANT = 0xFF8CB3FF;
    private static final int BAR_STATUS_STREET = 0xFFFF6B5E;
    private static final int BAR_STATUS_SYSTEM = 0xFFF3D45A;
    private static final int BAR_STATUS_BIOMONITOR = 0xFF64F0A4;
    private static final int BAR_STATUS_BLOOD_PUMP = 0xFFFF4F4F;
    private static final int BAR_STATUS_SECOND_HEART = 0xFFFF7CA8;
    private static final int BAR_STATUS_REFLEX = 0xFFF6D55F;
    private static final int BAR_STATUS_SHOCK = 0xFF57D2FF;
    private static final int BAR_STATUS_OVERHEAT = 0xFFFF8452;
    private static final int BAR_STATUS_CORROSION = 0xFF89D45B;
    private static final int BAR_STATUS_TRAUMA = 0xFFFF9B7A;
    private static final int BAR_STATUS_BLEED = 0xFFE05A6F;
    private static final int BAR_STATUS_MARK = 0xFFF0E36B;
    private static final int BAR_PSYCHOSIS_MAJOR = 0xFFFF5A5A;
    private static final int BAR_PSYCHOSIS_MINOR = 0xFFFFA347;
    private static final int BAR_PSYCHOSIS_IDLE = 0xFF3A2020;
    private static final long ROW_TRANSITION_MILLIS = 220L;
    private static final RowTransition TOP_ROW_TRANSITION = new RowTransition();
    private static final RowTransition LEFT_ROW_TRANSITION = new RowTransition();
    private static final RowTransition RIGHT_ROW_TRANSITION = new RowTransition();

    private CyberwareHudOverlay() {
    }

    static void render(GuiGraphics guiGraphics) {
        UiViewport viewport = ResponsiveUi.referenceViewport();
        guiGraphics.flush();
        GuiGraphics ui = new UiGraphics(guiGraphics, viewport);
        try {
            renderContent(ui);
        } finally {
            ui.flush();
        }
    }

    private static void renderContent(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen instanceof ResponsiveUi) {
            return;
        }

        Font font = minecraft.font;
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        List<HoverRegion> hoverRegions = new ArrayList<>();
        PsychosisOverlayPayload psychosisSnapshot = PsychosisOverlayClientState.get();
        if (!psychosisSnapshot.entries().isEmpty()) {
            renderAnimatedTopRow(
                    guiGraphics,
                    font,
                    hoverRegions,
                    TOP_ROW_TRANSITION,
                    buildPsychosisCards(psychosisSnapshot, width),
                    width / 2,
                    TOP_MARGIN
            );
        } else {
            renderAnimatedTopRow(guiGraphics, font, hoverRegions, TOP_ROW_TRANSITION, List.of(), width / 2, TOP_MARGIN);
        }

        CyberwareHudPayload snapshot = CyberwareHudClientState.get();
        if (snapshot == null) {
            return;
        }

        Vec2 mouse = scaledMousePosition(minecraft, width, height);
        int mouseX = (int) mouse.x;
        int mouseY = (int) mouse.y;
        int leftCardCount = snapshot.abilities().isEmpty() ? 1 : 2;
        int rightCardCount = countVisibleRightCards(snapshot);
        int heartRows = (int) Math.ceil((minecraft.player.getMaxHealth() + minecraft.player.getAbsorptionAmount()) / 20.0);
        int vanillaHeight = 80 + Math.max(0, heartRows - 2) * Math.max(3, 10 - Math.max(0, heartRows - 2));
        HudLayout layout = HudLayout.fit(ResponsiveUi.referenceViewport(), leftCardCount, rightCardCount, vanillaHeight);
        int leftBandWidth = layout.left().width();
        int rightBandWidth = layout.right().width();
        int leftRowCardWidth = Math.max(1, (leftBandWidth - CARD_GAP * (layout.left().columns() - 1)) / layout.left().columns());
        List<CardLayout> leftCards = new ArrayList<>(2);
        leftCards.add(buildSystemCard(snapshot, leftRowCardWidth));
        if (!snapshot.abilities().isEmpty()) {
            CardLayout leftAbilityCard = buildAbilityCard(snapshot, leftRowCardWidth);
            if (leftAbilityCard != null) {
                leftCards.add(leftAbilityCard);
            }
        }
        renderHudBand(guiGraphics, font, hoverRegions, LEFT_ROW_TRANSITION, leftCards, layout.left(), false);

        List<CardLayout> rightCards = List.of();
        if (rightCardCount > 0) {
            int rowCardWidth = Math.max(1, (rightBandWidth - CARD_GAP * (layout.right().columns() - 1)) / layout.right().columns());
            rightCards = buildBottomAnchoredRightCards(snapshot, rowCardWidth);
        }
        renderHudBand(guiGraphics, font, hoverRegions, RIGHT_ROW_TRANSITION, rightCards, layout.right(), true);

        for (HoverRegion hoverRegion : hoverRegions) {
            if (hoverRegion.contains(mouseX, mouseY)) {
                renderHoverTooltip(guiGraphics, font, width, height, hoverRegion.tooltip(), mouseX, mouseY);
                break;
            }
        }
    }

    private static void renderHudBand(GuiGraphics graphics, Font font, List<HoverRegion> hoverRegions,
                                      RowTransition transition, List<CardLayout> cards, HudLayout.Band band, boolean alignRight) {
        int firstRegion = hoverRegions.size();
        graphics.enableScissor(band.x(), TOP_MARGIN, band.x() + band.width(), band.bottom());
        renderAnimatedRow(graphics, font, hoverRegions, transition, cards,
                alignRight ? band.x() + band.width() : band.x(), band.bottom(), alignRight, band.columns() == 1);
        graphics.disableScissor();
        for (int index = hoverRegions.size() - 1; index >= firstRegion; index--) {
            HoverRegion region = hoverRegions.get(index);
            int x1 = Math.max(band.x(), region.x1());
            int y1 = Math.max(TOP_MARGIN, region.y1());
            int x2 = Math.min(band.x() + band.width(), region.x2());
            int y2 = Math.min(band.bottom(), region.y2());
            if (x1 >= x2 || y1 >= y2) {
                hoverRegions.remove(index);
            } else {
                hoverRegions.set(index, new HoverRegion(x1, y1, x2, y2, region.tooltip()));
            }
        }
    }

    private static List<CardLayout> buildPsychosisCards(PsychosisOverlayPayload snapshot, int screenWidth) {
        List<CardLayout> cards = new ArrayList<>(snapshot.entries().size());
        int maxRowWidth = Math.max(TOP_CARD_MIN_WIDTH, Math.min(220, screenWidth / Math.max(1, snapshot.entries().size()) - CARD_GAP));
        for (PsychosisOverlayPayload.Entry entry : snapshot.entries()) {
            Component playerLabel = Component.literal(entry.playerName());
            Component timeValue = Component.literal(formatSeconds(entry.remainingSeconds()));
            Component tierLabel = Component.translatable("hud.cyberneticenhancements.psychosis.tier." + entry.tier().toLowerCase(Locale.ROOT));
            List<Component> tooltip = List.of(
                    Component.translatable("hud.cyberneticenhancements.psychosis"),
                    Component.translatable("hud.cyberneticenhancements.psychosis.subject", entry.playerName()),
                    Component.translatable("hud.cyberneticenhancements.psychosis.tier", tierLabel),
                    Component.translatable("hud.cyberneticenhancements.psychosis.duration", formatSeconds(entry.remainingSeconds()), formatSeconds(entry.totalSeconds()))
            );
            RowData row = new RowData(
                    playerLabel,
                    timeValue,
                    PsychosisOverlayClientState.getRatio(entry),
                    "MAJOR".equals(entry.tier()) ? BAR_PSYCHOSIS_MAJOR : BAR_PSYCHOSIS_MINOR,
                    BAR_PSYCHOSIS_IDLE,
                    true,
                    tooltip
            );
            cards.add(new CardLayout(Component.translatable("hud.cyberneticenhancements.psychosis"), maxRowWidth, List.of(row)));
        }
        return cards;
    }

    private static CardLayout buildSystemCard(CyberwareHudPayload snapshot, int width) {
        List<RowData> rows = new ArrayList<>(2);
        int chromePercent = snapshot.chromeCapacity() <= 0 ? 0 : Math.round(snapshot.installedChrome() * 100.0F / snapshot.chromeCapacity());
        rows.add(new RowData(
                Component.translatable("hud.cyberneticenhancements.chrome"),
                Component.literal(snapshot.installedChrome() + " / " + snapshot.chromeCapacity()),
                safeRatio(snapshot.installedChrome(), snapshot.chromeCapacity()),
                BAR_CHROME,
                BAR_CHROME_IDLE,
                false,
                List.of(
                        Component.translatable("hud.cyberneticenhancements.chrome"),
                        Component.translatable("hud.cyberneticenhancements.installed", snapshot.installedChrome(), snapshot.chromeCapacity()),
                        Component.translatable("hud.cyberneticenhancements.load", chromePercent)
                )
        ));

        List<Component> strainTooltip = new ArrayList<>();
        strainTooltip.add(Component.translatable("hud.cyberneticenhancements.cyberstrain"));
        strainTooltip.add(Component.translatable("hud.cyberneticenhancements.state_value", Component.translatable(snapshot.psychosisStateKey())));
        strainTooltip.add(Component.translatable("hud.cyberneticenhancements.effective", snapshot.effectiveCyberstrain(), 50));
        strainTooltip.add(Component.translatable("hud.cyberneticenhancements.raw", snapshot.cyberstrain(), 50));
        if (snapshot.suppressionSeconds() > 0) {
            strainTooltip.add(Component.translatable("hud.cyberneticenhancements.suppression_detail", snapshot.suppressionAmount(), formatSeconds(snapshot.suppressionSeconds())));
        }
        if (snapshot.psychosisSeconds() > 0) {
            strainTooltip.add(Component.translatable("hud.cyberneticenhancements.episode_risk", formatSeconds(snapshot.psychosisSeconds())));
        }
        rows.add(new RowData(
                Component.translatable("hud.cyberneticenhancements.cyberstrain"),
                Component.translatable(snapshot.psychosisStateKey()),
                Math.min(1.0F, snapshot.effectiveCyberstrain() / 50.0F),
                strainColor(snapshot.effectiveCyberstrain()),
                BAR_BACKGROUND,
                snapshot.psychosisSeconds() > 0 || snapshot.effectiveCyberstrain() >= 35,
                strainTooltip
        ));

        for (CyberwareHudPayload.StatusEntry entry : snapshot.statuses()) {
            if (!entry.playerApplied()) {
                continue;
            }
            Component label = statusLabel(entry.translationKey());
            boolean neuralInstability = entry.translationKey().equals("hud.cyberneticenhancements.sandevistan.instability");
            rows.add(new RowData(
                    label,
                    Component.literal(neuralInstability ? Math.round(entry.amount()) + "%" : formatSeconds(entry.remainingSeconds())),
                    neuralInstability ? (float) Math.clamp(entry.amount() / 100, 0, 1) : CyberwareHudClientState.getStatusRatio(entry),
                    neuralInstability ? entry.amount() >= 75 ? BAR_STRAIN_HIGH : entry.amount() >= 50 ? BAR_STRAIN_MEDIUM : BAR_STRAIN_LOW : statusColor(entry.translationKey()),
                    BAR_BACKGROUND,
                    true,
                    neuralInstability ? List.of(label, Component.translatable("hud.cyberneticenhancements.sandevistan.instability.detail", Math.round(entry.amount()))) : buildStatusTooltip(entry, label)
            ));
        }

        return new CardLayout(Component.translatable("hud.cyberneticenhancements.system"), width, rows);
    }

    private static int countVisibleRightCards(CyberwareHudPayload snapshot) {
        int count = 0;
        if (hasRightSideStatuses(snapshot)) {
            count++;
        }
        for (CyberwareHudPayload.CooldownEntry entry : snapshot.cooldowns()) {
            if (entry.remainingSeconds() > 0) {
                count++;
                break;
            }
        }
        return count;
    }

    private static List<CardLayout> buildBottomAnchoredRightCards(CyberwareHudPayload snapshot, int width) {
        List<CardLayout> cards = new ArrayList<>(2);
        CardLayout statusCard = buildStatusCard(snapshot, width);
        if (statusCard != null) {
            cards.add(statusCard);
        }

        CardLayout cooldownCard = buildCooldownCard(snapshot, width);
        if (cooldownCard != null) {
            cards.add(cooldownCard);
        }
        return cards;
    }

    private static CardLayout buildAbilityCard(CyberwareHudPayload snapshot, int width) {
        if (snapshot.abilities().isEmpty()) {
            return null;
        }

        List<RowData> rows = new ArrayList<>(snapshot.abilities().size());
        for (CyberwareHudPayload.AbilityEntry entry : snapshot.abilities()) {
            List<Component> tooltip = new ArrayList<>();
            Component abilityName = Component.translatable(entry.translationKey());
            Component activationLabel = ModKeyMappings.getAbilityBindingLabel(entry.activationKey());
            String activationText = activationLabel.getString();
            Component labeledAbilityName = activationText.isBlank()
                    ? abilityName
                    : Component.literal("[" + activationText + "] ").append(abilityName.copy());
            tooltip.add(labeledAbilityName);
            if (!activationText.isBlank()) {
                tooltip.add(Component.translatable("hud.cyberneticenhancements.activate", activationText));
            }

            Component detail;
            float ratio;
            int color;
            if (entry.activeSeconds() > 0) {
                detail = Component.translatable("hud.cyberneticenhancements.active_short", formatSeconds(entry.activeSeconds()));
                ratio = CyberwareHudClientState.getAbilityActiveRatio(entry);
                color = BAR_ABILITY_ACTIVE;
                tooltip.add(Component.translatable("hud.cyberneticenhancements.active_duration", formatSeconds(entry.activeSeconds()), formatSeconds(entry.activeTotalSeconds())));
            } else if (entry.cooldownSeconds() > 0) {
                detail = Component.translatable("hud.cyberneticenhancements.cooldown_short_full", formatSeconds(entry.cooldownSeconds()));
                ratio = CyberwareHudClientState.getAbilityCooldownRatio(entry);
                color = BAR_ABILITY_COOLDOWN;
                tooltip.add(Component.translatable("hud.cyberneticenhancements.cooldown_duration", formatSeconds(entry.cooldownSeconds()), formatSeconds(entry.cooldownTotalSeconds())));
            } else {
                detail = Component.translatable("hud.cyberneticenhancements.ready");
                ratio = 1.0F;
                color = BAR_ABILITY_READY;
                tooltip.add(Component.translatable("hud.cyberneticenhancements.ready"));
            }

            rows.add(new RowData(labeledAbilityName, detail, ratio, color, BAR_IDLE, false, tooltip));
        }

        return new CardLayout(
                Component.translatable("hud.cyberneticenhancements.ability"),
                width,
                rows
        );
    }

    private static CardLayout buildStatusCard(CyberwareHudPayload snapshot, int width) {
        if (!hasRightSideStatuses(snapshot)) {
            return null;
        }

        List<RowData> rows = new ArrayList<>(snapshot.statuses().size());
        for (CyberwareHudPayload.StatusEntry entry : snapshot.statuses()) {
            if (entry.playerApplied()) {
                continue;
            }
            Component label = statusLabel(entry.translationKey());
            rows.add(new RowData(
                    label,
                    Component.literal(formatSeconds(entry.remainingSeconds())),
                    CyberwareHudClientState.getStatusRatio(entry),
                    statusColor(entry.translationKey()),
                    BAR_BACKGROUND,
                    isWarningStatus(entry.translationKey()),
                    buildStatusTooltip(entry, label)
            ));
        }

        return new CardLayout(Component.translatable("hud.cyberneticenhancements.status"), width, rows);
    }

    private static boolean hasRightSideStatuses(CyberwareHudPayload snapshot) {
        for (CyberwareHudPayload.StatusEntry entry : snapshot.statuses()) {
            if (!entry.playerApplied()) {
                return true;
            }
        }
        return false;
    }

    private static CardLayout buildCooldownCard(CyberwareHudPayload snapshot, int width) {
        List<RowData> rows = new ArrayList<>();
        for (CyberwareHudPayload.CooldownEntry entry : snapshot.cooldowns()) {
            if (entry.remainingSeconds() <= 0) {
                continue;
            }

            Component label = cooldownLabel(entry.translationKey());
            rows.add(new RowData(
                    label,
                    Component.literal(formatSeconds(entry.remainingSeconds())),
                    CyberwareHudClientState.getCooldownRatio(entry),
                    cooldownColor(entry.translationKey()),
                    BAR_IDLE,
                    false,
                    List.of(
                            label.copy(),
                            Component.translatable("hud.cyberneticenhancements.cooldown_duration", formatSeconds(entry.remainingSeconds()), formatSeconds(entry.totalSeconds()))
                    )
            ));
        }

        if (rows.isEmpty()) {
            return null;
        }
        return new CardLayout(Component.translatable("hud.cyberneticenhancements.cooldowns"), width, rows);
    }

    private static List<Component> buildStatusTooltip(CyberwareHudPayload.StatusEntry entry, Component label) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(label.copy());
        tooltip.add(describeStatus(entry));
        if (entry.playerApplied()) {
            tooltip.add(Component.translatable("hud.cyberneticenhancements.source_hostile_player"));
        }
        tooltip.add(Component.translatable("hud.cyberneticenhancements.duration", formatSeconds(entry.remainingSeconds()), formatSeconds(entry.totalSeconds())));
        return tooltip;
    }

    private static Component describeStatus(CyberwareHudPayload.StatusEntry entry) {
        if ("hud.cyberneticenhancements.suppression".equals(entry.translationKey())) {
            return Component.translatable("hud.cyberneticenhancements.amount", formatNumber(entry.amount()));
        }
        if ("hud.cyberneticenhancements.ram_jolt".equals(entry.translationKey())) {
            return Component.translatable("hud.cyberneticenhancements.cooldown_multiplier", formatNumber(entry.amount()));
        }
        Component combatStatusDescription = CombatStatusManager.describeStatus(entry.translationKey(), entry.amount());
        if (!combatStatusDescription.getString().isEmpty()) {
            return combatStatusDescription;
        }

        CyberwareEffectType effectType = effectTypeFromTranslationKey(entry.translationKey());
        if (effectType != null) {
            return new CyberwareEffect(effectType, entry.amount()).describe();
        }
        return Component.empty();
    }

    private static CyberwareEffectType effectTypeFromTranslationKey(String translationKey) {
        for (CyberwareEffectType effectType : CyberwareEffectType.values()) {
            if (effectType.translationKey().equals(translationKey)) {
                return effectType;
            }
        }
        return null;
    }

    private static Component statusLabel(String translationKey) {
        if ("hud.cyberneticenhancements.suppression".equals(translationKey)) {
            return Component.translatable("hud.cyberneticenhancements.suppression_label");
        }
        if ("hud.cyberneticenhancements.ram_jolt".equals(translationKey)) {
            return Component.translatable("hud.cyberneticenhancements.ram_jolt_label");
        }
        if (translationKey.startsWith("hud.cyberneticenhancements.status.")) {
            return Component.translatable(translationKey);
        }
        return humanizedLabel(translationKey);
    }

    private static void renderAnimatedRow(
            GuiGraphics guiGraphics,
            Font font,
            List<HoverRegion> hoverRegions,
            RowTransition transition,
            List<CardLayout> targetCards,
            int anchorX,
            int bottomY,
            boolean alignRight,
            boolean stacked
    ) {
        long now = System.currentTimeMillis();
        transition.update(targetCards, now);

        if (transition.isAnimating(now)) {
            float progress = transition.progress(now);
            int slideDistance = Math.max(
                    bandHeight(transition.previousCards(), stacked),
                    bandHeight(transition.currentCards(), stacked)
            ) + 12;

            if (!transition.previousCards().isEmpty()) {
                renderRow(
                        guiGraphics,
                        font,
                        transition.previousCards(),
                        rowX(anchorX, transition.previousCards(), alignRight, stacked),
                        bottomY + Math.round(progress * slideDistance),
                        null, stacked
                );
            }
            if (!transition.currentCards().isEmpty()) {
                renderRow(
                        guiGraphics,
                        font,
                        transition.currentCards(),
                        rowX(anchorX, transition.currentCards(), alignRight, stacked),
                        bottomY + Math.round((1.0F - progress) * slideDistance),
                        hoverRegions, stacked
                );
            }
            return;
        }

        renderRow(guiGraphics, font, transition.currentCards(), rowX(anchorX, transition.currentCards(), alignRight, stacked), bottomY, hoverRegions, stacked);
    }

    private static void renderAnimatedTopRow(
            GuiGraphics guiGraphics,
            Font font,
            List<HoverRegion> hoverRegions,
            RowTransition transition,
            List<CardLayout> targetCards,
            int centerX,
            int topY
    ) {
        long now = System.currentTimeMillis();
        transition.update(targetCards, now);

        if (transition.isAnimating(now)) {
            float progress = transition.progress(now);
            int slideDistance = Math.max(
                    rowHeight(transition.previousCards()),
                    rowHeight(transition.currentCards())
            ) + 12;

            if (!transition.previousCards().isEmpty()) {
                renderTopRow(
                        guiGraphics,
                        font,
                        transition.previousCards(),
                        centerX - rowWidth(transition.previousCards()) / 2,
                        topY - Math.round(progress * slideDistance),
                        null
                );
            }
            if (!transition.currentCards().isEmpty()) {
                renderTopRow(
                        guiGraphics,
                        font,
                        transition.currentCards(),
                        centerX - rowWidth(transition.currentCards()) / 2,
                        topY - Math.round((1.0F - progress) * slideDistance),
                        hoverRegions
                );
            }
            return;
        }

        renderTopRow(
                guiGraphics,
                font,
                transition.currentCards(),
                centerX - rowWidth(transition.currentCards()) / 2,
                topY,
                hoverRegions
        );
    }

    private static void renderRow(
            GuiGraphics guiGraphics,
            Font font,
            List<CardLayout> cards,
            int rowX,
            int bottomY,
            List<HoverRegion> hoverRegions, boolean stacked
    ) {
        for (int index = 0; index < cards.size(); index++) {
            CardLayout card = cards.get(index);
            int cardX = rowX + (stacked ? 0 : index * (card.width() + CARD_GAP));
            int cardY = bottomY - card.height();
            renderCard(guiGraphics, font, cardX, cardY, card, hoverRegions);
            if (stacked) bottomY = cardY - CARD_GAP;
        }
    }

    private static void renderTopRow(
            GuiGraphics guiGraphics,
            Font font,
            List<CardLayout> cards,
            int rowX,
            int topY,
            List<HoverRegion> hoverRegions
    ) {
        for (int index = 0; index < cards.size(); index++) {
            CardLayout card = cards.get(index);
            int cardX = rowX + index * (card.width() + CARD_GAP);
            renderCard(guiGraphics, font, cardX, topY, card, hoverRegions);
        }
    }

    private static int rowX(int anchorX, List<CardLayout> cards, boolean alignRight, boolean stacked) {
        int width = stacked ? cards.stream().mapToInt(CardLayout::width).max().orElse(0) : rowWidth(cards);
        return alignRight ? anchorX - width : anchorX;
    }

    private static int bandHeight(List<CardLayout> cards, boolean stacked) {
        return stacked ? cards.stream().mapToInt(CardLayout::height).sum() + Math.max(0, cards.size() - 1) * CARD_GAP : rowHeight(cards);
    }
    private static int rowWidth(List<CardLayout> cards) {
        int width = 0;
        for (int index = 0; index < cards.size(); index++) {
            width += cards.get(index).width();
            if (index + 1 < cards.size()) {
                width += CARD_GAP;
            }
        }
        return width;
    }

    private static int rowHeight(List<CardLayout> cards) {
        int height = 0;
        for (CardLayout card : cards) {
            height = Math.max(height, card.height());
        }
        return height;
    }

    private static String signature(List<CardLayout> cards) {
        return cards.stream()
                .map(card -> card.title().getString() + ":" + card.rows().stream().map(row -> row.label().getString()).collect(Collectors.joining(",")))
                .collect(Collectors.joining("|"));
    }

    private static Component cooldownLabel(String translationKey) {
        if (translationKey.startsWith("tooltip.cyberneticenhancements.consumable_category.")
                || translationKey.startsWith("item.cyberneticenhancements.")) {
            return Component.translatable(translationKey);
        }
        return humanizedLabel(translationKey);
    }

    private static Component humanizedLabel(String translationKey) {
        int lastDot = translationKey.lastIndexOf('.');
        String suffix = lastDot >= 0 ? translationKey.substring(lastDot + 1) : translationKey;
        StringBuilder builder = new StringBuilder();
        boolean capitalize = true;
        for (int index = 0; index < suffix.length(); index++) {
            char character = suffix.charAt(index);
            if (character == '_') {
                builder.append(' ');
                capitalize = true;
                continue;
            }
            if (capitalize) {
                builder.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                builder.append(character);
            }
        }
        return Component.literal(builder.toString());
    }

    private static void renderCard(
        GuiGraphics guiGraphics,
        Font font,
        int x,
        int y,
        CardLayout card,
        List<HoverRegion> hoverRegions
    ) {
        drawCardFrame(guiGraphics, x, y, card.width(), card.height());
        Component title = trimComponent(font, card.title(), card.width() - CARD_PADDING * 2);
        guiGraphics.drawString(font, title, x + CARD_PADDING, y + 5, TEXT_PRIMARY, true);

        int rowY = y + CARD_HEADER_HEIGHT + CARD_PADDING;
        for (RowData row : card.rows()) {
            renderRow(guiGraphics, font, x + CARD_PADDING, rowY, card.width() - CARD_PADDING * 2, row);
            if (hoverRegions != null) {
                hoverRegions.add(new HoverRegion(x + CARD_PADDING, rowY, x + card.width() - CARD_PADDING, rowY + ROW_HEIGHT, row.tooltip()));
            }
            rowY += ROW_HEIGHT + ROW_GAP;
        }
    }

    private static void renderRow(GuiGraphics guiGraphics, Font font, int x, int y, int width, RowData row) {
        Component value = trimComponent(font, row.value(), Math.max(1, width * 3 / 5));
        int valueWidth = font.width(value);
        int labelMaxWidth = Math.max(1, width - valueWidth - 8);
        Component label = trimComponent(font, row.label(), labelMaxWidth);
        guiGraphics.drawString(font, label, x, y, TEXT_SECONDARY, true);
        guiGraphics.drawString(font, value, x + width - valueWidth, y, TEXT_PRIMARY, true);

        int barY = y + font.lineHeight + BAR_TEXT_GAP;
        drawHorizontalBar(guiGraphics, x, barY, width, BAR_HEIGHT, row.ratio(), row.idleColor(), row.fillColor(), row.warning());
    }

    private static void drawCardFrame(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        UiRenderer.panel(guiGraphics, x, y, width, height, 0xB0181F28, 0xA02C4B63);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + CARD_HEADER_HEIGHT, 0x60232C36);
        guiGraphics.fill(x + 6, y + CARD_HEADER_HEIGHT, x + width - 6, y + CARD_HEADER_HEIGHT + 1, 0x808BDAF8);
    }

    private static void drawHorizontalBar(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            float ratio,
            int backgroundColor,
            int fillColor,
            boolean warning
    ) {
        guiGraphics.fill(x, y, x + width, y + height, StationScreenStyle.PANEL_EDGE);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, backgroundColor);

        int innerWidth = Math.max(0, width - 2);
        int fillWidth = Math.max(0, Math.min(innerWidth, Math.round(innerWidth * ratio)));
        if (fillWidth > 0) {
            guiGraphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + height - 1, fillColor);
            guiGraphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + 2, 0x55FFFFFF);
        }
    }

    private static void renderHoverTooltip(
            GuiGraphics guiGraphics,
            Font font,
            int screenWidth,
            int screenHeight,
            List<Component> lines,
            int mouseX,
            int mouseY
    ) {
        if (lines.isEmpty()) {
            return;
        }

        int tooltipWidth = 0;
        for (Component line : lines) {
            tooltipWidth = Math.max(tooltipWidth, font.width(line));
        }
        tooltipWidth += ModTooltipStyle.CONTENT_PADDING * 2;

        int lineAdvance = font.lineHeight + ModTooltipStyle.LINE_SPACING;
        int tooltipHeight = ModTooltipStyle.CONTENT_PADDING * 2 + lines.size() * lineAdvance - ModTooltipStyle.LINE_SPACING;

        int tooltipX = mouseX + ModTooltipStyle.TOOLTIP_CURSOR_OFFSET_X;
        if (tooltipX + tooltipWidth > screenWidth - ModTooltipStyle.SCREEN_EDGE_MARGIN) {
            tooltipX = mouseX - ModTooltipStyle.TOOLTIP_CURSOR_OFFSET_X - tooltipWidth;
        }
        tooltipX = Math.max(ModTooltipStyle.SCREEN_EDGE_MARGIN, tooltipX);

        int tooltipY = mouseY - ModTooltipStyle.TOOLTIP_CURSOR_OFFSET_Y;
        if (tooltipY + tooltipHeight > screenHeight - ModTooltipStyle.SCREEN_EDGE_MARGIN) {
            tooltipY = screenHeight - ModTooltipStyle.SCREEN_EDGE_MARGIN - tooltipHeight;
        }
        tooltipY = Math.max(ModTooltipStyle.SCREEN_EDGE_MARGIN, tooltipY);

        guiGraphics.flush();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
        RenderSystem.disableDepthTest();

        ModTooltipStyle.drawTooltipFrame(guiGraphics, tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight);

        int textY = tooltipY + ModTooltipStyle.CONTENT_PADDING;
        for (Component line : lines) {
            guiGraphics.drawString(font, line, tooltipX + ModTooltipStyle.CONTENT_PADDING, textY, ModTooltipStyle.TEXT_FALLBACK, false);
            textY += lineAdvance;
        }

        guiGraphics.flush();
        RenderSystem.enableDepthTest();
        guiGraphics.pose().popPose();
    }

    private static Component trimComponent(Font font, Component component, int maxWidth) {
        if (font.width(component) <= maxWidth) {
            return component;
        }
        String raw = component.getString();
        String trimmed = font.plainSubstrByWidth(raw, Math.max(0, maxWidth - font.width("..."))) + "...";
        return Component.literal(trimmed);
    }

    private static Vec2 scaledMousePosition(Minecraft minecraft, int guiWidth, int guiHeight) {
        double screenWidth = minecraft.getWindow().getScreenWidth();
        double screenHeight = minecraft.getWindow().getScreenHeight();
        if (screenWidth <= 0.0D || screenHeight <= 0.0D) {
            return new Vec2(0.0F, 0.0F);
        }
        float scaledX = (float) (minecraft.mouseHandler.xpos() * guiWidth / screenWidth);
        float scaledY = (float) (minecraft.mouseHandler.ypos() * guiHeight / screenHeight);
        return new Vec2(scaledX, scaledY);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatSeconds(int seconds) {
        return seconds + "s";
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static float safeRatio(int value, int max) {
        if (max <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (float) value / (float) max));
    }

    private static int strainColor(int effectiveCyberstrain) {
        if (effectiveCyberstrain >= 35) {
            return BAR_STRAIN_HIGH;
        }
        if (effectiveCyberstrain >= 20) {
            return BAR_STRAIN_MEDIUM;
        }
        return BAR_STRAIN_LOW;
    }

    private static int statusColor(String translationKey) {
        if ("hud.cyberneticenhancements.suppression".equals(translationKey)) {
            return BAR_STATUS_SUPPRESSANT;
        }
        if ("hud.cyberneticenhancements.ram_jolt".equals(translationKey)) {
            return BAR_STATUS_NEURAL;
        }
        return switch (translationKey) {
            case "hud.cyberneticenhancements.status.shock" -> BAR_STATUS_SHOCK;
            case "hud.cyberneticenhancements.status.overheat" -> BAR_STATUS_OVERHEAT;
            case "hud.cyberneticenhancements.status.corrosion" -> BAR_STATUS_CORROSION;
            case "hud.cyberneticenhancements.status.trauma" -> BAR_STATUS_TRAUMA;
            case "hud.cyberneticenhancements.status.bleed" -> BAR_STATUS_BLEED;
            case "hud.cyberneticenhancements.status.mark" -> BAR_STATUS_MARK;
            case "tooltip.cyberneticenhancements.effect.health_regen",
                 "tooltip.cyberneticenhancements.effect.max_health",
                 "tooltip.cyberneticenhancements.effect.bonus_absorption" -> BAR_STATUS_MEDICAL;
            case "tooltip.cyberneticenhancements.effect.movement_speed",
                 "tooltip.cyberneticenhancements.effect.attack_speed",
                 "tooltip.cyberneticenhancements.effect.attack_damage",
                 "tooltip.cyberneticenhancements.effect.block_break_speed" -> BAR_STATUS_SYSTEM;
            case "tooltip.cyberneticenhancements.effect.damage_reduction",
                 "tooltip.cyberneticenhancements.effect.chrome_capacity",
                 "tooltip.cyberneticenhancements.effect.water_breathing" -> BAR_STATUS_NEURAL;
            default -> BAR_STATUS_MEDICAL;
        };
    }

    private static boolean isWarningStatus(String translationKey) {
        return "hud.cyberneticenhancements.suppression".equals(translationKey)
                || "hud.cyberneticenhancements.status.overheat".equals(translationKey)
                || "hud.cyberneticenhancements.status.corrosion".equals(translationKey)
                || "hud.cyberneticenhancements.status.trauma".equals(translationKey)
                || "hud.cyberneticenhancements.status.bleed".equals(translationKey);
    }

    private static int cooldownColor(String translationKey) {
        return switch (translationKey) {
            case "tooltip.cyberneticenhancements.consumable_category.medical" -> BAR_STATUS_MEDICAL;
            case "tooltip.cyberneticenhancements.consumable_category.booster" -> BAR_STATUS_SYSTEM;
            case "tooltip.cyberneticenhancements.consumable_category.street" -> BAR_STATUS_STREET;
            case "tooltip.cyberneticenhancements.consumable_category.suppressant" -> BAR_STATUS_SUPPRESSANT;
            case "tooltip.cyberneticenhancements.consumable_category.neural" -> BAR_STATUS_NEURAL;
            case "item.cyberneticenhancements.biomonitor" -> BAR_STATUS_BIOMONITOR;
            case "item.cyberneticenhancements.blood_pump" -> BAR_STATUS_BLOOD_PUMP;
            case "item.cyberneticenhancements.second_heart" -> BAR_STATUS_SECOND_HEART;
            case "item.cyberneticenhancements.reflex_tuner" -> BAR_STATUS_REFLEX;
            default -> BAR_STATUS_SYSTEM;
        };
    }

    private record CardLayout(Component title, int width, List<RowData> rows) {
        int height() {
            return CARD_HEADER_HEIGHT + CARD_PADDING * 2 + rows.size() * ROW_HEIGHT + Math.max(0, rows.size() - 1) * ROW_GAP;
        }
    }

    private record RowData(
            Component label,
            Component value,
            float ratio,
            int fillColor,
            int idleColor,
            boolean warning,
            List<Component> tooltip
    ) {
    }

    private record HoverRegion(int x1, int y1, int x2, int y2, List<Component> tooltip) {
        boolean contains(int x, int y) {
            return x >= x1 && x < x2 && y >= y1 && y < y2;
        }
    }

    private static final class RowTransition {
        private String currentSignature = "";
        private List<CardLayout> currentCards = List.of();
        private List<CardLayout> previousCards = List.of();
        private long startTimeMs;
        private boolean animating;

        void update(List<CardLayout> nextCards, long now) {
            String nextSignature = signature(nextCards);
            if (currentSignature.equals(nextSignature)) {
                currentCards = nextCards;
                if (animating && now - startTimeMs >= ROW_TRANSITION_MILLIS) {
                    animating = false;
                    previousCards = List.of();
                }
                return;
            }

            previousCards = currentCards;
            currentCards = nextCards;
            currentSignature = nextSignature;
            startTimeMs = now;
            animating = true;
        }

        boolean isAnimating(long now) {
            if (animating && now - startTimeMs >= ROW_TRANSITION_MILLIS) {
                animating = false;
                previousCards = List.of();
            }
            return animating;
        }

        float progress(long now) {
            float linear = Math.max(0.0F, Math.min(1.0F, (float) (now - startTimeMs) / (float) ROW_TRANSITION_MILLIS));
            return easeInOutCubic(linear);
        }

        private static float easeInOutCubic(float value) {
            if (value < 0.5F) {
                return 4.0F * value * value * value;
            }
            float inverse = -2.0F * value + 2.0F;
            return 1.0F - inverse * inverse * inverse / 2.0F;
        }

        List<CardLayout> currentCards() {
            return currentCards;
        }

        List<CardLayout> previousCards() {
            return previousCards;
        }
    }
}
