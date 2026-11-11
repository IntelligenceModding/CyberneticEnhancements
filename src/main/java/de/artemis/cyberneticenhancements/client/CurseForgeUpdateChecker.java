package de.artemis.cyberneticenhancements.client;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

 public final class CurseForgeUpdateChecker {
    static final int PROJECT_ID = 1792556;
    private static final URI ENDPOINT = URI.create("https://api.cfwidget.com/" + PROJECT_ID);
    private static final String FILES_URL = "https://www.curseforge.com/minecraft/mc-mods/cybernetic-enhancements/files/";
    private static CompletableFuture<Optional<Release>> cached;
    private static String cachedMinecraft;
    private static long expires;

    public record Release(String version, String url) {}

    private CurseForgeUpdateChecker() {}

    public static synchronized CompletableFuture<Optional<Release>> check(String minecraftVersion, String installedVersion) {
        long now = System.nanoTime();
        if (cached == null || !minecraftVersion.equals(cachedMinecraft) || now - expires >= 0) {
            cachedMinecraft = minecraftVersion;
            expires = now + TimeUnit.HOURS.toNanos(1);
            cached = CompletableFuture.supplyAsync(() -> fetch(minecraftVersion), net.minecraft.Util.backgroundExecutor());
        }
        return cached.thenApply(release -> release.filter(value -> newer(value.version(), installedVersion)));
    }

    private static Optional<Release> fetch(String minecraftVersion) {
        try {
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
            var request = HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "CyberneticEnhancements-UpdateChecker/1")
                    .header("Accept", "application/json").GET().build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length > 1_048_576) return Optional.empty();
            return latest(JsonParser.parseString(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8)), minecraftVersion);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            LogUtils.getLogger().debug("CurseForge update metadata is unavailable", exception);
        }
        return Optional.empty();
    }

    static Optional<Release> latest(JsonElement json, String minecraftVersion) {
        if (!json.isJsonObject()) return Optional.empty();
        var root = json.getAsJsonObject();
        if (!root.has("id") || root.get("id").getAsInt() != PROJECT_ID || !root.has("files") || !root.get("files").isJsonArray()) return Optional.empty();
        Pattern filename = Pattern.compile("^cyberneticenhancements-([0-9][0-9A-Za-z.+_-]*)-neoforge-mc" + Pattern.quote(minecraftVersion) + "\\.jar$");
        Release best = null;
        for (var entry : root.getAsJsonArray("files")) {
            try {
                var file = entry.getAsJsonObject();
                String type = file.get("type").getAsString();
                if (!type.equals("release") && !type.equals("beta")) continue;
                boolean game = false, loader = false;
                for (var version : file.getAsJsonArray("versions")) {
                    game |= version.getAsString().equals(minecraftVersion);
                    loader |= version.getAsString().equalsIgnoreCase("NeoForge");
                }
                if (!game || !loader) continue;
                var match = filename.matcher(file.get("name").getAsString());
                if (!match.matches()) continue;
                long id = file.get("id").getAsLong();
                if (id <= 0) continue;
                Release candidate = new Release(match.group(1), FILES_URL + id);
                if (best == null || newer(candidate.version(), best.version())) best = candidate;
            } catch (RuntimeException ignored) {

            }
        }
        return Optional.ofNullable(best);
    }

    static boolean newer(String available, String installed) {
        return new DefaultArtifactVersion(available).compareTo(new DefaultArtifactVersion(installed)) > 0;
    }
}
