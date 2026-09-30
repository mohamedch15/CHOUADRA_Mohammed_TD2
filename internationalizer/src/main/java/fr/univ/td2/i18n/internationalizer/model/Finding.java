package fr.univ.td2.i18n.internationalizer.model;

import java.nio.file.Path;

/** Un evenement observe pendant l'analyse : une transformation, un cas non gere, ou une erreur. */
public record Finding(Path file, int line, FindingKind kind, String message) {

    public static Finding translated(Path file, int line, String key) {
        return new Finding(file, line, FindingKind.TRANSLATED, "cle '" + key + "' extraite");
    }

    public static Finding unsupported(Path file, int line, String reason) {
        return new Finding(file, line, FindingKind.UNSUPPORTED, reason);
    }

    public static Finding alreadyProcessed(Path file) {
        return new Finding(file, 0, FindingKind.ALREADY_PROCESSED, "fichier deja internationalise, ignore");
    }

    public static Finding parseError(Path file, String reason) {
        return new Finding(file, 0, FindingKind.PARSE_ERROR, reason);
    }
}
