package de.artemis.cyberneticenhancements.client.screen;

import com.mojang.brigadier.arguments.StringArgumentType;
import de.artemis.cyberneticenhancements.client.ClientModEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Links between wiki articles. In lang text a link is written {@code [[entry]]}, {@code [[entry#section]]} or
 * {@code [[entry#section|label]]}; players type {@code #entry} or {@code #entry/section} in chat. Both become blue underlined
 * text that opens the wiki on that entry, scrolled to that section.
 */
public final class WikiLinks {
    public static final String COMMAND = "cewiki";
    public static final int COLOR = 0x5C9DFF;
    private static final Pattern MARKUP = Pattern.compile("\\[\\[([a-z0-9_:.\\-]+)(?:#([a-z0-9_]+))?(?:\\|([^\\]]*))?]]");
    private static final Pattern CHAT = Pattern.compile("(?<![\\w#&/])#([A-Za-z0-9_]+)(?:/([A-Za-z0-9_]+))?");
    private static Map<String, CyberwareWikiData.WikiEntry> entries;
    private static Map<String, String> names;
    private static java.util.List<Suggestion> suggestions;

    /** One thing a player can type after #: {@code insert} is the text that follows the #. */
    public record Suggestion(String insert, Component title, Component kind) {}
    private static String pendingEntry, pendingAnchor;

    private WikiLinks() {
    }

    // ---- Link text ---------------------------------------------------------------------------------------------------

    /** The same text with every {@code [[...]]} turned into a link; text without markup is returned untouched. */
    public static Component parse(Component text) {
        String plain = text.getString();
        if (!plain.contains("[[")) return text;
        MutableComponent result = Component.empty().withStyle(text.getStyle());
        Matcher matcher = MARKUP.matcher(plain);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) result.append(Component.literal(plain.substring(last, matcher.start())));
            String label = matcher.group(3);
            result.append(link(matcher.group(1), matcher.group(2), label == null || label.isBlank() ? null : label));
            last = matcher.end();
        }
        if (last < plain.length()) result.append(Component.literal(plain.substring(last)));
        return result;
    }

    public static MutableComponent link(String entry, String anchor, String label) {
        CyberwareWikiData.WikiEntry target = entry(entry);
        Component text;
        if (label != null) text = Component.literal(label);
        else if (target == null) text = Component.literal(entry);
        else if (anchor != null && section(target, anchor) != null) text = section(target, anchor).title();
        else text = target.title();
        String value = anchor == null ? entry : entry + "#" + anchor;
        return text.copy().withStyle(Style.EMPTY.withColor(COLOR).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + COMMAND + " " + value))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("wiki.cyberneticenhancements.link.hover",
                        target == null ? Component.literal(entry) : target.title()))));
    }

    /** The target of a wiki link click, or null when the click was on something else. */
    public static String target(Style style) {
        if (style == null || style.getClickEvent() == null || style.getClickEvent().getAction() != ClickEvent.Action.RUN_COMMAND) return null;
        String value = style.getClickEvent().getValue();
        String prefix = "/" + COMMAND + " ";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : null;
    }

    // ---- Lookup ------------------------------------------------------------------------------------------------------

    static CyberwareWikiData.WikiEntry entry(String id) {
        load();
        return entries.get(id);
    }

    private static void load() {
        if (entries != null) return;
        Map<String, CyberwareWikiData.WikiEntry> built = new java.util.HashMap<>();
        var tabs = CyberwareWikiData.buildArchiveTabs();
        for (var tab : tabs) index(tab.topics(), built);
        // What a player can type after #: an entry's id or title first, then the name of a whole category such as Arms, which opens its first entry.
        Map<String, String> found = new java.util.HashMap<>();
        for (var entry : built.values()) found.putIfAbsent(normalize(entry.id()), entry.id());
        for (var entry : built.values()) found.putIfAbsent(normalize(entry.title().getString()), entry.id());
        for (var tab : tabs) categories(tab.topics(), found);
        java.util.List<Suggestion> list = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (var tab : tabs) suggestCategories(tab.topics(), list, seen);
        built.values().stream().sorted(java.util.Comparator.comparing(entry -> entry.title().getString())).forEach(entry -> {
            if (seen.add(normalize(entry.title().getString()))) list.add(new Suggestion(typed(entry.title().getString()), entry.title(), entry.badge()));
        });
        suggestions = list;
        names = found;
        entries = built;
    }

    private static void categories(java.util.List<CyberwareWikiData.WikiTopic> topics, Map<String, String> into) {
        for (var topic : topics) {
            if (topic.section() != null && !topic.section().entries().isEmpty()) into.putIfAbsent(normalize(topic.title().getString()), topic.section().entries().getFirst().id());
            categories(topic.children(), into);
            // A category that only holds other categories (Cyberware, Modules...) opens the first entry inside it.
            if (topic.section() == null) {
                String first = firstEntry(topic);
                if (first != null) into.putIfAbsent(normalize(topic.title().getString()), first);
            }
        }
    }

    private static void suggestCategories(java.util.List<CyberwareWikiData.WikiTopic> topics, java.util.List<Suggestion> into, java.util.Set<String> seen) {
        for (var topic : topics) {
            if (firstEntry(topic) != null && seen.add(normalize(topic.title().getString()))) into.add(new Suggestion(typed(topic.title().getString()), topic.title(), Component.translatable("wiki.cyberneticenhancements.link.category")));
            suggestCategories(topic.children(), into, seen);
        }
    }

    /** A title as one word that can follow the #. */
    static String typed(String title) {
        return title.strip().replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    /** Entries and categories whose name starts with, or else contains, what has been typed after the #. */
    public static java.util.List<Suggestion> suggest(String typed, int max) {
        load();
        String key = normalize(typed);
        java.util.List<Suggestion> result = new java.util.ArrayList<>();
        for (var suggestion : suggestions) if (normalize(suggestion.title().getString()).startsWith(key) || normalize(suggestion.insert()).startsWith(key)) result.add(suggestion);
        if (!key.isEmpty()) for (var suggestion : suggestions) if (result.size() < max && !result.contains(suggestion) && normalize(suggestion.title().getString()).contains(key)) result.add(suggestion);
        return result.size() > max ? result.subList(0, max) : result;
    }

    /** The sections of the article a typed name resolves to, as suggestions for what follows the /. */
    public static java.util.List<Suggestion> suggestSections(String typed, String typedSection) {
        load();
        String id = resolve(typed);
        var entry = id == null ? null : entry(id);
        java.util.List<Suggestion> result = new java.util.ArrayList<>();
        if (entry == null) return result;
        String key = typedSection.toLowerCase(Locale.ROOT);
        for (var section : entry.sections()) if (anchorOf(section).startsWith(key)) result.add(new Suggestion(anchorOf(section), section.title(), entry.title()));
        return result;
    }

    /** The article a complete #name or #name/section opens, or null when nothing matches. */
    public static Component describe(String typed, String section) {
        String id = resolve(typed);
        var entry = id == null ? null : entry(id);
        if (entry == null) return null;
        var part = section == null ? null : section(entry, section.toLowerCase(Locale.ROOT));
        if (section != null && part == null) return null;
        return part == null ? entry.title() : Component.empty().append(entry.title()).append(" › ").append(part.title());
    }

    private static String firstEntry(CyberwareWikiData.WikiTopic topic) {
        if (topic.section() != null && !topic.section().entries().isEmpty()) return topic.section().entries().getFirst().id();
        for (var child : topic.children()) {
            String first = firstEntry(child);
            if (first != null) return first;
        }
        return null;
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    /** The id of the entry a typed name refers to, or null. */
    static String resolve(String name) {
        load();
        return names.get(normalize(name));
    }

    private static void index(java.util.List<CyberwareWikiData.WikiTopic> topics, Map<String, CyberwareWikiData.WikiEntry> into) {
        for (var topic : topics) {
            if (topic.section() != null) for (var entry : topic.section().entries()) into.putIfAbsent(entry.id(), entry);
            index(topic.children(), into);
        }
    }

    /** Short name of a section as used after the {@code #} of a link: the last part of its lang key. */
    public static String anchorOf(CyberwareWikiData.ArticleSection section) {
        if (section.title().getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            return key.substring(key.lastIndexOf('.') + 1);
        }
        return section.title().getString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }

    static CyberwareWikiData.ArticleSection section(CyberwareWikiData.WikiEntry entry, String anchor) {
        if (entry == null || anchor == null) return null;
        for (var section : entry.sections()) if (anchorOf(section).equals(anchor)) return section;
        return null;
    }

    /** Splits {@code entry#section}. */
    static String[] split(String target) {
        int hash = target.indexOf('#');
        return hash < 0 ? new String[]{target, null} : new String[]{target.substring(0, hash), target.substring(hash + 1)};
    }

    // ---- Opening -----------------------------------------------------------------------------------------------------

    /** Queued rather than opened at once, because the click that asked for it may still be closing the chat screen. */
    private static void open(String target) {
        String[] parts = split(target.strip());
        pendingEntry = resolve(parts[0]);
        pendingAnchor = parts[1];
    }

    public static void onTick(ClientTickEvent.Post event) {
        if (pendingEntry == null) return;
        String entry = pendingEntry, anchor = pendingAnchor;
        pendingEntry = pendingAnchor = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (minecraft.screen instanceof CyberwareWikiScreen wiki) {
            wiki.openLink(entry, anchor);
            return;
        }
        ClientModEvents.openArchive(minecraft, minecraft.screen, entry, anchor);
    }

    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(COMMAND)
                .then(Commands.argument("entry", StringArgumentType.greedyString())
                        .suggests((context, builder) -> {
                            load();
                            return SharedSuggestionProvider.suggest(entries.keySet(), builder);
                        })
                        .executes(context -> {
                            String target = StringArgumentType.getString(context, "entry");
                            if (resolve(split(target.strip())[0]) == null) {
                                Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("wiki.cyberneticenhancements.link.unknown", target).withColor(0xE0707A));
                                return 0;
                            }
                            open(target);
                            return 1;
                        })));
    }

    // ---- Chat --------------------------------------------------------------------------------------------------------

    /** Turns {@code #entry} and {@code #entry/section} in player chat into links, for entries that exist. */
    public static void onChat(ClientChatReceivedEvent.Player event) {
        Component message = event.getMessage();
        if (!message.getString().contains("#")) return;
        MutableComponent result = Component.empty();
        boolean[] changed = {false};
        message.visit((style, text) -> {
            Matcher matcher = CHAT.matcher(text);
            int last = 0;
            while (matcher.find()) {
                String id = resolve(matcher.group(1)), anchor = matcher.group(2) == null ? null : matcher.group(2).toLowerCase(Locale.ROOT);
                var target = id == null ? null : entry(id);
                if (target == null || anchor != null && section(target, anchor) == null) continue;
                if (matcher.start() > last) result.append(Component.literal(text.substring(last, matcher.start())).withStyle(style));
                result.append(link(id, anchor, null));
                last = matcher.end();
                changed[0] = true;
            }
            if (last < text.length()) result.append(Component.literal(text.substring(last)).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        if (changed[0]) event.setMessage(result);
    }
}
