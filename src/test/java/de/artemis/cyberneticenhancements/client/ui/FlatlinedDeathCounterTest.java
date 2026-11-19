package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.client.FlatlinedDeathCounter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FlatlinedDeathCounterTest {
    @Test
    public void medicalRecordKeepsTheActualServerCountAndDoesNotTruncateLargeCounts() {
        FlatlinedDeathCounter.update(12);
        assertEquals("000012", FlatlinedDeathCounter.record());
        FlatlinedDeathCounter.update(1234567);
        assertEquals("1234567", FlatlinedDeathCounter.record());
        FlatlinedDeathCounter.update(0);
        assertEquals("000000", FlatlinedDeathCounter.record());
        FlatlinedDeathCounter.onLogout(null);
        assertEquals("------", FlatlinedDeathCounter.record());
    }
}
