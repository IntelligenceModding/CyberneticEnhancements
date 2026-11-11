package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.entity.NpcType;

import java.util.List;
import java.util.Map;

public final class NpcDialogueTrees {
    private static final int FIXER_ROOT = 0;
    private static final int FIXER_CREDS = 1;
    private static final int FIXER_VALUES = 2;

    private static final int RIPPERDOC_ROOT = 100;
    private static final int RIPPERDOC_RISK = 101;
    private static final int RIPPERDOC_PROTOCOL = 102;

    private static final int TECHIE_ROOT = 200;
    private static final int TECHIE_SALVAGE = 201;
    private static final int TECHIE_PRACTICAL = 202;

    private static final int NETRUNNER_ROOT = 300;
    private static final int NETRUNNER_ROUTE = 301;
    private static final int NETRUNNER_DISCIPLINE = 302;

    private static final int MERC_ROOT = 400;
    private static final int MERC_WORK = 401;
    private static final int MERC_CODE = 402;

    private static final Map<NpcType, NpcDialogueTree> BASE_TREES = Map.of(
            NpcType.FIXER, new NpcDialogueTree(FIXER_ROOT, Map.of(
                    FIXER_ROOT, new NpcDialogueNode(FIXER_ROOT, line("fixer", "root"), List.of(
                            choice(1000, key("fixer", "respect"), player("fixer", "respect"), FIXER_CREDS, NpcDialogueView.DIALOGUE, 1, 0, false, "fixer_respect"),
                            choice(1001, key("fixer", "skeptical"), player("fixer", "skeptical"), FIXER_VALUES, NpcDialogueView.DIALOGUE, 0, 0, false, ""),
                            choice(1020, key("fixer", "intel"), player("fixer", "caches"), FIXER_ROOT, NpcDialogueView.DIALOGUE, 0, 0, false, ""),
                            choice(1003, key("fixer", "insult"), player("fixer", "insult"), FIXER_ROOT, NpcDialogueView.CLOSE, -2, 2, true, "")
                    )),
                    FIXER_CREDS, new NpcDialogueNode(FIXER_CREDS, line("fixer", "credentials"), List.of(
                            choice(1010, key("fixer", "calm"), player("fixer", "calm"), FIXER_VALUES, NpcDialogueView.DIALOGUE, 1, 0, false, "fixer_calm"),
                            choice(1011, key("fixer", "services"), player("fixer", "services"), FIXER_CREDS, NpcDialogueView.SERVICES, 0, 0, false, ""),
                            choice(1012, key("fixer", "prices"), player("fixer", "prices"), FIXER_ROOT, NpcDialogueView.DIALOGUE, -1, 1, false, ""),
                            choice(1013, key("fixer", "dismiss"), player("fixer", "dismiss"), FIXER_ROOT, NpcDialogueView.CLOSE, -2, 2, true, "")
                    )),
                    FIXER_VALUES, new NpcDialogueNode(FIXER_VALUES, line("fixer", "values"), List.of(
                            choice(1002, key("fixer", "jobs"), player("fixer", "contracts"), FIXER_VALUES, NpcDialogueView.SERVICES, 0, 0, false, ""),
                            choice(1021, key("fixer", "stock"), player("fixer", "shop"), FIXER_VALUES, NpcDialogueView.SHOP, 0, 0, false, ""),
                            choice(1022, key("fixer", "challenge"), player("fixer", "challenge"), FIXER_ROOT, NpcDialogueView.DIALOGUE, -1, 1, false, ""),
                            choice(1023, key("fixer", "walk"), player("fixer", "back"), FIXER_ROOT, NpcDialogueView.CLOSE, 0, 0, true, "")
                    ))
            )),
            NpcType.RIPPERDOC, new NpcDialogueTree(RIPPERDOC_ROOT, Map.of(
                    RIPPERDOC_ROOT, new NpcDialogueNode(RIPPERDOC_ROOT, line("ripperdoc", "root"), List.of(
                            choice(1100, key("ripperdoc", "respect"), player("ripperdoc", "respect"), RIPPERDOC_RISK, NpcDialogueView.DIALOGUE, 1, 0, false, "ripperdoc_respect"),
                            choice(1101, key("ripperdoc", "nervous"), player("ripperdoc", "nervous"), RIPPERDOC_PROTOCOL, NpcDialogueView.DIALOGUE, 0, 0, false, ""),
                            choice(1102, key("ripperdoc", "clinic"), player("ripperdoc", "services"), RIPPERDOC_ROOT, NpcDialogueView.SERVICES, 0, 0, false, ""),
                            choice(1103, key("ripperdoc", "accuse"), player("ripperdoc", "accuse"), RIPPERDOC_ROOT, NpcDialogueView.CLOSE, -2, 2, true, "")
                    )),
                    RIPPERDOC_RISK, new NpcDialogueNode(RIPPERDOC_RISK, line("ripperdoc", "risk"), List.of(
                            choice(1110, key("ripperdoc", "trust"), player("ripperdoc", "trust"), RIPPERDOC_PROTOCOL, NpcDialogueView.DIALOGUE, 1, 0, false, "ripperdoc_trust"),
                            choice(1111, key("ripperdoc", "stock"), player("ripperdoc", "shop"), RIPPERDOC_RISK, NpcDialogueView.SHOP, 0, 0, false, ""),
                            choice(1112, key("ripperdoc", "argue"), player("ripperdoc", "argue"), RIPPERDOC_ROOT, NpcDialogueView.DIALOGUE, -1, 1, false, ""),
                            choice(1113, key("ripperdoc", "leave"), player("ripperdoc", "back"), RIPPERDOC_ROOT, NpcDialogueView.CLOSE, 0, 0, true, "")
                    )),
                    RIPPERDOC_PROTOCOL, new NpcDialogueNode(RIPPERDOC_PROTOCOL, line("ripperdoc", "protocol"), List.of(
                            choice(1120, key("ripperdoc", "recovery"), player("ripperdoc", "contracts"), RIPPERDOC_PROTOCOL, NpcDialogueView.CONTRACTS, 0, 0, false, ""),
                            choice(1121, key("ripperdoc", "calm"), player("ripperdoc", "calm"), RIPPERDOC_RISK, NpcDialogueView.DIALOGUE, 1, 0, false, "ripperdoc_calm"),
                            choice(1122, key("ripperdoc", "mock"), player("ripperdoc", "mock"), RIPPERDOC_ROOT, NpcDialogueView.CLOSE, -2, 2, true, ""),
                            choice(1123, key("ripperdoc", "back"), player("ripperdoc", "back"), RIPPERDOC_ROOT, NpcDialogueView.DIALOGUE, 0, 0, false, "")
                    ))
            )),
            NpcType.TECHIE, new NpcDialogueTree(TECHIE_ROOT, Map.of(
                    TECHIE_ROOT, new NpcDialogueNode(TECHIE_ROOT, line("techie", "root"), List.of(
                            choice(1200, key("techie", "respect"), player("techie", "respect"), TECHIE_SALVAGE, NpcDialogueView.DIALOGUE, 1, 0, false, "techie_respect"),
                            choice(1201, key("techie", "gear"), player("techie", "services"), TECHIE_ROOT, NpcDialogueView.SERVICES, 0, 0, false, ""),
                            choice(1202, key("techie", "parts"), player("techie", "shop"), TECHIE_ROOT, NpcDialogueView.SHOP, 0, 0, false, ""),
                            choice(1203, key("techie", "trash_talk"), player("techie", "trash_talk"), TECHIE_ROOT, NpcDialogueView.CLOSE, -2, 2, true, "")
                    )),
                    TECHIE_SALVAGE, new NpcDialogueNode(TECHIE_SALVAGE, line("techie", "salvage"), List.of(
                            choice(1210, key("techie", "contracts"), player("techie", "contracts"), TECHIE_SALVAGE, NpcDialogueView.CONTRACTS, 0, 0, false, ""),
                            choice(1211, key("techie", "practical"), player("techie", "practical"), TECHIE_PRACTICAL, NpcDialogueView.DIALOGUE, 1, 0, false, "techie_practical"),
                            choice(1212, key("techie", "complain"), player("techie", "complain"), TECHIE_ROOT, NpcDialogueView.DIALOGUE, -1, 1, false, ""),
                            choice(1213, key("techie", "leave"), player("techie", "back"), TECHIE_ROOT, NpcDialogueView.CLOSE, 0, 0, true, "")
                    )),
                    TECHIE_PRACTICAL, new NpcDialogueNode(TECHIE_PRACTICAL, line("techie", "practical"), List.of(
                            choice(1220, key("techie", "agree"), player("techie", "agree"), TECHIE_SALVAGE, NpcDialogueView.DIALOGUE, 1, 0, false, "techie_agree"),
                            choice(1221, key("techie", "skeptical"), player("techie", "skeptical"), TECHIE_ROOT, NpcDialogueView.DIALOGUE, -1, 1, false, ""),
                            choice(1222, key("techie", "stock"), player("techie", "shop"), TECHIE_PRACTICAL, NpcDialogueView.SHOP, 0, 0, false, ""),
                            choice(1223, key("techie", "done"), player("techie", "back"), TECHIE_ROOT, NpcDialogueView.CLOSE, 0, 0, true, "")
                    ))
            )),
            NpcType.NETRUNNER, new NpcDialogueTree(NETRUNNER_ROOT, Map.of(
                    NETRUNNER_ROOT, new NpcDialogueNode(NETRUNNER_ROOT, line("netrunner", "root"), List.of(
                            choice(1300, key("netrunner", "respect"), player("netrunner", "respect"), NETRUNNER_ROUTE, NpcDialogueView.DIALOGUE, 1, 0, false, "netrunner_respect"),
                            choice(1301, key("netrunner", "route"), player("netrunner", "caches"), NETRUNNER_DISCIPLINE, NpcDialogueView.DIALOGUE, 0, 0, false, ""),
                            choice(1302, key("netrunner", "tools"), player("netrunner", "shop"), NETRUNNER_ROOT, NpcDialogueView.SHOP, 0, 0, false, ""),
                            choice(1303, key("netrunner", "insult"), player("netrunner", "insult"), NETRUNNER_ROOT, NpcDialogueView.CLOSE, -2, 2, true, "")
                    )),
                    NETRUNNER_ROUTE, new NpcDialogueNode(NETRUNNER_ROUTE, line("netrunner", "route"), List.of(
                            choice(1310, key("netrunner", "contracts"), player("netrunner", "contracts"), NETRUNNER_ROUTE, NpcDialogueView.CONTRACTS, 0, 0, false, ""),
                            choice(1311, key("netrunner", "discipline"), player("netrunner", "discipline"), NETRUNNER_DISCIPLINE, NpcDialogueView.DIALOGUE, 1, 0, false, "netrunner_discipline"),
                            choice(1312, key("netrunner", "push"), player("netrunner", "push"), NETRUNNER_ROOT, NpcDialogueView.DIALOGUE, -1, 1, false, ""),
                            choice(1313, key("netrunner", "leave"), player("netrunner", "back"), NETRUNNER_ROOT, NpcDialogueView.CLOSE, 0, 0, true, "")
                    )),
                    NETRUNNER_DISCIPLINE, new NpcDialogueNode(NETRUNNER_DISCIPLINE, line("netrunner", "discipline"), List.of(
                            choice(1320, key("netrunner", "agree"), player("netrunner", "agree"), NETRUNNER_ROUTE, NpcDialogueView.DIALOGUE, 1, 0, false, "netrunner_agree"),
                            choice(1321, key("netrunner", "services"), player("netrunner", "services"), NETRUNNER_DISCIPLINE, NpcDialogueView.SERVICES, 0, 0, false, ""),
                            choice(1322, key("netrunner", "challenge"), player("netrunner", "challenge"), NETRUNNER_ROOT, NpcDialogueView.DIALOGUE, -1, 1, false, ""),
                            choice(1323, key("netrunner", "close"), player("netrunner", "back"), NETRUNNER_ROOT, NpcDialogueView.CLOSE, 0, 0, true, "")
                    ))
            )),
            NpcType.MERC, new NpcDialogueTree(MERC_ROOT, Map.of(
                    MERC_ROOT, new NpcDialogueNode(MERC_ROOT, line("merc", "root"), List.of(
                            choice(1400, key("merc", "respect"), player("merc", "respect"), MERC_WORK, NpcDialogueView.DIALOGUE, 1, 0, false, "merc_respect"),
                            choice(1401, key("merc", "jobs"), player("merc", "contracts"), MERC_ROOT, NpcDialogueView.CONTRACTS, 0, 0, false, ""),
                            choice(1402, key("merc", "supplies"), player("merc", "shop"), MERC_ROOT, NpcDialogueView.SHOP, 0, 0, false, ""),
                            choice(1403, key("merc", "mock"), player("merc", "mock"), MERC_ROOT, NpcDialogueView.CLOSE, -2, 2, true, "")
                    )),
                    MERC_WORK, new NpcDialogueNode(MERC_WORK, line("merc", "work"), List.of(
                            choice(1410, key("merc", "code"), player("merc", "code"), MERC_CODE, NpcDialogueView.DIALOGUE, 1, 0, false, "merc_code"),
                            choice(1411, key("merc", "services"), player("merc", "services"), MERC_WORK, NpcDialogueView.SERVICES, 0, 0, false, ""),
                            choice(1412, key("merc", "disagree"), player("merc", "disagree"), MERC_ROOT, NpcDialogueView.DIALOGUE, -1, 1, false, ""),
                            choice(1413, key("merc", "leave"), player("merc", "back"), MERC_ROOT, NpcDialogueView.CLOSE, 0, 0, true, "")
                    )),
                    MERC_CODE, new NpcDialogueNode(MERC_CODE, line("merc", "code"), List.of(
                            choice(1420, key("merc", "agree"), player("merc", "agree"), MERC_WORK, NpcDialogueView.DIALOGUE, 1, 0, false, "merc_agree"),
                            choice(1421, key("merc", "challenge"), player("merc", "challenge"), MERC_ROOT, NpcDialogueView.DIALOGUE, -1, 1, false, ""),
                            choice(1422, key("merc", "targets"), player("merc", "contracts"), MERC_CODE, NpcDialogueView.CONTRACTS, 0, 0, false, ""),
                            choice(1423, key("merc", "done"), player("merc", "back"), MERC_ROOT, NpcDialogueView.CLOSE, 0, 0, true, "")
                    ))
            ))
    );

    private static final Map<NpcType, NpcDialogueTree> TREES = expandedTrees();

    private static Map<NpcType, NpcDialogueTree> expandedTrees() {
        Map<NpcType, NpcDialogueTree> result = new java.util.EnumMap<>(NpcType.class);
        for (Map.Entry<NpcType, NpcDialogueTree> entry : BASE_TREES.entrySet()) {
            NpcType type = entry.getKey();
            NpcDialogueTree base = entry.getValue();
            String id = type.id();
            int root = base.rootNodeId();
            int briefing = root + 3;
            int preparation = root + 4;
            int consequences = root + 5;
            int entryChoice = switch (type) {
                case FIXER -> 1002;
                case RIPPERDOC -> 1121;
                case TECHIE -> 1220;
                case NETRUNNER -> 1311;
                case MERC -> 1420;
            };
            Map<Integer, NpcDialogueNode> nodes = new java.util.HashMap<>(base.nodes());

            for (NpcDialogueNode node : base.nodes().values()) {
                nodes.put(node.id(), new NpcDialogueNode(node.id(), node.npcLineKey(), node.choices().stream().map(c ->
                        c.id() == entryChoice ? new NpcDialogueChoice(c.id(), c.translationKey(), c.playerLineKey(),
                                briefing, c.view(), c.trustDelta(), c.annoyanceDelta(), c.closeConversation(), c.rewardMemoryKey()) : c).toList()));
            }
            int first = 2000 + root;
            nodes.put(briefing, new NpcDialogueNode(briefing, line(id, "briefing"), List.of(
                    choice(first, key(id, "prepare"), player(id, "prepare"), preparation, NpcDialogueView.DIALOGUE, 0, 0, false, ""),
                    choice(first + 1, key(id, "consequences"), player(id, "consequences"), consequences, NpcDialogueView.DIALOGUE, 0, 0, false, ""),
                    choice(first + 2, key(id, "take_work"), player(id, "take_work"), briefing, type.supportsContracts() ? NpcDialogueView.CONTRACTS : NpcDialogueView.SERVICES, 0, 0, false, ""),
                    choice(first + 3, key(id, "return"), player(id, "return"), root, NpcDialogueView.DIALOGUE, 0, 0, false, "")
            )));
            nodes.put(preparation, new NpcDialogueNode(preparation, line(id, "preparation"), List.of(
                    choice(first + 4, key(id, "understood"), player(id, "understood"), consequences, NpcDialogueView.DIALOGUE, 1, 0, false, id + "_preparation_understood"),
                    choice(first + 5, key(id, "supplies"), player(id, "supplies"), preparation, NpcDialogueView.SHOP, 0, 0, false, ""),
                    choice(first + 6, key(id, "treatment"), player(id, "treatment"), preparation, NpcDialogueView.SERVICES, 0, 0, false, ""),
                    choice(first + 7, key(id, "return"), player(id, "return"), briefing, NpcDialogueView.DIALOGUE, 0, 0, false, "")
            )));
            nodes.put(consequences, new NpcDialogueNode(consequences, line(id, "consequences"), List.of(
                    choice(first + 8, key(id, "take_work"), player(id, "take_work"), consequences, type.supportsContracts() ? NpcDialogueView.CONTRACTS : NpcDialogueView.SERVICES, 0, 0, false, ""),
                    choice(first + 9, key(id, "prepare"), player(id, "prepare"), preparation, NpcDialogueView.DIALOGUE, 0, 0, false, ""),
                    choice(first + 10, key(id, "reckless"), player(id, "reckless"), root, NpcDialogueView.DIALOGUE, -1, 1, false, ""),
                    choice(first + 11, key(id, "return"), player(id, "return"), briefing, NpcDialogueView.DIALOGUE, 0, 0, false, "")
            )));
            result.put(type, new NpcDialogueTree(root, Map.copyOf(nodes)));
        }
        return Map.copyOf(result);
    }

    private NpcDialogueTrees() {
    }

    public static NpcDialogueTree tree(NpcType npcType) {
        return TREES.getOrDefault(npcType, TREES.get(NpcType.FIXER));
    }

    public static boolean isDialogueChoice(int optionId) {
        return optionId >= 1000;
    }

    private static NpcDialogueChoice choice(
            int id,
            String translationKey,
            String playerLineKey,
            int nextNodeId,
            NpcDialogueView view,
            int trustDelta,
            int annoyanceDelta,
            boolean closeConversation,
            String rewardMemoryKey
    ) {
        return new NpcDialogueChoice(id, translationKey, playerLineKey, nextNodeId, view, trustDelta, annoyanceDelta, closeConversation, rewardMemoryKey);
    }

    private static String key(String type, String suffix) {
        return "screen.cyberneticenhancements.npc.dialogue." + type + ".choice." + suffix;
    }

    private static String player(String type, String suffix) {
        return "screen.cyberneticenhancements.npc.dialogue." + type + ".player." + suffix;
    }

    private static String line(String type, String suffix) {
        return "screen.cyberneticenhancements.npc.dialogue." + type + ".line." + suffix;
    }
}
