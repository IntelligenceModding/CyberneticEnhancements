package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.WikiSearch;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import static de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.*;

final class WikiSearchIndex {
    private WikiSearchIndex() {}

    static List<WikiSearch.Document> build(List<ArchiveTab> tabs) {
        var documents = new ArrayList<WikiSearch.Document>();
        tabs.stream().filter(tab -> tab.id().equals("wiki")).forEach(tab -> collect(tab.topics(), "", documents));
        return List.copyOf(documents);
    }

    private static void collect(List<WikiTopic> topics, String parent, List<WikiSearch.Document> documents) {
        for (var topic : topics) {
            String path = parent.isEmpty() ? topic.title().getString() : parent + " / " + topic.title().getString();
            if (topic.section() != null) for (var entry : topic.section().entries()) documents.add(document(entry, path));
            collect(topic.children(), path, documents);
        }
    }

    static WikiSearch.Document document(WikiEntry entry, String category) {
        var passages = new ArrayList<String>();
        entry.chips().forEach(chip -> passages.add(chip.getString()));
        for (var section : entry.sections()) {
            passages.add(section.title().getString());
            section.lines().forEach(line -> passages.add(line.getString()));
            if (section.visual() != null) appendVisual(section.visual(), passages);
        }
        return new WikiSearch.Document(entry.id(), entry.title().getString(), category,
                entry.badge().getString() + " · " + entry.subtitle().getString(), passages);
    }

    private static void appendItem(ItemStack item, List<String> text) {
        if (item != null && !item.isEmpty()) text.add(item.getHoverName().getString());
    }

    private static void appendVisual(SectionVisual visual, List<String> text) {
        switch (visual) {
            case ReferenceTableVisual table -> table.groups().forEach(group -> {
                text.add(group.title().getString());
                group.rows().forEach(row -> text.add((row.fact() ? row.label().getString() + ": " : "") + row.value().getString()));
                group.notes().forEach(note -> text.add(note.getString()));
            });
            case CraftingGridVisual grid -> {
                // Encrypted designs must not leak ingredients or outputs through search.
                if (grid.unlocked()) {
                    appendItem(grid.output(), text);
                    for (var cells : grid.grids()) for (var item : cells) appendItem(item, text);
                }
            }
            case CompactTransformVisual transform -> {
                appendItem(transform.input(), text);
                transform.outputs().forEach(item -> appendItem(item, text));
            }
            case ServiceOperationsVisual service -> {
                service.summaryChips().forEach(chip -> text.add(chip.getString()));
                service.operations().forEach(operation -> {
                    text.add(operation.label().getString() + ": " + operation.note().getString());
                    appendItem(operation.input(), text);
                    appendItem(operation.output(), text);
                    operation.materials().forEach(item -> appendItem(item, text));
                });
                service.upgradeStages().forEach(stage -> {
                    text.add(stage.label().getString());
                    stage.changes().forEach(change -> text.add(change.getString()));
                    stage.materials().forEach(item -> appendItem(item, text));
                });
            }
            case RelicCacheExampleVisual ignored -> { /* Puzzle tokens are not article content. */ }
            case StructureViewVisual ignored -> { /* A picture, not text. */ }
        }
    }
}
