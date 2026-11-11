package de.artemis.cyberneticenhancements.client;

import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;

 public final class ArchiveReferenceData {
    private static int revision;
    private ArchiveReferenceData() {}
    public static int revision() { return revision; }
    public static void onRecipesUpdated(RecipesUpdatedEvent event) { revision++; }
}