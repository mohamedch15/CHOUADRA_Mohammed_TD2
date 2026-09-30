package fr.univ.td2.i18n.internationalizer.literal;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatStringConverterTest {

    @Test
    void convertsTwoPlaceholdersAndStripsTrailingNewline() {
        Optional<FormatStringConverter.Result> result =
                FormatStringConverter.convert("Commande %s confirmee pour %d article(s).%n", 2);

        assertTrue(result.isPresent());
        assertEquals("Commande {0} confirmee pour {1} article(s).", result.get().messageFormatText());
        assertTrue(result.get().trailingNewline());
        assertEquals(2, result.get().placeholderCount());
    }

    @Test
    void convertsWithoutTrailingNewline() {
        Optional<FormatStringConverter.Result> result = FormatStringConverter.convert("Valeur : %d", 1);

        assertTrue(result.isPresent());
        assertEquals("Valeur : {0}", result.get().messageFormatText());
        assertFalse(result.get().trailingNewline());
    }

    @Test
    void keepsLiteralPercentSign() {
        Optional<FormatStringConverter.Result> result = FormatStringConverter.convert("Progression : 100%%", 0);

        assertTrue(result.isPresent());
        assertEquals("Progression : 100%", result.get().messageFormatText());
    }

    @Test
    void rejectsWhenArgumentCountMismatches() {
        Optional<FormatStringConverter.Result> result = FormatStringConverter.convert("Commande %s pour %d articles", 1);

        assertTrue(result.isEmpty());
    }

    @Test
    void noPlaceholdersWithNoArguments() {
        Optional<FormatStringConverter.Result> result = FormatStringConverter.convert("Bienvenue !%n", 0);

        assertTrue(result.isPresent());
        assertEquals("Bienvenue !", result.get().messageFormatText());
        assertEquals(0, result.get().placeholderCount());
    }
}
