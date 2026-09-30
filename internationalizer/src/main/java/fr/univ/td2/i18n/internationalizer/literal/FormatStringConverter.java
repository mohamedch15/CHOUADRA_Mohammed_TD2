package fr.univ.td2.i18n.internationalizer.literal;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convertit un format de type {@code printf} ("Commande %s pour %d articles.%n")
 * en un message {@link java.text.MessageFormat} ("Commande {0} pour {1} articles.").
 * Le %n final est retire : l'appel genere utilisera {@code println} pour le restituer.
 */
public final class FormatStringConverter {

    private static final Pattern SPECIFIER = Pattern.compile("%(?:%|n|[sdfboxXce])");

    private FormatStringConverter() {
    }

    public record Result(String messageFormatText, int placeholderCount, boolean trailingNewline) {
    }

    /**
     * @param printfFormat  le texte du litteral de format (sans les guillemets Java)
     * @param argumentCount nombre d'arguments varargs fournis a l'appel printf, pour verifier la coherence
     */
    public static Optional<Result> convert(String printfFormat, int argumentCount) {
        String working = printfFormat;
        boolean trailingNewline = false;
        if (working.endsWith("%n")) {
            working = working.substring(0, working.length() - 2);
            trailingNewline = true;
        }

        StringBuilder out = new StringBuilder();
        Matcher matcher = SPECIFIER.matcher(working);
        int placeholderIndex = 0;
        int lastEnd = 0;
        while (matcher.find()) {
            out.append(working, lastEnd, matcher.start());
            String specifier = matcher.group();
            switch (specifier) {
                case "%%" -> out.append('%');
                case "%n" -> out.append(System.lineSeparator());
                default -> out.append('{').append(placeholderIndex++).append('}');
            }
            lastEnd = matcher.end();
        }
        out.append(working.substring(lastEnd));

        if (placeholderIndex != argumentCount) {
            return Optional.empty();
        }
        return Optional.of(new Result(out.toString(), placeholderIndex, trailingNewline));
    }
}
