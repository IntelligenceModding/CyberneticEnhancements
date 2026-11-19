package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.core.BlockPos;

public record CompassLandmark(String id, String title, String dimension, BlockPos pos, Kind kind, int color) {
    public enum Kind {
        QUEST, TURN_IN, BANNER, CITY, MAX_TAC, MEDTEC, CUSTOM
    }
}
