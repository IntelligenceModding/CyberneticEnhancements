package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.screen.FixerDialogScreen;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiScreen;
import de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.ArrayList;
import java.util.Locale;

public final class GameplayHudVisibility {
    public enum Context { GAMEPLAY, CHAT, INVENTORY, PAUSE, ARCHIVE, OTHER }
    public enum Element {
        CYBERWARE, COMPASS, QUESTS, CONTACTS, CALLS, PSYCHOSIS_TIMER, TAROT, TUTORIAL,
        HOTBAR, VITALS, EFFECTS, BOSS_BARS, CHAT, NOTIFICATIONS
    }
    public enum Choice { DEFAULT, SHOW, HIDE }

    private GameplayHudVisibility() {}

    public static boolean gameplay() {
        return status() && Minecraft.getInstance().screen == null;
    }

    public static boolean status() {
        var minecraft = Minecraft.getInstance();
        return available(minecraft) && (minecraft.screen == null || minecraft.screen instanceof ChatScreen);
    }

    public static Context context() {
        var minecraft = Minecraft.getInstance();
        var screen = minecraft.screen;
        if (screen == null) return Context.GAMEPLAY;
        if (screen instanceof ChatScreen) return Context.CHAT;
        if (screen instanceof PauseScreen) return Context.PAUSE;
        if (screen instanceof CyberwareWikiScreen) return Context.ARCHIVE;
        if (screen instanceof AbstractContainerScreen<?> && !(screen instanceof FixerDialogScreen)) return Context.INVENTORY;
        return Context.OTHER;
    }

    public static Choice choice(Context context, Element element) {
        if (!ClientConfig.SPEC.isLoaded() || locked(element)) return Choice.DEFAULT;
        String key = key(context, element);
        var values = ClientConfig.HUD_VISIBILITY_OVERRIDES.get();
        if (values.contains(key + "hide")) return Choice.HIDE;
        if (values.contains(key + "show")) return Choice.SHOW;
        return Choice.DEFAULT;
    }

    public static void set(Context context, Element element, Choice choice) {
        if (locked(element)) return;
        String key = key(context, element);
        var values = new ArrayList<String>(ClientConfig.HUD_VISIBILITY_OVERRIDES.get());
        values.removeIf(value -> value.startsWith(key));
        if (choice != Choice.DEFAULT) values.add(key + choice.name().toLowerCase(Locale.ROOT));
        ClientConfig.HUD_VISIBILITY_OVERRIDES.set(values);
        ClientConfig.SPEC.save();
    }

    public static void reset(Context context) {
        var values = new ArrayList<String>(ClientConfig.HUD_VISIBILITY_OVERRIDES.get());
        for (var element : Element.values()) {
            if (!locked(element)) values.removeIf(value -> value.startsWith(key(context, element)));
        }
        ClientConfig.HUD_VISIBILITY_OVERRIDES.set(values);
        ClientConfig.SPEC.save();
    }

    private static String key(Context context, Element element) {
        return (context.name() + "/" + element.name() + "=").toLowerCase(Locale.ROOT);
    }

    public static boolean locked(Element element) {
        boolean masterLocked = HudVisibilityController.enforced();
        return switch (element) {
            case CYBERWARE, QUESTS -> masterLocked;
            case COMPASS -> masterLocked || EnforcedSettings.locked(ClientConfig.SHOW_COMPASS);
            case PSYCHOSIS_TIMER -> EnforcedSettings.locked(ClientConfig.SHOW_CYBERPSYCHOSIS_TIMER);
            case CALLS -> EnforcedSettings.locked(ClientConfig.ENABLE_PLAYER_CALLS);
            default -> false;
        };
    }

    public static boolean defaultVisible(Context context, Element element) {
        boolean enabled = switch (element) {
            case CYBERWARE, QUESTS -> HudVisibilityController.isHudEnabled();
            case COMPASS -> ClientConfig.SHOW_COMPASS.get() && HudVisibilityController.isHudEnabled();
            case PSYCHOSIS_TIMER -> ClientConfig.SHOW_CYBERPSYCHOSIS_TIMER.get();
            default -> true;
        };
        return resolve(context, element, Choice.DEFAULT, enabled);
    }

    public static boolean shows(Element element, boolean enabledBySettings) {
        if (!available(Minecraft.getInstance())) return false;
        Context context = context();
        return resolve(context, element, choice(context, element), enabledBySettings);
    }

    static boolean resolve(Context context, Element element, Choice choice, boolean enabledBySettings) {
        return switch (choice) {
            case SHOW -> true;
            case HIDE -> false;
            case DEFAULT -> enabledBySettings && shownByDefault(context, element);
        };
    }

    private static boolean shownByDefault(Context context, Element element) {
        return switch (context) {
            case GAMEPLAY -> true;
            case CHAT -> switch (element) {
                case QUESTS, CONTACTS, CALLS, TAROT, TUTORIAL -> false;
                default -> true;
            };
            case INVENTORY -> element == Element.COMPASS;
            default -> false;
        };
    }

    static void renderToggleable(Element element, boolean enabledBySettings, GuiGraphics graphics, Runnable draw) {
        if (!shows(element, enabledBySettings)) return;
        if (choice(context(), element) == Choice.SHOW) draw.run();
        else HudVisibilityController.render(graphics, draw);
    }

    private static boolean available(Minecraft minecraft) {
        return minecraft != null && minecraft.player != null && minecraft.level != null && minecraft.player.isAlive()
                && !minecraft.options.hideGui && !FlatlinedPresentation.hidesGameplayUi();
    }
}
