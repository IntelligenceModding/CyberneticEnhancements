package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.StringSplitter;

import java.util.List;

/** Link targets for the manually browsed notices; the timed intro keeps its existing plain text. */
public final class NoticeLinks {
    public static final String SETTINGS = "cyberneticenhancements:settings";
    public static final String DISCORD = "https://discord.intelligence-modding.de/";
    public static final String PROJECT_GITHUB = "https://github.com/IntelligenceModding/CyberneticEnhancements";
    public static final String CREATOR_GITHUB = "https://github.com/DoomedArtemis";
    public static final String YOUTUBE = "https://www.youtube.com/@intelligencemodding4093";
    public static final String PROJECT_MODRINTH = "https://modrinth.com/mod/cybernetic-enhancements";
    public static final String CREATOR_MODRINTH = "https://modrinth.com/user/Doomed_Artemis";
    public static final String ORGANIZATION_MODRINTH = "https://modrinth.com/organization/intelligence";
    public static final String MED_TEC_AV = "https://www.planetminecraft.com/project/zetatech-atlus/";
    public static final String CURSEFORGE ="https://www.curseforge.com/members/doomed_artemis/projects";

    private NoticeLinks() {}

    public record Span(int x, int width, Style style, Component label) {}

    /** Hit regions follow the rendered glyph widths, including links split across wrapped lines. */
    public static List<Span> spans(FormattedCharSequence line, StringSplitter splitter) {
        var result = new java.util.ArrayList<Span>();
        class Builder {
            Style style;
            float start, end;
            final StringBuilder label = new StringBuilder();
            void flush() {
                if (style == null) return;
                result.add(new Span((int) Math.floor(start), Math.max(1, (int) Math.ceil(end) - (int) Math.floor(start)),
                        style, Component.literal(label.toString())));
                style = null; label.setLength(0);
            }
        }
        var span = new Builder();
        float[] pen = {0};
        line.accept((index, style, codePoint) -> {
            String glyph = new String(Character.toChars(codePoint));
            float advance = splitter.stringWidth(net.minecraft.network.chat.FormattedText.of(glyph, style));
            if (style.getClickEvent() == null) span.flush();
            else {
                if (span.style != null && !span.style.getClickEvent().equals(style.getClickEvent())) span.flush();
                if (span.style == null) { span.style = style; span.start = pen[0]; }
                span.end = pen[0] + advance; span.label.append(glyph);
            }
            pen[0] += advance;
            return true;
        });
        span.flush();
        return List.copyOf(result);
    }

    public static Component link(String label, String destination) {
        return Component.literal(label).withStyle(style -> style.withColor(0xFFB3BC).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, destination)));
    }

    private static Component text(String key, Object... arguments) {
        return Component.translatable("screen.cyberneticenhancements.intro.links." + key, arguments);
    }

    public static List<Component> paragraphs(LaunchIntroPresentation.NoticePage page) {
        if (page == LaunchIntroPresentation.NoticePage.THANK_YOU) return List.of(
                Component.translatable("screen.cyberneticenhancements.intro.thanks_message"), text("thanks_intro"),
                text("project", link("Modrinth", PROJECT_MODRINTH), link("GitHub", PROJECT_GITHUB)),
                text("creator", link("Modrinth", CREATOR_MODRINTH), link("GitHub", CREATOR_GITHUB), link("CurseForge", CURSEFORGE)),
                text("community", link("Modrinth", ORGANIZATION_MODRINTH), link("Discord", DISCORD), link("YouTube", YOUTUBE)));
        return page.paragraphKeys().stream().filter(key -> !key.equals("beta_disclaimers")).map(key -> switch (key) {
            case "disclaimer_minecraft" -> text(key, link("Minecraft", "https://www.minecraft.net/en-us"),
                    Component.literal("Mojang"), link("Microsoft", "https://www.microsoft.com/"));
            case "disclaimer_neoforge" -> text(key, link("NeoForge", "https://neoforged.net/"));
            case "disclaimer_artwork" -> text(key, link("Cyberpunk 2077", "https://www.cyberpunk.net/us/en/"),
                    link("CD PROJEKT S.A.", "https://www.cdprojekt.com/en/"));
            case "credits_structure" -> text(key, link("Captain_JEK", MED_TEC_AV));
            case "settings_open" -> text(key, link(text("client_settings").getString(), SETTINGS));
            case "beta_reports" -> text(key, link(text("report_issue").getString(), PROJECT_GITHUB + "/issues"));
            default -> Component.translatable("screen.cyberneticenhancements.intro." + key);
        }).toList();
    }

    /** A complete link label is one wrapping unit, even when it contains spaces. */
    public static List<FormattedCharSequence> wrap(Component paragraph, StringSplitter splitter, int maximum) {
        record Glyph(String text, Style style, float width) {}
        var words = new java.util.ArrayList<List<Glyph>>();
        var word = new java.util.ArrayList<Glyph>();
        paragraph.visit((Style style, String value) -> {
            value.codePoints().forEach(codePoint -> {
                String glyph = new String(Character.toChars(codePoint));
                if (Character.isWhitespace(codePoint) && style.getClickEvent() == null) {
                    if (!word.isEmpty()) { words.add(List.copyOf(word)); word.clear(); }
                } else word.add(new Glyph(glyph, style, splitter.stringWidth(net.minecraft.network.chat.FormattedText.of(glyph, style))));
            });
            return java.util.Optional.empty();
        }, Style.EMPTY);
        if (!word.isEmpty()) words.add(List.copyOf(word));
        var result = new java.util.ArrayList<FormattedCharSequence>();
        var line = new java.util.ArrayList<net.minecraft.network.chat.FormattedText>();
        float width = 0, space = splitter.stringWidth(" ");
        for (var token : words) {
            float tokenWidth = 0;
            for (var glyph : token) tokenWidth += glyph.width();
            if (!line.isEmpty() && width + space + tokenWidth > maximum) {
                result.add(net.minecraft.locale.Language.getInstance().getVisualOrder(net.minecraft.network.chat.FormattedText.composite(List.copyOf(line))));
                line.clear(); width = 0;
            }
            if (!line.isEmpty()) { line.add(net.minecraft.network.chat.FormattedText.of(" ")); width += space; }
            for (var glyph : token) {
                // An unusually long translated label may still need a hard break to fit the screen.
                if (width > 0 && width + glyph.width() > maximum) {
                    result.add(net.minecraft.locale.Language.getInstance().getVisualOrder(net.minecraft.network.chat.FormattedText.composite(List.copyOf(line))));
                    line.clear(); width = 0;
                }
                line.add(net.minecraft.network.chat.FormattedText.of(glyph.text(), glyph.style())); width += glyph.width();
            }
        }
        if (!line.isEmpty()) result.add(net.minecraft.locale.Language.getInstance().getVisualOrder(net.minecraft.network.chat.FormattedText.composite(List.copyOf(line))));
        return List.copyOf(result);
    }
}
