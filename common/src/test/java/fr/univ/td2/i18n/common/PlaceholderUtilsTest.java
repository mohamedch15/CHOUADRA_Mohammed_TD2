package fr.univ.td2.i18n.common;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderUtilsTest {

    @Test
    void extractsIndexesInOrder() {
        assertEquals(Set.of(0, 1), PlaceholderUtils.indexes("Commande {0} confirmee pour {1} articles."));
    }

    @Test
    void noPlaceholdersMeansEmptySet() {
        assertTrue(PlaceholderUtils.indexes("Bienvenue !").isEmpty());
    }

    @Test
    void detectsMatchingPlaceholders() {
        assertTrue(PlaceholderUtils.hasSamePlaceholders("Bonjour, {0} !", "Hello, {0}!"));
    }

    @Test
    void detectsMissingPlaceholderInTranslation() {
        assertFalse(PlaceholderUtils.hasSamePlaceholders("Bonjour, {0} !", "Hello!"));
    }

    @Test
    void detectsExtraPlaceholderInTranslation() {
        assertFalse(PlaceholderUtils.hasSamePlaceholders("Bonjour !", "Hello {0}!"));
    }
}
