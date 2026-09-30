package fr.univ.td2.i18n.localizer.translate;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationValidatorTest {

    @Test
    void acceptsValidTranslationWithMatchingPlaceholders() {
        Map<String, String> requested = Map.of("app.greeting", "Bonjour, {0} !");
        Map<String, String> received = Map.of("app.greeting", "Hello, {0}!");

        TranslationValidator.ValidationResult result = TranslationValidator.validate(requested, received);

        assertEquals("Hello, {0}!", result.accepted().get("app.greeting"));
        assertTrue(result.rejected().isEmpty());
    }

    @Test
    void rejectsTranslationMissingAPlaceholder() {
        Map<String, String> requested = Map.of("app.greeting", "Bonjour, {0} !");
        Map<String, String> received = Map.of("app.greeting", "Bonjour !");

        TranslationValidator.ValidationResult result = TranslationValidator.validate(requested, received);

        assertTrue(result.accepted().isEmpty());
        assertTrue(result.rejected().containsKey("app.greeting"));
    }

    @Test
    void rejectsMissingKeyInResponse() {
        Map<String, String> requested = Map.of("app.a", "Un", "app.b", "Deux");
        Map<String, String> received = Map.of("app.a", "One");

        TranslationValidator.ValidationResult result = TranslationValidator.validate(requested, received);

        assertEquals(1, result.accepted().size());
        assertTrue(result.rejected().containsKey("app.b"));
    }

    @Test
    void rejectsBlankTranslation() {
        Map<String, String> requested = Map.of("app.a", "Un");
        Map<String, String> received = Map.of("app.a", "   ");

        TranslationValidator.ValidationResult result = TranslationValidator.validate(requested, received);

        assertFalse(result.rejected().isEmpty());
    }

    @Test
    void ignoresExtraKeysNotRequested() {
        Map<String, String> requested = Map.of("app.a", "Un");
        Map<String, String> received = Map.of("app.a", "One", "app.unexpected", "???");

        TranslationValidator.ValidationResult result = TranslationValidator.validate(requested, received);

        assertEquals(1, result.accepted().size());
        assertFalse(result.accepted().containsKey("app.unexpected"));
    }
}
