package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.render.SandevistanEchoRenderer;
import de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanTimeDilation;
import de.artemis.cyberneticenhancements.common.network.SandevistanSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SandevistanClientEffects {
    private static final Map<UUID, SandevistanSnapshotPayload.User> USERS = new HashMap<>();
    private static long receivedAt;
    private static long pulseAt = Long.MIN_VALUE / 2;
    private static long overloadAt = Long.MIN_VALUE / 2;

    private SandevistanClientEffects() {
    }

    public static void receive(SandevistanSnapshotPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(payload.dimension())) return;
        SandevistanTimeDilation.applyClientSnapshot(minecraft.level, payload);
        long tick = minecraft.level.getGameTime();
        Map<UUID, SandevistanSnapshotPayload.User> next = new HashMap<>();
        for (var user : payload.users()) {
            if (user.mode() < 0 || user.mode() > 2) continue;
            var previous = USERS.get(user.uuid());
            boolean newActivation = previous == null || previous.activationTick() != user.activationTick();
            var entity = minecraft.level.getEntity(user.entityId());
            if (user.mode() == 1 && newActivation && entity != null && entity.getUUID().equals(user.uuid())) {
                minecraft.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_ACTIVATE,
                        SoundSource.PLAYERS, SandevistanClientOptions.soundVolume * 0.65F, 1.5F, false);
                if (entity == minecraft.player) pulseAt = tick;
            } else if (user.mode() != 1 && previous != null && previous.mode() == 1 && entity != null) {
                minecraft.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                        user.mode() == 2 ? SoundEvents.RESPAWN_ANCHOR_DEPLETE.value() : SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS, SandevistanClientOptions.soundVolume * 0.65F, user.mode() == 2 ? 0.75F : 1.25F, false);
                if (entity == minecraft.player && user.mode() == 2) overloadAt = tick;
            }
            next.put(user.uuid(), user);
        }
        USERS.clear();
        USERS.putAll(next);
        receivedAt = tick;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (minecraft.level.getGameTime() - receivedAt > 16) USERS.clear();
        SandevistanEchoRenderer.tick(USERS.values());
        if (active() && minecraft.player.tickCount % 40 == 0) {
            minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(),
                    SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.PLAYERS, SandevistanClientOptions.soundVolume * 0.07F, 1.5F, false);
        }
    }

    private static boolean active() {
        var player = Minecraft.getInstance().player;
        var user = player == null ? null : USERS.get(player.getUUID());
        return user != null && user.mode() == 1;
    }

    public static boolean isAccelerated(UUID uuid) {
        var user = USERS.get(uuid);
        return user != null && user.mode() == 1;
    }

    public static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) clear();
    }

    public static void onFov(ComputeFovModifierEvent event) {
        if (event.getPlayer() != Minecraft.getInstance().player || !active() || !SandevistanClientOptions.restrainedFov) return;
        float pulse = pulse(pulseAt, 8, 0);
        event.setNewFovModifier(Math.min(event.getNewFovModifier(), 1.08F) + pulse * 0.025F);
    }

    private static float pulse(long started, float duration, float partialTick) {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : Mth.clamp(1 - (level.getGameTime() - started + partialTick) / duration, 0, 1);
    }

    public static void renderOverlay(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || minecraft.options.hideGui || !SandevistanClientOptions.edgeEffects) return;
        float activation = pulse(pulseAt, 8, partialTick);
        float overload = pulse(overloadAt, 12, partialTick);
        float intensity = (active() ? 0.018F : 0) + activation * SandevistanClientOptions.flashIntensity
                + overload * SandevistanClientOptions.flashIntensity;
        if (intensity < 0.002F) return;
        int rgb = overload > 0 ? 0xFF6856 : activation > 0 ? 0x4AFFA0 : 0x50DFFF;
        int color = ((int) (Math.min(0.4F, intensity) * 255) << 24) | rgb;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int edgeX = Math.max(6, width / 14);
        int edgeY = Math.max(6, height / 10);
        graphics.fillGradient(0, 0, width, edgeY, color, rgb);
        graphics.fillGradient(0, height - edgeY, width, height, rgb, color);
        var matrix = graphics.pose().last().pose();
        var vertices = graphics.bufferSource().getBuffer(RenderType.guiOverlay());
        vertices.addVertex(matrix, 0, 0, 0).setColor(color);
        vertices.addVertex(matrix, 0, height, 0).setColor(color);
        vertices.addVertex(matrix, edgeX, height, 0).setColor(rgb);
        vertices.addVertex(matrix, edgeX, 0, 0).setColor(rgb);
        vertices.addVertex(matrix, width - edgeX, 0, 0).setColor(rgb);
        vertices.addVertex(matrix, width - edgeX, height, 0).setColor(rgb);
        vertices.addVertex(matrix, width, height, 0).setColor(color);
        vertices.addVertex(matrix, width, 0, 0).setColor(color);
        graphics.flush();
    }

    public static void clear() {
        USERS.clear();
        pulseAt = Long.MIN_VALUE / 2;
        overloadAt = Long.MIN_VALUE / 2;
        SandevistanTimeDilation.clearClient();
        SandevistanEchoRenderer.clear();
    }
}
