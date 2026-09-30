package fr.univ.td2.i18n.localizer.translate;

import fr.univ.td2.i18n.common.PlaceholderUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controle chaque traduction recue avant qu'elle ne soit integree aux ressources :
 * presence, contenu non vide, et conservation exacte des marqueurs {0}, {1}, etc.
 * Pure fonction de donnees en entree vers un resultat : aucun effet de bord.
 */
public final class TranslationValidator {

    private TranslationValidator() {
    }

    public record ValidationResult(Map<String, String> accepted, Map<String, String> rejected) {
    }

    private record Evaluation(String key, String translation, String rejectionReason) {
        boolean isAccepted() {
            return rejectionReason == null;
        }
    }

    public static ValidationResult validate(Map<String, String> requested, Map<String, String> received) {
        List<Evaluation> evaluations = requested.entrySet().stream()
                .map(entry -> evaluate(entry.getKey(), entry.getValue(), received.get(entry.getKey())))
                .toList();

        Map<String, String> accepted = new LinkedHashMap<>();
        Map<String, String> rejected = new LinkedHashMap<>();
        evaluations.forEach(ev -> {
            if (ev.isAccepted()) {
                accepted.put(ev.key(), ev.translation());
            } else {
                rejected.put(ev.key(), ev.rejectionReason());
            }
        });
        return new ValidationResult(accepted, rejected);
    }

    private static Evaluation evaluate(String key, String sourceValue, String translatedValue) {
        if (translatedValue == null) {
            return new Evaluation(key, null, "traduction manquante dans la reponse");
        }
        if (translatedValue.isBlank()) {
            return new Evaluation(key, null, "traduction vide");
        }
        if (!PlaceholderUtils.hasSamePlaceholders(sourceValue, translatedValue)) {
            return new Evaluation(key, null, "parametres {n} non preserves");
        }
        return new Evaluation(key, translatedValue, null);
    }
}
