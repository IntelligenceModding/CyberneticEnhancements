package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/** Shared article flow keeps measured scroll bounds identical to the rendered content. */
public final class WikiArticleLayout {
    public static final int LINE_HEIGHT = 12;
    public static final int MAX_PROSE_WIDTH = 360;
    public static final int GAP = 5;
    public static final int COLUMN_GAP = 12;
    private WikiArticleLayout() {}

    public record Block(Component label, Component value) {
        public boolean fact() { return label != null; }
        public static Block paragraph(Component text) { return new Block(null, text); }
    }
    public record Row(Block block, int y, int height, int labelWidth, int valueWidth) {}
    public record Flow(List<Row> rows, int height) {}
    @FunctionalInterface public interface LineCounter { int count(Component text, int width); }

    public static Flow layout(List<Block> blocks, int width, ToIntFunction<Component> textWidth, LineCounter lines) {
        int inner = Math.max(1, width - 12);
        int widestLabel = blocks.stream().filter(Block::fact).mapToInt(block -> textWidth.applyAsInt(block.label())).max().orElse(0);
        int labelWidth = Math.max(1, Math.min(widestLabel, Math.min(112, inner / 3)));
        List<Row> rows = new ArrayList<>();
        int y = 0;
        for (Block block : blocks) {
            int valueWidth = block.fact() ? Math.max(1, inner - labelWidth - COLUMN_GAP)
                    : Math.min(Math.max(1, width), MAX_PROSE_WIDTH);
            int count = lines.count(block.value(), valueWidth);
            if (block.fact()) count = Math.max(count, lines.count(block.label(), Math.max(1, labelWidth)));
            int height = Math.max(1, count) * LINE_HEIGHT + (block.fact() ? 8 : 0);
            rows.add(new Row(block, y, height, labelWidth, valueWidth));
            y += height + GAP;
        }
        return new Flow(List.copyOf(rows), rows.isEmpty() ? 0 : y - GAP);
    }

    /** Short label/value facts become aligned rows; explanatory sentences retain paragraph flow. */
    public static Block describe(Component text) {
        String plain = text.getString();
        int colon = plain.indexOf(':');
        if (colon < 1 || colon > 28 || colon + 1 >= plain.length()) return Block.paragraph(text);
        String label = plain.substring(0, colon);
        if (!label.chars().anyMatch(Character::isLetter) || label.matches(".*[.!?\\n].*")) return Block.paragraph(text);
        int valueStart = colon + 1;
        while (valueStart < plain.length() && Character.isWhitespace(plain.charAt(valueStart))) valueStart++;
        if (valueStart == plain.length()) return Block.paragraph(text);
        return new Block(slice(text, 0, colon), slice(text, valueStart, plain.length()));
    }

    private static Component slice(Component text, int start, int end) {
        var result = Component.empty();
        int[] position = {0};
        text.visit((style, segment) -> {
            int from = Math.max(0, start - position[0]);
            int to = Math.min(segment.length(), end - position[0]);
            if (from < to) result.append(Component.literal(segment.substring(from, to)).setStyle(style));
            position[0] += segment.length();
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}
