package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.CompassLandmark;
import de.artemis.cyberneticenhancements.client.ui.CompassMarkerOverrides;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CompassPreferences {
    private static Object connection;
    private static String scope = "";
    private static List<? extends String> lastValues;
    private static CompassMarkerOverrides overrides = new CompassMarkerOverrides();
    private static final int[] COLORS = {0xFF8BDAF8, 0xFFF0B36A, 0xFF64D99C, 0xFFF17883, 0xFFC49AF5, 0xFFF1F4F8};

    private CompassPreferences() {
    }

    static String scope() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return "";
        if (connection != minecraft.getConnection()) {
            connection = minecraft.getConnection();
            String address = minecraft.getSingleplayerServer() != null
                    ? minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString()
                    : minecraft.getCurrentServer() == null ? "local" : minecraft.getCurrentServer().ip;
            scope = UUID.nameUUIDFromBytes((address + "/" + minecraft.player.getUUID()).getBytes(StandardCharsets.UTF_8)).toString();
        }
        return scope;
    }

    private static CompassMarkerOverrides overrides() {
        var values = ClientConfig.COMPASS_MARKER_OVERRIDES.get();
        if (values != lastValues) {
            overrides = CompassMarkerOverrides.parse(values);
            lastValues = values;
        }
        return overrides;
    }

    public static List<CompassLandmark> all() {
        var result = new ArrayList<CompassLandmark>();
        result.addAll(CustomCompassMarkers.all());
        var level = Minecraft.getInstance().level;
        if (level != null) {
            for (var entity : level.entitiesForRendering()) {
                if (entity instanceof de.artemis.cyberneticenhancements.common.entity.MaxTacEntity officer && officer.isAlive() && !officer.isInvisible())
                    result.add(new CompassLandmark("maxtac:" + officer.getUUID(), officer.getName().getString(), level.dimension().location().toString(),
                            officer.blockPosition(), CompassLandmark.Kind.MAX_TAC, 0xFFF17883));
                if (entity instanceof de.artemis.cyberneticenhancements.common.entity.TraumaTeamEntity medic
                        && Minecraft.getInstance().player != null && medic.protects(Minecraft.getInstance().player.getUUID()) && !medic.isInvisible())
                    result.add(new CompassLandmark("medtec:" + medic.getUUID(), medic.getName().getString(), level.dimension().location().toString(),
                            medic.blockPosition(), CompassLandmark.Kind.MEDTEC, 0xFF66F5A5));
            }
        }
        for (var target : QuestNavigationClientState.get().targets())
            result.add(new CompassLandmark(target.questId(), target.title(), target.dimension(), target.pos(),
                    target.turnIn() ? CompassLandmark.Kind.TURN_IN : CompassLandmark.Kind.QUEST,
                    target.turnIn() ? UiRenderer.ACCENT : UiRenderer.WARNING));
        for (var banner : BannerWaypointsClientState.get().waypoints())
            result.add(new CompassLandmark(banner.id(), banner.name(), banner.dimension(), banner.pos(), CompassLandmark.Kind.BANNER, banner.color()));
        var city = CityNavigationClientState.get();
        if (city.present()) result.add(new CompassLandmark(CityNavigationClientState.MARKER_ID,
                net.minecraft.network.chat.Component.translatable("hud.cyberneticenhancements.night_city").getString(), city.dimension(), city.pos(),
                CompassLandmark.Kind.CITY, 0xFFC49AF5));
        String currentScope = scope();
        var preferences = overrides();
        return result.stream().filter(point -> !preferences.get(currentScope, point.id()).deleted()).toList();
    }

    public static CompassMarkerOverrides.Entry entry(String id) {
        var entry = overrides().get(scope(), id);
        return id.equals(CityNavigationClientState.MARKER_ID) && !CityNavigationClientState.get().enabled()
                ? new CompassMarkerOverrides.Entry(true, entry.label(), entry.color()) : entry;
    }

    public static CompassLandmark customized(CompassLandmark point) {
        var entry = entry(point.id());
        return new CompassLandmark(point.id(), entry.label().isEmpty() ? point.title() : entry.label(), point.dimension(), point.pos(), point.kind(),
                entry.color() == null ? point.color() : entry.color());
    }

    public static boolean categoryEnabled(CompassLandmark point) {
        return switch (point.kind()) {
            case QUEST -> ClientConfig.COMPASS_QUESTS.get();
            case TURN_IN -> ClientConfig.COMPASS_TURN_INS.get();
            case BANNER -> ClientConfig.COMPASS_BANNERS.get();
            case CITY -> ClientConfig.COMPASS_CITIES.get();
            case MAX_TAC -> ClientConfig.COMPASS_MAX_TAC.get();
            case MEDTEC -> ClientConfig.COMPASS_MEDTEC.get();
            case CUSTOM, DEATH -> true;
        };
    }

    public static List<CompassLandmark> visible() {
        return all().stream().filter(CompassPreferences::categoryEnabled).filter(point -> !entry(point.id()).hidden())
                .map(CompassPreferences::customized).toList();
    }

    private static void update(String id, CompassMarkerOverrides.Entry entry) {
        overrides().set(scope(), id, entry);
        ClientConfig.COMPASS_MARKER_OVERRIDES.set(overrides.serialize());
        ClientConfig.SPEC.save();
    }

    public static void toggleHidden(String id) {
        var entry = entry(id);
        if (id.equals(CityNavigationClientState.MARKER_ID)) {
            update(id, new CompassMarkerOverrides.Entry(false, entry.label(), entry.color()));
            CityNavigationClientState.setEnabled(entry.hidden());
            return;
        }
        update(id, new CompassMarkerOverrides.Entry(!entry.hidden(), entry.label(), entry.color()));
    }

    public static void rename(String id, String label) {
        var entry = overrides().get(scope(), id);
        update(id, new CompassMarkerOverrides.Entry(entry.hidden(), label, entry.color()));
    }

    public static void cycleColor(String id) {
        var entry = overrides().get(scope(), id);
        Integer next = COLORS[0];
        if (entry.color() != null) {
            next = null;
            for (int index = 0; index < COLORS.length - 1; index++) if (COLORS[index] == entry.color()) next = COLORS[index + 1];
        }
        update(id, new CompassMarkerOverrides.Entry(entry.hidden(), entry.label(), next));
    }

    public static void restoreMarkers() {
        overrides().clear(scope());
        ClientConfig.COMPASS_MARKER_OVERRIDES.set(overrides.serialize());
        ClientConfig.SPEC.save();
        var city = CityNavigationClientState.get();
        if (city.present()) CityNavigationClientState.setEnabled(!city.visited());
    }

    public static void delete(CompassLandmark point) {
        String id = point.id();
        if(point.kind()==CompassLandmark.Kind.CUSTOM||point.kind()==CompassLandmark.Kind.DEATH){
            CustomCompassMarkers.delete(id);
            update(id,new CompassMarkerOverrides.Entry(false,"",null));
            return;
        }
        boolean saved = point.kind() == CompassLandmark.Kind.BANNER || id.startsWith("chromium:") || id.startsWith("cache:");
        if (saved) {
            // Remove saved markers themselves; an explicit banner/cache interaction can add them again later.
            update(id, new CompassMarkerOverrides.Entry(false, "", null));
            var quests = QuestNavigationClientState.get();
            QuestNavigationClientState.update(new de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload(
                    quests.targets().stream().filter(target -> !target.questId().equals(id)).toList(), quests.turnIns()));
            BannerWaypointsClientState.update(new de.artemis.cyberneticenhancements.common.network.BannerWaypointsPayload(
                    BannerWaypointsClientState.get().waypoints().stream().filter(target -> !target.id().equals(id)).toList()));
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new de.artemis.cyberneticenhancements.common.network.DeleteCompassMarkerPayload(id));
        } else {
            // Live quest/NPC entries have no independent saved waypoint. Dismiss the entry, preserving gameplay.
            update(id, new CompassMarkerOverrides.Entry(false, "", null, true));
            if (id.equals(CityNavigationClientState.MARKER_ID)) CityNavigationClientState.setEnabled(false);
        }
    }

    public static void clearCache() {
        connection = null;
        scope = "";
        lastValues = null;
        overrides = new CompassMarkerOverrides();
    }
}
