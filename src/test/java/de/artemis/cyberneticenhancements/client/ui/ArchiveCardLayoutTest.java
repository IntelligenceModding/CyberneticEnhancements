package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ArchiveCardLayoutTest {
    @Test
    public void wrappedSectionsAndActionsAlwaysHaveTheSamePaddingAndSeparation() {
        for (int title : new int[]{1, 2, 5}) for (int issuer : new int[]{1, 3}) for (boolean active : new boolean[]{false, true}) {
            var counts = List.of(1, 4, 2);
            var flow = ArchiveCardLayout.flow(title, issuer, counts, active);
            assertEquals(10, flow.headingY());
            assertEquals(10, flow.subtitleY() - flow.headingY() - title * ArchiveCardLayout.LINE);
            assertEquals(10, flow.dividerY() - flow.subtitleY() - issuer * ArchiveCardLayout.LINE);
            int end = flow.dividerY() + 1;
            for (int index = 0; index < counts.size(); index++) {
                assertEquals(10, flow.paragraphs().get(index) - end);
                end = flow.paragraphs().get(index) + counts.get(index) * ArchiveCardLayout.LINE;
            }
            if (active) {
                assertEquals(10, flow.footerY() - end);
                assertEquals(10, flow.buttonY() - flow.footerY() - 1);
                assertEquals(10, flow.height() - flow.buttonY() - ArchiveCardLayout.BUTTON);
            } else assertEquals(10, flow.height() - end);
        }
    }

    @Test
    public void contactPortraitDetailsAndFooterShareTheMeasuredCardRules() {
        assertEquals(10, ArchiveContactLayout.FOOTER_Y - ArchiveContactLayout.BODY_TOP - ArchiveContactLayout.PORTRAIT_SIZE);
        assertEquals(10, ArchiveContactLayout.BUTTON_Y - ArchiveContactLayout.FOOTER_Y - 1);
        assertEquals(10, ArchiveContactLayout.CARD_HEIGHT - ArchiveContactLayout.BUTTON_Y - ArchiveCardLayout.BUTTON);
        assertTrue(3 * 16 + 9 <= ArchiveContactLayout.PORTRAIT_SIZE);
    }
}
