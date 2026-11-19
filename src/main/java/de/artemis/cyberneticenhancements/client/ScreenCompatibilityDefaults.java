package de.artemis.cyberneticenhancements.client;

import java.util.function.Predicate;

/** Initial/reset defaults only. Never overrides a saved config choice. */
public record ScreenCompatibilityDefaults(boolean deathScreen, boolean loadingScreen, boolean titlePresentation) {
    public static ScreenCompatibilityDefaults forMods(Predicate<String> installed) {
        boolean fancyMenu = installed.test("fancymenu");
        boolean loading = !fancyMenu && !installed.test("drippyloadingscreen");
        boolean title = !fancyMenu && !installed.test("packmenu") && !installed.test("custommainmenu");
        return new ScreenCompatibilityDefaults(true, loading, title);
    }

    public static ScreenCompatibilityDefaults detected() {
        var mods = net.neoforged.fml.ModList.get();
        var loading = net.neoforged.fml.loading.FMLLoader.getLoadingModList();
        return forMods(id -> mods != null ? mods.isLoaded(id)
                : loading != null && loading.getMods().stream().anyMatch(mod -> mod.getModId().equals(id)));
    }
}
