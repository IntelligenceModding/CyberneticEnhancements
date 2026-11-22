package de.artemis.cyberneticenhancements.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.client.CompassHudOverlay;
import de.artemis.cyberneticenhancements.client.CompassPreferences;
import de.artemis.cyberneticenhancements.client.CustomCompassMarkers;
import de.artemis.cyberneticenhancements.client.FastTravelClientState;
import de.artemis.cyberneticenhancements.common.network.FastTravelPointsPayload;
import de.artemis.cyberneticenhancements.client.QuestNavigationClientState;
import de.artemis.cyberneticenhancements.client.WorldMapCache;
import de.artemis.cyberneticenhancements.client.ui.CompassLandmark;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * The map tab: everything explored so far as a pan-and-zoom picture, with the same markers the compass HUD points at. What
 * the compass shows or hides is toggled by clicking it here, and a marker dropped on the map lands on the compass too.
 */
final class MapBrowser {
    private static final int GAP = 10, TOOLBAR = 22, REFRESH_MS = 250;
    private static final double MIN_ZOOM = 0.125, MAX_ZOOM = 16;
    private static final int[] SCALES = {1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000};
    private static final int BACKGROUND = 0xFF0A1016, GRID = 0x12FFFFFF;
    // The view outlives the screen, so reopening the map finds it where it was left.
    private static double camX, camZ, zoom = 1;
    private static boolean following = true, fullscreen;

    /** Whether the map takes the whole screen instead of the archive's pane; the archive reads this to know what to draw. */
    static boolean fullscreen() { return fullscreen; }

    static void leaveFullscreen() { fullscreen = false; }

    static void enterFullscreen() { fullscreen = true; }

    private final List<Action> actions = new ArrayList<>();
    private List<CompassLandmark> markers = List.of();
    private List<Blip> blips = List.of();
    private final MapSettingsPanel settings = new MapSettingsPanel();
    private final java.util.Map<net.minecraft.world.entity.EntityType<?>, java.util.Optional<net.minecraft.client.model.geom.ModelPart>> heads = new java.util.HashMap<>();
    private final java.util.Map<net.minecraft.world.entity.EntityType<?>, java.util.Optional<float[]>> faces = new java.util.HashMap<>();

    /** Who is around, by what they are to the player: each kind has its own switch and colour in the settings. */
    private enum Kind {
        PERSON(0xFFF0B36A), PLAYER(0xFFFFFFFF), PET(0xFF6FD6FF), VILLAGER(0xFFE8D36A), HOSTILE(0xFFFF5C5C), ANIMAL(0xFF7BE07B), AMBIENT(0xFFB48CFF);
        final int color;
        Kind(int color) { this.color = color; }
    }

    private record Blip(net.minecraft.world.entity.LivingEntity entity, Kind kind, double distance) {}
    private long refreshed;
    /** A dimension looked at without being in it, from what was saved there; null while the map shows the one the player is in. */
    private String viewed;

    private static String currentDimension() { return Minecraft.getInstance().level.dimension().location().toString(); }

    private String shown() { return viewed != null && !viewed.equals(currentDimension()) ? viewed : currentDimension(); }

    private boolean away() { return !shown().equals(currentDimension()); }

    /** Every dimension of this world that is either the current one or has a saved map, current first. */
    private List<String> dimensions() {
        var found = new ArrayList<String>();
        found.add(currentDimension());
        for (var key : Minecraft.getInstance().player.connection.levels()) {
            String id = key.location().toString();
            if (!found.contains(id) && WorldMapCache.hasSaved(id)) found.add(id);
        }
        return found;
    }

    private void nextDimension() {
        var all = dimensions();
        String next = all.get((all.indexOf(shown()) + 1) % all.size());
        if (next.equals(currentDimension())) { viewed = null; following = true; }
        else {
            viewed = next; following = false;
            var middle = WorldMapCache.center(next);
            if (middle != null) { camX = middle[0]; camZ = middle[1]; }
        }
        refreshed = 0;
    }
    private Font font;
    private int x, y, width, height, mouseX, mouseY, viewX, viewY, viewWidth, viewHeight;
    private boolean pressing, wasHovering, sliding;
    private int sliderX, sliderY, sliderWidth, sliderLow, sliderHigh;
    private double moved;
    private CompassLandmark hovered;

    private record Action(int x, int y, int width, int height, Runnable run) {}

    private static net.minecraft.network.chat.MutableComponent text(String key, Object... args) {
        return Component.translatable("screen.cyberneticenhancements.archive.map." + key, args);
    }

    void render(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
        this.font = font; this.x = x; this.y = y; this.width = width; this.height = height;
        this.mouseX = mouseX; this.mouseY = mouseY;
        actions.clear();
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) return;
        MapSettings.load();
        UiRenderer.panel(graphics, x, y, width, height, UiRenderer.PANEL);
        toolbar(graphics, player);
        viewX = x + GAP; viewY = y + GAP + TOOLBAR + 6; viewWidth = width - GAP * 2; viewHeight = y + height - GAP - viewY;
        if (viewWidth < 40 || viewHeight < 40) return;
        if (following && !away()) { camX = player.getX(); camZ = player.getZ(); }
        refreshMarkers();

        graphics.fill(viewX, viewY, viewX + viewWidth, viewY + viewHeight, BACKGROUND);
        graphics.enableScissor(viewX, viewY, viewX + viewWidth, viewY + viewHeight);
        try {
            grid(graphics);
            terrain(graphics);
            if (!away() && WorldMapCache.empty()) graphics.drawCenteredString(font, text("empty"), viewX + viewWidth / 2, viewY + viewHeight / 2 - 4, UiRenderer.TEXT_SECONDARY);
            hovered = null;
            if (!away()) drawRadar(graphics, player);
            if (MapSettings.MARKERS.on()) drawMarkers(graphics, player);
            drawTravelNodes(graphics);
            if (MapSettings.SELF.on() && !away()) drawPlayer(graphics, player);
            graphics.fillGradient(viewX, viewY, viewX + viewWidth, viewY + 18, 0x90000000, 0);
            graphics.fillGradient(viewX, viewY + viewHeight - 18, viewX + viewWidth, viewY + viewHeight, 0, 0x90000000);
        } finally {
            graphics.disableScissor();
        }
        UiRenderer.outline(graphics, viewX - 1, viewY - 1, viewWidth + 2, viewHeight + 2, UiRenderer.PANEL_EDGE_ACTIVE);
        overlay(graphics);
        if (settings.open()) { hovered = null; travelHovered = null; }
        if (hovered != null) tooltip(graphics, player);
        if (travelHovered != null) travelTooltip(graphics);
        boolean hovering = hovered != null || travelHovered != null;
        if (hovering && !wasHovering) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.9F, 0.15F));
        wasHovering = hovering;
        settings.render(graphics, font, x, y, width, height, mouseX, mouseY);
    }

    // ---- layout ----

    private double screenX(double worldX) { return viewX + viewWidth / 2.0 + (worldX - camX) * zoom; }
    private double screenZ(double worldZ) { return viewY + viewHeight / 2.0 + (worldZ - camZ) * zoom; }
    private double worldX(double screen) { return camX + (screen - viewX - viewWidth / 2.0) / zoom; }
    private double worldZ(double screen) { return camZ + (screen - viewY - viewHeight / 2.0) / zoom; }
    private boolean inView(double px, double py) { return px >= viewX && px < viewX + viewWidth && py >= viewY && py < viewY + viewHeight; }

    private void toolbar(GuiGraphics graphics, net.minecraft.world.entity.player.Player player) {
        int bx = x + GAP, by = y + GAP;
        button(graphics, bx, by, 110, TOOLBAR, text("recenter"), following && !away(), () -> { viewed = null; following = true; refreshed = 0; });
        button(graphics, bx + 118, by, 24, TOOLBAR, Component.literal("-"), false, () -> zoomBy(1 / 1.5));
        button(graphics, bx + 146, by, 24, TOOLBAR, Component.literal("+"), false, () -> zoomBy(1.5));
        button(graphics, bx + 178, by, 120, TOOLBAR, text(fullscreen ? "fullscreen_on" : "fullscreen_off"), fullscreen, () -> fullscreen = !fullscreen);
        button(graphics, bx + 306, by, 100, TOOLBAR, text("settings"), settings.open(), () -> settings.open(true));
        String id = shown();
        Component dimension = Component.literal(id.substring(id.indexOf(':') + 1).replace('_', ' ').toUpperCase(java.util.Locale.ROOT));
        int right = x + width - GAP, buttonWidth = font.width(dimension) + 20;
        if (MapSettings.DIMENSION.on()) {
            // With nowhere else to go it is only a label: no highlight, no click.
            if (dimensions().size() > 1) button(graphics, right - buttonWidth, by, buttonWidth, TOOLBAR, dimension, away(), this::nextDimension);
            else UiRenderer.button(graphics, font, right - buttonWidth, by, buttonWidth, TOOLBAR, dimension, false, false, false, -1);
        }
        // A cave has no surface to look down on, so what is shown is a layer of it: a slider for the height, or Auto to follow the player.
        sliderWidth = 0;
        // Here that is the layer being mapped; in another dimension it picks among the layers saved there.
        int[] span = away() ? WorldMapCache.savedLayers(shown()) : WorldMapCache.layered() ? new int[] {WorldMapCache.layerLow(), WorldMapCache.layerHigh()} : null;
        int roomFor = (away() ? 0 : 58) + 160;
        if (span != null && bx + 414 + roomFor < right - (MapSettings.DIMENSION.on() ? buttonWidth : 0) - 8) {
            int lx = bx + 414;
            if (!away()) { button(graphics, lx, by, 52, TOOLBAR, text("layer_auto"), WorldMapCache.autoLayer(), () -> WorldMapCache.autoLayer(true)); lx += 58; }
            sliderX = lx; sliderY = by; sliderWidth = 160; sliderLow = span[0]; sliderHigh = span[1];
            int layer = away() ? WorldMapCache.viewedLayer(shown()) : WorldMapCache.layer();
            float fraction = layer == WorldMapCache.SURFACE ? 1F : sliderHigh > sliderLow ? Mth.clamp((layer - sliderLow) / (float) (sliderHigh - sliderLow), 0F, 1F) : 0F;
            int thumb = sliderX + Math.round(fraction * (sliderWidth - 6));
            graphics.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + TOOLBAR, UiRenderer.FIELD);
            graphics.fill(sliderX + 1, sliderY + 1, thumb + 3, sliderY + TOOLBAR - 1, UiRenderer.ROW_SELECTED);
            graphics.fill(thumb, sliderY, thumb + 6, sliderY + TOOLBAR, sliding ? UiRenderer.TEXT : UiRenderer.ACCENT);
            UiRenderer.outline(graphics, sliderX, sliderY, sliderWidth, TOOLBAR, UiRenderer.PANEL_EDGE_ACTIVE);
            graphics.drawCenteredString(font, layer == WorldMapCache.SURFACE ? text("layer_surface") : text("layer", layer + WorldMapCache.LAYER_SPAN - 1), sliderX + sliderWidth / 2, sliderY + 7, UiRenderer.TEXT);
        }
    }

    private void button(GuiGraphics graphics, int bx, int by, int bw, int bh, Component label, boolean selected, Runnable run) {
        UiRenderer.button(graphics, font, bx, by, bw, bh, label, true, selected, inside(mouseX, mouseY, bx, by, bw, bh), -1);
        actions.add(new Action(bx, by, bw, bh, run));
    }

    private static boolean inside(double px, double py, int bx, int by, int bw, int bh) {
        return px >= bx && px < bx + bw && py >= by && py < by + bh;
    }

    // ---- drawing ----

    private void grid(GuiGraphics graphics) {
        double step = 16 * zoom;
        if (!MapSettings.GRID.on() || step < 14) return;
        int firstX = (int) Math.floor(worldX(viewX) / 16), lastX = (int) Math.ceil(worldX(viewX + viewWidth) / 16);
        int firstZ = (int) Math.floor(worldZ(viewY) / 16), lastZ = (int) Math.ceil(worldZ(viewY + viewHeight) / 16);
        for (int cx = firstX; cx <= lastX; cx++) { int px = (int) Math.round(screenX(cx * 16.0)); graphics.fill(px, viewY, px + 1, viewY + viewHeight, GRID); }
        for (int cz = firstZ; cz <= lastZ; cz++) { int py = (int) Math.round(screenZ(cz * 16.0)); graphics.fill(viewX, py, viewX + viewWidth, py + 1, GRID); }
    }

    private void terrain(GuiGraphics graphics) {
        int size = WorldMapCache.REGION_SIZE;
        int minX = Math.floorDiv((int) Math.floor(worldX(viewX)), size), maxX = Math.floorDiv((int) Math.floor(worldX(viewX + viewWidth)), size);
        int minZ = Math.floorDiv((int) Math.floor(worldZ(viewY)), size), maxZ = Math.floorDiv((int) Math.floor(worldZ(viewY + viewHeight)), size);
        boolean smooth = zoom < 1 && MapSettings.SMOOTH.on();
        RenderSystem.enableBlend();
        float light = MapSettings.BRIGHTNESS.scale();
        RenderSystem.setShaderColor(light, light, light, 1F);
        for (int rx = minX; rx <= maxX; rx++) {
            for (int rz = minZ; rz <= maxZ; rz++) {
                var region = away() ? WorldMapCache.peek(shown(), rx, rz) : WorldMapCache.peek(rx, rz);
                if (region == null) continue;
                var texture = region.texture(smooth);
                graphics.pose().pushPose();
                // Each picture runs from its own whole-pixel edge to the next one's, so zoomed out no hairline of background shows between them.
                int left = (int) Math.round(screenX(rx * (double) size)), right = (int) Math.round(screenX((rx + 1) * (double) size));
                int top = (int) Math.round(screenZ(rz * (double) size)), bottom = (int) Math.round(screenZ((rz + 1) * (double) size));
                graphics.pose().translate((float) left, (float) top, 0);
                graphics.pose().scale((right - left) / (float) size, (bottom - top) / (float) size, 1);
                graphics.blit(texture, 0, 0, 0F, 0F, size, size, size, size);
                graphics.pose().popPose();
            }
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private void refreshMarkers() {
        long now = System.currentTimeMillis();
        if (now - refreshed < REFRESH_MS && !markers.isEmpty()) return;
        refreshed = now;
        String dimension = shown();
        blips = away() ? List.of() : collect();
        markers = CompassPreferences.all().stream().filter(point -> point.dimension().equals(dimension)).toList();
    }

    private Kind kind(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.entity.player.Player player) {
        if (entity instanceof de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity
                || entity instanceof de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity) return MapSettings.PEOPLE.on() ? Kind.PERSON : null;
        if (entity instanceof net.minecraft.world.entity.player.Player) return MapSettings.PLAYERS.on() ? Kind.PLAYER : null;
        if (entity instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isTame()
                || entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed()) return MapSettings.PETS.on() ? Kind.PET : null;
        if (entity instanceof net.minecraft.world.entity.npc.AbstractVillager) return MapSettings.VILLAGERS.on() ? Kind.VILLAGER : null;
        if (entity instanceof net.minecraft.world.entity.monster.Enemy) return MapSettings.HOSTILES.on() ? Kind.HOSTILE : null;
        if (entity instanceof net.minecraft.world.entity.ambient.AmbientCreature) return MapSettings.AMBIENT.on() ? Kind.AMBIENT : null;
        if (entity instanceof net.minecraft.world.entity.Mob) return MapSettings.ANIMALS.on() ? Kind.ANIMAL : null;
        return null;
    }

    /** The nearest few of whatever is switched on, within the radar range; refreshed a few times a second, drawn every frame. */
    private List<Blip> collect() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        double range = MapSettings.RANGE.value();
        var found = new ArrayList<Blip>();
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity living) || living == player || !living.isAlive() || living.isInvisible() || living.isSpectator()) continue;
            if (MapSettings.HIDE_SNEAKING.on() && living.isCrouching()) continue;
            var kind = kind(living, player);
            if (kind == null) continue;
            double distance = Math.hypot(living.getX() - player.getX(), living.getZ() - player.getZ());
            if (distance <= range) found.add(new Blip(living, kind, distance));
        }
        found.sort(java.util.Comparator.comparingDouble(Blip::distance));
        return found.size() > MapSettings.MAXIMUM.value() ? List.copyOf(found.subList(0, MapSettings.MAXIMUM.value())) : found;
    }

    /** Everyone around, live: a dot or an icon where they stand, optionally which way they face, and a name under the ones worth naming. */
    @SuppressWarnings("unchecked")
    private void drawRadar(GuiGraphics graphics, net.minecraft.world.entity.player.Player player) {
        if (blips.isEmpty()) return;
        var minecraft = Minecraft.getInstance();
        float partial = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        RenderSystem.enableBlend();
        float scale = MapSettings.RADAR_SCALE.scale();
        boolean icons = MapSettings.RADAR_STYLE.value() == 1 && zoom >= 0.35;
        // Nearest drawn last, so what is close to you is never covered by what is far.
        for (int index = blips.size() - 1; index >= 0; index--) {
            var blip = blips.get(index);
            var entity = blip.entity();
            if (!entity.isAlive()) continue;
            double px = screenX(Mth.lerp(partial, entity.xo, entity.getX())), py = screenZ(Mth.lerp(partial, entity.zo, entity.getZ()));
            if (px < viewX - 40 || px > viewX + viewWidth + 40 || py < viewY - 40 || py > viewY + viewHeight + 40) continue;
            int ix = (int) Math.round(px), iy = (int) Math.round(py);
            float alpha = 1F;
            if (MapSettings.FADE.on()) alpha = Mth.clamp(1F - ((float) Math.abs(entity.getY() - player.getY()) - 8F) / 32F, 0.35F, 1F);
            int half = Math.max(2, Math.round(3 * scale));
            if (!(icons && drawBlipIcon(graphics, blip, ix, iy, scale, alpha))) {
                int color = (int) (alpha * 255) << 24 | blip.kind().color & 0xFFFFFF;
                graphics.fill(ix - half - 1, iy - half - 1, ix + half + 1, iy + half + 1, (int) (alpha * 0xCC) << 24 | 0x081018);
                graphics.fill(ix - half, iy - half, ix + half, iy + half, color);
                half = Math.max(half, 3);
            } else half = Math.round(6 * scale);
            if (MapSettings.HEADINGS.on()) {
                float yaw = entity.getYHeadRot() * Mth.DEG_TO_RAD;
                for (int step = half + 2; step <= half + 6; step++) {
                    int hx = ix + Math.round(-Mth.sin(yaw) * step), hy = iy + Math.round(Mth.cos(yaw) * step);
                    graphics.fill(hx, hy, hx + 1, hy + 1, (int) (alpha * 255) << 24 | blip.kind().color & 0xFFFFFF);
                }
            }
            boolean named = blip.kind() == Kind.PERSON || blip.kind() == Kind.PLAYER || blip.kind() == Kind.PET || blip.kind() == Kind.VILLAGER || entity.hasCustomName();
            if (MapSettings.NAMES.on() && named && zoom >= 0.5) label(graphics, entity.getName().getString(), ix, iy + half - 2, 0.65F, UiRenderer.TEXT);
        }
    }

    /** Their face for people and players, the front of the head for creatures, drawn from their own model; false when there is nothing to draw, so a dot stands in. */
    private boolean drawBlipIcon(GuiGraphics graphics, Blip blip, int ix, int iy, float scale, float alpha) {
        var minecraft = Minecraft.getInstance();
        var entity = blip.entity();
        net.minecraft.resources.ResourceLocation skin = null;
        if (blip.kind() == Kind.PLAYER) {
            var info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(entity.getUUID());
            if (info != null) skin = info.getSkin().texture();
        } else if (blip.kind() == Kind.PERSON
                && minecraft.getEntityRenderDispatcher().getRenderer(entity) instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> renderer) {
            skin = ((net.minecraft.client.renderer.entity.LivingEntityRenderer<net.minecraft.world.entity.LivingEntity, ?>) renderer).getTextureLocation(entity);
        }
        if (skin != null) {
            int half = Math.round(6 * scale);
            RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
            graphics.fill(ix - half - 1, iy - half - 1, ix + half + 1, iy + half + 1, (int) (alpha * 255) << 24 | blip.kind().color & 0xFFFFFF);
            graphics.blit(skin, ix - half, iy - half, half * 2, half * 2, 8F, 8F, 8, 8, 64, 64);
            graphics.blit(skin, ix - half, iy - half, half * 2, half * 2, 40F, 8F, 8, 8, 64, 64);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            return true;
        }
        return drawHead(graphics, blip, ix, iy, scale, alpha);
    }

    /** The model a renderer draws with: the dragon and its kin keep theirs in a private field instead of behind getModel. */
    private static net.minecraft.client.model.EntityModel<?> modelOf(net.minecraft.client.renderer.entity.EntityRenderer<?> renderer) {
        if (renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> living) return living.getModel();
        for (Class<?> c = renderer.getClass(); c != null && c != Object.class; c = c.getSuperclass())
            for (var field : c.getDeclaredFields()) {
                if (!net.minecraft.client.model.EntityModel.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    if (field.get(renderer) instanceof net.minecraft.client.model.EntityModel<?> model) return model;
                } catch (ReflectiveOperationException | RuntimeException ignored) {}
            }
        return null;
    }

    /** A part by name anywhere below this one: a hoglin's head hangs off its body, a ghast has no head at all. */
    @SuppressWarnings("unchecked")
    private static java.util.Optional<net.minecraft.client.model.geom.ModelPart> find(net.minecraft.client.model.geom.ModelPart part, String name) {
        try {
            var field = net.minecraft.client.model.geom.ModelPart.class.getDeclaredField("children");
            field.setAccessible(true);
            var children = (java.util.Map<String, net.minecraft.client.model.geom.ModelPart>) field.get(part);
            var direct = children.get(name);
            if (direct != null) return java.util.Optional.of(direct);
            for (var child : children.values()) {
                var found = find(child, name);
                if (found.isPresent()) return found;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {}
        return java.util.Optional.empty();
    }

    /** The creature's head part, found once per type: through HeadedModel, else a part called head anywhere in the model, else its body, else whatever the model draws first. */
    private java.util.Optional<net.minecraft.client.model.geom.ModelPart> head(net.minecraft.world.entity.Entity entity,
            net.minecraft.client.renderer.entity.EntityRenderer<?> renderer) {
        return heads.computeIfAbsent(entity.getType(), type -> {
            var model = modelOf(renderer);
            if (model == null) return java.util.Optional.empty();
            if (model instanceof net.minecraft.client.model.HeadedModel headed && headed.getHead() != null) return java.util.Optional.of(headed.getHead());
            net.minecraft.client.model.geom.ModelPart root = null;
            for (Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass())
                for (var field : c.getDeclaredFields()) {
                    if (field.getType() != net.minecraft.client.model.geom.ModelPart.class) continue;
                    try {
                        field.setAccessible(true);
                        var part = (net.minecraft.client.model.geom.ModelPart) field.get(model);
                        if (part == null) continue;
                        if (field.getName().equals("head")) return java.util.Optional.of(part);
                        if (root == null || field.getName().equals("root")) root = part;
                    } catch (ReflectiveOperationException | RuntimeException ignored) {}
                }
            if (root == null) return java.util.Optional.empty();
            var head = find(root, "head");
            if (head.isEmpty()) head = find(root, "center_head"); // the wither's middle head
            if (head.isPresent()) return head;
            var body = find(root, "body");
            return body.isPresent() ? body : java.util.Optional.of(root);
        });
    }

    /** Remembers the four corners of a face as it comes out of a model cube: where each is on the cube and where it is in the texture. */
    private static final class FaceCapture implements com.mojang.blaze3d.vertex.VertexConsumer {
        final float[] corners = new float[16]; // x, y, u, v per corner
        int count;
        float normalZ;
        float[] front, side;
        final float[] depth = new float[4];
        float sideArea;

        @Override public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) {
            if (count == 4) count = 0;
            corners[count * 4] = x; corners[count * 4 + 1] = y; depth[count] = z;
            count++;
            return this;
        }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) {
            corners[(count - 1) * 4 + 2] = u; corners[(count - 1) * 4 + 3] = v;
            return this;
        }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float nx, float ny, float nz) {
            if (count != 4) return this;
            if (front == null && nz < -0.5F) front = corners.clone();
            // The largest face seen from the side (the one facing -x, front to the left), laid flat with depth as its width.
            if (nx < -0.5F) {
                float low = Math.min(Math.min(depth[0], depth[1]), Math.min(depth[2], depth[3])), high = Math.max(Math.max(depth[0], depth[1]), Math.max(depth[2], depth[3]));
                float top = Math.min(Math.min(corners[1], corners[5]), Math.min(corners[9], corners[13])), bottom = Math.max(Math.max(corners[1], corners[5]), Math.max(corners[9], corners[13]));
                float area = (high - low) * (bottom - top);
                if (area > sideArea) {
                    sideArea = area;
                    side = corners.clone();
                    for (int corner = 0; corner < 4; corner++) side[corner * 4] = depth[corner];
                }
            }
            return this;
        }
    }

    /** The front face of the creature's head, found once per type from the model itself: its corners and its place in the texture. */
    private java.util.Optional<float[]> face(net.minecraft.world.entity.Entity entity,
            net.minecraft.client.renderer.entity.EntityRenderer<?> renderer) {
        return faces.computeIfAbsent(entity.getType(), type -> {
            var found = head(entity, renderer);
            if (found.isEmpty()) return java.util.Optional.empty();
            var head = found.get();
            var capture = new FaceCapture();
            head.resetPose();
            // The head's own cubes come before its children (a hat layer, a snout), so the first front face is the head itself.
            head.visit(new com.mojang.blaze3d.vertex.PoseStack(), (pose, path, index, cube) -> {
                if (capture.front == null) cube.compile(pose, capture, 0, 0, -1);
            });
            float[] chosen = capture.front;
            // Seen from the front a fish is a sliver, so a creature with a slender or missing face is shown in profile instead: the biggest side of its whole body.
            if (chosen == null || slender(chosen)) {
                var whole = new FaceCapture();
                root(entity, renderer).ifPresent(part -> part.visit(new com.mojang.blaze3d.vertex.PoseStack(), (pose, path, index, cube) -> cube.compile(pose, whole, 0, 0, -1)));
                if (whole.side != null) chosen = whole.side;
            }
            return java.util.Optional.ofNullable(chosen);
        });
    }

    private static boolean slender(float[] face) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int corner = 0; corner < 4; corner++) {
            minX = Math.min(minX, face[corner * 4]); maxX = Math.max(maxX, face[corner * 4]);
            minY = Math.min(minY, face[corner * 4 + 1]); maxY = Math.max(maxY, face[corner * 4 + 1]);
        }
        float width = maxX - minX, height = maxY - minY;
        return width <= 0 || height <= 0 || width / height > 1.7F || height / width > 1.7F;
    }

    private final java.util.Map<net.minecraft.world.entity.EntityType<?>, java.util.Optional<net.minecraft.client.model.geom.ModelPart>> roots = new java.util.HashMap<>();

    /** The whole model, from its root part (or, failing that, the first part the model keeps). */
    private java.util.Optional<net.minecraft.client.model.geom.ModelPart> root(net.minecraft.world.entity.Entity entity,
            net.minecraft.client.renderer.entity.EntityRenderer<?> renderer) {
        return roots.computeIfAbsent(entity.getType(), type -> {
            var model = modelOf(renderer);
            if (model == null) return java.util.Optional.empty();
            net.minecraft.client.model.geom.ModelPart found = null;
            for (Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass())
                for (var field : c.getDeclaredFields()) {
                    if (field.getType() != net.minecraft.client.model.geom.ModelPart.class) continue;
                    try {
                        field.setAccessible(true);
                        var part = (net.minecraft.client.model.geom.ModelPart) field.get(model);
                        if (part != null && (found == null || field.getName().equals("root"))) found = part;
                    } catch (ReflectiveOperationException | RuntimeException ignored) {}
                }
            return java.util.Optional.ofNullable(found);
        });
    }

    /** The front of the creature's head, cut from its own texture, in a small framed square; false when it has no head to show. */
    @SuppressWarnings("unchecked")
    private boolean drawHead(GuiGraphics graphics, Blip blip, int ix, int iy, float scale, float alpha) {
        var entity = blip.entity();
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity)
                instanceof net.minecraft.client.renderer.entity.EntityRenderer<?> renderer)) return false;
        var found = face(entity, renderer);
        if (found.isEmpty()) return false;
        var face = found.get();
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int corner = 0; corner < 4; corner++) {
            minX = Math.min(minX, face[corner * 4]); maxX = Math.max(maxX, face[corner * 4]);
            minY = Math.min(minY, face[corner * 4 + 1]); maxY = Math.max(maxY, face[corner * 4 + 1]);
        }
        float extent = Math.max(maxX - minX, maxY - minY);
        if (extent <= 0) return false;
        var texture = ((net.minecraft.client.renderer.entity.EntityRenderer<net.minecraft.world.entity.Entity>) renderer).getTextureLocation(entity);
        // The frame takes the face's own proportions, the longer side fixed: a wide snout is wide, a tall head is tall, nothing is letterboxed.
        int half = Math.round(6 * scale);
        float k = half * 2 / extent;
        int halfX = Math.max(2, Math.round((maxX - minX) * k / 2)), halfY = Math.max(2, Math.round((maxY - minY) * k / 2));
        graphics.fill(ix - halfX - 1, iy - halfY - 1, ix + halfX + 1, iy + halfY + 1, (int) (alpha * 255) << 24 | blip.kind().color & 0xFFFFFF);
        graphics.fill(ix - halfX, iy - halfY, ix + halfX, iy + halfY, (int) (alpha * 255) << 24 | 0x081018);
        graphics.flush();
        // Model space has y down and the face looking toward -z: seen from the front, x and y map straight onto the screen.
        float cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
        var matrix = graphics.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
        var builder = com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);
        for (int corner = 0; corner < 4; corner++)
            builder.addVertex(matrix, ix + (face[corner * 4] - cx) * k, iy + (face[corner * 4 + 1] - cy) * k, 0F).setUv(face[corner * 4 + 2], face[corner * 4 + 3]);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        return true;
    }

    /** The people a marker is attached to (turn-in givers, MaxTac, Medtec), by marker id, as of this frame; everything else stays where it was put. */
    private final java.util.Map<String, net.minecraft.world.entity.Entity> followed = new java.util.HashMap<>();
    private float followedPartial;

    private void followMarkers() {
        followed.clear();
        var minecraft = Minecraft.getInstance();
        followedPartial = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        var byId = new java.util.HashMap<java.util.UUID, net.minecraft.world.entity.Entity>();
        for (var entity : minecraft.level.entitiesForRendering()) byId.put(entity.getUUID(), entity);
        var issuers = new java.util.HashMap<String, java.util.UUID>();
        for (var turnIn : QuestNavigationClientState.get().turnIns()) issuers.put(turnIn.questId(), turnIn.issuerId());
        for (var point : markers) {
            java.util.UUID uuid = null;
            try {
                if (point.id().startsWith("maxtac:")) uuid = java.util.UUID.fromString(point.id().substring(7));
                else if (point.id().startsWith("medtec:")) uuid = java.util.UUID.fromString(point.id().substring(7));
                else if (point.kind() == CompassLandmark.Kind.TURN_IN) uuid = issuers.get(point.id());
            } catch (IllegalArgumentException ignored) {}
            var entity = uuid == null ? null : byId.get(uuid);
            if (entity != null && entity.isAlive()) followed.put(point.id(), entity);
        }
    }

    private double liveX(CompassLandmark point) {
        var entity = followed.get(point.id());
        return entity == null ? point.pos().getX() + 0.5 : Mth.lerp(followedPartial, entity.xo, entity.getX());
    }

    private double liveZ(CompassLandmark point) {
        var entity = followed.get(point.id());
        return entity == null ? point.pos().getZ() + 0.5 : Mth.lerp(followedPartial, entity.zo, entity.getZ());
    }

    private void drawMarkers(GuiGraphics graphics, net.minecraft.world.entity.player.Player player) {
        hovered = null;
        followMarkers();
        double best = 64; // Pointer reach of a marker, squared.
        var drawn = new ArrayList<CompassLandmark>(markers);
        // Quests and turn-ins on top; they are what you came to find.
        drawn.sort((a, b) -> Boolean.compare(weight(a), weight(b)));
        for (var original : drawn) {
            var point = CompassPreferences.customized(original);
            double px = screenX(liveX(point)), py = screenZ(liveZ(point));
            boolean shown = !CompassPreferences.entry(original.id()).hidden() && CompassPreferences.categoryEnabled(original);
            boolean urgent = original.kind() == CompassLandmark.Kind.QUEST || original.kind() == CompassLandmark.Kind.TURN_IN;
            if (!inView(px, py)) {
                if (urgent && shown && MapSettings.EDGE_ARROWS.on()) edgeArrow(graphics, px, py, point.color());
                continue;
            }
            int ix = (int) Math.round(px), iy = (int) Math.round(py);
            double distance = (mouseX - px) * (mouseX - px) + (mouseY - py) * (mouseY - py);
            boolean over = inside(mouseX, mouseY, viewX, viewY, viewWidth, viewHeight) && distance <= best;
            if (over) { best = distance; hovered = original; }
            if (!over) drawIcon(graphics, point, ix, iy, shown, false);
        }
        if (hovered != null) {
            // Redrawn last so that a marker under the pointer is never half hidden by a neighbour.
            var point = CompassPreferences.customized(hovered);
            drawIcon(graphics, point, (int) Math.round(screenX(liveX(point))), (int) Math.round(screenZ(liveZ(point))),
                    !CompassPreferences.entry(hovered.id()).hidden() && CompassPreferences.categoryEnabled(hovered), true);
        }
        if (MapSettings.LABELS.on() && zoom >= 0.6) {
            for (var original : drawn) {
                var point = CompassPreferences.customized(original);
                double px = screenX(liveX(point)), py = screenZ(liveZ(point));
                if (!inView(px, py) || original == hovered) continue;
                boolean shown = !CompassPreferences.entry(original.id()).hidden() && CompassPreferences.categoryEnabled(original);
                label(graphics, point.title(), (int) Math.round(px), (int) Math.round(py), shown ? UiRenderer.TEXT : UiRenderer.TEXT_SECONDARY);
            }
        }
        if (hovered != null) label(graphics, CompassPreferences.customized(hovered).title(),
                (int) Math.round(screenX(liveX(hovered))), (int) Math.round(screenZ(liveZ(hovered))), UiRenderer.TEXT);
    }

    // ---- fast travel ----

    /** What the map is for when it was opened at a terminal: where the player stands, what a trip costs and what a click does. */
    record Travel(BlockPos origin, int cost, java.util.function.Consumer<FastTravelPointsPayload.Point> go) {}

    private Travel travel;
    private FastTravelPointsPayload.Point travelHovered;

    void travel(Travel travel) { this.travel = travel; }

    /** The terminals found so far, in this dimension; they are on the map wherever it is opened, and a click travels only from a terminal. */
    private void drawTravelNodes(GuiGraphics graphics) {
        travelHovered = null;
        String dimension = shown();
        double best = 100;
        var drawn = new ArrayList<FastTravelPointsPayload.Point>();
        for (var point : FastTravelClientState.points()) if (point.dimension().equals(dimension)) drawn.add(point);
        float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 260.0));
        for (var point : drawn) {
            double px = screenX(point.pos().getX() + 0.5), py = screenZ(point.pos().getZ() + 0.5);
            if (!inView(px, py)) continue;
            boolean here = travel != null && point.pos().equals(travel.origin);
            double distance = (mouseX - px) * (mouseX - px) + (mouseY - py) * (mouseY - py);
            boolean over = inside(mouseX, mouseY, viewX, viewY, viewWidth, viewHeight) && distance <= best;
            if (over) { best = distance; travelHovered = point; }
            int ix = (int) Math.round(px), iy = (int) Math.round(py);
            graphics.pose().pushPose();
            graphics.pose().translate(ix, iy, 0);
            float size = MapSettings.ICON_SCALE.scale() * (over ? 1.5F : 1F);
            if (size != 1F) graphics.pose().scale(size, size, 1);
            // A hologram diamond in a ring that breathes; the terminal the player stands at is solid.
            diamond(graphics, 0, 0, 7, (int) (40 + 50 * pulse) << 24 | 0x5DDEFF);
            diamond(graphics, 0, 0, 5, 0xE0081018);
            diamond(graphics, 0, 0, 4, 0xFF5DDEFF);
            diamond(graphics, 0, 0, here ? 2 : 1, 0xFF081018);
            graphics.pose().popPose();
            if (MapSettings.LABELS.on() && zoom >= 0.3 || over) label(graphics, point.name(), ix, iy + 4, over ? UiRenderer.TEXT : 0xFF9CEBFF);
        }
        if (travelHovered != null) hovered = null;
    }

    private void travelTooltip(GuiGraphics graphics) {
        var lines = new ArrayList<Component>();
        lines.add(Component.literal(travelHovered.name()));
        lines.add(Component.translatable("screen.cyberneticenhancements.fast_travel.node").withStyle(net.minecraft.ChatFormatting.AQUA));
        lines.add(Component.literal(travelHovered.pos().getX() + ", " + travelHovered.pos().getY() + ", " + travelHovered.pos().getZ()).withStyle(net.minecraft.ChatFormatting.GRAY));
        if (travel != null) {
            if (travelHovered.pos().equals(travel.origin)) lines.add(Component.translatable("screen.cyberneticenhancements.fast_travel.here").withStyle(net.minecraft.ChatFormatting.GRAY));
            else {
                lines.add(Component.translatable("screen.cyberneticenhancements.fast_travel.cost", travel.cost).withStyle(net.minecraft.ChatFormatting.GOLD));
                lines.add(Component.translatable("screen.cyberneticenhancements.fast_travel.click").withStyle(net.minecraft.ChatFormatting.GREEN));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private static boolean weight(CompassLandmark point) {
        return point.kind() == CompassLandmark.Kind.QUEST || point.kind() == CompassLandmark.Kind.TURN_IN;
    }

    private void label(GuiGraphics graphics, String title, int px, int py, int color) { label(graphics, title, px, py, 1F, color); }

    private void label(GuiGraphics graphics, String title, int px, int py, float size, int color) {
        int textWidth = font.width(title);
        graphics.pose().pushPose();
        graphics.pose().translate(px, py + 8, 0);
        float scale = size * MapSettings.FONT_SCALE.scale();
        graphics.pose().scale(scale, scale, 1);
        graphics.fill(-textWidth / 2 - 2, -1, textWidth / 2 + textWidth % 2 + 2, 9, 0x99081018);
        graphics.drawString(font, title, -textWidth / 2, 0, color, false);
        graphics.pose().popPose();
    }

    /** A hovered marker is drawn half as large again; nothing else about it changes. */
    private void drawIcon(GuiGraphics graphics, CompassLandmark point, int px, int py, boolean shown, boolean enlarged) {
        int color = (shown ? 0xFF000000 : 0x80000000) | point.color() & 0xFFFFFF;
        graphics.pose().pushPose();
        graphics.pose().translate(px, py, 0);
        float size = MapSettings.ICON_SCALE.scale() * (enlarged ? 1.5F : 1F);
        if (size != 1F) graphics.pose().scale(size, size, 1);
        switch (point.kind()) {
            case BANNER -> CompassHudOverlay.renderBannerIcon(graphics, -4, -4, point.color());
            case CITY -> CompassHudOverlay.renderCityIcon(graphics, -4, -4, color);
            case MAX_TAC -> CompassHudOverlay.renderMaxTacIcon(graphics, -4, -4, color);
            case MEDTEC -> CompassHudOverlay.renderMedtecIcon(graphics, -4, -4, color);
            default -> {
                diamond(graphics, 0, 0, 5, 0xCC081018);
                diamond(graphics, 0, 0, 4, color);
                if (point.kind() == CompassLandmark.Kind.TURN_IN) diamond(graphics, 0, 0, 1, 0xFF081018);
            }
        }
        graphics.pose().popPose();
    }

    private static void diamond(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int row = -radius; row <= radius; row++) {
            int half = radius - Math.abs(row);
            graphics.fill(cx - half, cy + row, cx + half + 1, cy + row + 1, color);
        }
    }

    /** Quests and turn-ins that are off the picture are pointed at from its edge. */
    private void edgeArrow(GuiGraphics graphics, double px, double py, int color) {
        double cx = viewX + viewWidth / 2.0, cy = viewY + viewHeight / 2.0, dx = px - cx, dy = py - cy;
        double scale = Math.min((viewWidth / 2.0 - 10) / Math.max(1, Math.abs(dx)), (viewHeight / 2.0 - 10) / Math.max(1, Math.abs(dy)));
        int ex = (int) Math.round(cx + dx * scale), ey = (int) Math.round(cy + dy * scale);
        graphics.pose().pushPose();
        graphics.pose().translate(ex, ey, 0);
        graphics.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx) + Mth.HALF_PI));
        for (int row = 0; row < 5; row++) graphics.fill(-row, -4 + row, row + 1, -3 + row, 0xFF000000 | color & 0xFFFFFF);
        graphics.pose().popPose();
    }

    private void drawPlayer(GuiGraphics graphics, net.minecraft.world.entity.player.Player player) {
        double px = screenX(player.getX()), py = screenZ(player.getZ());
        if (!inView(px, py)) {
            edgeArrow(graphics, px, py, 0xFFFFFF);
            return;
        }
        float pulse = 0.5F + 0.5F * Mth.sin(System.nanoTime() / 4.0E8F);
        int ring = (int) (5 + pulse * 3);
        UiRenderer.outline(graphics, (int) Math.round(px) - ring, (int) Math.round(py) - ring, ring * 2 + 1, ring * 2 + 1, (int) (0x50 * (1 - pulse)) << 24 | 0x8BDAF8);
        graphics.pose().pushPose();
        graphics.pose().translate((float) px, (float) py, 0);
        // Facing north is a yaw of 180 and points up the picture; turning east swings it clockwise.
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(player.getViewYRot(1) - 180F));
        for (int row = 0; row < 11; row++) {
            int half = row / 2;
            graphics.fill(-half - 1, -6 + row - 1, half + 2, -6 + row + 1, 0xDD081018);
        }
        for (int row = 0; row < 10; row++) {
            int half = row / 2;
            graphics.fill(-half, -6 + row, half + 1, -5 + row, row < 3 ? 0xFFFFFFFF : UiRenderer.ACCENT);
        }
        graphics.pose().popPose();
    }

    private void overlay(GuiGraphics graphics) {
        // Coordinates under the pointer.
        if (MapSettings.MOUSE_LOCATION.on() && !settings.open() && inside(mouseX, mouseY, viewX, viewY, viewWidth, viewHeight)) {
            int bx = Mth.floor(worldX(mouseX)), bz = Mth.floor(worldZ(mouseY));
            Component coordinates = text("coordinates", bx, bz);
            graphics.fill(viewX + 4, viewY + viewHeight - 16, viewX + 10 + font.width(coordinates), viewY + viewHeight - 4, 0xAA081018);
            graphics.drawString(font, coordinates, viewX + 7, viewY + viewHeight - 14, UiRenderer.TEXT, false);
        }
        // A scale bar: the longest round distance that fits in about 110 pixels.
        if (!MapSettings.SCALE_BAR.on()) return;
        int blocks = SCALES[0];
        for (int scale : SCALES) if (scale * zoom <= 110) blocks = scale;
        int length = (int) Math.round(blocks * zoom), right = viewX + viewWidth - 8, baseY = viewY + viewHeight - 9;
        Component label = text("scale", blocks);
        graphics.fill(right - length - 3 - font.width(label) / 2, baseY - 16, right + 3, baseY + 4, 0x66081018);
        graphics.fill(right - length, baseY, right, baseY + 1, UiRenderer.TEXT);
        graphics.fill(right - length, baseY - 3, right - length + 1, baseY + 1, UiRenderer.TEXT);
        graphics.fill(right - 1, baseY - 3, right, baseY + 1, UiRenderer.TEXT);
        graphics.drawString(font, label, right - length / 2 - font.width(label) / 2, baseY - 13, UiRenderer.TEXT_SECONDARY, false);
    }

    private void tooltip(GuiGraphics graphics, net.minecraft.world.entity.player.Player player) {
        var point = CompassPreferences.customized(hovered);
        var preference = CompassPreferences.entry(hovered.id());
        var lines = new ArrayList<Component>();
        lines.add(Component.literal(point.title()));
        lines.add(Component.translatable("screen.cyberneticenhancements.archive.compass.type." + (point.kind().ordinal() + 1)).withStyle(net.minecraft.ChatFormatting.AQUA));
        lines.add(Component.literal(point.pos().getX() + ", " + point.pos().getY() + ", " + point.pos().getZ()).withStyle(net.minecraft.ChatFormatting.GRAY));
        int distance = (int) Math.round(Math.hypot(point.pos().getX() + 0.5 - player.getX(), point.pos().getZ() + 0.5 - player.getZ()));
        lines.add(text("distance", distance).withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(text(preference.hidden() ? "status_hidden" : !CompassPreferences.categoryEnabled(hovered) ? "status_category" : "status_shown")
                .withStyle(preference.hidden() || !CompassPreferences.categoryEnabled(hovered) ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.GREEN));
        lines.add(text(preference.hidden() ? "hint_show" : "hint_hide").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        if (hovered.kind() == CompassLandmark.Kind.CUSTOM || hovered.kind() == CompassLandmark.Kind.DEATH || hovered.kind() == CompassLandmark.Kind.BANNER)
            lines.add(text("hint_remove").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    // ---- input ----

    private void zoomBy(double factor) {
        zoom = Mth.clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
    }

    /** The cave layer under the pointer: the slider's whole length spans every layer there is. */
    private void slide(double mx) {
        int steps = Math.max(1, (sliderHigh - sliderLow) / WorldMapCache.LAYER_SPAN);
        double fraction = Mth.clamp((mx - sliderX - 3) / (sliderWidth - 6), 0, 1);
        int value = sliderLow + (int) Math.round(fraction * steps) * WorldMapCache.LAYER_SPAN;
        if (away()) WorldMapCache.viewLayer(shown(), value); else WorldMapCache.setLayer(value);
    }

    boolean click(double mx, double my, int button) {
        if (settings.open()) return settings.click(mx, my, button);
        if (button == 0 && sliderWidth > 0 && inside(mx, my, sliderX, sliderY, sliderWidth, TOOLBAR)) { sliding = true; slide(mx); return true; }
        if (button == 0) {
            for (var action : actions) {
                if (inside(mx, my, action.x, action.y, action.width, action.height)) {
                    action.run.run();
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
                    return true;
                }
            }
        }
        if (!inside(mx, my, viewX, viewY, viewWidth, viewHeight)) return false;
        if (button == 0) {
            pressing = true; moved = 0;
            return true;
        }
        if (button != 1) return false;
        if (travelHovered != null) return true;
        var sounds = Minecraft.getInstance().getSoundManager();
        if (hovered != null) {
            if (hovered.kind() == CompassLandmark.Kind.CUSTOM || hovered.kind() == CompassLandmark.Kind.DEATH || hovered.kind() == CompassLandmark.Kind.BANNER) {
                CompassPreferences.delete(hovered);
                refreshed = 0;
                sounds.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.8F));
            } else sounds.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F, 0.5F));
            return true;
        }
        if (away()) return true;
        var level = Minecraft.getInstance().level;
        int bx = Mth.floor(worldX(mx)), bz = Mth.floor(worldZ(my));
        int by = level.hasChunk(bx >> 4, bz >> 4) ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) : Minecraft.getInstance().player.getBlockY();
        CustomCompassMarkers.add(new BlockPos(bx, by, bz));
        refreshed = 0;
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.4F));
        return true;
    }

    boolean drag(double mx, double my, double dx, double dy) {
        if (settings.open()) return settings.drag(mx, my);
        if (sliding) { slide(mx); return true; }
        if (!pressing) return false;
        moved += Math.abs(dx) + Math.abs(dy);
        if (moved > 3) {
            following = false;
            camX -= dx / zoom;
            camZ -= dy / zoom;
        }
        return true;
    }

    /** A press that never turned into a drag is a click: on a marker it shows or hides it on the compass. */
    void release() {
        sliding = false;
        boolean wasOpen = settings.open();
        settings.release();
        if (wasOpen) { pressing = false; return; }
        if (!pressing) return;
        pressing = false;
        if (moved > 3) return;
        if (travelHovered != null) {
            if (travel != null && !travelHovered.pos().equals(travel.origin)) travel.go.accept(travelHovered);
            return;
        }
        if (hovered == null) return;
        CompassPreferences.toggleHidden(hovered.id());
        refreshed = 0;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
    }

    boolean scroll(double mx, double my, double amount) {
        if (settings.open()) return settings.scroll(amount);
        if (!inside(mx, my, viewX, viewY, viewWidth, viewHeight)) return false;
        double beforeX = worldX(mx), beforeZ = worldZ(my);
        zoomBy(amount > 0 ? 1.25 : 1 / 1.25);
        if (!following) {
            // Zooming keeps the spot under the pointer where it is.
            camX = beforeX - (mx - viewX - viewWidth / 2.0) / zoom;
            camZ = beforeZ - (my - viewY - viewHeight / 2.0) / zoom;
        }
        return true;
    }

    boolean key(int key, int scan, int modifiers) {
        if (settings.open()) return settings.key(key);
        if (key == 61 || key == 334) { zoomBy(1.5); return true; }
        if (key == 45 || key == 333) { zoomBy(1 / 1.5); return true; }
        return false;
    }

    /** Esc closes the settings before anything else; true when there was something to close. */
    boolean closeSettings() {
        if (!settings.open()) return false;
        settings.open(false);
        return true;
    }

    void hide() {
        settings.open(false);
        pressing = false;
        hovered = null;
        wasHovering = false;
    }
}
