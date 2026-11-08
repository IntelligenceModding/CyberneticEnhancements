package de.artemis.cyberneticenhancements.common.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.Test;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.*;

public class BankingPersistenceTest {
    @Test
    public void damagedRequestsDoNotHideValidRequests() throws Exception {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        PlayerEurodollarManager.PaymentRequest valid = new PlayerEurodollarManager.PaymentRequest(
                UUID.randomUUID(), from, "from", to, "to", 50, 1);
        ListTag requests = new ListTag();
        requests.add(new CompoundTag());
        requests.add(valid.toTag());
        CompoundTag invalidAmount = valid.toTag();
        invalidAmount.putInt("Amount", -50);
        requests.add(invalidAmount);
        CompoundTag self = valid.toTag();
        self.putString("ToPlayerId", from.toString());
        requests.add(self);
        CompoundTag data = new CompoundTag();
        data.put("cyberneticenhancements.eurodollar_requests", requests);
        Method read = PlayerEurodollarManager.class.getDeclaredMethod("readRequests", CompoundTag.class);
        read.setAccessible(true);
        assertEquals(List.of(valid), read.invoke(null, data));
    }

    @Test
    public void sharedLedgerKeepsNewest120EntriesAndSequence() throws Exception {
        CompoundTag data = new CompoundTag();
        Method append = PlayerEurodollarManager.class.getDeclaredMethod("appendTransaction", CompoundTag.class,
                long.class, String.class, int.class, String.class, UUID.class, String.class);
        append.setAccessible(true);
        for (int i = 1; i <= 130; i++) {
            append.invoke(null, data, 1L, "Income", i, "test", null, null);
        }
        ListTag ledger = data.getList("cyberneticenhancements.eurodollar_ledger", 10);
        assertEquals(120, ledger.size());
        assertEquals(130L, ledger.getCompound(0).getLong("Sequence"));
        assertEquals(11L, ledger.getCompound(119).getLong("Sequence"));
        assertEquals("income", ledger.getCompound(0).getString("Type"));
        assertEquals(130, ledger.getCompound(0).getInt("Amount"));
    }
}
