package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.client.BarredSitesClientState;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.common.block.HoloBarrierBlock;
import de.artemis.cyberneticenhancements.common.blockentity.HoloBarrierBlockEntity;
import de.artemis.cyberneticenhancements.common.world.WorldEncounterOffenses;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The doorway's projection, built like the tarot cards: a pixel panel of scanline stripes with a bright sweep and a slight
 * flicker. It shows a warning "!" in yellow, a crossed-out mark in red for anyone the site has stopped dealing with for good,
 * and a red countdown for someone locked out only for a while after a failed quest. When that countdown ends the panel eases
 * back to yellow the way the tarot scan eases to its completed colour, with the same sound.
 */
public final class HoloBarrierRenderer implements BlockEntityRenderer<HoloBarrierBlockEntity> {
    private static final int COLUMNS = 32, ROWS = 32;
    private static final float HEIGHT = 2;
    private static final int YELLOW = 0xFFC21A, YELLOW_SWEEP = 0xFFE98A, RED = 0xFF3A2E, RED_SWEEP = 0xFF8C7A;
    /** How quickly the colour eases between the two states: the tarot scan's own completion easing. */
    private static final double EASE_SECONDS = .12;
    /** Not drawn for this long (in milliseconds) means the panel was out of sight, so it is shown as it is without easing. */
    private static final long OUT_OF_SIGHT_MILLIS = 1000;
    private static final int TIMER_TOP_ROW = 21, BAR_FROM = 3, BAR_TO = 28, BAR_ROW_LOW = 7, BAR_ROW_HIGH = 8;
    /** A 3 by 5 font for the digits, one string of rows from the top each. */
    private static final String[] DIGITS = {
            "111101101101111", "010110010010111", "111001111100111", "111001111001111", "101101111001001",
            "111100111001111", "111100111101111", "111001001010010", "111101111101111", "111101111001111"};

    /** What one barrier is currently showing, eased towards what it should be. */
    private static final class Look {
        long drawnAt = -1;
        boolean known, wasLocked;
        float red, lock;
        long shownTicks;
    }

    private final Map<HoloBarrierBlockEntity, Look> looks = new WeakHashMap<>();

    public HoloBarrierRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(HoloBarrierBlockEntity barrier, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (ShaderPackCompatibility.isRenderingShadowPass() || barrier.getLevel() == null) return;
        var state = barrier.getBlockState();
        if (!state.hasProperty(HoloBarrierBlock.FACING)) return;
        var level = barrier.getLevel();
        boolean barred = BarredSitesClientState.holdsAgainst(barrier.getBlockPos());
        long left = barred ? 0 : BarredSitesClientState.lockoutTicks(barrier.getBlockPos(), level.getGameTime());
        boolean locked = left > 0;
        var look = ease(barrier, level, barred, locked, left);
        float motion = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        float time = (level.getGameTime() + partialTick) * motion;
        float flicker = .9F + .07F * (float) Math.sin(time * .47F);
        int sweepRow = HologramPixels.sweepRow(time, ROWS);
        int rgb = UiRenderer.blend(0xFF000000 | YELLOW, 0xFF000000 | RED, look.red) & 0xFFFFFF;
        int sweepRgb = UiRenderer.blend(0xFF000000 | YELLOW_SWEEP, 0xFF000000 | RED_SWEEP, look.red) & 0xFFFFFF;
        long seconds = Math.ceilDiv(look.shownTicks, 20);
        String clock = seconds / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds % 60);
        float fill = Math.min(1, look.shownTicks / (float) WorldEncounterOffenses.LOCKOUT_TICKS);
        var vertices = buffers.getBuffer(HologramRenderTypes.BARRIER);

        pose.pushPose();
        // The panel starts at the middle of this block and runs on to the middle of the next, facing the way the block does.
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(HoloBarrierBlock.FACING).toYRot()));
        var matrix = pose.last().pose();
        for (int row = 0; row < ROWS; row++) {
            float stripe = .7F + .3F * HologramPixels.stripe(row, time);
            float scanline = 1 - motion + motion * stripe;
            int start = 0, startColor = 0;
            for (int column = 0; column <= COLUMNS; column++) {
                int color = column < COLUMNS ? cell(column, row, barred, look, clock, fill, row == sweepRow ? sweepRgb : rgb, row == sweepRow, scanline * flicker) : 0;
                if (column < COLUMNS && color == startColor) continue;
                if (startColor != 0) { // Close the run of equal pixels as one quad.
                    float left0 = -.5F + start * HologramPixels.SIZE, right = -.5F + column * HologramPixels.SIZE;
                    float bottom = row * HologramPixels.SIZE, top = bottom + HologramPixels.SIZE;
                    vertices.addVertex(matrix, left0, bottom, 0).setColor(startColor);
                    vertices.addVertex(matrix, right, bottom, 0).setColor(startColor);
                    vertices.addVertex(matrix, right, top, 0).setColor(startColor);
                    vertices.addVertex(matrix, left0, top, 0).setColor(startColor);
                }
                start = column;
                startColor = color;
            }
        }
        pose.popPose();
    }

    /** Moves the barrier's look a step towards its state, and chimes once when a lockout has just run out. */
    private Look ease(HoloBarrierBlockEntity barrier, net.minecraft.world.level.Level level, boolean barred, boolean locked, long left) {
        var look = looks.computeIfAbsent(barrier, key -> new Look());
        long now = Util.getMillis();
        if (look.drawnAt >= 0 && now - look.drawnAt > OUT_OF_SIGHT_MILLIS) { look.known = false; look.wasLocked = false; }
        double seconds = look.drawnAt < 0 ? 0 : Math.clamp((now - look.drawnAt) / 1000D, 0, .1);
        look.drawnAt = now;
        float ease = (float) (1 - Math.exp(-seconds / EASE_SECONDS));
        float redTarget = barred || locked ? 1 : 0, lockTarget = locked ? 1 : 0;
        if (!look.known) { // First seen: show it as it is, with nothing to ease from.
            look.known = true;
            look.red = redTarget;
            look.lock = lockTarget;
        } else {
            look.red += (redTarget - look.red) * ease;
            look.lock += (lockTarget - look.lock) * ease;
        }
        if (locked) look.shownTicks = left;
        if (look.wasLocked && !locked && !barred) {
            var at = barrier.getBlockPos();
            level.playLocalSound(at.getX() + .5, at.getY() + 1, at.getZ() + .5, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, .25F, 1.3F, false);
        }
        look.wasLocked = locked;
        return look;
    }

    /** The colour of one pixel of the panel, or 0 where there is none. Row 0 is the bottom. */
    private static int cell(int column, int row, boolean barred, Look look, String clock, float fill, int rgb, boolean sweep, float brightness) {
        float alpha;
        boolean frame = column == 0 || column == COLUMNS - 1 || row == 0 || row == ROWS - 1;
        boolean hazard = (row >= 2 && row <= 4 || row >= ROWS - 5 && row <= ROWS - 3) && column >= 2 && column < COLUMNS - 2
                && Math.floorMod((column + row) / 3, 2) == 0;
        if (frame) alpha = 210;
        else if (hazard) alpha = 150;
        else {
            alpha = 34;
            if (exclamation(column, row)) alpha = Math.max(alpha, 200 * (1 - look.red));
            if (barred && cross(column, row)) alpha = Math.max(alpha, 200 * look.red);
            if (look.lock > .01F) {
                if (timer(column, row, clock)) alpha = Math.max(alpha, 200 * look.lock);
                else if (row >= BAR_ROW_LOW && row <= BAR_ROW_HIGH && column >= BAR_FROM && column <= BAR_TO)
                    alpha = Math.max(alpha, (column - BAR_FROM < Math.round((BAR_TO - BAR_FROM + 1) * fill) ? 170 : 60) * look.lock);
            }
        }
        if (sweep) alpha += 60;
        int value = Math.min(215, Math.round(alpha * brightness));
        if (value <= 0) return 0;
        return value << 24 | rgb;
    }

    private static boolean exclamation(int column, int row) {
        boolean middle = column == 15 || column == 16;
        return middle && (row >= 14 && row <= 24 || row >= 8 && row <= 10);
    }

    private static boolean cross(int column, int row) {
        int i = row - 8; // A square cross between rows 8 and 23.
        if (i < 0 || i > 15) return false;
        int left = 8 + i, right = 23 - i;
        return Math.abs(column - left) <= 1 || Math.abs(column - right) <= 1;
    }

    /** Whether a pixel belongs to the countdown: digits twice the size of the font, centred, a space between each. */
    private static boolean timer(int column, int row, String clock) {
        int fontRow = (TIMER_TOP_ROW - row) / 2;
        if (row > TIMER_TOP_ROW || fontRow > 4) return false;
        int width = -1;
        for (int i = 0; i < clock.length(); i++) width += (clock.charAt(i) == ':' ? 2 : 6) + 1;
        int x = (COLUMNS - width) / 2;
        for (int i = 0; i < clock.length(); i++) {
            char character = clock.charAt(i);
            int size = character == ':' ? 2 : 6;
            if (column >= x && column < x + size) {
                if (character == ':') return fontRow == 1 || fontRow == 3;
                return DIGITS[character - '0'].charAt(fontRow * 3 + (column - x) / 2) == '1';
            }
            x += size + 1;
        }
        return false;
    }

    @Override
    public int getViewDistance() { return 48; }

    @Override
    public AABB getRenderBoundingBox(HoloBarrierBlockEntity barrier) {
        return new AABB(barrier.getBlockPos()).inflate(2.5, 0, 2.5).expandTowards(0, HEIGHT, 0);
    }
}
