package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.Test;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import static org.junit.Assert.*;

public class WikiArticleLayoutTest {
    @Test public void factsSeparateLabelsWithoutLosingStyledValues() {
        var text = Component.literal("Capacity: ").append(Component.literal("+7 Chrome").withStyle(style -> style.withColor(0x8BDAF8)));
        var fact = WikiArticleLayout.describe(text);
        assertTrue(fact.fact());
        assertEquals("Capacity", fact.label().getString());
        assertEquals("+7 Chrome", fact.value().getString());
        List<Integer> colors = new ArrayList<>();
        fact.value().visit((style, segment) -> {
            if (!segment.isEmpty() && style.getColor() != null) colors.add(style.getColor().getValue());
            return Optional.empty();
        }, Style.EMPTY);
        assertEquals(List.of(0x8BDAF8), colors);
    }

    @Test public void narrativeSentencesAndTimesStayParagraphs() {
        for (String text : List.of("Restores integrity after each completed cycle.", "12:30", "First, prepare. Next: start the cycle.",
                "This is a long explanation before the colon: continue reading.", "Materials:   ")) {
            var block = WikiArticleLayout.describe(Component.literal(text));
            assertFalse(text, block.fact());
            assertEquals(text, block.value().getString());
        }
    }

    @Test public void narrowAndWideFlowsKeepEveryRowInsideTheMeasuredBounds() {
        var blocks = List.of(
                WikiArticleLayout.describe(Component.literal("Payout: 950-1093€$")),
                WikiArticleLayout.describe(Component.literal("Supplies: 4 x Chromium Alloy Ingots")),
                WikiArticleLayout.Block.paragraph(Component.literal("Explanatory text ".repeat(20))));
        for (int width : new int[]{96, 180, 380, 640}) {
            var flow = WikiArticleLayout.layout(blocks, width, text -> text.getString().length() * 6,
                    (text, wrapWidth) -> Math.max(1, (text.getString().length() * 6 + wrapWidth - 1) / wrapWidth));
            int previousBottom = -WikiArticleLayout.GAP;
            for (var row : flow.rows()) {
                assertEquals(previousBottom + WikiArticleLayout.GAP, row.y());
                assertTrue(row.valueWidth() > 0);
                if (row.block().fact()) assertTrue(row.labelWidth() + WikiArticleLayout.COLUMN_GAP + row.valueWidth() + 12 <= width);
                else assertTrue(row.valueWidth() <= WikiArticleLayout.MAX_PROSE_WIDTH);
                previousBottom = row.y() + row.height();
            }
            assertEquals(previousBottom, flow.height());
        }
        assertEquals(0, WikiArticleLayout.layout(List.of(), 100, text -> 0, (text, width) -> 0).height());
    }
}
