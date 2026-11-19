package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class CallHudLayoutTest {
    @Test public void wrappedDialogueAndChoicesStayInsideThePanelWithoutOverlapping() {
        for (int lines : new int[]{1, 3, 8, 20}) {
            var layout = CallHudLayout.connected(lines, new int[]{1, 3, 0, 0, 2}, true, false);
            assertTrue(layout.dialogueY() >= layout.portraitY() + CallHudLayout.PORTRAIT_HEIGHT + CallHudLayout.GAP);
            int bottom = layout.dialogueY() + layout.dialogueHeight();
            for (var row : layout.rows()) {
                if (row.height() == 0) continue;
                assertTrue(row.y() >= bottom + CallHudLayout.GAP);
                bottom = row.y() + row.height();
            }
            assertTrue(layout.footerY() >= bottom + CallHudLayout.GAP);
            assertTrue(layout.height() >= layout.footerY() + 16 + NeuralLinkStyle.INSET);
        }
    }

    @Test public void shortPlayerVoiceStatusChangesDoNotMoveTheControls() {
        var quiet = CallHudLayout.connected(1, new int[]{1, 1, 0, 0, 1}, true, false);
        var speaking = CallHudLayout.connected(3, new int[]{1, 1, 0, 0, 1}, true, false);
        assertEquals(quiet, speaking);
    }

    @Test public void endedCallsDoNotLeaveSpaceForAPortraitOrInvisibleChoices() {
        var ended = CallHudLayout.connected(8, new int[]{2, 3, 4}, false, true);
        assertEquals(NeuralLinkStyle.CONTENT_TOP, ended.dialogueY());
        assertTrue(ended.rows().isEmpty());
        assertTrue(ended.height() >= ended.footerY() + 16);
    }

    @Test public void unusuallyLongReboundKeysCannotTakeOverTheLabelColumn() {
        assertTrue(CallHudLayout.keyWidth(400, 194) <= 194 / 3);
        assertTrue(CallHudLayout.keyWidth(25, 194) >= 25 + 8);
    }
}
