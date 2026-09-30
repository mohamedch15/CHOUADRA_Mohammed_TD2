package fr.univ.td2.i18n.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture et ecriture de fichiers {@code .properties} en UTF-8, en conservant
 * l'ordre d'insertion des cles. Contrairement a {@link java.util.Properties},
 * cette implementation n'echappe pas les caracteres accentues (\\uXXXX), ce qui
 * garde les ressources lisibles et compatibles avec {@link java.util.ResourceBundle}
 * depuis Java 9 (chargement par defaut en UTF-8).
 */
public final class PropertiesFileStore {

    private PropertiesFileStore() {
    }

    public static Map<String, String> load(Path file) {
        if (!Files.isRegularFile(file)) {
            return new LinkedHashMap<>();
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            Map<String, String> result = new LinkedHashMap<>();
            StringBuilder pending = null;
            String pendingKey = null;
            for (String rawLine : lines) {
                String line = pending != null ? pending + rawLine.stripLeading() : rawLine;
                if (pending == null && (line.isBlank() || line.stripLeading().startsWith("#") || line.stripLeading().startsWith("!"))) {
                    continue;
                }
                if (line.endsWith("\\") && !line.endsWith("\\\\")) {
                    String withoutContinuation = line.substring(0, line.length() - 1);
                    if (pending == null) {
                        int sep = findSeparator(withoutContinuation);
                        pendingKey = unescape(withoutContinuation.substring(0, sep).strip());
                        pending = new StringBuilder(withoutContinuation.substring(sep + 1).stripLeading());
                    } else {
                        pending = new StringBuilder(withoutContinuation);
                    }
                    continue;
                }
                if (pending != null) {
                    result.put(pendingKey, unescape(line));
                    pending = null;
                    pendingKey = null;
                    continue;
                }
                int sep = findSeparator(line);
                if (sep < 0) {
                    continue;
                }
                String key = unescape(line.substring(0, sep).strip());
                String value = unescape(line.substring(sep + 1).stripLeading());
                result.put(key, value);
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture impossible : " + file, e);
        }
    }

    private static int findSeparator(String line) {
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '=' || c == ':') {
                return i;
            }
        }
        return -1;
    }

    public static void save(Path file, Map<String, String> entries, String headerComment) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            StringBuilder sb = new StringBuilder();
            if (headerComment != null && !headerComment.isBlank()) {
                sb.append("# ").append(headerComment).append(System.lineSeparator());
            }
            entries.forEach((key, value) -> sb.append(escapeKey(key))
                    .append('=')
                    .append(escapeValue(value))
                    .append(System.lineSeparator()));
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Ecriture impossible : " + file, e);
        }
    }

    private static String escapeKey(String key) {
        StringBuilder sb = new StringBuilder();
        for (char c : key.toCharArray()) {
            switch (c) {
                case '=' -> sb.append("\\=");
                case ':' -> sb.append("\\:");
                case ' ' -> sb.append("\\ ");
                case '\\' -> sb.append("\\\\");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String escapeValue(String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unescape(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                switch (next) {
                    case 'n' -> {
                        sb.append('\n');
                        i++;
                    }
                    case 'r' -> {
                        sb.append('\r');
                        i++;
                    }
                    case 't' -> {
                        sb.append('\t');
                        i++;
                    }
                    case '\\', '=', ':', ' ' -> {
                        sb.append(next);
                        i++;
                    }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
