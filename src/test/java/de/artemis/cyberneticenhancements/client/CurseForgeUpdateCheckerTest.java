package de.artemis.cyberneticenhancements.client;

import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;

public class CurseForgeUpdateCheckerTest {
    private static String file(long id, String version, String game, String loader, String type) {
        return "{\"id\":" + id + ",\"name\":\"cyberneticenhancements-" + version + "-neoforge-mc" + game + ".jar\",\"versions\":[\"" + game + "\",\"" + loader + "\"],\"type\":\"" + type + "\"}";
    }
    @Test public void selectsNewestCompatiblePublishedReleaseRatherThanAnotherLoaderOrGameVersion() {
        var json = JsonParser.parseString("{\"id\":1792556,\"files\":[" +
                file(1,"0.2b","1.21.1","NeoForge","beta") + "," +
                file(2,"0.10b","1.21.1","NeoForge","beta") + "," +
                file(3,"9.0","1.21.1","Forge","release") + "," +
                file(4,"9.0","1.22","NeoForge","release") + "," +
                file(5,"9.0-alpha","1.21.1","NeoForge","alpha") + ",{}]}");
        var result = CurseForgeUpdateChecker.latest(json,"1.21.1").orElseThrow();
        assertEquals("0.10b",result.version());
        assertEquals("https://www.curseforge.com/minecraft/mc-mods/cybernetic-enhancements/files/2",result.url());
    }
    @Test public void currentAndDevelopmentVersionsNeverProduceDowngradeNotices() {
        assertTrue(CurseForgeUpdateChecker.newer("0.1b", "0.0.1"));
        assertFalse(CurseForgeUpdateChecker.newer("0.1b", "0.2b"));
        assertFalse(CurseForgeUpdateChecker.newer("0.2b", "0.2b"));
        assertTrue(CurseForgeUpdateChecker.newer("0.10b", "0.9b"));
        assertTrue(CurseForgeUpdateChecker.newer("1.0", "1.0-beta"));
    }
    @Test public void unavailableOrWrongProjectDataDoesNotClaimAnUpdate() {
        assertTrue(CurseForgeUpdateChecker.latest(JsonParser.parseString("{}"),"1.21.1").isEmpty());
        assertTrue(CurseForgeUpdateChecker.latest(JsonParser.parseString("{\"id\":1,\"files\":[]}"),"1.21.1").isEmpty());
        assertTrue(CurseForgeUpdateChecker.latest(JsonParser.parseString("{\"id\":1792556,\"files\":[{}]}"),"1.21.1").isEmpty());
    }
}
