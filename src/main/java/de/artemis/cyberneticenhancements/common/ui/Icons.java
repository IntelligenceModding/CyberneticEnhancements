package de.artemis.cyberneticenhancements.common.ui;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public enum Icons {
    SHOP('\uE000', "[SHOP]"),
    DIALOGUE('\uE001', "[TALK]"),
    QUEST('\uE002', "[QUEST]"),
    OBJECTIVE('\uE003', "[GOAL]"),
    WARP('\uE004', "[WARP]"),
    HOME('\uE005', "[HOME]"),
    TRADE('\uE006', "[TRADE]"),
    REWARD('\uE007', "[REWARD]"),
    COMPLETED('\uE008', "[DONE]"),
    LOCKED('\uE009', "[LOCKED]"),
    COMBAT('\uE00A', "[COMBAT]"),
    CRAFTING('\uE00B', "[CRAFT]"),
    INVENTORY('\uE00C', "[INV]"),
    RANK('\uE00D', "[RANK]"),
    SETTINGS('\uE00E', "[SETTINGS]"),
    MAIL('\uE00F', "[MAIL]"),
    WARNING('\uE010', "[WARN]"),
    INFO('\uE011', "[INFO]"),
    COOLDOWN('\uE012', "[TIME]"),
    BACK('\uE013', "[BACK]"),
    NEXT('\uE014', "[NEXT]"),
    MENU('\uE015', "[MENU]"),
    RESET('\uE016', "[RESET]");

    public static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "icons");
    private static final Component SPACE = Component.literal(" ");

    private final char codepoint;
    private final String fallback;

    Icons(char codepoint, String fallback) {
        this.codepoint = codepoint;
        this.fallback = fallback;
    }

    public char codepoint() {
        return codepoint;
    }

    public String fallback() {
        return fallback;
    }

    public String glyph() {
        return Character.toString(codepoint);
    }

    public MutableComponent component() {
        return component(false);
    }

    public MutableComponent component(boolean asciiFallback) {
        return component(Style.EMPTY, asciiFallback);
    }

    public MutableComponent withText(String text) {
        return withText(Component.literal(text));
    }

    public MutableComponent withText(Component text) {
        return withText(text, false);
    }

    public MutableComponent withText(Component text, boolean asciiFallback) {
        return icon(this, text, asciiFallback);
    }

    public static MutableComponent icon(Icons icon, Component text) {
        return icon(icon, text, false);
    }

    public static MutableComponent icon(Icons icon, Component text, boolean asciiFallback) {
        MutableComponent result = Component.empty();
        result.append(icon.component(text.getStyle(), asciiFallback));
        result.append(SPACE.copy());
        result.append(text.copy());
        return result;
    }

    private MutableComponent component(Style baseStyle, boolean asciiFallback) {
        if (asciiFallback) {
            return Component.literal(fallback).withStyle(baseStyle);
        }
        return Component.literal(glyph()).withStyle(baseStyle.withFont(FONT));
    }
}
