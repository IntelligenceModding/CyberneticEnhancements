package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;

public final class TextInputFocus {
    private TextInputFocus() {}

    public static boolean isTyping(GuiEventListener listener) {
        while (listener != null) {
            if (listener instanceof EditBox field) {
                return field.isVisible() && field.canConsumeInput();
            }
            if (listener instanceof MultiLineEditBox field) {
                return field.visible && field.active && field.isFocused();
            }
            listener = listener instanceof ContainerEventHandler container ? container.getFocused() : null;
        }
        return false;
    }
}
