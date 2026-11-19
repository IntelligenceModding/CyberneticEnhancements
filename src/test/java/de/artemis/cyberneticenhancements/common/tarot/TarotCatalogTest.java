package de.artemis.cyberneticenhancements.common.tarot;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.Assert.*;
import static de.artemis.cyberneticenhancements.common.tarot.TarotCatalog.BonusType.*;

public class TarotCatalogTest {
    @Test
    public void thirtyEightDistinctCollectiblesEachHaveTheirOwnSuppliedArtwork() throws Exception {
        assertEquals(38, TarotCatalog.CARDS.size());
        assertEquals(38, TarotCatalog.CARDS.stream().map(TarotCatalog.Card::id).distinct().count());
        Path assets = Path.of("src/main/resources/assets/cyberneticenhancements");
        Set<Path> usedArtwork = new HashSet<>();
        for (int i = 0; i < 38; i++) {
            var card = TarotCatalog.CARDS.get(i);
            assertEquals(i, card.number());
            assertSame(card, TarotCatalog.find(card.id()));
            assertEquals(2, card.rewards().size());
            assertNotEquals(card.rewards().get(0).type(), card.rewards().get(1).type());
            for (var reward : card.rewards()) assertTrue(reward.amount() > 0 && Double.isFinite(reward.amount()));
            assertFalse(card.displayName().isBlank()); assertFalse(card.descriptionText().isBlank());
            String individual = "textures/tarot/" + card.id() + ".png";
            assertEquals(individual, card.texture().getPath());
            Path artwork = assets.resolve(card.texture().getPath());
            assertTrue("Missing card artwork: " + card.id(), Files.isRegularFile(artwork));
            usedArtwork.add(artwork);
        }
        assertEquals("the_fool", TarotCatalog.CARDS.getFirst().id());
        assertEquals("Every card must have dedicated artwork", 38, usedArtwork.size());
        try (var files = Files.list(assets.resolve("textures/tarot"))) {
            assertEquals("Unreferenced artwork would unnecessarily increase the mod size", usedArtwork,
                    files.filter(file -> file.toString().endsWith(".png")).collect(java.util.stream.Collectors.toSet()));
        }
    }
    @Test
    public void fullCollectionHasSmallBoundedTotalsAndDisplaysTheCorrectUnits() {
        Map<TarotCatalog.BonusType, Double> expected = Map.ofEntries(Map.entry(HEALTH, 10D), Map.entry(HEADROOM, 60D), Map.entry(ARMOR, 3D),
                Map.entry(ARMOR_TOUGHNESS, 2.5D), Map.entry(ATTACK_DAMAGE, 1.25D), Map.entry(ATTACK_SPEED, .06D), Map.entry(MOVEMENT_SPEED, .025D),
                Map.entry(LUCK, 1.5D), Map.entry(MINING_SPEED, .06D), Map.entry(BLOCK_REACH, .5D), Map.entry(KNOCKBACK_RESISTANCE, .05D), Map.entry(SAFE_FALL_DISTANCE, 2.5D));
        expected.forEach((type, amount) -> assertEquals(amount, TarotCatalog.total(type, id -> true), 1e-9));
        assertEquals("5", HEALTH.format(TarotCatalog.total(HEALTH, id -> true)));
        assertEquals("0.5", HEALTH.format(1));
        assertEquals("3%", ATTACK_SPEED.format(.03)); assertEquals("1.5%", MOVEMENT_SPEED.format(.015));
        assertEquals("2%", KNOCKBACK_RESISTANCE.format(.02));
        assertTrue(MINING_SPEED.multipliesBase()); assertFalse(KNOCKBACK_RESISTANCE.multipliesBase());
        Set<String> collection = Set.of("the_fool", "invalid");
        assertEquals(1, TarotCatalog.total(HEALTH, collection::contains), 0);
        assertEquals(.5, TarotCatalog.total(SAFE_FALL_DISTANCE, collection::contains), 0);
        assertEquals(0, TarotCatalog.total(HEADROOM, collection::contains), 0);
        assertEquals(0, TarotCatalog.total(HEALTH, id -> false), 0);
    }
    @Test
    public void cardsRequireTwoDistinctValidBonuses() {
        var reward = new TarotCatalog.Reward(HEALTH, 1);
        assertThrows(IllegalArgumentException.class, () -> new TarotCatalog.Reward(HEALTH, 0));
        assertThrows(IllegalArgumentException.class, () -> new TarotCatalog.Reward(HEALTH, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new TarotCatalog.Card("test", 0, "Test", "Test", List.of(reward)));
        assertThrows(IllegalArgumentException.class, () -> new TarotCatalog.Card("test", 0, "Test", "Test", List.of(reward, reward)));
    }
    @Test
    public void structureSelectionIsStableAndCanProduceEveryCard() {
        Set<String> selected = new HashSet<>();
        for (int i = 0; i < 4096; i++) {
            String key = "minecraft:desert_pyramid@" + i;
            var card = TarotCatalog.forStructure(7321, key);
            assertSame(card, TarotCatalog.forStructure(7321, key)); selected.add(card.id());
        }
        assertEquals(38, selected.size());
    }
    @Test
    public void everyCardAndRewardHasShippedTranslations() throws Exception {
        var json = com.google.gson.JsonParser.parseString(Files.readString(Path.of("src/generated/resources/assets/cyberneticenhancements/lang/en_us.json"))).getAsJsonObject();
        for (var card : TarotCatalog.CARDS) {
            assertEquals(card.displayName(), json.get("tarot.cyberneticenhancements." + card.id()).getAsString());
            assertEquals(card.descriptionText(), json.get("tarot.cyberneticenhancements." + card.id() + ".description").getAsString());
            for (var reward : card.rewards()) assertTrue(json.get("tarot.cyberneticenhancements.reward." + reward.type().id()).getAsString().contains("%s"));
        }
    }
}
