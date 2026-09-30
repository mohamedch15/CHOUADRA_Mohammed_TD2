package fr.univ.td2.i18n.localizer.translate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Map;

/** Construit les messages systeme/utilisateur envoyes au modele pour une locale cible. */
public final class TranslationRequestBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TranslationRequestBuilder() {
    }

    public record Prompt(String system, String user) {
    }

    public static Prompt build(String localeCode, Map<String, String> keysToTranslate) {
        String languageName = Locale.forLanguageTag(localeCode).getDisplayLanguage(Locale.FRENCH);
        String system = """
                Tu es un traducteur technique. Tu recois un objet JSON dont les cles sont des \
                identifiants de ressource et les valeurs des messages en francais destines a une \
                interface utilisateur. Traduis uniquement les VALEURS vers la langue cible ; ne \
                traduis jamais les cles. Conserve exactement les marqueurs de la forme {0}, {1}, etc. \
                (ne les traduis pas, ne les supprime pas, ne change pas leur nombre). Reponds \
                uniquement avec un objet JSON valide, sans commentaire, sans bloc de code markdown, \
                associant chaque cle recue a sa traduction.""";

        String user;
        try {
            user = "Langue cible : " + languageName + " (" + localeCode + ").\n"
                    + "Messages a traduire :\n" + MAPPER.writeValueAsString(keysToTranslate);
        } catch (Exception e) {
            throw new IllegalStateException("Construction du prompt impossible", e);
        }
        return new Prompt(system, user);
    }
}
