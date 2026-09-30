package fr.univ.td2.i18n.localizer.report;

import fr.univ.td2.i18n.localizer.orchestrate.LocaleTranslationResult;

import java.io.PrintStream;
import java.util.List;

public record LocalizationReport(List<LocaleTranslationResult> results) {

    public void printTo(PrintStream out) {
        out.println("=== Rapport de localisation ===");
        results.forEach(result -> {
            out.printf("Locale %s -> %s%n", result.localeCode(), result.targetFile());
            if (!result.isSuccess()) {
                out.printf("  ECHEC : %s%n", result.failureReason());
                return;
            }
            out.printf("  ajoutees=%d deja-presentes=%d rejetees=%d%n",
                    result.addedCount(), result.alreadyPresentCount(), result.rejected().size());
            result.rejected().forEach((key, reason) -> out.printf("    rejet %s : %s%n", key, reason));
        });
    }
}
