package de.artemis.cyberneticenhancements.common.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public final class InvestigationSurvey {
    public static final int READINGS = 3;
    public static final int RANGE = 160;
    public static final int SCAN_TICKS = 60;
    private final BlockPos origin;
    private final BlockPos source;
    private final List<BlockPos> readings = new ArrayList<>();

    public InvestigationSurvey(BlockPos origin, BlockPos source) {
        this.origin = origin.immutable();
        this.source = source.immutable();
    }

    public BlockPos origin() { return origin; }
    public BlockPos source() { return source; }
    public int count() { return readings.size(); }
    public boolean located() { return count() == READINGS; }

    public String rejection(BlockPos position, int tier) {
        if (horizontalDistanceSquared(position, origin) > RANGE * RANGE) return "outside";
        if (horizontalDistanceSquared(position, source) < 24 * 24) return "near_source";
        int separation = 32 + Math.max(0, Math.min(2, tier - 1)) * 8;
        for (var reading : readings) {
            if (horizontalDistanceSquared(position, reading) < separation * separation) return "separation";
            double first = Math.toRadians(bearing(reading));
            double next = Math.toRadians(bearing(position));
            if (Math.abs(Math.sin(first - next)) < Math.sin(Math.toRadians(20))) return "angle";
        }
        return "";
    }

    public boolean record(BlockPos position, int tier) {
        if (located() || !rejection(position, tier).isEmpty()) return false;
        readings.add(position.immutable());
        return true;
    }

    public double bearing(BlockPos position) {
        return (Math.toDegrees(Math.atan2(source.getX() - position.getX(), position.getZ() - source.getZ())) + 360) % 360;
    }

    public BlockPos triangulatedPosition() {
        if (!located()) throw new IllegalStateException("Three bearings are required");
        double xx = 0, xz = 0, zz = 0, bx = 0, bz = 0;
        for (var reading : readings) {
            double angle = Math.toRadians(bearing(reading));
            double nx = Math.cos(angle), nz = Math.sin(angle);
            double projection = nx * reading.getX() + nz * reading.getZ();
            xx += nx * nx;
            xz += nx * nz;
            zz += nz * nz;
            bx += nx * projection;
            bz += nz * projection;
        }
        double determinant = xx * zz - xz * xz;
        if (determinant < 0.000001) throw new IllegalStateException("Bearings do not intersect");
        return new BlockPos((int) Math.round((bx * zz - bz * xz) / determinant), source.getY(),
                (int) Math.round((bz * xx - bx * xz) / determinant));
    }

    public CompoundTag toTag() {
        var tag = new CompoundTag();
        tag.putLong("Origin", origin.asLong());
        tag.putLong("Source", source.asLong());
        var samples = new ListTag();
        for (var reading : readings) {
            var entry = new CompoundTag();
            entry.putLong("Pos", reading.asLong());
            samples.add(entry);
        }
        tag.put("Readings", samples);
        return tag;
    }

    public static InvestigationSurvey fromTag(CompoundTag tag) {
        var survey = new InvestigationSurvey(BlockPos.of(tag.getLong("Origin")), BlockPos.of(tag.getLong("Source")));
        for (var entry : tag.getList("Readings", Tag.TAG_COMPOUND)) {
            if (survey.located()) break;
            survey.record(BlockPos.of(((CompoundTag) entry).getLong("Pos")), 1);
        }
        return survey;
    }

    private static double horizontalDistanceSquared(BlockPos first, BlockPos second) {
        double dx = (double) first.getX() - second.getX(), dz = (double) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }
}
