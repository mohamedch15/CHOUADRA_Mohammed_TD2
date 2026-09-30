package fr.univ.td2.i18n.localizer.orchestrate;

import fr.univ.td2.i18n.common.PropertiesFileStore;
import fr.univ.td2.i18n.localizer.client.AlbertClient;
import fr.univ.td2.i18n.localizer.client.AlbertClientException;
import fr.univ.td2.i18n.localizer.translate.AlbertResponseParser;
import fr.univ.td2.i18n.localizer.translate.TranslationRequestBuilder;
import fr.univ.td2.i18n.localizer.translate.TranslationValidator;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestre la traduction d'un bundle de ressources vers plusieurs locales : pour
 * chaque locale, ne demande que les cles manquantes, valide la reponse, et n'ecrit
 * le fichier resultat qu'apres validation complete (aucune ecriture partielle).
 * L'echec d'une locale (reseau, JSON invalide) n'empeche pas le traitement des autres.
 */
public final class LocalizationOrchestrator {

    private final AlbertClient client;
    private final String model;

    public LocalizationOrchestrator(AlbertClient client, String model) {
        this.client = client;
        this.model = model;
    }

    public List<LocaleTranslationResult> translateAll(Path baseFile, List<String> localeCodes, boolean force) {
        Map<String, String> baseBundle = PropertiesFileStore.load(baseFile);
        return localeCodes.stream()
                .map(locale -> translateLocale(baseBundle, baseFile, locale, force))
                .toList();
    }

    public LocaleTranslationResult translateLocale(Map<String, String> baseBundle, Path baseFile, String localeCode, boolean force) {
        Path targetFile = targetFileFor(baseFile, localeCode);
        Map<String, String> existing = PropertiesFileStore.load(targetFile);
        long alreadyPresent = baseBundle.keySet().stream().filter(existing::containsKey).count();

        Map<String, String> toTranslate = new LinkedHashMap<>();
        baseBundle.forEach((key, value) -> {
            if (force || !existing.containsKey(key)) {
                toTranslate.put(key, value);
            }
        });

        if (toTranslate.isEmpty()) {
            return new LocaleTranslationResult(localeCode, targetFile, 0, alreadyPresent, Map.of(), null);
        }

        TranslationRequestBuilder.Prompt prompt = TranslationRequestBuilder.build(localeCode, toTranslate);
        String raw;
        try {
            raw = client.chatComplete(model, prompt.system(), prompt.user());
        } catch (AlbertClientException e) {
            return new LocaleTranslationResult(localeCode, targetFile, 0, alreadyPresent, Map.of(),
                    "appel a Albert echoue : " + e.getMessage());
        }

        Optional<Map<String, String>> parsed = AlbertResponseParser.parse(raw);
        if (parsed.isEmpty()) {
            return new LocaleTranslationResult(localeCode, targetFile, 0, alreadyPresent, Map.of(),
                    "reponse Albert non exploitable (JSON invalide)");
        }

        TranslationValidator.ValidationResult validated = TranslationValidator.validate(toTranslate, parsed.get());
        if (!validated.accepted().isEmpty()) {
            Map<String, String> merged = new LinkedHashMap<>(existing);
            merged.putAll(validated.accepted());
            PropertiesFileStore.save(targetFile, merged, "Traductions " + localeCode + " generees via Albert - a relire");
        }

        return new LocaleTranslationResult(localeCode, targetFile, validated.accepted().size(), alreadyPresent,
                validated.rejected(), null);
    }

    public static Path targetFileFor(Path baseFile, String localeCode) {
        String fileName = baseFile.getFileName().toString();
        String stem = fileName.endsWith(".properties") ? fileName.substring(0, fileName.length() - ".properties".length()) : fileName;
        return baseFile.resolveSibling(stem + "_" + localeCode + ".properties");
    }
}
