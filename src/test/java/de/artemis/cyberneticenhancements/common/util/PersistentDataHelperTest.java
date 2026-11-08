package de.artemis.cyberneticenhancements.common.util;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import static org.junit.Assert.*;

public class PersistentDataHelperTest {
    @Test
    public void cloningCopiesValuesAndClearsStaleState() {
        CompoundTag from = new CompoundTag();
        CompoundTag to = new CompoundTag();
        from.putInt("int", 12);
        from.putLong("long", 123456789012L);
        from.putDouble("double", 0.125);
        from.putFloat("float", 0.5F);
        from.putString("string", "MAJOR");
        to.putString("unrelated", "keep");
        copy(from, to);
        assertEquals(12, to.getInt("int"));
        assertEquals(123456789012L, to.getLong("long"));
        assertEquals(0.125, to.getDouble("double"), 0);
        assertEquals(0.5F, to.getFloat("float"), 0);
        assertEquals("MAJOR", to.getString("string"));
        copy(new CompoundTag(), to);
        assertFalse(to.contains("int"));
        assertFalse(to.contains("long"));
        assertFalse(to.contains("double"));
        assertFalse(to.contains("float"));
        assertFalse(to.contains("string"));
        assertEquals("keep", to.getString("unrelated"));
        assertEquals(12, from.getInt("int"));
    }

    private static void copy(CompoundTag from, CompoundTag to) {
        PersistentDataHelper.copyInt(from, to, "int");
        PersistentDataHelper.copyLong(from, to, "long");
        PersistentDataHelper.copyDouble(from, to, "double");
        PersistentDataHelper.copyFloat(from, to, "float");
        PersistentDataHelper.copyString(from, to, "string");
    }
}
