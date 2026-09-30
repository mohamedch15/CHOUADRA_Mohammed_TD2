package fr.univ.td2.i18n.internationalizer.literal;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import fr.univ.td2.i18n.internationalizer.model.FindingKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiteralCollectorTest {

    private static final Path DUMMY_FILE = Path.of("Dummy.java");

    @Test
    void detectsSimplePrintlnLiteral() {
        CompilationUnit cu = StaticJavaParser.parse("""
                class Dummy {
                    void run() {
                        System.out.println("Bonjour !");
                    }
                }
                """);

        FileScanResult result = LiteralCollector.scan(DUMMY_FILE, cu);

        assertEquals(1, result.callSites().size());
        CallSite site = result.callSites().get(0);
        assertEquals(CallShape.SIMPLE, site.shape());
        assertEquals("Bonjour !", site.convertedMessage());
    }

    @Test
    void detectsPrintfWithPlaceholders() {
        CompilationUnit cu = StaticJavaParser.parse("""
                class Dummy {
                    void run(String name) {
                        System.out.printf("Bonjour, %s !%n", name);
                    }
                }
                """);

        FileScanResult result = LiteralCollector.scan(DUMMY_FILE, cu);

        assertEquals(1, result.callSites().size());
        CallSite site = result.callSites().get(0);
        assertEquals(CallShape.PRINTF, site.shape());
        assertEquals("Bonjour, {0} !", site.convertedMessage());
        assertTrue(site.trailingNewline());
    }

    @Test
    void ignoresTextInsideComments() {
        CompilationUnit cu = StaticJavaParser.parse("""
                class Dummy {
                    // System.out.println("Ceci est un commentaire, pas un appel reel");
                    void run() {
                        // rien ici
                    }
                }
                """);

        FileScanResult result = LiteralCollector.scan(DUMMY_FILE, cu);

        assertTrue(result.callSites().isEmpty());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void flagsStringConcatenationAsUnsupported() {
        CompilationUnit cu = StaticJavaParser.parse("""
                class Dummy {
                    void run(String result) {
                        System.out.println("Resultat : " + result);
                    }
                }
                """);

        FileScanResult result = LiteralCollector.scan(DUMMY_FILE, cu);

        assertTrue(result.callSites().isEmpty());
        assertEquals(1, result.findings().size());
        assertEquals(FindingKind.UNSUPPORTED, result.findings().get(0).kind());
    }

    @Test
    void doesNotFlagNonDisplayFieldConstants() {
        CompilationUnit cu = StaticJavaParser.parse("""
                class Dummy {
                    private static final String CONFIG_KEY = "app.mode";
                    void run() {
                    }
                }
                """);

        FileScanResult result = LiteralCollector.scan(DUMMY_FILE, cu);

        assertTrue(result.callSites().isEmpty());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void alreadyInternationalizedFileIsSkipped() {
        CompilationUnit cu = StaticJavaParser.parse("""
                import java.util.ResourceBundle;
                class Dummy {
                    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");
                    void run() {
                        System.out.println(BUNDLE.getString("msg.hello"));
                    }
                }
                """);

        FileScanResult result = LiteralCollector.scan(DUMMY_FILE, cu);

        assertTrue(result.alreadyProcessed());
        assertTrue(result.callSites().isEmpty());
        assertEquals(FindingKind.ALREADY_PROCESSED, result.findings().get(0).kind());
    }

    @Test
    void detectsJOptionPaneDialog() {
        CompilationUnit cu = StaticJavaParser.parse("""
                import javax.swing.JOptionPane;
                class Dummy {
                    void run() {
                        JOptionPane.showMessageDialog(null, "Une erreur est survenue.");
                    }
                }
                """);

        FileScanResult result = LiteralCollector.scan(DUMMY_FILE, cu);

        assertEquals(1, result.callSites().size());
        assertEquals(CallShape.DIALOG, result.callSites().get(0).shape());
    }
}
