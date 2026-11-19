package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.entity.NpcType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class NpcDialogueTrees {
    private static final String PREFIX = "screen.cyberneticenhancements.npc.dialogue.";
    private static final Map<NpcType, NpcDialogueTree> TREES = buildTrees();

    private NpcDialogueTrees() {
    }

    private static Map<NpcType, NpcDialogueTree> buildTrees() {
        Map<NpcType, NpcDialogueTree> trees = new EnumMap<>(NpcType.class);
        for (NpcType type : NpcType.values()) {
            int root = type.ordinal() * 100;
            int first = 1000 + root;
            List<NpcDialogueChoice> actions = new ArrayList<>();
            actions.add(choice(type, first, "shop", root, NpcDialogueView.SHOP, 0, ""));
            actions.add(choice(type, first + 1, "services", root, NpcDialogueView.SERVICES, 0, ""));
            if (type.supportsContracts()) actions.add(choice(type, first + 2, "contracts", root, NpcDialogueView.CONTRACTS, 0, ""));
            if (type.supportsIntel()) actions.add(choice(type, first + 3, "cache", root, NpcDialogueView.CACHE_INTEL, 0, ""));
            actions.add(choice(type, first + 4, "talk", root + 1, NpcDialogueView.DIALOGUE, 0, ""));
            trees.put(type, new NpcDialogueTree(root, Map.of(
                    root, new NpcDialogueNode(root, line(type, "root"), List.copyOf(actions)),
                    root + 1, new NpcDialogueNode(root + 1, line(type, "about"), List.of(
                            choice(type, first + 10, "work", root + 2, NpcDialogueView.DIALOGUE, 0, ""),
                            choice(type, first + 11, "advice", root + 3, NpcDialogueView.DIALOGUE, 0, ""),
                            choice(type, first + 12, "trust", root + 4, NpcDialogueView.DIALOGUE, 0, ""),
                            choice(type, first + 13, "goodbye", root, NpcDialogueView.CLOSE, 0, ""))),
                    root + 2, new NpcDialogueNode(root + 2, line(type, "work"), List.of(
                            choice(type, first + 20, "shop", root, NpcDialogueView.SHOP, 0, ""),
                            choice(type, first + 21, "services", root, NpcDialogueView.SERVICES, 0, ""),
                            choice(type, first + 22, "terms", root + 5, NpcDialogueView.DIALOGUE, 0, ""),
                            choice(type, first + 23, "back", root, NpcDialogueView.DIALOGUE, 0, ""))),
                    root + 3, new NpcDialogueNode(root + 3, line(type, "advice"), List.of(
                            choice(type, first + 30, "thanks", root, NpcDialogueView.DIALOGUE, 1, type.id() + "_advice_acknowledged"),
                            choice(type, first + 31, "work", root + 2, NpcDialogueView.DIALOGUE, 0, ""),
                            choice(type, first + 32, "terms", root + 5, NpcDialogueView.DIALOGUE, 0, ""),
                            choice(type, first + 33, "back", root, NpcDialogueView.DIALOGUE, 0, ""))),
                    root + 4, new NpcDialogueNode(root + 4, line(type, "trust"), List.of(
                            choice(type, first + 40, "understood", root, NpcDialogueView.DIALOGUE, 1, type.id() + "_trust_acknowledged"),
                            choice(type, first + 41, "terms", root + 5, NpcDialogueView.DIALOGUE, 0, ""),
                            choice(type, first + 42, "shop", root, NpcDialogueView.SHOP, 0, ""),
                            choice(type, first + 43, "back", root, NpcDialogueView.DIALOGUE, 0, ""))),
                    root + 5, new NpcDialogueNode(root + 5, line(type, "terms"), List.of(
                            choice(type, first + 50, "agreed", root, NpcDialogueView.DIALOGUE, 1, type.id() + "_terms_acknowledged"),
                            choice(type, first + 51, "shop", root, NpcDialogueView.SHOP, 0, ""),
                            choice(type, first + 52, "services", root, NpcDialogueView.SERVICES, 0, ""),
                            choice(type, first + 53, "back", root, NpcDialogueView.DIALOGUE, 0, ""))))));
        }
        trees.put(NpcType.WORLD_MED_TECH, encounterTree(NpcType.WORLD_MED_TECH));
        var guard = trees.get(NpcType.MAXTEC);
        var guardNodes = new java.util.HashMap<Integer, NpcDialogueNode>();
        guard.nodes().forEach((id, node) -> guardNodes.put(id, new NpcDialogueNode(id, node.npcLineKey(),
                node.choices().stream().filter(choice -> choice.view() != NpcDialogueView.SHOP && choice.view() != NpcDialogueView.SERVICES).toList())));
        trees.put(NpcType.MAXTEC, new NpcDialogueTree(guard.rootNodeId(), Map.copyOf(guardNodes)));
        return Map.copyOf(trees);
    }

    /** A world encounter's contact only explains their problem and hands out its quest: no shop, services or trust talk. */
    private static NpcDialogueTree encounterTree(NpcType type) {
        int root = type.ordinal() * 100, first = 1000 + root;
        return new NpcDialogueTree(root, Map.of(
                root, new NpcDialogueNode(root, encounterLine("root"), List.of(
                        encounterChoice(first, "talk", root + 1, NpcDialogueView.DIALOGUE),
                        encounterChoice(first + 1, "goodbye", root, NpcDialogueView.CLOSE))),
                root + 1, new NpcDialogueNode(root + 1, encounterLine("about"), List.of(
                        encounterChoice(first + 10, "work", root + 2, NpcDialogueView.DIALOGUE),
                        encounterChoice(first + 11, "goodbye", root, NpcDialogueView.CLOSE))),
                root + 2, new NpcDialogueNode(root + 2, encounterLine("work"), List.of(
                        encounterChoice(first + 21, "back", root, NpcDialogueView.DIALOGUE)))));
    }

    private static String encounterLine(String topic) {
        return PREFIX + "world_encounter.line." + topic;
    }

    private static NpcDialogueChoice encounterChoice(int id, String label, int target, NpcDialogueView view) {
        return new NpcDialogueChoice(id, PREFIX + "choice." + label, PREFIX + "world_encounter.player." + label,
                target, view, 0, 0, view == NpcDialogueView.CLOSE, "");
    }

    public static NpcDialogueTree tree(NpcType type) {
        return TREES.getOrDefault(type, TREES.get(NpcType.FIXER));
    }

    public static boolean isDialogueChoice(int id) {
        return id >= 1000;
    }

    private static String line(NpcType type, String topic) {
        return PREFIX + type.id() + ".line." + topic;
    }

    private static NpcDialogueChoice choice(NpcType type, int id, String label, int target, NpcDialogueView view, int trust, String memory) {
        return new NpcDialogueChoice(id, PREFIX + "choice." + label, PREFIX + type.id() + ".player." + label,
                target, view, trust, 0, view == NpcDialogueView.CLOSE, memory);
    }
}
