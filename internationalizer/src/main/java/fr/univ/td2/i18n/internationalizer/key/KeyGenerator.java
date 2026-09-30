package fr.univ.td2.i18n.internationalizer.key;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Construit un identifiant de ressource lisible a partir du texte d'un message. */
public final class KeyGenerator {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9]+");

    private KeyGenerator() {
    }

    public static String slugify(String message) {
        String ascii = DIACRITICS.matcher(Normalizer.normalize(message, Normalizer.Form.NFD)).replaceAll("");
        String[] words = NON_WORD.split(ascii.toLowerCase());
        String slug = Arrays.stream(words)
                .filter(w -> !w.isBlank())
                .limit(6)
                .collect(Collectors.joining("_"));
        return slug.isBlank() ? "msg" : slug;
    }
}
