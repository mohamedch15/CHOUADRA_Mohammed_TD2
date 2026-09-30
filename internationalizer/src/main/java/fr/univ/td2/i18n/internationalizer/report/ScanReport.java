package fr.univ.td2.i18n.internationalizer.report;

import fr.univ.td2.i18n.internationalizer.model.Finding;
import fr.univ.td2.i18n.internationalizer.model.FindingKind;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Regroupe et affiche les constats d'une execution, par categorie. */
public record ScanReport(List<Finding> findings) {

    public long count(FindingKind kind) {
        return findings.stream().filter(f -> f.kind() == kind).count();
    }

    public void printTo(PrintStream out) {
        Map<FindingKind, List<Finding>> byKind = findings.stream()
                .collect(Collectors.groupingBy(Finding::kind));

        out.println("=== Rapport d'internationalisation ===");
        out.printf("Chaines transformees   : %d%n", byKind.getOrDefault(FindingKind.TRANSLATED, List.of()).size());
        out.printf("Fichiers deja traites   : %d%n", byKind.getOrDefault(FindingKind.ALREADY_PROCESSED, List.of()).size());
        out.printf("Cas non geres (signales): %d%n", byKind.getOrDefault(FindingKind.UNSUPPORTED, List.of()).size());
        out.printf("Erreurs de lecture      : %d%n", byKind.getOrDefault(FindingKind.PARSE_ERROR, List.of()).size());

        printDetails(out, byKind, FindingKind.UNSUPPORTED, "Non gere");
        printDetails(out, byKind, FindingKind.PARSE_ERROR, "Erreur");
    }

    private void printDetails(PrintStream out, Map<FindingKind, List<Finding>> byKind, FindingKind kind, String label) {
        List<Finding> items = byKind.getOrDefault(kind, List.of());
        if (items.isEmpty()) {
            return;
        }
        out.println("--- " + label + " ---");
        items.forEach(f -> out.printf("  %s:%d - %s%n", f.file(), f.line(), f.message()));
    }
}
