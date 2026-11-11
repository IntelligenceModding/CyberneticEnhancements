package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.network.chat.Component;

public final class UiCloseControl {
    public static final int WIDTH = 84, HEIGHT = 18;

    private UiCloseControl() {
    }

    public static Component label() {
        return Component.translatable("screen.cyberneticenhancements.close");
    }
}
