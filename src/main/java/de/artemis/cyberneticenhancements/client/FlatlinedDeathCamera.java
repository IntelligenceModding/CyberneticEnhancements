package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.FlatlinedDeathMotion;
import de.artemis.cyberneticenhancements.client.ui.DeathBodyMotion;
import de.artemis.cyberneticenhancements.client.render.DeathBodies;
import de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class FlatlinedDeathCamera {
    private static Screen screen;
    private static LocalPlayer player;
    private static long startedAt;
    private static float yaw;
    private static float pitch;
    private static double eyeHeight;
    private static DeathBodyMotion.Pose lastBodyMotion;
    private static Vec3 lastBodyPosition;
    private static float lastFallYaw;

    private FlatlinedDeathCamera() {
    }

    public static void begin(Screen deathScreen) {
        Minecraft minecraft = Minecraft.getInstance();
        if ((!FlatlinedPresentation.enabled(deathScreen) && !DeathBodies.enabled()) || minecraft.player == null) return;
        if (screen == deathScreen && player == minecraft.player) return;
        screen = deathScreen;
        player = minecraft.player;
        Camera camera = minecraft.gameRenderer.getMainCamera();
        yaw = camera.isInitialized() ? camera.getYRot() : player.getYRot();
        pitch = camera.isInitialized() ? camera.getXRot() : player.getXRot();
        eyeHeight = camera.isInitialized() ? Mth.clamp(camera.getPosition().y - player.getY(), 0.3, 2.5) : 1.62;
        startedAt = Util.getMillis();
        lastBodyMotion = null; lastBodyPosition = null;
        DeathBodies.beginLocal(player);
    }

    public static boolean active(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return player != null && entity == player && minecraft.player == player && minecraft.level == player.level()
                && (ClientConfig.CUSTOM_DEATH_SCREEN.get() || DeathBodies.enabled())
                && (minecraft.options.screenEffectScale().get() > 0 || DeathBodies.enabled())
                && (minecraft.screen == screen || minecraft.screen instanceof DeathScreen.TitleConfirmScreen);
    }

    public static View view(float partialTick) {
        var body = DeathBodies.enabled() ? DeathBodies.motion(player, partialTick) : null;
        if (DeathBodies.enabled()) {
            Vec3 feet = DeathBodies.position(player);
            float fallYaw = DeathBodies.fallYaw(player);
            if (body != null) {
                lastBodyMotion = body; lastBodyPosition = feet; lastFallYaw = fallYaw;
            } else if (lastBodyMotion != null) {
                // Keep the settled view when the corpse dissolves or the screen resizes.
                body = lastBodyMotion; feet = lastBodyPosition; fallYaw = lastFallYaw;
            } else body = DeathBodyMotion.sample((Util.getMillis() - startedAt) / 1000.0,
                    Minecraft.getInstance().options.screenEffectScale().get() <= 0);
            double scale = player.getScale();
            double radians = Math.toRadians(fallYaw);
            Vec3 desired = feet.add(Math.sin(radians) * body.eyeBackward(eyeHeight),
                    body.eyeHeight(eyeHeight / scale) * scale, -Math.cos(radians) * body.eyeBackward(eyeHeight));
            Vec3 position = collisionLimited(feet.add(0, eyeHeight, 0), desired);
            var view = body.view(fallYaw, yaw, pitch);
            return new View(position, view.yaw(), view.pitch(), view.roll());
        }
        var motion = FlatlinedDeathMotion.sample(TraumaRescueClient.rise() > 0 ? 0.95 * (1 - TraumaRescueClient.rise()) : (Util.getMillis() - startedAt) / 1000.0);
        Vec3 feet = new Vec3(Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()), Mth.lerp(partialTick, player.zo, player.getZ()));
        Vec3 origin = feet.add(0, eyeHeight, 0);
        double radians = Math.toRadians(yaw);
        double backward = motion.fall() * 0.45;
        Vec3 desired = feet.add(Math.sin(radians) * backward,
                Mth.lerp(motion.fall(), eyeHeight, 0.28), -Math.cos(radians) * backward);
        Vec3 position = collisionLimited(origin, desired);
        float lookPitch = (float) (Mth.lerp(motion.tilt(), pitch, -88) + motion.impact());
        float roll = (float) (motion.roll() * Minecraft.getInstance().options.damageTiltStrength().get());
        return new View(position, yaw, lookPitch, roll);
    }

    private static Vec3 collisionLimited(Vec3 origin, Vec3 desired) {
        Vec3 movement = desired.subtract(origin);
        double length = movement.length();
        if (length < 0.0001) return origin;
        double fraction = 1;
        for (int corner = 0; corner < 8; corner++) {
            Vec3 offset = new Vec3((corner & 1) == 0 ? -0.1 : 0.1,
                    (corner & 2) == 0 ? -0.1 : 0.1, (corner & 4) == 0 ? -0.1 : 0.1);
            Vec3 from = origin.add(offset);
            var hit = player.level().clip(new ClipContext(from, desired.add(offset), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                fraction = Math.min(fraction, Math.max(0, (hit.getLocation().distanceTo(from) - 0.03) / length));
            }
        }
        return origin.add(movement.scale(fraction));
    }

    public static void onTick(ClientTickEvent.Post event) {
        if (player != null && !active(player)) clear();
    }

    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        screen = null;
        player = null;
        lastBodyMotion = null; lastBodyPosition = null;
    }

    public record View(Vec3 position, float yaw, float pitch, float roll) {
    }
}
