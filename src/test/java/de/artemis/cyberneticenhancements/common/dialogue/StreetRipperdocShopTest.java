package de.artemis.cyberneticenhancements.common.dialogue;

import com.google.gson.JsonParser;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class StreetRipperdocShopTest {
    @Test public void everyStreetOfferIsPricedOpenToEveryoneAndDescribed() throws Exception {
        var language = JsonParser.parseString(Files.readString(Path.of("src/generated/resources/assets/cyberneticenhancements/lang/en_us.json"))).getAsJsonObject();
        var offers = NpcShopCatalog.offers(NpcType.STREET_RIPPERDOC);
        assertTrue("A street doctor should out-stock the clinic", offers.size() > NpcShopCatalog.offers(NpcType.RIPPERDOC).size());
        for (var offer : offers) {
            assertTrue(offer.priceKey(), offer.basePrice() > 1);
            assertEquals("Temporary NPCs build no trust, so nothing may be gated on it: " + offer.priceKey(), 0, offer.requiredTrust());
            assertTrue(offer.labelKey(), language.has(offer.labelKey()));
            assertTrue(offer.descriptionKey(), language.has(offer.descriptionKey()));
        }
    }

    @Test public void streetServicesOutdoTheClinicAndAreNeverLocked() {
        assertEquals(2, NpcService.forNpc(NpcType.STREET_RIPPERDOC).size());
        assertTrue(NpcService.FIELD_TRIAGE.basePrice() > NpcService.CLINIC_CARE.basePrice());
        assertTrue(NpcService.NEURAL_FLUSH.basePrice() > NpcService.DETOX.basePrice());
        for (var service : NpcService.forNpc(NpcType.STREET_RIPPERDOC)) assertEquals(0, service.requiredTrust());
    }

    @Test public void theStreetDoctorSharesTheRipperdocVoice() {
        assertEquals(NpcType.RIPPERDOC.id(), NpcType.STREET_RIPPERDOC.id());
        assertSame("Saved ids must keep resolving to the permanent ripperdoc", NpcType.RIPPERDOC, NpcType.fromId("ripperdoc"));
    }
}
