package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TextInputFocusTest {
    @Test
    public void onlyVisibleEditableActiveFocusedFieldsBlockInventoryShortcuts() {
        EditBox field = new EditBox(null, 100, 20, Component.empty());
        assertFalse(TextInputFocus.isTyping(field));
        field.setFocused(true);
        assertTrue(TextInputFocus.isTyping(field));
        field.visible = false;
        assertFalse(TextInputFocus.isTyping(field));
        field.visible = true;
        field.active = false;
        assertFalse(TextInputFocus.isTyping(field));
        field.active = true;
        field.setEditable(false);
        assertFalse(TextInputFocus.isTyping(field));
        field.setEditable(true);
        assertTrue(TextInputFocus.isTyping(field));
    }

    @Test
    public void followsNestedFocusAndStopsProtectingAfterFocusMovesAway() {
        EditBox field = new EditBox(null, 100, 20, Component.empty());
        FocusGroup panel = new FocusGroup(field);
        FocusGroup screen = new FocusGroup(panel);
        screen.setFocused(panel);
        panel.setFocused(field);
        assertTrue(TextInputFocus.isTyping(screen));
        panel.setFocused(null);
        assertFalse(TextInputFocus.isTyping(screen));
        assertFalse(TextInputFocus.isTyping(null));
    }

    private static final class FocusGroup extends AbstractContainerEventHandler {
        private final GuiEventListener child;

        FocusGroup(GuiEventListener child) { this.child = child; }

        @Override
        public List<? extends GuiEventListener> children() { return List.of(child); }
    }
}
