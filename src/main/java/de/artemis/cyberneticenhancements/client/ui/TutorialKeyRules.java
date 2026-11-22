package de.artemis.cyberneticenhancements.client.ui;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Which key mappings work in the tutorial world. Rules are per action (the mapping's name), never per physical key,
 * so rebinding keys changes nothing, and any mapping not listed here, including every other mod's, is blocked.
 */
public final class TutorialKeyRules {
    /** Movement and the basic interactions the tutorial always needs, plus harmless screenshots. */
    private static final Set<String> ALWAYS = Set.of("key.forward", "key.left", "key.back", "key.right", "key.jump", "key.sneak",
            "key.sprint", "key.attack", "key.use", "key.screenshot");
    /** Mappings that unlock together with the interface element that introduces them. */
    private static final Map<String, String> BY_ELEMENT = Map.ofEntries(
            Map.entry("key.hotbar.1", "hotbar"), Map.entry("key.hotbar.2", "hotbar"), Map.entry("key.hotbar.3", "hotbar"),
            Map.entry("key.hotbar.4", "hotbar"), Map.entry("key.hotbar.5", "hotbar"), Map.entry("key.hotbar.6", "hotbar"),
            Map.entry("key.hotbar.7", "hotbar"), Map.entry("key.hotbar.8", "hotbar"), Map.entry("key.hotbar.9", "hotbar"),
            Map.entry("key.swapOffhand", "hotbar"), Map.entry("key.drop", "hotbar"),
            Map.entry("key.inventory", "inventory"),
            Map.entry("key.chat", "chat"), Map.entry("key.command", "chat"),
            Map.entry("key.cyberneticenhancements.open_archive", "archive"),
            Map.entry("key.cyberneticenhancements.open_map", "archive"),
            Map.entry("key.cyberneticenhancements.open_contacts", "contacts"),
            Map.entry("key.cyberneticenhancements.add_compass_marker", "markers"),
            Map.entry("key.cyberneticenhancements.toggle_hud", "cyberware"),
            Map.entry("key.cyberneticenhancements.activate_cyberware", "activation"),
            Map.entry("key.cyberneticenhancements.activate_auxiliary_cyberware", "activation"),
            Map.entry("key.cyberneticenhancements.activate_arm_cyberware", "activation"),
            Map.entry("key.cyberneticenhancements.activate_face_cyberware", "activation"),
            Map.entry("key.cyberneticenhancements.scan_tarot", "scan"),
            Map.entry("key.cyberneticenhancements.end_call", "calls"),
            Map.entry("key.cyberneticenhancements.call_answer_1", "calls"), Map.entry("key.cyberneticenhancements.call_answer_2", "calls"),
            Map.entry("key.cyberneticenhancements.call_answer_3", "calls"), Map.entry("key.cyberneticenhancements.call_answer_4", "calls"),
            Map.entry("key.cyberneticenhancements.call_answer_5", "calls"),
            Map.entry("key.cyberneticenhancements.next_quest_page", "quests"),
            Map.entry("key.cyberneticenhancements.call_quest_issuer_1", "quests"), Map.entry("key.cyberneticenhancements.call_quest_issuer_2", "quests"),
            Map.entry("key.cyberneticenhancements.call_quest_issuer_3", "quests"));

    private TutorialKeyRules() {}

    /** {@code revealed} answers for interface elements and for single actions written as {@code key:<mapping name>}. */
    public static boolean blocked(String mapping, boolean restricting, Predicate<String> revealed) {
        if (!restricting || ALWAYS.contains(mapping) || revealed.test("key:" + mapping)) return false;
        String element = BY_ELEMENT.get(mapping);
        return element == null || !revealed.test(element);
    }

    /** Whether the tutorial normally unlocks this action at some point, as opposed to leaving it off for the whole run. */
    public static boolean unlockable(String mapping) { return BY_ELEMENT.containsKey(mapping); }

    /** The interface elements that unlock keys, so the script's element list can be checked against this table. */
    public static Set<String> elements() { return Set.copyOf(BY_ELEMENT.values()); }
}
