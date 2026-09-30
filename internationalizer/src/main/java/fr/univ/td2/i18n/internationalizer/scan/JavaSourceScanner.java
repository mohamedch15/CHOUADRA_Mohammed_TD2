package fr.univ.td2.i18n.internationalizer.scan;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** Parcours recursif d'un projet pour reperer ses sources Java pertinentes. */
public final class JavaSourceScanner {

    private static final List<String> EXCLUDED_DIRECTORIES = List.of("target", ".git", "build", "node_modules");

    private JavaSourceScanner() {
    }

    public static List<Path> findJavaSources(Path projectRoot) {
        Path sourceRoot = projectRoot.resolve("src").resolve("main").resolve("java");
        Path base = Files.isDirectory(sourceRoot) ? sourceRoot : projectRoot;
        try (Stream<Path> walk = Files.walk(base)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(JavaSourceScanner::isNotExcluded)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Parcours impossible : " + base, e);
        }
    }

    private static boolean isNotExcluded(Path path) {
        return EXCLUDED_DIRECTORIES.stream()
                .noneMatch(excluded -> {
                    for (Path part : path) {
                        if (part.toString().equals(excluded)) {
                            return true;
                        }
                    }
                    return false;
                });
    }
}
