package fr.univ.td2.i18n.localizer.orchestrate;

import fr.univ.td2.i18n.common.PropertiesFileStore;
import fr.univ.td2.i18n.localizer.client.AlbertClientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizationOrchestratorTest {

    @Test
    void translatesMissingKeysAndCreatesTargetFile(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("messages.properties");
        PropertiesFileStore.save(baseFile, Map.of("app.hello", "Bonjour !", "app.bye", "Au revoir !"), null);

        FakeAlbertClient client = FakeAlbertClient.returning(Map.of(
                "app.hello", "Hello!", "app.bye", "Goodbye!"));
        LocalizationOrchestrator orchestrator = new LocalizationOrchestrator(client, "fake-model");

        LocaleTranslationResult result = orchestrator.translateLocale(
                PropertiesFileStore.load(baseFile), baseFile, "en", false);

        assertTrue(result.isSuccess());
        assertEquals(2, result.addedCount());
        Map<String, String> target = PropertiesFileStore.load(LocalizationOrchestrator.targetFileFor(baseFile, "en"));
        assertEquals("Hello!", target.get("app.hello"));
        assertEquals("Goodbye!", target.get("app.bye"));
    }

    @Test
    void neverOverwritesAnExistingTranslationEvenIfModelReturnsOne(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("messages.properties");
        PropertiesFileStore.save(baseFile, Map.of("app.hello", "Bonjour !", "app.bye", "Au revoir !"), null);
        Path targetFile = LocalizationOrchestrator.targetFileFor(baseFile, "en");
        PropertiesFileStore.save(targetFile, Map.of("app.hello", "Hello! (validee par un humain)"), null);

        // Le faux client renvoie une traduction differente pour app.hello : elle ne doit jamais etre utilisee.
        FakeAlbertClient client = FakeAlbertClient.returning(Map.of(
                "app.hello", "Hi there (ne doit pas apparaitre)", "app.bye", "Goodbye!"));
        LocalizationOrchestrator orchestrator = new LocalizationOrchestrator(client, "fake-model");

        LocaleTranslationResult result = orchestrator.translateLocale(
                PropertiesFileStore.load(baseFile), baseFile, "en", false);

        assertEquals(1, result.addedCount());
        assertEquals(1, result.alreadyPresentCount());
        Map<String, String> target = PropertiesFileStore.load(targetFile);
        assertEquals("Hello! (validee par un humain)", target.get("app.hello"));
        assertEquals("Goodbye!", target.get("app.bye"));
    }

    @Test
    void invalidJsonResponseIsRejectedWithoutTouchingExistingFile(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("messages.properties");
        PropertiesFileStore.save(baseFile, Map.of("app.hello", "Bonjour !"), null);
        Path targetFile = LocalizationOrchestrator.targetFileFor(baseFile, "en");

        LocalizationOrchestrator orchestrator = new LocalizationOrchestrator(FakeAlbertClient.returningInvalidJson(), "fake-model");
        LocaleTranslationResult result = orchestrator.translateLocale(
                PropertiesFileStore.load(baseFile), baseFile, "en", false);

        assertFalse(result.isSuccess());
        assertFalse(Files.exists(targetFile), "aucun fichier ne doit etre cree a partir d'une reponse invalide");
    }

    @Test
    void networkFailureOnOneLocaleDoesNotBlockTheOthers(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("messages.properties");
        PropertiesFileStore.save(baseFile, Map.of("app.hello", "Bonjour !"), null);

        FakeAlbertClient client = new FakeAlbertClient(prompt -> {
            if (prompt.contains("(de)")) {
                throw new AlbertClientException("panne simulee pour l'allemand");
            }
            return "{\"app.hello\": \"Hello!\"}";
        });
        LocalizationOrchestrator orchestrator = new LocalizationOrchestrator(client, "fake-model");

        List<LocaleTranslationResult> results = orchestrator.translateAll(baseFile, List.of("en", "de"), false);

        LocaleTranslationResult en = results.stream().filter(r -> r.localeCode().equals("en")).findFirst().orElseThrow();
        LocaleTranslationResult de = results.stream().filter(r -> r.localeCode().equals("de")).findFirst().orElseThrow();
        assertTrue(en.isSuccess());
        assertFalse(de.isSuccess());
        assertTrue(Files.exists(LocalizationOrchestrator.targetFileFor(baseFile, "en")));
        assertFalse(Files.exists(LocalizationOrchestrator.targetFileFor(baseFile, "de")));
    }

    @Test
    void translationsPreservingMismatchedPlaceholdersAreRejectedNotWritten(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("messages.properties");
        PropertiesFileStore.save(baseFile, Map.of("app.greet", "Bonjour, {0} !"), null);

        FakeAlbertClient client = FakeAlbertClient.returning(Map.of("app.greet", "Hello!"));
        LocalizationOrchestrator orchestrator = new LocalizationOrchestrator(client, "fake-model");

        LocaleTranslationResult result = orchestrator.translateLocale(
                PropertiesFileStore.load(baseFile), baseFile, "en", false);

        assertTrue(result.isSuccess());
        assertEquals(0, result.addedCount());
        assertTrue(result.rejected().containsKey("app.greet"));
        assertFalse(Files.exists(LocalizationOrchestrator.targetFileFor(baseFile, "en")));
    }

    @Test
    void doesNothingWhenAllKeysAlreadyTranslated(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("messages.properties");
        PropertiesFileStore.save(baseFile, Map.of("app.hello", "Bonjour !"), null);
        Path targetFile = LocalizationOrchestrator.targetFileFor(baseFile, "en");
        PropertiesFileStore.save(targetFile, Map.of("app.hello", "Hello!"), null);

        FakeAlbertClient client = FakeAlbertClient.returning(Map.of("app.hello", "SHOULD NOT BE CALLED"));
        LocalizationOrchestrator orchestrator = new LocalizationOrchestrator(client, "fake-model");

        LocaleTranslationResult result = orchestrator.translateLocale(
                PropertiesFileStore.load(baseFile), baseFile, "en", false);

        assertEquals(0, result.addedCount());
        assertTrue(client.requestedUserPrompts.isEmpty(), "l'API ne doit pas etre appelee si rien n'est manquant");
    }
}
