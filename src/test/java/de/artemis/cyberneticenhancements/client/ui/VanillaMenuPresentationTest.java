package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class VanillaMenuPresentationTest {
    @Test public void chatAndCommandEntryScreensKeepTheirNativeColors() {
        for (Class<?> screen : new Class<?>[]{
                net.minecraft.client.gui.screens.ChatScreen.class,
                net.minecraft.client.gui.screens.InBedChatScreen.class,
                net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen.class,
                net.minecraft.client.gui.screens.inventory.MinecartCommandBlockEditScreen.class}) {
            assertFalse(screen.getName(), TitleScreenPresentation.supportsMenuTheme(screen));
        }
        assertTrue(TitleScreenPresentation.supportsMenuTheme(net.minecraft.client.gui.screens.options.OptionsScreen.class));
        assertTrue(TitleScreenPresentation.supportsMenuTheme(net.minecraft.client.gui.screens.GenericMessageScreen.class));
    }

    @Test public void neutralTextIsReadableAndRetainsItsOpacity() {
        assertEquals(0x80FFD5DC, VanillaMenuPresentation.textColor(0x80FFFFFF));
        assertEquals(0xFFD5A6B0, VanillaMenuPresentation.textColor(0xFF808080));
        assertEquals(0xFFD5A6B0, VanillaMenuPresentation.textColor(0xFFCCCCCC));
        assertEquals(0x00FFD5DC, VanillaMenuPresentation.textColor(0x00E0E0E0));
        assertTrue(contrast(VanillaMenuPresentation.TEXT, VanillaMenuPresentation.FIELD) > 7);
        assertTrue(contrast(VanillaMenuPresentation.MUTED, VanillaMenuPresentation.FIELD) > 4.5);
    }

    @Test public void warningSuccessAndCustomColorsRetainTheirMeaning() {
        for (int color : new int[]{0xFFFF5555, 0xFFFFFF55, 0xFF55FF55, 0xFF5599FF, 0x713541, 0xFF000000})
            assertEquals(color, VanillaMenuPresentation.textColor(color));
    }

    private static double contrast(int a, int b) {
        double first = luminance(a), second = luminance(b);
        return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05);
    }
    private static double luminance(int color) {
        return channel(color >> 16) * 0.2126 + channel(color >> 8) * 0.7152 + channel(color) * 0.0722;
    }
    private static double channel(int color) {
        double value = (color & 255) / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
