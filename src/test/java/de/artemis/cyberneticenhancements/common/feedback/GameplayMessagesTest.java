package de.artemis.cyberneticenhancements.common.feedback;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.Test;
import static org.junit.Assert.*;

public class GameplayMessagesTest {
    @Test public void formatsNestedArgumentsAndKeepsClickableCoordinates() {
        var position = Component.literal("12, 64, -30").withStyle(style -> style.withColor(0xFF0000)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp 12 64 -30")));
        var message = Component.translatable("message.cyberneticenhancements.test", position).withColor(0xFFFF00);
        message.append(Component.literal(" extra").withColor(0x00FF00));
        var formatted = GameplayMessages.format(message);
        assertEquals(GameplayMessages.ACCENT, formatted.getStyle().getColor().getValue());
        var wrapper = (TranslatableContents) formatted.getContents();
        assertEquals("message.cyberneticenhancements.gameplay", wrapper.getKey());
        var body = (Component) wrapper.getArgs()[0];
        assertEquals(GameplayMessages.TEXT, body.getStyle().getColor().getValue());
        var destination = (Component) ((TranslatableContents) body.getContents()).getArgs()[0];
        assertEquals(GameplayMessages.TEXT, destination.getStyle().getColor().getValue());
        assertEquals(position.getStyle().getClickEvent(), destination.getStyle().getClickEvent());
        assertEquals(GameplayMessages.TEXT, body.getSiblings().getFirst().getStyle().getColor().getValue());
        assertEquals(0xFF0000, position.getStyle().getColor().getValue());
        assertEquals(0xFFFF00, message.getStyle().getColor().getValue());
    }

    @Test public void doesNotDuplicatePrefixesAndRetainsFallbackTranslations() {
        var source = Component.translatableWithFallback("example.message", "Found %s", Component.literal("cache"));
        var formatted = GameplayMessages.format(source);
        assertSame(formatted, GameplayMessages.format(formatted));
        var body = (Component) ((TranslatableContents) formatted.getContents()).getArgs()[0];
        assertEquals("Found %s", ((TranslatableContents) body.getContents()).getFallback());
    }
}
