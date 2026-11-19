package de.artemis.cyberneticenhancements.client.ui;

import com.google.gson.JsonParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class NoticeLinksTest {
    @Test public void completeLinkLabelsMoveToTheNextLineInsteadOfBreakingAtTheirSpaces() {
        var splitter = new net.minecraft.client.StringSplitter((codePoint, style) -> 6);
        String label = "DoomedArtemis on GitHub";
        var paragraph = Component.literal("See ").append(NoticeLinks.link(label, NoticeLinks.CREATOR_GITHUB)).append(" for more.");
        var lines = NoticeLinks.wrap(paragraph, splitter, 150);
        var links = lines.stream().flatMap(line -> NoticeLinks.spans(line, splitter).stream()).toList();
        assertEquals(1, links.size()); assertEquals(label, links.getFirst().label().getString());
        assertEquals(0, links.getFirst().x());
        for (var line : lines) assertTrue(splitter.stringWidth(line) <= 150);
    }

    @Test public void viewerContainsNoSelfReferenceOrDuplicateDestinationsAcrossPages() {
        Set<String> destinations = new HashSet<>();
        for (var page : LaunchIntroPresentation.NoticePage.values())
            for (var paragraph : NoticeLinks.paragraphs(page))
                for (var component : components(paragraph)) {
                    if (component.getContents() instanceof TranslatableContents text)
                        assertFalse("Viewer repeats its startup navigation hint", text.getKey().contains("beta_disclaimers"));
                    var click = component.getStyle().getClickEvent();
                    if (click != null) {
                        assertFalse(click.getValue().contains("cyberneticenhancements:notices"));
                        assertTrue("Duplicate link: " + click.getValue(), destinations.add(click.getValue()));
                    }
                }
    }

    @Test public void wrappedLinksKeepTheirDestinationAndExcludeSurroundingPlainText() {
        var splitter = new net.minecraft.client.StringSplitter((codePoint, style) -> 6);
        var style = NoticeLinks.link("DoomedArtemis", NoticeLinks.CREATOR_GITHUB).getStyle();
        var first = net.minecraft.util.FormattedCharSequence.composite(
                net.minecraft.util.FormattedCharSequence.forward("See ", net.minecraft.network.chat.Style.EMPTY),
                net.minecraft.util.FormattedCharSequence.forward("Doomed", style));
        var second = net.minecraft.util.FormattedCharSequence.composite(
                net.minecraft.util.FormattedCharSequence.forward("Artemis", style),
                net.minecraft.util.FormattedCharSequence.forward(" for more.", net.minecraft.network.chat.Style.EMPTY));
        var spans = NoticeLinks.spans(first, splitter);
        assertEquals(1, spans.size()); assertEquals(24, spans.getFirst().x()); assertEquals(36, spans.getFirst().width());
        var wrapped = NoticeLinks.spans(second, splitter);
        assertEquals(1, wrapped.size()); assertEquals(0, wrapped.getFirst().x()); assertEquals(42, wrapped.getFirst().width());
        assertEquals(NoticeLinks.CREATOR_GITHUB, wrapped.getFirst().style().getClickEvent().getValue());
    }

    private static List<Component> components(Component component) {
        var result = new ArrayList<Component>(); result.add(component);
        if (component.getContents() instanceof TranslatableContents text)
            for (Object argument : text.getArgs()) if (argument instanceof Component child) result.addAll(components(child));
        for (var sibling : component.getSiblings()) result.addAll(components(sibling));
        return result;
    }

    @Test public void allSuppliedProjectCreatorAndCommunityUrlsAreReachableFromTheThankYouPage() {
        Set<String> destinations = new HashSet<>();
        for (var paragraph : NoticeLinks.paragraphs(LaunchIntroPresentation.NoticePage.THANK_YOU))
            for (var component : components(paragraph)) {
                var click = component.getStyle().getClickEvent();
                if (click != null) destinations.add(click.getValue());
            }
        assertEquals(Set.of("https://discord.intelligence-modding.de/", "https://github.com/IntelligenceModding/CyberneticEnhancements",
                "https://github.com/DoomedArtemis", "https://www.youtube.com/@intelligencemodding4093",
                "https://modrinth.com/mod/cybernetic-enhancements", "https://modrinth.com/user/Doomed_Artemis",
                "https://modrinth.com/organization/intelligence", "https://www.curseforge.com/members/doomed_artemis/projects"), destinations);
    }

    @Test public void localizedParagraphsContainEveryLinkArgumentAndEveryLinkHasADestination() throws Exception {
        var language = JsonParser.parseString(Files.readString(Path.of(
                "src/generated/resources/assets/cyberneticenhancements/lang/en_us.json"))).getAsJsonObject();
        for (var page : LaunchIntroPresentation.NoticePage.values())
            for (var paragraph : NoticeLinks.paragraphs(page))
                for (var component : components(paragraph)) {
                    if (component.getContents() instanceof TranslatableContents text) {
                        assertTrue("Missing translation: " + text.getKey(), language.has(text.getKey()));
                        var template = language.get(text.getKey()).getAsString();
                        assertEquals("Link arguments lost in " + text.getKey(), text.getArgs().length,
                                (template.length() - template.replace("%s", "").length()) / 2);
                    }
                    var style = component.getStyle();
                    if (style.getClickEvent() != null) {
                        assertEquals(ClickEvent.Action.OPEN_URL, style.getClickEvent().getAction());
                        assertTrue(style.isUnderlined());
                        assertFalse(style.getClickEvent().getValue().isBlank());
                    }
                }
    }
}
