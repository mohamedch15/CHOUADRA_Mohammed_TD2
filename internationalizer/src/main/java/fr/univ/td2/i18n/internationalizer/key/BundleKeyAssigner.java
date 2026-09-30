package fr.univ.td2.i18n.internationalizer.key;

import fr.univ.td2.i18n.internationalizer.literal.CallSite;
import fr.univ.td2.i18n.internationalizer.literal.FileScanResult;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Attribue une cle de ressource a chaque appel detecte, en reutilisant la cle
 * existante pour un contenu deja present dans le bundle (deduplication des
 * chaines repetees) et en evitant toute collision de cle. L'assignation est
 * intrinsequement sequentielle et etat-dependante : elle reste imperative,
 * contrairement a la collecte des appels qui, elle, est un pipeline de Streams.
 */
public final class BundleKeyAssigner {

    private BundleKeyAssigner() {
    }

    public record Assignment(Map<CallSite, String> keysByCallSite, Map<String, String> mergedBundle) {
    }

    public static Assignment assign(List<FileScanResult> scanResults, Map<String, String> existingBundle) {
        Map<String, String> bundle = new LinkedHashMap<>(existingBundle);
        Map<String, String> keyByContent = new LinkedHashMap<>();
        existingBundle.forEach((key, value) -> keyByContent.putIfAbsent(value, key));

        Set<String> usedKeys = new HashSet<>(bundle.keySet());
        IdentityHashMap<CallSite, String> keysByCallSite = new IdentityHashMap<>();

        List<CallSite> allSites = scanResults.stream()
                .flatMap(result -> result.callSites().stream())
                .toList();

        for (CallSite site : allSites) {
            String content = site.convertedMessage();
            String key = keyByContent.get(content);
            if (key == null) {
                key = uniqueKey(KeyGenerator.slugify(content), usedKeys);
                usedKeys.add(key);
                keyByContent.put(content, key);
                bundle.put(key, content);
            }
            keysByCallSite.put(site, key);
        }

        return new Assignment(keysByCallSite, bundle);
    }

    private static String uniqueKey(String slug, Set<String> used) {
        String candidate = "msg." + slug;
        if (!used.contains(candidate)) {
            return candidate;
        }
        int suffix = 2;
        while (used.contains(candidate + "_" + suffix)) {
            suffix++;
        }
        return candidate + "_" + suffix;
    }
}
