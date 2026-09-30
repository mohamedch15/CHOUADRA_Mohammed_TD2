package fr.univ.td2.i18n.internationalizer;

import com.github.javaparser.StaticJavaParser;
import fr.univ.td2.i18n.common.PropertiesFileStore;
import fr.univ.td2.i18n.internationalizer.model.FindingKind;
import fr.univ.td2.i18n.internationalizer.report.ScanReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternationalizerIntegrationTest {

    @Test
    void endToEndRunProducesValidCodeAndBundleThenBecomesANoOp(@TempDir Path projectRoot) throws IOException {
        writeSource(projectRoot, "fr/example/app/Main.java", """
                package fr.example.app;

                public final class Main {
                    public static void main(String[] args) {
                        System.out.println("Bienvenue !");
                        System.out.printf("Bonjour, %s !%n", args[0]);
                    }
                }
                """);
        writeSource(projectRoot, "fr/example/app/util/Greeter.java", """
                package fr.example.app.util;

                public final class Greeter {
                    public void greet() {
                        System.out.println("Bienvenue !");
                    }
                }
                """);

        // Fichier hors src/main/java (deja compile) : ne doit pas etre modifie.
        Path targetDecoy = projectRoot.resolve("target/classes/fr/example/app/Main.java");
        Files.createDirectories(targetDecoy.getParent());
        Files.writeString(targetDecoy, "class Decoy { void m() { System.out.println(\"NE PAS TOUCHER\"); } }");

        ScanReport firstRun = Internationalizer.run(projectRoot);
        assertEquals(3, firstRun.count(FindingKind.TRANSLATED));

        Path bundlePath = projectRoot.resolve("src/main/resources/messages.properties");
        Map<String, String> bundle = PropertiesFileStore.load(bundlePath);
        assertEquals(2, bundle.size(), "les deux 'Bienvenue !' identiques doivent partager une seule cle");
        assertTrue(bundle.containsValue("Bienvenue !"));
        assertTrue(bundle.containsValue("Bonjour, {0} !"));

        String decoyContent = Files.readString(targetDecoy);
        assertTrue(decoyContent.contains("NE PAS TOUCHER"), "le contenu sous target/ ne doit jamais etre modifie");

        for (Path javaFile : List.of(
                projectRoot.resolve("src/main/java/fr/example/app/Main.java"),
                projectRoot.resolve("src/main/java/fr/example/app/util/Greeter.java"))) {
            String content = Files.readString(javaFile, StandardCharsets.UTF_8);
            assertDoesNotThrow(() -> StaticJavaParser.parse(content), "le fichier transforme doit rester syntaxiquement valide");
            assertTrue(content.contains("BUNDLE.getString"));
        }

        byte[] mainBefore = Files.readAllBytes(projectRoot.resolve("src/main/java/fr/example/app/Main.java"));
        byte[] bundleBefore = Files.readAllBytes(bundlePath);

        ScanReport secondRun = Internationalizer.run(projectRoot);
        assertEquals(0, secondRun.count(FindingKind.TRANSLATED), "une seconde execution ne doit rien transformer de plus");
        assertEquals(2, secondRun.count(FindingKind.ALREADY_PROCESSED));

        assertTrue(java.util.Arrays.equals(mainBefore, Files.readAllBytes(projectRoot.resolve("src/main/java/fr/example/app/Main.java"))));
        assertTrue(java.util.Arrays.equals(bundleBefore, Files.readAllBytes(bundlePath)));
    }

    @Test
    void reportsUnsupportedConcatenationWithoutModifyingTheFile(@TempDir Path projectRoot) throws IOException {
        String original = """
                package fr.example.app;

                public final class Risky {
                    void run(String value) {
                        System.out.println("Valeur : " + value);
                    }
                }
                """;
        writeSource(projectRoot, "fr/example/app/Risky.java", original);

        ScanReport report = Internationalizer.run(projectRoot);

        assertEquals(1, report.count(FindingKind.UNSUPPORTED));
        assertEquals(0, report.count(FindingKind.TRANSLATED));
        String after = Files.readString(projectRoot.resolve("src/main/java/fr/example/app/Risky.java"), StandardCharsets.UTF_8);
        assertEquals(original, after, "un fichier sans transformation appliquee ne doit pas etre reecrit");
    }

    private static void writeSource(Path projectRoot, String relativePath, String content) throws IOException {
        Path file = projectRoot.resolve("src/main/java").resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
