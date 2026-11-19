package de.artemis.cyberneticenhancements.common.dialogue;

public enum NpcCallReason {
    CHECK_IN("checkin"), OVERDUE("overdue"), MONEY("money"), URGENT_QUEST("urgent"), LOST_ITEM("lost_item"), CACHE_FAILED("cache_failed");

    private final String id;

    NpcCallReason(String id) { this.id = id; }

    public String id() { return id; }
}
