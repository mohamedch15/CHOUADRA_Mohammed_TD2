package fr.univ.td2.i18n.localizer.orchestrate;

import java.nio.file.Path;
import java.util.Map;

public record LocaleTranslationResult(
        String localeCode,
        Path targetFile,
        int addedCount,
        long alreadyPresentCount,
        Map<String, String> rejected,
        String failureReason
) {
    public boolean isSuccess() {
        return failureReason == null;
    }
}
