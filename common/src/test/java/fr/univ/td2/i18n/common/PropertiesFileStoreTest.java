package fr.univ.td2.i18n.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesFileStoreTest {

    @Test
    void roundTripPreservesOrderAndAccents(@TempDir Path dir) {
        Path file = dir.resolve("messages.properties");
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("app.greeting", "Bonjour, {0} !");
        entries.put("app.farewell", "A bientot, éàç !");
        entries.put("app.quote", "Il a dit \"salut\" et 'coucou'.");

        PropertiesFileStore.save(file, entries, "Ressources de demonstration");
        Map<String, String> reloaded = PropertiesFileStore.load(file);

        assertEquals(entries, reloaded);
        assertEquals(new java.util.ArrayList<>(entries.keySet()), new java.util.ArrayList<>(reloaded.keySet()));
    }

    @Test
    void savedFileIsReadableAsUtf8Text(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("messages.properties");
        Map<String, String> entries = Map.of("app.title", "Événement spécial");
        PropertiesFileStore.save(file, entries, null);

        String content = Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("Événement spécial"), "les accents doivent rester lisibles, pas en \\uXXXX");
    }

    @Test
    void loadMissingFileReturnsEmptyMap(@TempDir Path dir) {
        Map<String, String> result = PropertiesFileStore.load(dir.resolve("absent.properties"));
        assertTrue(result.isEmpty());
    }

    @Test
    void loadIgnoresCommentsAndBlankLines(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("messages.properties");
        Files.writeString(file, "# commentaire\n\napp.key=valeur\n! autre commentaire\n");

        Map<String, String> result = PropertiesFileStore.load(file);
        assertEquals(1, result.size());
        assertEquals("valeur", result.get("app.key"));
        assertFalse(result.containsKey("# commentaire"));
    }

    @Test
    void escapesEqualsAndColonInKeyAndValue(@TempDir Path dir) {
        Path file = dir.resolve("messages.properties");
        Map<String, String> entries = Map.of("app.ratio", "1:2 = deux fois plus");
        PropertiesFileStore.save(file, entries, null);
        assertEquals(entries, PropertiesFileStore.load(file));
    }
}
