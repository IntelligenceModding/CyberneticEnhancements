package de.artemis.cyberneticenhancements.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import java.util.List;
import java.util.stream.IntStream;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.cyberneticenhancements";
    public static final KeyMapping ADD_COMPASS_MARKER = new KeyMapping("key.cyberneticenhancements.add_compass_marker",
            net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,KeyModifier.ALT,
            InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_M,CATEGORY);
    public static final String ABILITY_OS = "activate_cyberware";
    public static final String ABILITY_AUX = "activate_auxiliary_cyberware";
    public static final String ABILITY_ARMS = "activate_arm_cyberware";
    public static final String ABILITY_FACE = "activate_face_cyberware";
    private static final IKeyConflictContext CALL_CONTEXT = new IKeyConflictContext() {
        @Override
        public boolean isActive() { return GameplayHudVisibility.gameplay() && (NpcCallOverlay.isVisible() || NpcContactsOverlay.isActive()); }
        @Override
        public boolean conflicts(IKeyConflictContext other) { return other == this; }
    };
    public static final List<KeyMapping> CALL_ANSWERS = IntStream.range(0, 5).mapToObj(index -> new KeyMapping(
            "key.cyberneticenhancements.call_answer_" + (index + 1), CALL_CONTEXT, KeyModifier.ALT,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_1 + index, CATEGORY)).toList();
    public static final KeyMapping END_CALL = new KeyMapping("key.cyberneticenhancements.end_call", new IKeyConflictContext() {
        @Override public boolean isActive() { return GameplayHudVisibility.gameplay() && (NpcCallOverlay.isActive() || NpcContactsOverlay.isActive()); }
        @Override public boolean conflicts(IKeyConflictContext other) { return other == this; }
    }, KeyModifier.ALT,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_0, CATEGORY);
    public static final KeyMapping TOGGLE_PLAYER_CALL_WINDOW = new KeyMapping("key.cyberneticenhancements.toggle_player_call_window", new IKeyConflictContext() {
        @Override public boolean isActive() { return GameplayHudVisibility.gameplay() && NpcCallOverlay.canTogglePlayerWindow(); }
        @Override public boolean conflicts(IKeyConflictContext other) { return other == this; }
    }, KeyModifier.ALT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_6, CATEGORY);
    public static final KeyMapping NEXT_QUEST_PAGE = new KeyMapping("key.cyberneticenhancements.next_quest_page", new IKeyConflictContext() {
        @Override public boolean isActive() { return QuestProgressOverlay.canPage(); }
        @Override public boolean conflicts(IKeyConflictContext other) { return other == this; }
    }, KeyModifier.ALT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_4, CATEGORY);
    public static final List<KeyMapping> CALL_QUEST_ISSUERS = IntStream.range(0, 3).mapToObj(index -> new KeyMapping(
            "key.cyberneticenhancements.call_quest_issuer_" + (index + 1), new IKeyConflictContext() {
                @Override public boolean isActive() { return QuestProgressOverlay.canCall(index); }
                @Override public boolean conflicts(IKeyConflictContext other) { return other == this; }
            }, KeyModifier.ALT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_1 + index, CATEGORY)).toList();
    public static final KeyMapping DIALOGUE_UP = new KeyMapping("key.cyberneticenhancements.dialogue_up",
            net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP, CATEGORY);
    public static final KeyMapping DIALOGUE_DOWN = new KeyMapping("key.cyberneticenhancements.dialogue_down",
            net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, CATEGORY);
    public static final KeyMapping DIALOGUE_SELECT = new KeyMapping("key.cyberneticenhancements.dialogue_select",
            net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, CATEGORY);
    private static KeyMapping dialogueWindow(String name, int key) {
        return new KeyMapping("key.cyberneticenhancements.dialogue_" + name,
                net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    public static final List<KeyMapping> DIALOGUE_WINDOWS = List.of(
            dialogueWindow("shop", GLFW.GLFW_KEY_Z), dialogueWindow("services", GLFW.GLFW_KEY_X),
            dialogueWindow("contracts", GLFW.GLFW_KEY_R), dialogueWindow("customize", GLFW.GLFW_KEY_N));
    public static final KeyMapping OPEN_CONTACTS = new KeyMapping("key.cyberneticenhancements.open_contacts",
            net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping ACTIVATE_CYBERWARE = new KeyMapping(
            "key.cyberneticenhancements.activate_cyberware",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );
    public static final KeyMapping ACTIVATE_AUXILIARY_CYBERWARE = new KeyMapping(
            "key.cyberneticenhancements.activate_auxiliary_cyberware",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );
    public static final KeyMapping ACTIVATE_ARM_CYBERWARE = new KeyMapping(
            "key.cyberneticenhancements.activate_arm_cyberware",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );
    public static final KeyMapping ACTIVATE_FACE_CYBERWARE = new KeyMapping(
            "key.cyberneticenhancements.activate_face_cyberware",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );
    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.cyberneticenhancements.toggle_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
    );
    public static final KeyMapping TOGGLE_MINIMAP = new KeyMapping(
            "key.cyberneticenhancements.toggle_minimap",
            net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );
    public static final KeyMapping OPEN_ARCHIVE = new KeyMapping(
            "key.cyberneticenhancements.open_archive",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    );

    public static final KeyMapping OPEN_MAP = new KeyMapping(
            "key.cyberneticenhancements.open_map",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    public static final KeyMapping SCAN_TAROT = new KeyMapping("key.cyberneticenhancements.scan_tarot", new IKeyConflictContext() {
        @Override public boolean isActive() { return TarotScanClient.available(); }
        @Override public boolean conflicts(IKeyConflictContext other) { return other == this; }
    }, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORY);

    private ModKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ADD_COMPASS_MARKER);
        event.register(ACTIVATE_CYBERWARE);
        event.register(ACTIVATE_AUXILIARY_CYBERWARE);
        event.register(ACTIVATE_ARM_CYBERWARE);
        event.register(ACTIVATE_FACE_CYBERWARE);
        event.register(TOGGLE_HUD);
        event.register(TOGGLE_MINIMAP);
        event.register(OPEN_ARCHIVE);
        event.register(OPEN_MAP);
        event.register(SCAN_TAROT);
        event.register(OPEN_CONTACTS);
        event.register(DIALOGUE_UP);
        event.register(DIALOGUE_DOWN);
        event.register(DIALOGUE_SELECT);
        DIALOGUE_WINDOWS.forEach(event::register);
        event.register(NEXT_QUEST_PAGE);
        CALL_QUEST_ISSUERS.forEach(event::register);
        CALL_ANSWERS.forEach(event::register);
        event.register(END_CALL);
        event.register(TOGGLE_PLAYER_CALL_WINDOW);
    }

    public static Component getAbilityBindingLabel(String bindingId) {
        return switch (bindingId) {
            case ABILITY_OS -> ACTIVATE_CYBERWARE.getTranslatedKeyMessage();
            case ABILITY_AUX -> ACTIVATE_AUXILIARY_CYBERWARE.getTranslatedKeyMessage();
            case ABILITY_ARMS -> ACTIVATE_ARM_CYBERWARE.getTranslatedKeyMessage();
            case ABILITY_FACE -> ACTIVATE_FACE_CYBERWARE.getTranslatedKeyMessage();
            default -> Component.empty();
        };
    }
}
