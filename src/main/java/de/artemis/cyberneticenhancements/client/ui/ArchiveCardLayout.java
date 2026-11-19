package de.artemis.cyberneticenhancements.client.ui;

import java.util.ArrayList;
import java.util.List;

public final class ArchiveCardLayout {
    public static final int PADDING = 10, GAP = 10, LINE = 11, BUTTON = 22;

    private ArchiveCardLayout() {}

    public record Flow(int headingY, int subtitleY, int dividerY, List<Integer> paragraphs, int footerY, int buttonY, int height) {}

    public static Flow flow(int headingLines, int subtitleLines, List<Integer> paragraphLines, boolean actions) {
        int heading = PADDING;
        int subtitle = heading + Math.max(1, headingLines) * LINE + GAP;
        int divider = subtitle + Math.max(1, subtitleLines) * LINE + GAP;
        int end = divider + 1;
        var paragraphs = new ArrayList<Integer>();
        for (int count : paragraphLines) {
            int start = end + GAP;
            paragraphs.add(start);
            end = start + Math.max(1, count) * LINE;
        }
        int footer = actions ? end + GAP : -1;
        int button = actions ? footer + 1 + GAP : -1;
        return new Flow(heading, subtitle, divider, List.copyOf(paragraphs), footer, button, (actions ? button + BUTTON : end) + PADDING);
    }
}
