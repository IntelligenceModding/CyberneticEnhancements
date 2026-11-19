package de.artemis.cyberneticenhancements.client.ui;

import java.util.ArrayList;
import java.util.List;

/** Positions are relative to the panel's top edge. Text is measured before laying out the panel. */
public record CallHudLayout(int portraitY, int dialogueY, int dialogueHeight, List<Row> rows, int footerY, int height) {
    public static final int PORTRAIT_HEIGHT = 90, LINE_HEIGHT = 11, GAP = 6;
    public record Row(int y, int height) {}

    public static CallHudLayout connected(int dialogueLines, int[] choiceLines, boolean playerCall, boolean ended) {
        int portraitY = NeuralLinkStyle.CONTENT_TOP;
        int dialogueY = ended ? portraitY : portraitY + PORTRAIT_HEIGHT + GAP;
        int dialogueHeight = ended ? Math.max(64, dialogueLines * LINE_HEIGHT + 16) : Math.max(3, dialogueLines) * LINE_HEIGHT + 12;
        int cursor = dialogueY + dialogueHeight + GAP;
        var rows = new ArrayList<Row>();
        if (!ended) for (int lines : choiceLines) {
            int height = lines == 0 ? 0 : Math.max(22, lines * LINE_HEIGHT + 10);
            rows.add(new Row(cursor, height));
            if (height > 0) cursor += height + GAP;
        }
        int footerY = cursor;
        if (ended || playerCall) cursor += 16;
        return new CallHudLayout(portraitY, dialogueY, dialogueHeight, List.copyOf(rows), footerY, cursor + NeuralLinkStyle.INSET);
    }

    public static int keyWidth(int measuredWidth, int innerWidth) { return Math.clamp(measuredWidth + 8, 38, Math.max(38, innerWidth / 3)); }
}
