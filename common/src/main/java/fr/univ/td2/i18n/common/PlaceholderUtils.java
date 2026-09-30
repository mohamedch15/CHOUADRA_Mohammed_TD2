package fr.univ.td2.i18n.common;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extraction des marqueurs de substitution {@code {0}}, {@code {1}}, ... utilises
 * par {@link java.text.MessageFormat}. Sert a verifier qu'une traduction conserve
 * exactement les memes parametres que le message d'origine.
 */
public final class PlaceholderUtils {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)(?:,[^}]*)?}");

    private PlaceholderUtils() {
    }

    /** Renvoie l'ensemble ordonne des index de parametres presents dans le message. */
    public static Set<Integer> indexes(String message) {
        Set<Integer> found = new TreeSet<>();
        Matcher matcher = PLACEHOLDER.matcher(message);
        while (matcher.find()) {
            found.add(Integer.parseInt(matcher.group(1)));
        }
        return found;
    }

    public static boolean hasSamePlaceholders(String original, String translated) {
        return indexes(original).equals(indexes(translated));
    }
}
