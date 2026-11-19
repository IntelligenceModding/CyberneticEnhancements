package de.artemis.cyberneticenhancements.common.quest;

public enum QuestStatus {
    OFFERED("offered"),
    ACTIVE("active"),
    READY("ready"),
    COMPLETED("completed"),
    FAILED("failed"),
    ABANDONED("abandoned"),
    CANCELLED("cancelled");

    private final String id;

    QuestStatus(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static QuestStatus fromId(String id) {
        for (QuestStatus status : values()) {
            if (status.id.equals(id)) {
                return status;
            }
        }
        return OFFERED;
    }
}
