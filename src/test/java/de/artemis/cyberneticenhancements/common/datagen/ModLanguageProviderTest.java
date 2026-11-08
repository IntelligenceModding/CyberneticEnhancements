package de.artemis.cyberneticenhancements.common.datagen;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

public class ModLanguageProviderTest {
    @Test
    public void literalTranslationKeysAreUnique() throws Exception {
        String source = Files.readString(Path.of("src/main/java/de/artemis/cyberneticenhancements/common/datagen/ModLanguageProvider.java"));
        var matcher = Pattern.compile("\\badd\\(\\s*\"([^\"]+)\"").matcher(source);
        Set<String> keys = new HashSet<>();
        while (matcher.find()) {
            assertTrue("Duplicate translation key: " + matcher.group(1), keys.add(matcher.group(1)));
        }
        assertTrue("Expected the full language provider to be checked", keys.size() > 1000);
    }
}
