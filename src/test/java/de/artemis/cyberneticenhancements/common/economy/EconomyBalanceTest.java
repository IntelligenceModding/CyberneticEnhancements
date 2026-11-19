package de.artemis.cyberneticenhancements.common.economy;

import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;

public class EconomyBalanceTest {
    @Test
    public void starterBudgetsAndBundlePricesFormAProgression() {
        var values = EconomyBalance.parse(JsonParser.parseString("{}"));
        int delivery = values.get("quest.delivery.base.1");
        assertTrue(values.get("shop.fixer.maxdoc") < delivery / 2);
        assertTrue(values.get("service.clinic_care") <= delivery);
        assertTrue(values.get("shop.netrunner.memory_boost") >= delivery * 3);
        assertTrue(values.get("shop.netrunner.memory_boost") <= values.get("quest.cyberpsycho.base.1"));
        assertTrue(values.get("shop.ripperdoc.maxdoc") / 2.0 < values.get("shop.fixer.maxdoc"));
        assertTrue(values.get("shop.netrunner.ram_jolt") / 2.0 < values.get("shop.fixer.ram_jolt"));
        assertTrue(values.get("shop.techie.legendary_components") > values.get("shop.techie.epic_components"));
        assertEquals(0, (int) values.get("access.service.detox"));
        assertEquals(0, (int) values.get("access.service.clinic_care"));
    }

    @Test
    public void recoveryPaysForScarcityAndHigherTiersRemainWorthwhile() {
        var v = EconomyBalance.parse(JsonParser.parseString("{}"));
        assertTrue(v.get("recovery.unit.3") > v.get("recovery.unit.0") * 10);
        for (String type : new String[]{"delivery", "extraction", "investigation", "breach", "elimination", "recovery", "cyberpsycho"}) {
            assertTrue(v.get("quest." + type + ".base.1") < v.get("quest." + type + ".base.2"));
            assertTrue(v.get("quest." + type + ".base.2") < v.get("quest." + type + ".base.3"));
        }
        int mostValuableRecovery = v.get("quest.recovery.base.3") + 4 * v.get("recovery.unit.3") + v.get("quest.recovery.spread.3");
        assertTrue(mostValuableRecovery < v.get("quest.cyberpsycho.base.1"));
        assertTrue(v.get("discovery.recovery") < v.get("discovery.investigation"));
    }

    @Test
    public void partialDatapacksKeepDefaultsAndPublishImmutableDefinitions() {
        var v = EconomyBalance.parse(JsonParser.parseString("{\"shop.fixer.maxdoc\":60}"));
        assertEquals(60, (int) v.get("shop.fixer.maxdoc"));
        assertEquals(70, (int) v.get("service.clinic_care"));
        try { v.put("service.clinic_care", 0); fail("Mutable economy snapshot"); }
        catch (UnsupportedOperationException expected) {}
    }

    @Test
    public void malformedOrUnsafeOverridesAreRejected() {
        for (String json : new String[]{"[]", "{\"shop.fixer.maxdoc\":0}", "{\"shop.fixer.maxdoc\":-1}",
                "{\"shop.fixer.maxdoc\":1.5}", "{\"shop.fixer.maxdoc\":\"50\"}", "{\"unknown\":1}",
                "{\"discovery.recovery\":101}", "{\"access.service.detox\":5}", "{\"support.minimum\":500}",
                "{\"shop.fixer.maxdoc\":2147483648}"}) {
            try { EconomyBalance.parse(JsonParser.parseString(json)); fail("Accepted invalid economy: " + json); }
            catch (IllegalArgumentException expected) {}
        }
    }
}
