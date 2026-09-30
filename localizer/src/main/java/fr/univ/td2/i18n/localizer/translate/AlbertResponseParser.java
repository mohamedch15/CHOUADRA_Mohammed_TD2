package fr.univ.td2.i18n.localizer.translate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Extrait un objet cle/valeur d'une reponse brute du modele, en tolerant un
 * habillage markdown (```json ... ```) que certains modeles ajoutent malgre la
 * consigne. Une reponse qui n'est pas un objet JSON valide est rejetee.
 */
public final class AlbertResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern CODE_FENCE = Pattern.compile("^```(?:json)?\\s*|\\s*```$", Pattern.MULTILINE);

    private AlbertResponseParser() {
    }

    public static Optional<Map<String, String>> parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return Optional.empty();
        }
        String cleaned = CODE_FENCE.matcher(rawContent.strip()).replaceAll("").strip();
        JsonNode node;
        try {
            node = MAPPER.readTree(cleaned);
        } catch (Exception e) {
            return Optional.empty();
        }
        if (!node.isObject()) {
            return Optional.empty();
        }
        Map<String, String> result = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            if (entry.getValue().isTextual()) {
                result.put(entry.getKey(), entry.getValue().asText());
            }
        });
        return Optional.of(result);
    }
}
