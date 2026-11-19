package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload.ContactEntry;
import net.minecraft.ChatFormatting;
import org.junit.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class QuickCallContactsTest {
    @Test
    public void onlyKnownVisibleLivingContactsAreListedWithPinsFirst() {
        var a = contact("Alpha", false, false, false, "aqua");
        var b = contact("beta", false, false, false, "green");
        var pinned = contact("Zulu", true, false, false, "gold");
        var hidden = contact("Hidden", true, true, false, "red");
        var deceased = contact("Dead", true, false, true, "red");
        assertEquals(List.of(pinned, a, b), QuickCallContacts.available(List.of(b, hidden, deceased, a, pinned)));
        assertEquals(List.of(), QuickCallContacts.available(List.of(hidden, deceased)));
    }

    @Test
    public void everyContactCanBeReachedAndShrinkingListsClampThePage() {
        var contacts = IntStream.range(0, 8).mapToObj(index -> contact("NPC " + index, false, false, false, "aqua")).toList();
        assertEquals(3, QuickCallContacts.pageCount(contacts.size()));
        assertEquals(contacts.subList(0, 3), QuickCallContacts.page(contacts, 0));
        assertEquals(contacts.subList(3, 6), QuickCallContacts.page(contacts, 1));
        assertEquals(contacts.subList(6, 8), QuickCallContacts.page(contacts, 2));
        assertEquals(contacts.subList(6, 8), QuickCallContacts.page(contacts, 50));
        assertEquals(contacts.subList(0, 2), QuickCallContacts.page(contacts.subList(0, 2), 2));
        assertEquals(List.of(), QuickCallContacts.page(List.of(), 3));
        assertEquals(1, QuickCallContacts.pageCount(0));
    }

    @Test
    public void displayNamesPreserveNpcColorsAndHandleHiddenNameplates() {
        assertEquals(ChatFormatting.GOLD.getColor().intValue(), contact("Gold", false, false, false, "gold").displayName().getStyle().getColor().getValue());
        assertEquals(ChatFormatting.WHITE.getColor().intValue(), contact("Hidden nameplate", false, false, false, "hidden").displayName().getStyle().getColor().getValue());
        assertEquals(ChatFormatting.AQUA.getColor().intValue(), contact("Fallback", false, false, false, "invalid").displayName().getStyle().getColor().getValue());
    }

    private static ContactEntry contact(String name, boolean pinned, boolean hidden, boolean deceased, String color) {
        return new ContactEntry(name, "techie", name, "generic", 1, pinned, hidden, false, false, false,
                "", false, 0, 0, 0, deceased, "", color);
    }

    @Test
    public void playersShareTheRosterButBlockedAndOfflinePlayersAreExcludedFromQuickCalls() {
        var available = player("Available", "available", false);
        var busy = player("Busy", "busy", true);
        var offline = player("Offline", "offline", true);
        var blocked = player("Blocked", "blocked", true);
        var npc = contact("NPC", false, false, false, "aqua");
        assertEquals(List.of(busy, available, npc), QuickCallContacts.available(List.of(npc, offline, available, blocked, busy)));
    }

    private static ContactEntry player(String name, String status, boolean pinned) {
        return new ContactEntry(java.util.UUID.randomUUID().toString(), "player", name, "generic", 0, pinned, false, false,
                false, false, "", false, 0, 0, 0, false, "", "aqua", status);
    }
}
