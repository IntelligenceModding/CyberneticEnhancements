package de.artemis.cyberneticenhancements.common.quest;

public enum QuestType {
    DELIVERY("delivery"),
    EXTRACTION("extraction"),
    INVESTIGATION("investigation"),
    BREACH("breach"),
    ELIMINATION("elimination"),
    RECOVERY("recovery"),
    CYBERPSYCHO("cyberpsycho"),
    /** A world-encounter quest: revive a patient near rough coordinates, mark them with a beacon and bring the beacon back. Never offered on a board. */
    LOCATE("locate");

    private final String id;

    QuestType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Whether the quest has the player carry something back in person. */
    public boolean returnsItems() {
        return requiresInPersonTurnIn();
    }

    public boolean requiresInPersonTurnIn() {
        return switch (this) {
            case EXTRACTION, RECOVERY, CYBERPSYCHO, LOCATE -> true;
            default -> false;
        };
    }

    public static QuestType fromId(String id) {
        for (QuestType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return DELIVERY;
    }
}
