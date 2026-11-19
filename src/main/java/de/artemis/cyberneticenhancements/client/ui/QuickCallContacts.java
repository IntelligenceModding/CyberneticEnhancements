package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload.ContactEntry;

import java.util.Comparator;
import java.util.List;

public final class QuickCallContacts {
    public static final int PAGE_SIZE = 3;

    private QuickCallContacts() {
    }

    public static List<ContactEntry> available(List<ContactEntry> contacts) {
        return contacts.stream().filter(contact -> !contact.hidden() && !contact.deceased())
                .filter(contact -> !contact.isPlayer() || !contact.callStatus().equals("blocked") && !contact.callStatus().equals("offline"))
                .sorted(Comparator.comparing(ContactEntry::pinned).reversed()
                        .thenComparing(ContactEntry::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ContactEntry::npcId)).toList();
    }

    public static int pageCount(int count) {
        return Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    public static List<ContactEntry> page(List<ContactEntry> contacts, int page) {
        int start = Math.clamp(page, 0, pageCount(contacts.size()) - 1) * PAGE_SIZE;
        return contacts.subList(start, Math.min(contacts.size(), start + PAGE_SIZE));
    }
}
