package de.artemis.cyberneticenhancements.common.economy;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import java.util.UUID;
import static org.junit.Assert.*;

public class BankingPlayerDirectoryTest {
    @Test
    public void realDaysUseTwentyFourHoursAndClampFutureClockValues() {
        long now = 1800000000000L;
        assertEquals(24, BankingPlayerDirectory.daysAgo(now - 24 * 86400000L - 3600000L, now));
        assertEquals(0, BankingPlayerDirectory.daysAgo(now - 86399999L, now));
        assertEquals(0, BankingPlayerDirectory.daysAgo(now + 86400000L, now));
    }

    @Test
    public void namesAndLastSeenSurviveSavingAndNameChangesDoNotDuplicatePlayers() {
        var directory = new BankingPlayerDirectory();
        UUID id = UUID.randomUUID();
        directory.record(id, "OldName", 1000);
        directory.record(id, "NewName", 2000);
        var restored = BankingPlayerDirectory.load(directory.save(new CompoundTag(), null), null);
        assertEquals(1, restored.entries().size());
        assertEquals(new BankingPlayerDirectory.Entry(id, "NewName", 2000), restored.find("newname"));
        assertEquals(restored.find("NewName"), restored.find(id.toString()));
        assertNull(restored.find("OldName"));
    }
}
