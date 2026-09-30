package fr.univ.td2.i18n.internationalizer.key;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import fr.univ.td2.i18n.internationalizer.literal.FileScanResult;
import fr.univ.td2.i18n.internationalizer.literal.LiteralCollector;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleKeyAssignerTest {

    @Test
    void identicalMessagesInDifferentFilesShareTheSameKey() {
        CompilationUnit cuA = StaticJavaParser.parse("""
                class A {
                    void run() { System.out.println("Bienvenue !"); }
                }
                """);
        CompilationUnit cuB = StaticJavaParser.parse("""
                class B {
                    void run() { System.out.println("Bienvenue !"); }
                }
                """);
        FileScanResult resultA = LiteralCollector.scan(Path.of("A.java"), cuA);
        FileScanResult resultB = LiteralCollector.scan(Path.of("B.java"), cuB);

        BundleKeyAssigner.Assignment assignment = BundleKeyAssigner.assign(List.of(resultA, resultB), Map.of());

        String keyA = assignment.keysByCallSite().get(resultA.callSites().get(0));
        String keyB = assignment.keysByCallSite().get(resultB.callSites().get(0));
        assertEquals(keyA, keyB);
        assertEquals(1, assignment.mergedBundle().size());
    }

    @Test
    void reusesExistingKeyForSameContent() {
        CompilationUnit cu = StaticJavaParser.parse("""
                class A {
                    void run() { System.out.println("Bonjour !"); }
                }
                """);
        FileScanResult result = LiteralCollector.scan(Path.of("A.java"), cu);

        BundleKeyAssigner.Assignment assignment = BundleKeyAssigner.assign(
                List.of(result), Map.of("app.existing", "Bonjour !"));

        String key = assignment.keysByCallSite().get(result.callSites().get(0));
        assertEquals("app.existing", key);
        assertEquals(1, assignment.mergedBundle().size());
    }

    @Test
    void distinctMessagesGetDistinctKeys() {
        CompilationUnit cu = StaticJavaParser.parse("""
                class A {
                    void run() {
                        System.out.println("Bonjour !");
                        System.out.println("Au revoir !");
                    }
                }
                """);
        FileScanResult result = LiteralCollector.scan(Path.of("A.java"), cu);

        BundleKeyAssigner.Assignment assignment = BundleKeyAssigner.assign(List.of(result), Map.of());

        assertEquals(2, assignment.mergedBundle().size());
        assertTrue(assignment.mergedBundle().containsValue("Bonjour !"));
        assertTrue(assignment.mergedBundle().containsValue("Au revoir !"));
    }
}
