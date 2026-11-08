package de.artemis.cyberneticenhancements.common.util;

import net.minecraft.nbt.CompoundTag;

public final class PersistentDataHelper {
    private PersistentDataHelper() {
    }

    public static void copyLong(CompoundTag fromData, CompoundTag toData, String key) {
        if (fromData.contains(key)) {
            toData.putLong(key, fromData.getLong(key));
        } else {
            toData.remove(key);
        }
    }

    public static void copyInt(CompoundTag fromData, CompoundTag toData, String key) {
        if (fromData.contains(key)) {
            toData.putInt(key, fromData.getInt(key));
        } else {
            toData.remove(key);
        }
    }

    public static void copyDouble(CompoundTag fromData, CompoundTag toData, String key) {
        if (fromData.contains(key)) {
            toData.putDouble(key, fromData.getDouble(key));
        } else {
            toData.remove(key);
        }
    }

    public static void copyFloat(CompoundTag fromData, CompoundTag toData, String key) {
        if (fromData.contains(key)) {
            toData.putFloat(key, fromData.getFloat(key));
        } else {
            toData.remove(key);
        }
    }

    public static void copyString(CompoundTag fromData, CompoundTag toData, String key) {
        if (fromData.contains(key)) {
            toData.putString(key, fromData.getString(key));
        } else {
            toData.remove(key);
        }
    }
}
